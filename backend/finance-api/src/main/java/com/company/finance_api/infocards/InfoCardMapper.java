package com.company.finance_api.infocards;

import com.company.finance_api.domain.InfoCardEntity;
import com.company.finance_api.infocards.dto.InfoCardDto;
import com.company.finance_api.infocards.dto.InfoCardInputDto;
import com.company.finance_api.infocards.dto.InfoCardLocaleContentDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class InfoCardMapper {

    private InfoCardMapper() {
    }

    public static InfoCardDto toDto(InfoCardEntity entity) {
        return toDto(entity, null, true);
    }

    public static InfoCardDto toPortalDto(InfoCardEntity entity, String locale) {
        return toDto(entity, locale, false);
    }

    private static InfoCardDto toDto(InfoCardEntity entity, String locale, boolean includeTranslations) {
        InfoCardLocaleContentDto resolved = locale != null
                ? InfoCardLocaleResolver.resolve(entity, locale)
                : InfoCardLocaleResolver.pickPrimary(entity.getTranslations());
        if (resolved == null) {
            resolved = new InfoCardLocaleContentDto(
                    entity.getTitle(),
                    entity.getShortDescription(),
                    entity.getDetailedDescription(),
                    entity.getHowToInterpret(),
                    entity.getCommonMistake(),
                    entity.getExampleText(),
                    List.copyOf(entity.getRelatedTerms())
            );
        }
        Map<String, InfoCardLocaleContentDto> translations = includeTranslations
                ? copyTranslations(entity.getTranslations())
                : null;
        return new InfoCardDto(
                entity.getId().toString(),
                resolved.title(),
                entity.getSlug(),
                List.copyOf(entity.getTargetTerms()),
                List.copyOf(entity.getTargetElementIds()),
                List.copyOf(entity.getTargetInstrumentSymbols()),
                List.copyOf(entity.getPages()),
                entity.getCategory(),
                entity.getCardType(),
                entity.getDifficulty(),
                entity.getStatus(),
                resolved.shortDescription(),
                blankToEmpty(resolved.detailedDescription()),
                resolved.howToInterpret(),
                resolved.commonMistake(),
                resolved.example(),
                resolved.relatedTerms() != null ? List.copyOf(resolved.relatedTerms()) : List.of(),
                entity.isAdminOnly(),
                translations,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static void applyInput(InfoCardEntity entity, InfoCardInputDto input) {
        Map<String, InfoCardLocaleContentDto> translations = normalizeTranslations(input);
        if (translations.isEmpty() && hasFlatContent(input)) {
            translations = buildUniformTranslations(input);
        }
        if (!translations.isEmpty()) {
            entity.setTranslations(translations);
            syncPrimaryColumns(entity, InfoCardLocaleResolver.pickPrimary(translations));
        } else {
            entity.setTitle(input.title() != null ? input.title().trim() : "");
            entity.setShortDescription(input.shortDescription() != null ? input.shortDescription().trim() : "");
            entity.setDetailedDescription(blankToEmpty(input.detailedDescription()));
            entity.setHowToInterpret(blankToNull(input.howToInterpret()));
            entity.setCommonMistake(blankToNull(input.commonMistake()));
            entity.setExampleText(blankToNull(input.example()));
            entity.setRelatedTerms(normalizeList(input.relatedTerms()));
        }
        entity.setSlug(resolveSlug(input));
        entity.setTargetTerms(normalizeList(input.targetTerms()));
        entity.setTargetElementIds(normalizeList(input.targetElementIds()));
        entity.setTargetInstrumentSymbols(normalizeInstrumentSymbols(input.targetInstrumentSymbols()));
        entity.setPages(normalizeList(input.pages()));
        entity.setCategory(input.category());
        entity.setCardType(input.type());
        entity.setDifficulty(input.difficulty());
        entity.setStatus(input.status());
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
        Map<String, InfoCardLocaleContentDto> translations = normalizeTranslations(input);
        InfoCardLocaleContentDto primary = InfoCardLocaleResolver.pickPrimary(translations);
        if (primary != null && primary.title() != null && !primary.title().isBlank()) {
            return slugify(primary.title());
        }
        if (input.title() != null && !input.title().isBlank()) {
            return slugify(input.title());
        }
        return "card";
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

    private static Map<String, InfoCardLocaleContentDto> normalizeTranslations(InfoCardInputDto input) {
        if (input.translations() == null || input.translations().isEmpty()) {
            return Map.of();
        }
        Map<String, InfoCardLocaleContentDto> normalized = new LinkedHashMap<>();
        for (String locale : InfoCardLocaleResolver.SUPPORTED_LOCALES) {
            InfoCardLocaleContentDto raw = input.translations().get(locale);
            if (raw == null) {
                continue;
            }
            normalized.put(locale, normalizeLocaleContent(raw));
        }
        return normalized;
    }

    private static InfoCardLocaleContentDto normalizeLocaleContent(InfoCardLocaleContentDto raw) {
        return new InfoCardLocaleContentDto(
                raw.title() != null ? raw.title().trim() : "",
                raw.shortDescription() != null ? raw.shortDescription().trim() : "",
                blankToEmpty(raw.detailedDescription()),
                blankToNull(raw.howToInterpret()),
                blankToNull(raw.commonMistake()),
                blankToNull(raw.example()),
                normalizeList(raw.relatedTerms())
        );
    }

    private static void syncPrimaryColumns(InfoCardEntity entity, InfoCardLocaleContentDto primary) {
        if (primary == null) {
            return;
        }
        entity.setTitle(primary.title());
        entity.setShortDescription(primary.shortDescription());
        entity.setDetailedDescription(blankToEmpty(primary.detailedDescription()));
        entity.setHowToInterpret(blankToNull(primary.howToInterpret()));
        entity.setCommonMistake(blankToNull(primary.commonMistake()));
        entity.setExampleText(blankToNull(primary.example()));
        entity.setRelatedTerms(normalizeList(primary.relatedTerms()));
    }

    private static boolean hasFlatContent(InfoCardInputDto input) {
        return input.title() != null
                && !input.title().isBlank()
                && input.shortDescription() != null
                && !input.shortDescription().isBlank();
    }

    private static Map<String, InfoCardLocaleContentDto> buildUniformTranslations(InfoCardInputDto input) {
        InfoCardLocaleContentDto content = new InfoCardLocaleContentDto(
                input.title().trim(),
                input.shortDescription().trim(),
                blankToEmpty(input.detailedDescription()),
                blankToNull(input.howToInterpret()),
                blankToNull(input.commonMistake()),
                blankToNull(input.example()),
                normalizeList(input.relatedTerms())
        );
        Map<String, InfoCardLocaleContentDto> map = new LinkedHashMap<>();
        for (String locale : InfoCardLocaleResolver.SUPPORTED_LOCALES) {
            map.put(locale, content);
        }
        return map;
    }

    private static Map<String, InfoCardLocaleContentDto> copyTranslations(
            Map<String, InfoCardLocaleContentDto> translations
    ) {
        if (translations == null || translations.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(translations);
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
