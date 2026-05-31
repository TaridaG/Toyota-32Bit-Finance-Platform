package com.company.finance_api.chart.application;

import com.company.finance_api.chart.domain.ChartDrawingSave;
import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveDetailDto;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.chart.infrastructure.http.dto.ChartDrawingSaveSummaryDto;
import com.company.finance_api.chart.infrastructure.http.dto.CreateChartDrawingSaveRequest;
import com.company.finance_api.chart.infrastructure.http.dto.DrawingMarkerDto;
import com.company.finance_api.chart.infrastructure.persistence.ChartDrawingSaveRepository;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** ChartDrawingSaveServiceImpl iş mantığını uygular (chart drawing save service). */
@Service
@Transactional
public class ChartDrawingSaveServiceImpl implements ChartDrawingSaveService {

  private static final String DEFAULT_DRAW_COLOR = "#f59e0b";

  private final ChartDrawingSaveRepository chartDrawingSaveRepository;
  private final InstrumentRepository instrumentRepository;
  private final CurrentUserResolver currentUserResolver;
  private final ObjectMapper objectMapper;

  public ChartDrawingSaveServiceImpl(
      ChartDrawingSaveRepository chartDrawingSaveRepository,
      InstrumentRepository instrumentRepository,
      CurrentUserResolver currentUserResolver,
      ObjectMapper objectMapper) {
    this.chartDrawingSaveRepository = chartDrawingSaveRepository;
    this.instrumentRepository = instrumentRepository;
    this.currentUserResolver = currentUserResolver;
    this.objectMapper = objectMapper;
  }

  /** Yeni kaydı oluşturur. */
  @Override
  public ChartDrawingSaveDetailDto create(CreateChartDrawingSaveRequest request) {
    UUID userId = currentUserResolver.getCurrentUserId();
    validateDrawings(request.getDrawings());

    String assetKey = normalizeAssetKey(request.getAssetKey());
    String assetSymbol = request.getAssetSymbol().trim().toUpperCase(Locale.ROOT);
    Instrument instrument = instrumentRepository.findBySymbol(assetSymbol).orElse(null);

    String drawingsJson = writeJson(request.getDrawings());
    ChartDrawingSave saved =
        chartDrawingSaveRepository.save(
            ChartDrawingSave.create(
                userId,
                instrument,
                assetKey,
                assetSymbol,
                normalizeAssetType(request.getAssetType()),
                request.getName(),
                drawingsJson));
    return toDetail(saved);
  }

  @Override
  @Transactional(readOnly = true)
  /** listForAsset işlemini gerçekleştirir. */
  public List<ChartDrawingSaveSummaryDto> listForAsset(String assetKey) {
    UUID userId = currentUserResolver.getCurrentUserId();
    String normalizedKey = normalizeAssetKey(assetKey);
    return chartDrawingSaveRepository
        .findByUserIdAndAssetKeyOrderByCreatedAtDesc(userId, normalizedKey)
        .stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  /** listPage işlemini gerçekleştirir. */
  public ChartDrawingSavePageResponse listPage(int page, int size) {
    UUID userId = currentUserResolver.getCurrentUserId();
    int resolvedPage = Math.max(page, 0);
    int resolvedSize = Math.min(Math.max(size, 1), 50);
    Page<ChartDrawingSave> result =
        chartDrawingSaveRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(resolvedPage, resolvedSize));
    List<ChartDrawingSaveSummaryDto> content =
        result.getContent().stream().map(this::toSummary).toList();
    return new ChartDrawingSavePageResponse(
        content,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  /** ById sorgusunu döner. */
  public ChartDrawingSaveDetailDto getById(Long id) {
    UUID userId = currentUserResolver.getCurrentUserId();
    ChartDrawingSave row =
        chartDrawingSaveRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Chart drawing save not found: " + id));
    return toDetail(row);
  }

  /** delete işlemini uygular. */
  @Override
  public void delete(Long id) {
    UUID userId = currentUserResolver.getCurrentUserId();
    ChartDrawingSave row =
        chartDrawingSaveRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Chart drawing save not found: " + id));
    chartDrawingSaveRepository.delete(row);
  }

  private ChartDrawingSaveSummaryDto toSummary(ChartDrawingSave row) {
    DrawingMeta meta = parseDrawingMeta(readJson(row.getDrawingsJson()));
    return new ChartDrawingSaveSummaryDto(
        row.getId(),
        row.getName(),
        row.getAssetKey(),
        row.getAssetSymbol(),
        row.getAssetType(),
        row.getCreatedAt(),
        meta.drawingCount(),
        meta.drawingTypes(),
        meta.drawingMarkers(),
        meta.minAnchorTime(),
        meta.maxAnchorTime(),
        meta.minPrice(),
        meta.maxPrice());
  }

  private ChartDrawingSaveDetailDto toDetail(ChartDrawingSave row) {
    JsonNode drawings = readJson(row.getDrawingsJson());
    DrawingMeta meta = parseDrawingMeta(drawings);
    return new ChartDrawingSaveDetailDto(
        row.getId(),
        row.getName(),
        row.getAssetKey(),
        row.getAssetSymbol(),
        row.getAssetType(),
        row.getCreatedAt(),
        meta.drawingCount(),
        meta.drawingTypes(),
        meta.drawingMarkers(),
        meta.minAnchorTime(),
        meta.maxAnchorTime(),
        meta.minPrice(),
        meta.maxPrice(),
        drawings);
  }

  private void validateDrawings(JsonNode drawings) {
    if (drawings == null || !drawings.isArray() || drawings.isEmpty()) {
      throw new IllegalArgumentException("At least one drawing is required");
    }
  }

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Stored drawing payload is invalid", ex);
    }
  }

  private String writeJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node);
    } catch (Exception ex) {
      throw new IllegalStateException("Drawing payload serialization failed", ex);
    }
  }

  private static String normalizeAssetKey(String assetKey) {
    if (!StringUtils.hasText(assetKey)) {
      throw new IllegalArgumentException("assetKey is required");
    }
    return assetKey.trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeAssetType(String assetType) {
    if (!StringUtils.hasText(assetType)) {
      return null;
    }
    return assetType.trim().toLowerCase(Locale.ROOT);
  }

  private DrawingMeta parseDrawingMeta(JsonNode drawings) {
    Set<String> types = new LinkedHashSet<>();
    List<DrawingMarkerDto> markers = new ArrayList<>();
    Long minTime = null;
    Long maxTime = null;
    Double minPrice = null;
    Double maxPrice = null;

    if (drawings != null && drawings.isArray()) {
      for (JsonNode item : drawings) {
        String type = item.path("type").asText(null);
        if (StringUtils.hasText(type)) {
          types.add(type);
          String color = item.path("color").asText(null);
          markers.add(
              new DrawingMarkerDto(
                  type, StringUtils.hasText(color) ? color.trim() : DEFAULT_DRAW_COLOR));
        }
        minTime = mergeMinTime(minTime, item.path("time"));
        maxTime = mergeMaxTime(maxTime, item.path("time"));
        minPrice = mergeMinPrice(minPrice, item.path("price"));
        maxPrice = mergeMaxPrice(maxPrice, item.path("price"));
        minTime = mergeAnchorTime(minTime, item.path("anchor"), true);
        maxTime = mergeAnchorTime(maxTime, item.path("anchor"), false);
        minPrice = mergeAnchorPrice(minPrice, item.path("anchor"), true);
        maxPrice = mergeAnchorPrice(maxPrice, item.path("anchor"), false);
        minTime = mergeAnchorTime(minTime, item.path("a"), true);
        maxTime = mergeAnchorTime(maxTime, item.path("a"), false);
        minPrice = mergeAnchorPrice(minPrice, item.path("a"), true);
        maxPrice = mergeAnchorPrice(maxPrice, item.path("a"), false);
        minTime = mergeAnchorTime(minTime, item.path("b"), true);
        maxTime = mergeAnchorTime(maxTime, item.path("b"), false);
        minPrice = mergeAnchorPrice(minPrice, item.path("b"), true);
        maxPrice = mergeAnchorPrice(maxPrice, item.path("b"), false);
      }
    }

    int drawingCount = drawings != null && drawings.isArray() ? drawings.size() : 0;
    return new DrawingMeta(
        drawingCount,
        List.copyOf(types),
        List.copyOf(markers),
        minTime,
        maxTime,
        minPrice,
        maxPrice);
  }

  private static String normalizeDrawColor(String color) {
    if (!StringUtils.hasText(color)) {
      return DEFAULT_DRAW_COLOR;
    }
    String trimmed = color.trim();
    if (trimmed.matches("#[0-9a-fA-F]{6}")) {
      return trimmed.toLowerCase(Locale.ROOT);
    }
    return DEFAULT_DRAW_COLOR;
  }

  private static Long mergeMinTime(Long current, JsonNode node) {
    if (node == null || !node.isNumber()) {
      return current;
    }
    long value = node.asLong();
    return current == null ? value : Math.min(current, value);
  }

  private static Long mergeMaxTime(Long current, JsonNode node) {
    if (node == null || !node.isNumber()) {
      return current;
    }
    long value = node.asLong();
    return current == null ? value : Math.max(current, value);
  }

  private static Double mergeMinPrice(Double current, JsonNode node) {
    if (node == null || !node.isNumber()) {
      return current;
    }
    double value = node.asDouble();
    return current == null ? value : Math.min(current, value);
  }

  private static Double mergeMaxPrice(Double current, JsonNode node) {
    if (node == null || !node.isNumber()) {
      return current;
    }
    double value = node.asDouble();
    return current == null ? value : Math.max(current, value);
  }

  private static Long mergeAnchorTime(Long current, JsonNode anchor, boolean min) {
    if (anchor == null || !anchor.has("time") || !anchor.get("time").isNumber()) {
      return current;
    }
    long value = anchor.get("time").asLong();
    if (current == null) {
      return value;
    }
    return min ? Math.min(current, value) : Math.max(current, value);
  }

  private static Double mergeAnchorPrice(Double current, JsonNode anchor, boolean min) {
    if (anchor == null || !anchor.has("price") || !anchor.get("price").isNumber()) {
      return current;
    }
    double value = anchor.get("price").asDouble();
    if (current == null) {
      return value;
    }
    return min ? Math.min(current, value) : Math.max(current, value);
  }

  private record DrawingMeta(
      int drawingCount,
      List<String> drawingTypes,
      List<DrawingMarkerDto> drawingMarkers,
      Long minAnchorTime,
      Long maxAnchorTime,
      Double minPrice,
      Double maxPrice) {}
}
