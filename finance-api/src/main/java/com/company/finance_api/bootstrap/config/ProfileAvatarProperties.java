package com.company.finance_api.bootstrap.config;

import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Profil avatar JPEG depolama kökü, resize limitleri ve upload boyutu ({@code app.profile-avatar}).
 */
@ConfigurationProperties(prefix = "app.profile-avatar")
public class ProfileAvatarProperties {

  /**
   * Kalıcı avatar JPEG dosyalarının kök dizini (varsayılan: repo kökünde {@code
   * photos/{userId}/avatar.jpg}).
   */
  private String storageRoot = Paths.get("../../photos").toAbsolutePath().normalize().toString();

  /** Resize sonrası en uzun kenar (kare bounding box, aspect ratio korunur). */
  private int maxEdgePixels = 512;

  /** JPEG çıktı kalitesi (0–1). */
  private float jpegQuality = 0.82f;

  /** Decode öncesi kabul edilen maksimum upload boyutu (byte). */
  private long maxUploadBytes = 5L * 1024 * 1024;

  public String getStorageRoot() {
    return storageRoot;
  }

  public void setStorageRoot(String storageRoot) {
    this.storageRoot = storageRoot;
  }

  public int getMaxEdgePixels() {
    return maxEdgePixels;
  }

  public void setMaxEdgePixels(int maxEdgePixels) {
    this.maxEdgePixels = maxEdgePixels;
  }

  public float getJpegQuality() {
    return jpegQuality;
  }

  public void setJpegQuality(float jpegQuality) {
    this.jpegQuality = jpegQuality;
  }

  public long getMaxUploadBytes() {
    return maxUploadBytes;
  }

  public void setMaxUploadBytes(long maxUploadBytes) {
    this.maxUploadBytes = maxUploadBytes;
  }
}
