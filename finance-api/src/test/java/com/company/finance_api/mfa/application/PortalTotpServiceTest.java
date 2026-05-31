package com.company.finance_api.mfa.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.company.finance_api.bootstrap.config.MfaProperties;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortalTotpServiceTest {

  PortalTotpService totpService;

  @BeforeEach
  void setUp() {
    MfaProperties properties = new MfaProperties();
    properties.setIssuer("Finance Portal Test");
    totpService = new PortalTotpService(properties);
  }

  @Test
  void generateSecret_should_returnNonBlankBase32Secret() {
    String secret = totpService.generateSecret();

    assertNotNull(secret);
    assertFalse(secret.isBlank());
    assertTrue(secret.matches("[A-Z2-7]+"));
  }

  @Test
  void verifyCode_should_passNormalizedSecretAndCodeToVerifier() {
    CodeVerifier codeVerifier = mock(CodeVerifier.class);
    ReflectionTestUtils.setField(totpService, "codeVerifier", codeVerifier);
    when(codeVerifier.isValidCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);

    assertTrue(totpService.verifyCode(" JBSWY3DPEHPK3PXP ", "123 456"));
    verify(codeVerifier).isValidCode("JBSWY3DPEHPK3PXP", "123456");
  }

  @Test
  void verifyCode_should_shortCircuitBeforeVerifierWhenInputInvalid() {
    CodeVerifier codeVerifier = mock(CodeVerifier.class);
    ReflectionTestUtils.setField(totpService, "codeVerifier", codeVerifier);

    assertFalse(totpService.verifyCode(null, "123456"));
    assertFalse(totpService.verifyCode("JBSWY3DPEHPK3PXP", null));
    assertFalse(totpService.verifyCode("JBSWY3DPEHPK3PXP", "12345"));
    assertFalse(totpService.verifyCode("JBSWY3DPEHPK3PXP", "abcdef"));

    verifyNoInteractions(codeVerifier);
  }

  @Test
  void buildQrData_should_useIssuerAndAccountLabel() {
    String secret = totpService.generateSecret();

    QrData qrData = totpService.buildQrData("  trader@example.com  ", secret);

    assertEquals("trader@example.com", qrData.getLabel());
    assertEquals(secret, qrData.getSecret());
    assertEquals("Finance Portal Test", qrData.getIssuer());
    assertEquals("SHA1", qrData.getAlgorithm());
    assertEquals(6, qrData.getDigits());
    assertEquals(30, qrData.getPeriod());
  }

  @Test
  void buildOtpAuthUri_should_matchQrDataUri() {
    QrData qrData = totpService.buildQrData("user", totpService.generateSecret());

    assertEquals(qrData.getUri(), totpService.buildOtpAuthUri(qrData));
  }

  @Test
  void generateQrPng_should_returnPngBytes() {
    QrData qrData = totpService.buildQrData("user", totpService.generateSecret());

    byte[] png = totpService.generateQrPng(qrData);

    assertNotNull(png);
    assertTrue(png.length > 8);
    assertArrayEquals(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}, new byte[] {png[0], png[1], png[2], png[3]});
  }
}
