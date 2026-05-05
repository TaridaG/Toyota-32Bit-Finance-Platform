package com.company.finance_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Paths;

@ConfigurationProperties(prefix = "app.profile-avatar")
public class ProfileAvatarProperties {

    /**
     * Root directory for persisted avatar JPEG files (one file per user id).
     */
    private String storageRoot = Paths.get(System.getProperty("java.io.tmpdir"), "finance-api", "profile-avatars")
            .toString();

    /** Longest edge after resize (square bounding box, aspect ratio preserved). */
    private int maxEdgePixels = 512;

    /** JPEG output quality (0–1). */
    private float jpegQuality = 0.82f;

    /** Maximum accepted upload size before decode (bytes). */
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
