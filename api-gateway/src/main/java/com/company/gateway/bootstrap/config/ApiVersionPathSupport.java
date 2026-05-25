package com.company.gateway.bootstrap.config;

/**
 * External REST API version prefix ({@code /api/v1}). Gateway rewrites {@code /api/v1/...} to
 * downstream {@code /api/...}; security matchers normalize paths the same way.
 */
public final class ApiVersionPathSupport {

    public static final String API_VERSION = "v1";
    public static final String EXTERNAL_API_VERSION_PREFIX = "/api/" + API_VERSION;

    private static final String LEGACY_API_PREFIX = "/api/";
    private static final String VERSION_SEGMENT = "/api/v1/";

    private ApiVersionPathSupport() {}

    /** Spring Cloud Gateway rewrite: strip {@code /v1} before forwarding to microservices. */
    public static final String REWRITE_API_V1_PATTERN = "/api/v1/(?<segment>.*)";

    public static final String REWRITE_API_V1_REPLACEMENT = "/api/${segment}";

    /**
     * Maps {@code /api/v1/portfolio/overview} → {@code /api/portfolio/overview} for authorization rules
     * shared with legacy {@code /api/...} routes.
     */
    public static String normalizeForSecurity(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        String p = path;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.equals(EXTERNAL_API_VERSION_PREFIX)) {
            return "/api";
        }
        if (p.startsWith(VERSION_SEGMENT)) {
            return LEGACY_API_PREFIX + p.substring(VERSION_SEGMENT.length());
        }
        return p;
    }

    /** Legacy unversioned {@code /api/...} paths (excluding {@code /api/v1/...}). */
    public static boolean isLegacyApiPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String p = path;
        if (!p.startsWith(LEGACY_API_PREFIX)) {
            return false;
        }
        return !p.startsWith(VERSION_SEGMENT) && !p.equals(EXTERNAL_API_VERSION_PREFIX);
    }
}
