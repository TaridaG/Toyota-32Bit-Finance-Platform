package com.company.finance_api.bootstrap.http;

import com.company.finance_api.shared.web.ApiResponse;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Uygulama ve veritabanı sağlık durumunu dönen basit health endpoint'i. */
@RestController
public class HealthController {

  private final DataSource dataSource;

  public HealthController(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** {@code /health} — uygulama ve JDBC bağlantı durumunu kontrol eder. */
  @GetMapping("/health")
  public ApiResponse<Map<String, Object>> health() {

    Map<String, Object> status = new HashMap<>();
    status.put("application", "UP");

    try (Connection connection = dataSource.getConnection()) {
      status.put("database", "UP");
    } catch (Exception e) {
      status.put("database", "DOWN");
    }

    return ApiResponse.success(status);
  }
}
