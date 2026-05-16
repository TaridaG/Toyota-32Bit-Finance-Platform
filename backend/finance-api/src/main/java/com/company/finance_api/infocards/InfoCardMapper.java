package com.company.finance_api.infocards;

import com.company.finance_api.domain.InfoCardEntity;
import com.company.finance_api.infocards.dto.InfoCardDto;
import com.company.finance_api.infocards.dto.InfoCardInputDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class InfoCardMapper {

    private InfoCardMapper() {
    }

    public static InfoCardDto toDto(InfoCardEntity entity) {
        return new InfoCardDto(
                entity.getId().toString(),
                entity.getTitle(),
                entity.getSlug(),
                List.copyOf(entity.getTargetTerms()),
                List.copyOf(entity.getTargetElementIds()),
                List.copyOf(entity.getTargetInstrumentSymbols()),
                List.copyOf(entity.getPages()),
                entity.getCategory(),
                entity.getCardType(),
                entity.getDifficulty(),
                entity.getStatus(),
                entity.getShortDescription(),
                entity.getDetailedDescription(),
                entity.getHowToInterpret(),
                entity.getCommonMistake(),
                entity.getExampleText(),
                List.copyOf(entity.getRelatedTerms()),
                entity.isAdminOnly(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static void applyInput(InfoCardEntity entity, InfoCardInputDto input) {
        entity.setTitle(input.title().trim());
        entity.setSlug(resolveSlug(input));
        entity.setTargetTerms(normalizeList(input.targetTerms()));
        entity.setTargetElementIds(normalizeList(input.targetElementIds()));
        entity.setTargetInstrumentSymbols(normalizeInstrumentSymbols(input.targetInstrumentSymbols()));
        entity.setPages(normalizeList(input.pages()));
        entity.setCategory(input.category());
        entity.setCardType(input.type());
        entity.setDifficulty(input.difficulty());
        entity.setStatus(input.status());
        entity.setShortDescription(input.shortDescription().trim());
        entity.setDetailedDescription(blankToEmpty(input.detailedDescription()));
        entity.setHowToInterpret(blankToNull(input.howToInterpret()));
        entity.setCommonMistake(blankToNull(input.commonMistake()));
        entity.setExampleText(blankToNull(input.example()));
        entity.setRelatedTerms(normalizeList(input.relatedTerms()));
        entity.setAdminOnly(Boolean.TRUE.equals(input.adminOnly()));
    }

    public static InfoCardEntity newEntity(InfoCardInputDto input) {
        InfoCardEntity entity = InfoCardEntity.createEmpty();
        if (input.id() != null && !input.id().isBlank() && isUuid(input.id())) {
            entity.setId(UUID.fromString(input.id()));
        }
        applyInput(entity, input);
        return entity;
    }

    public static String resolveSlug(InfoCardInputDto input) {
        if (input.slug() != null && !input.slug().isBlank()) {
            return slugify(input.slug());
        }
        return slugify(input.title());
    }

    public static String slugify(String title) {
        String normalized = title.toLowerCase(Locale.ROOT)
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ç', 'c')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "card" : normalized;
    }

    private static List<String> normalizeInstrumentSymbols(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
