package com.company.logconsumer.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class IndexNameResolver {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String todayIndex(String prefix) {
        return prefix + "-" + LocalDate.now().format(FMT);
    }
}