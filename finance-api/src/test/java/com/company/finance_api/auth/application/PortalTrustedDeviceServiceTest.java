package com.company.finance_api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.bootstrap.config.TrustedDeviceProperties;
import com.company.finance_api.auth.infrastructure.persistence.TrustedLoginDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalTrustedDeviceServiceTest {

  @Mock TrustedLoginDeviceRepository repository;
  @Mock HttpServletRequest request;

  TrustedDeviceProperties properties;
  PortalTrustedDeviceService service;

  @BeforeEach
  void setUp() {
    properties = new TrustedDeviceProperties();
    properties.setEnabled(true);
    properties.setSigningSecret("test-secret");
    service = new PortalTrustedDeviceService(properties, repository);
  }

  @Test
  void isTrustedForUser_returnsFalseWhenDisabled() {
    properties.setEnabled(false);

    assertThat(service.isTrustedForUser(request, UUID.randomUUID())).isFalse();
    verify(repository, never()).findById(any());
  }

  @Test
  void isTrustedForUser_returnsFalseWhenUserIdNull() {
    assertThat(service.isTrustedForUser(request, null)).isFalse();
  }

  @Test
  void listForUser_returnsDisabledPayloadWhenFeatureOff() {
    properties.setEnabled(false);

    var response = service.listForUser(UUID.randomUUID(), request);

    assertThat(response.featureEnabled()).isFalse();
    assertThat(response.devices()).isEmpty();
  }

  @Test
  void revokeAllForUser_deletesAllRows() {
    UUID userId = UUID.randomUUID();

    service.revokeAllForUser(userId);

    verify(repository).deleteAllForUser(userId);
  }

  @Test
  void revokeAllForUser_noOpWhenUserIdNull() {
    service.revokeAllForUser(null);

    verify(repository, never()).deleteAllForUser(any());
  }

  @Test
  void issueTrustedDeviceCookie_returnsEmptyWhenDisabled() {
    properties.setEnabled(false);

    assertThat(service.issueTrustedDeviceCookie(UUID.randomUUID())).isEmpty();
  }
}
