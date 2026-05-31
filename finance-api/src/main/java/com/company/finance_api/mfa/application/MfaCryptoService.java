package com.company.finance_api.mfa.application;

import com.company.finance_api.bootstrap.config.MfaProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** TOTP secret ve login challenge token'larının AES-GCM ile şifrelenmesi. */
@Service
public class MfaCryptoService {

  private static final int GCM_IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom secureRandom = new SecureRandom();

  public MfaCryptoService(MfaProperties properties) {
    this.key = deriveKey(properties.getEncryptionSecret());
  }

  /** Metni AES-GCM ile şifreler (Base64). */
  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      byte[] iv = new byte[GCM_IV_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] packed = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, packed, 0, iv.length);
      System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(packed);
    } catch (Exception ex) {
      throw new IllegalStateException("MFA encryption failed", ex);
    }
  }

  /** Base64 şifreli metni çözer. */
  public String decrypt(String encoded) {
    if (!StringUtils.hasText(encoded)) {
      return null;
    }
    try {
      byte[] packed = Base64.getDecoder().decode(encoded.trim());
      if (packed.length <= GCM_IV_BYTES) {
        throw new IllegalArgumentException("Invalid MFA ciphertext");
      }
      byte[] iv = new byte[GCM_IV_BYTES];
      System.arraycopy(packed, 0, iv, 0, GCM_IV_BYTES);
      byte[] ciphertext = new byte[packed.length - GCM_IV_BYTES];
      System.arraycopy(packed, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] plain = cipher.doFinal(ciphertext);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("MFA decryption failed", ex);
    }
  }

  private static SecretKeySpec deriveKey(String secret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] raw =
          digest.digest((secret != null ? secret : "change-me").getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(raw, "AES");
    } catch (Exception ex) {
      throw new IllegalStateException("MFA key derivation failed", ex);
    }
  }
}
