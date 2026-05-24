package com.company.finance_api.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.finance_api.bootstrap.config.MfaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MfaCryptoServiceTest {

  MfaCryptoService cryptoService;

  @BeforeEach
  void setUp() {
    MfaProperties properties = new MfaProperties();
    properties.setEncryptionSecret("unit-test-mfa-secret-key");
    cryptoService = new MfaCryptoService(properties);
  }

  @Test
  void encryptDecrypt_should_roundTripPlaintext() {
    String plaintext = "JBSWY3DPEHPK3PXP";

    String encrypted = cryptoService.encrypt(plaintext);
    String decrypted = cryptoService.decrypt(encrypted);

    assertEquals(plaintext, decrypted);
  }

  @Test
  void encrypt_should_produceDifferentCiphertextEachTime() {
    String plaintext = "same-secret-value";

    String first = cryptoService.encrypt(plaintext);
    String second = cryptoService.encrypt(plaintext);

    assertNotEquals(first, second);
    assertEquals(plaintext, cryptoService.decrypt(first));
    assertEquals(plaintext, cryptoService.decrypt(second));
  }

  @Test
  void encrypt_should_returnNullForNullInput() {
    assertNull(cryptoService.encrypt(null));
  }

  @Test
  void decrypt_should_returnNullForBlankInput() {
    assertNull(cryptoService.decrypt(null));
    assertNull(cryptoService.decrypt(""));
    assertNull(cryptoService.decrypt("   "));
  }

  @Test
  void decrypt_should_failForInvalidCiphertext() {
    assertThrows(IllegalStateException.class, () -> cryptoService.decrypt("not-valid-base64!!!"));
  }
}
