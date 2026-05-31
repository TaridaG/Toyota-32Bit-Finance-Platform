package com.company.finance_api.infocards.application;

import com.company.finance_api.domain.InfoCardEntity;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardDto;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardInputDto;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardLocaleContentDto;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardsDashboardDto;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardsPageDto;
import com.company.finance_api.infocards.infrastructure.http.dto.LiteracyCatalogPageDto;
import com.company.finance_api.infocards.infrastructure.http.dto.LiteracyCatalogStatsDto;
import com.company.finance_api.repository.InfoCardRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Info-card CRUD, portal listeleme ve financial literacy catalog iş mantığını yürütür. */
@Service
public class InfoCardService {

  private static final String ACTIVE = "ACTIVE";
  private static final String PASSIVE = "PASSIVE";
  private static final String FINANCIAL_LITERACY = "FINANCIAL_LITERACY";
  private static final int LITERACY_CATALOG_DEFAULT_SIZE = 30;
  private static final int LITERACY_CATALOG_MAX_SIZE = 50;

  private final InfoCardRepository repository;
  private final ObjectMapper objectMapper;

  public InfoCardService(InfoCardRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  /** Portal için aktif info-card'ları locale ile listeler. */
  @Transactional(readOnly = true)
  public List<InfoCardDto> listPortalCards(
      String pageKey, boolean includeAdminOnly, String locale) {
    String resolvedLocale = InfoCardLocaleResolver.normalizeLocale(locale);
    return repository.findByStatusOrderByUpdatedAtDesc(ACTIVE).stream()
        .filter(card -> pageKey == null || pageKey.isBlank() || card.getPages().contains(pageKey))
        .filter(card -> includeAdminOnly || !card.isAdminOnly())
        .map(card -> InfoCardMapper.toPortalDto(card, resolvedLocale))
        .toList();
  }

  /** Financial literacy catalog sayfasını filtrelerle döner. */
  @Transactional(readOnly = true)
  public LiteracyCatalogPageDto listLiteracyCatalog(
      int page,
      int size,
      boolean includeAdminOnly,
      String locale,
      String query,
      String category,
      List<String> difficulties,
      List<String> contentTypes,
      List<String> portalPages) {
    String resolvedLocale = InfoCardLocaleResolver.normalizeLocale(locale);
    int safePage = Math.max(0, page);
    int safeSize =
        Math.min(
            LITERACY_CATALOG_MAX_SIZE,
            Math.max(1, size > 0 ? size : LITERACY_CATALOG_DEFAULT_SIZE));

    List<InfoCardEntity> filtered =
        repository.findByStatusOrderByUpdatedAtDesc(ACTIVE).stream()
            .filter(card -> card.getPages().contains(FINANCIAL_LITERACY))
            .filter(card -> includeAdminOnly || !card.isAdminOnly())
            .filter(card -> matchesLiteracyCategory(card, category))
            .filter(card -> matchesLiteracyList(card.getDifficulty(), difficulties))
            .filter(card -> matchesLiteracyList(card.getCardType(), contentTypes))
            .filter(card -> matchesLiteracyPortalPages(card, portalPages))
            .filter(card -> matchesLiteracyQuery(card, query, resolvedLocale))
            .sorted(
                Comparator.comparing(
                    card ->
                        InfoCardLocaleResolver.resolve(card, resolvedLocale)
                            .title()
                            .toLowerCase(Locale.ROOT)))
            .toList();

    LiteracyCatalogStatsDto stats = buildLiteracyStats(filtered);
    int totalElements = filtered.size();
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
    int from = Math.min(safePage * safeSize, totalElements);
    int to = Math.min(from + safeSize, totalElements);
    List<InfoCardDto> content =
        filtered.subList(from, to).stream()
            .map(card -> InfoCardMapper.toPortalDto(card, resolvedLocale))
            .toList();
    return new LiteracyCatalogPageDto(
        content, safePage, safeSize, totalElements, totalPages, stats);
  }

  /** Slug ile tek info-card arar. */
  @Transactional(readOnly = true)
  public Optional<InfoCardDto> findBySlug(String slug, String locale) {
    return repository.findBySlug(slug).map(card -> InfoCardMapper.toPortalDto(card, locale));
  }

  /** Kimlik ile tek info-card arar. */
  @Transactional(readOnly = true)
  public Optional<InfoCardDto> findById(UUID id) {
    return repository.findById(id).map(InfoCardMapper::toDto);
  }

  /** Sayfa, terim veya element hedefine göre yardım kartı arar. */
  @Transactional(readOnly = true)
  public Optional<InfoCardDto> lookupHelpTarget(
      String pageKey, String term, String elementId, String instrumentSymbol, String locale) {
    if (pageKey == null || pageKey.isBlank()) {
      return Optional.empty();
    }
    String normalizedTerm = term != null ? term.trim().toLowerCase(Locale.ROOT) : "";
    String normalizedInstrument =
        instrumentSymbol != null ? instrumentSymbol.trim().toUpperCase(Locale.ROOT) : "";
    return repository.findByStatusOrderByUpdatedAtDesc(ACTIVE).stream()
        .filter(card -> card.getPages().contains(pageKey))
        .filter(
            card ->
                matchesElement(card, elementId)
                    || matchesTerm(card, normalizedTerm)
                    || matchesInstrument(card, normalizedInstrument))
        .findFirst()
        .map(card -> InfoCardMapper.toPortalDto(card, locale));
  }

  /** Admin panel için sayfalı info-card listesi döner. */
  @Transactional(readOnly = true)
  public InfoCardsPageDto listAdmin(
      int page, int size, String portalPage, String query, String statusFilter) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(100, Math.max(1, size));
    List<InfoCardEntity> filtered =
        repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
            .filter(card -> matchesListStatus(card, statusFilter))
            .filter(
                card ->
                    portalPage == null
                        || portalPage.isBlank()
                        || card.getPages().contains(portalPage))
            .filter(card -> matchesQuery(card, query))
            .toList();
    int totalElements = filtered.size();
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
    int from = Math.min(safePage * safeSize, totalElements);
    int to = Math.min(from + safeSize, totalElements);
    List<InfoCardDto> content =
        filtered.subList(from, to).stream().map(InfoCardMapper::toDto).toList();
    return new InfoCardsPageDto(content, safePage, safeSize, totalElements, totalPages);
  }

  /** Admin info-card dashboard özet metriklerini döner. */
  @Transactional(readOnly = true)
  public InfoCardsDashboardDto dashboard() {
    List<InfoCardEntity> all = repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    long active = all.stream().filter(c -> ACTIVE.equals(c.getStatus())).count();
    long passive = all.size() - active;
    double avgWords =
        all.stream().mapToInt(c -> wordCount(c.getShortDescription())).average().orElse(0);
    Set<String> pages = new HashSet<>();
    Map<String, Long> pageCounts = new java.util.HashMap<>();
    long beginner = 0;
    long intermediate = 0;
    long advanced = 0;
    for (InfoCardEntity card : all) {
      for (String page : card.getPages()) {
        pages.add(page);
        pageCounts.merge(page, 1L, Long::sum);
      }
      switch (card.getDifficulty()) {
        case "BEGINNER" -> beginner++;
        case "INTERMEDIATE" -> intermediate++;
        case "ADVANCED" -> advanced++;
        default -> {}
      }
    }
    String mostPage =
        pageCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    String lastTitle = all.stream().findFirst().map(InfoCardEntity::getTitle).orElse(null);
    return new InfoCardsDashboardDto(
        active,
        passive,
        Math.round(avgWords * 10.0) / 10.0,
        pages.size(),
        mostPage,
        beginner,
        intermediate,
        advanced,
        lastTitle);
  }

  /** Yeni info-card oluşturur. */
  @Transactional
  public InfoCardDto create(InfoCardInputDto input) {
    validateInput(input);
    InfoCardEntity entity = InfoCardMapper.newEntity(input);
    entity.setSlug(resolveUniqueSlug(entity.getSlug(), null));
    return InfoCardMapper.toDto(repository.save(entity));
  }

  /** Mevcut info-card'ı günceller. */
  @Transactional
  public InfoCardDto update(UUID id, InfoCardInputDto input) {
    validateInput(input);
    InfoCardEntity entity =
        repository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info card not found"));
    InfoCardMapper.applyInput(entity, input);
    entity.setSlug(resolveUniqueSlug(InfoCardMapper.resolveSlug(input), id));
    return InfoCardMapper.toDto(repository.save(entity));
  }

  /** Info-card ACTIVE/PASSIVE durumunu değiştirir. */
  @Transactional
  public InfoCardDto toggleStatus(UUID id) {
    InfoCardEntity entity =
        repository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Info card not found"));
    entity.setStatus(ACTIVE.equals(entity.getStatus()) ? PASSIVE : ACTIVE);
    return InfoCardMapper.toDto(repository.save(entity));
  }

  /** Info-card'ı kalıcı siler. */
  @Transactional
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Info card not found");
    }
    repository.deleteById(id);
  }

  /** Varsayılan info-card seed kayıtlarını oluşturur. */
  @Transactional
  public int seedDefaults() {
    if (repository.count() > 0) {
      return 0;
    }
    List<InfoCardInputDto> seeds = loadDefaultSeeds();
    int created = 0;
    for (InfoCardInputDto seed : seeds) {
      repository.save(InfoCardMapper.newEntity(seed));
      created++;
    }
    return created;
  }

  private boolean matchesQuery(InfoCardEntity card, String query) {
    return matchesLiteracyQuery(card, query, "tr");
  }

  private boolean matchesLiteracyQuery(InfoCardEntity card, String query, String locale) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String q = query.trim().toLowerCase(Locale.ROOT);
    InfoCardLocaleContentDto content = InfoCardLocaleResolver.resolve(card, locale);
    if (content.title().toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    if (content.shortDescription().toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    String detailed = content.detailedDescription();
    if (detailed != null && detailed.toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    String howTo = content.howToInterpret();
    if (howTo != null && howTo.toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    String mistake = content.commonMistake();
    if (mistake != null && mistake.toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    String example = content.example();
    if (example != null && example.toLowerCase(Locale.ROOT).contains(q)) {
      return true;
    }
    List<String> related = content.relatedTerms() != null ? content.relatedTerms() : List.of();
    return card.getTargetTerms().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(q))
        || related.stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(q));
  }

  private boolean matchesLiteracyCategory(InfoCardEntity card, String category) {
    if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
      return true;
    }
    return category.equalsIgnoreCase(card.getCategory());
  }

  private boolean matchesLiteracyList(String value, List<String> allowed) {
    if (allowed == null || allowed.isEmpty()) {
      return true;
    }
    return allowed.stream().anyMatch(item -> item != null && item.equalsIgnoreCase(value));
  }

  private boolean matchesLiteracyPortalPages(InfoCardEntity card, List<String> portalPages) {
    if (portalPages == null || portalPages.isEmpty()) {
      return true;
    }
    return portalPages.stream()
        .filter(page -> page != null && !page.isBlank())
        .anyMatch(page -> card.getPages().contains(page.trim()));
  }

  private LiteracyCatalogStatsDto buildLiteracyStats(List<InfoCardEntity> cards) {
    long terms = cards.stream().filter(c -> "TERM".equals(c.getCardType())).count();
    long charts = cards.stream().filter(c -> "CHART".equals(c.getCardType())).count();
    long analysisTools =
        cards.stream().filter(c -> "ANALYSIS_TOOL".equals(c.getCardType())).count();
    long macro = cards.stream().filter(c -> "MACRO_INDICATOR".equals(c.getCardType())).count();
    return new LiteracyCatalogStatsDto(cards.size(), terms, charts, analysisTools, macro);
  }

  private boolean matchesElement(InfoCardEntity card, String elementId) {
    return elementId != null
        && !elementId.isBlank()
        && card.getTargetElementIds().contains(elementId.trim());
  }

  private boolean matchesTerm(InfoCardEntity card, String normalizedTerm) {
    if (normalizedTerm.isEmpty()) {
      return false;
    }
    return card.getTargetTerms().stream()
        .anyMatch(term -> term.trim().toLowerCase(Locale.ROOT).equals(normalizedTerm));
  }

  private boolean matchesListStatus(InfoCardEntity card, String statusFilter) {
    if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
      return true;
    }
    if ("PASSIVE".equalsIgnoreCase(statusFilter)) {
      return PASSIVE.equals(card.getStatus());
    }
    return ACTIVE.equals(card.getStatus());
  }

  private boolean matchesInstrument(InfoCardEntity card, String normalizedSymbol) {
    if (normalizedSymbol.isEmpty()) {
      return false;
    }
    return card.getTargetInstrumentSymbols().stream()
        .anyMatch(symbol -> symbol.trim().toUpperCase(Locale.ROOT).equals(normalizedSymbol));
  }

  private void validateInput(InfoCardInputDto input) {
    if (input.pages() == null || input.pages().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one page is required");
    }
    boolean hasTerms = input.targetTerms() != null && !input.targetTerms().isEmpty();
    boolean hasElements = input.targetElementIds() != null && !input.targetElementIds().isEmpty();
    boolean hasInstruments =
        input.targetInstrumentSymbols() != null && !input.targetInstrumentSymbols().isEmpty();
    if (!hasTerms && !hasElements && !hasInstruments) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "At least one target term, page element, or instrument symbol is required");
    }
    if (hasCompleteTranslations(input)) {
      return;
    }
    if (input.title() == null || input.title().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
    }
    if (input.shortDescription() == null || input.shortDescription().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Short description is required");
    }
  }

  private boolean hasCompleteTranslations(InfoCardInputDto input) {
    if (input.translations() == null || input.translations().isEmpty()) {
      return false;
    }
    for (String locale : InfoCardLocaleResolver.SUPPORTED_LOCALES) {
      InfoCardLocaleContentDto content = input.translations().get(locale);
      if (content == null
          || content.title() == null
          || content.title().isBlank()
          || content.shortDescription() == null
          || content.shortDescription().isBlank()) {
        return false;
      }
    }
    return true;
  }

  private String resolveUniqueSlug(String baseSlug, UUID excludeId) {
    String normalized = InfoCardMapper.slugify(baseSlug);
    if (normalized.isBlank()) {
      normalized = "card";
    }
    String candidate = normalized;
    int suffix = 2;
    while (true) {
      Optional<InfoCardEntity> existing = repository.findBySlug(candidate);
      if (existing.isEmpty() || (excludeId != null && existing.get().getId().equals(excludeId))) {
        return candidate;
      }
      candidate = normalized + "-" + suffix;
      suffix++;
    }
  }

  private int wordCount(String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return text.trim().split("\\s+").length;
  }

  private List<InfoCardInputDto> loadDefaultSeeds() {
    try (InputStream in = new ClassPathResource("seed/info-cards-default.json").getInputStream()) {
      List<Map<String, Object>> raw = objectMapper.readValue(in, new TypeReference<>() {});
      return raw.stream().map(this::mapSeed).collect(Collectors.toCollection(ArrayList::new));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load default info cards seed", e);
    }
  }

  @SuppressWarnings("unchecked")
  private InfoCardInputDto mapSeed(Map<String, Object> row) {
    return new InfoCardInputDto(
        stringVal(row.get("id")),
        stringVal(row.get("slug")),
        stringVal(row.get("title")),
        stringList(row.get("targetTerms")),
        stringList(row.get("targetElementIds")),
        stringList(row.get("targetInstrumentSymbols")),
        stringList(row.get("pages")),
        stringVal(row.get("category")),
        stringVal(row.get("type")),
        stringVal(row.get("difficulty")),
        stringVal(row.get("status")),
        stringVal(row.get("shortDescription")),
        stringVal(row.get("detailedDescription")),
        stringVal(row.get("howToInterpret")),
        stringVal(row.get("commonMistake")),
        stringVal(row.get("example")),
        stringList(row.get("relatedTerms")),
        Boolean.TRUE.equals(row.get("adminOnly")),
        null);
  }

  private String stringVal(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  @SuppressWarnings("unchecked")
  private List<String> stringList(Object value) {
    if (value instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of();
  }
}
