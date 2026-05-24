package com.company.finance_api.test.support;

import com.company.finance_api.shared.security.PortalAccountGuardService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

/** WebMvcTest slice'larında {@code FrozenAccountFilter} bağımlılığını karşılar. */
@TestConfiguration
public class WebMvcTestSecuritySupport {

  @MockBean private PortalAccountGuardService portalAccountGuardService;
}
