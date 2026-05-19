package com.company.finance_api.ai;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AdminInfoCardAiSecurityTest {

    @Test
    void controller_requiresAdminRole() throws Exception {
        PreAuthorize annotation = com.company.finance_api.ai.controller.AdminInfoCardAiController.class
                .getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains("ADMIN");

        Method complete = com.company.finance_api.ai.controller.AdminInfoCardAiController.class
                .getDeclaredMethod("complete", com.company.finance_api.ai.dto.CompleteInfoCardAiRequest.class);
        assertThat(complete).isNotNull();
    }
}
