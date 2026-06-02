package com.company.gateway.bootstrap.config;

/** Spring Security path patterns for canonical {@code /api/v1/**} routes. */
final class GatewaySecurityPaths {

    private static final String V1 = ApiVersionPathSupport.EXTERNAL_API_VERSION_PREFIX;

    private GatewaySecurityPaths() {}

    static String[] publicAuthPosts() {
        return new String[] {
                V1 + "/public/register",
                V1 + "/public/register/send-code",
                V1 + "/public/login",
                V1 + "/public/login/mfa",
                V1 + "/public/refresh"
        };
    }

    static String[] publicAuthGets() {
        return new String[] {V1 + "/public/**"};
    }

    static String[] publicAnonymousGets() {
        return new String[] {V1 + "/market/**", V1 + "/rates/**"};
    }

    static String[] newsFavorites() {
        return new String[] {V1 + "/news/favorites", V1 + "/news/favorites/**"};
    }

    static String[] publicNews() {
        return new String[] {V1 + "/news/**"};
    }

    static String[] instruments() {
        return new String[] {V1 + "/instruments", V1 + "/instruments/", V1 + "/instruments/**"};
    }

    static String[] analytics() {
        return new String[] {V1 + "/analytics/**"};
    }

    static String[] portalInfoCards() {
        return new String[] {V1 + "/portal/info-cards", V1 + "/portal/info-cards/**"};
    }

    static String[] newsAdmin() {
        return new String[] {V1 + "/news/admin/**"};
    }

    static String[] admin() {
        return new String[] {V1 + "/admin/**"};
    }

    static String[] ingestAdminActions() {
        return new String[] {V1 + "/market/ingest/actions/**"};
    }

    static String[] userProfile() {
        return new String[] {V1 + "/users/me/**", V1 + "/profile/**", V1 + "/portal/profile/**"};
    }

    static String[] portfolioWrites() {
        return new String[] {
                V1 + "/portfolio/**",
                V1 + "/accounts/**",
                V1 + "/balances/**",
                V1 + "/transactions/**",
                V1 + "/trades/**",
                V1 + "/orders/**"
        };
    }

    static String[] versionedApi() {
        return new String[] {V1, V1 + "/**"};
    }
}
