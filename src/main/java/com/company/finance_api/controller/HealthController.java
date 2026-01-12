package com.company.finance_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public String health() {
        try (Connection connection = dataSource.getConnection()) {
            return "OK - DB CONNECTED";
        } catch (Exception e) {
            return "DB ERROR: " + e.getMessage();
        }
    }
}
