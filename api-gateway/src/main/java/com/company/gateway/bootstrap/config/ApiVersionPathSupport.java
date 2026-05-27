package com.company.gateway.bootstrap.config;

/** External REST API version prefix ({@code /api/v1}). */
public final class ApiVersionPathSupport {

    public static final String API_VERSION = "v1";
    public static final String EXTERNAL_API_VERSION_PREFIX = "/api/" + API_VERSION;

    private ApiVersionPathSupport() {}
}
