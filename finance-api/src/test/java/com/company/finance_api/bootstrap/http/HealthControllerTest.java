package com.company.finance_api.bootstrap.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestSecuritySupport.class)
@ActiveProfiles("test")
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private DataSource dataSource;

  @Test
  void health_should_reportDatabaseUpWhenConnectionSucceeds() throws Exception {
    Connection connection = org.mockito.Mockito.mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);

    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.application").value("UP"))
        .andExpect(jsonPath("$.data.database").value("UP"));
  }

  @Test
  void health_should_reportDatabaseDownWhenConnectionFails() throws Exception {
    when(dataSource.getConnection()).thenThrow(new RuntimeException("connection refused"));

    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.application").value("UP"))
        .andExpect(jsonPath("$.data.database").value("DOWN"));
  }
}
