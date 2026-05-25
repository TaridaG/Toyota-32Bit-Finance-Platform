package com.company.gateway.bootstrap.config;

/**
 * Spring Security path patterns for legacy {@code /api/**} and canonical {@code /api/v1/**} routes.
 */
final class GatewaySecurityPaths {

    private static final String V1 = ApiVersionPathSupport.EXTERNAL_API_VERSION_PREFIX;

    private GatewaySecurityPaths() {}

    static String[] publicAuthPosts() {
        return dual(
                "/api/public/register",
                "/api/public/register/send-code",
                "/api/public/login",
                "/api/public/login/mfa",
                "/api/public/refresh");
    }

    static String[] publicAuthGets() {
        return dual("/api/public/**");
    }

    static String[] publicAnonymousGets() {
        return dual(
                "/api/market/**",
                "/api/rates/**",
                "/market/**");
    }

    static String[] newsFavorites() {
        return dual("/api/news/favorites", "/api/news/favorites/**");
    }

    static String[] publicNews() {
        return dual("/api/news/**");
    }

    static String[] instruments() {
        return dual("/api/instruments", "/api/instruments/", "/api/instruments/**");
    }

    static String[] analytics() {
        return dual("/api/analytics/**");
    }

    static String[] portalInfoCards() {
        return dual("/api/portal/info-cards", "/api/portal/info-cards/**");
    }

    static String[] newsAdmin() {
        return dual("/api/news/admin/**");
    }

    static String[] admin() {
        return dual("/api/admin/**");
    }

    static String[] userProfile() {
        return dual("/api/users/me/**", "/api/profile/**");
    }

    static String[] portfolioWrites() {
        return dual(
                "/api/portfolio/**",
                "/api/accounts/**",
                "/api/balances/**",
                "/api/transactions/**",
                "/api/trades/**",
                "/api/orders/**");
    }

    static String[] versionedApi() {
        return new String[] {V1, V1 + "/**"};
    }

    static String[] legacyApi() {
        return new String[] {"/api/**"};
    }

    private static String[] dual(String... legacyPatterns) {
        int n = legacyPatterns.length;
        String[] all = new String[n * 2];
        for (int i = 0; i < n; i++) {
            String legacy = legacyPatterns[i];
            all[i] = legacy;
            all[i + n] = toV1(legacy);
        }
        return all;
    }

    private static String toV1(String legacyPath) {
        if (legacyPath.startsWith("/api/")) {
            return V1 + legacyPath.substring(4);
        }
        if (legacyPath.equals("/api")) {
            return V1;
        }
        return legacyPath;
    }
}
