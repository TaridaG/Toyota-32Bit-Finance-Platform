package com.company.finance_api.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AdminInfoCardAiSecurityTest {

  @Test
  void controller_requiresAdminRole() throws Exception {
    PreAuthorize annotation =
        com.company.finance_api.ai.infrastructure.http.AdminInfoCardAiController.class
            .getAnnotation(PreAuthorize.class);
    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).contains("ADMIN");

    Method complete =
        com.company.finance_api.ai.infrastructure.http.AdminInfoCardAiController.class
            .getDeclaredMethod(
                "complete", com.company.finance_api.ai.infrastructure.http.dto.CompleteInfoCardAiRequest.class);
    assertThat(complete).isNotNull();
  }
}
