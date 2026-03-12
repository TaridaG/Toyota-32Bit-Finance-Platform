package com.company.reporting.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public final class JsonMapperUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonMapperUtil() {
    }

    public static <T> List<T> convertList(Object source, Class<T> type) {
        if (source == null) {
            return Collections.emptyList();
        }
        return OBJECT_MAPPER.convertValue(
                source,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, type)
        );
    }
}