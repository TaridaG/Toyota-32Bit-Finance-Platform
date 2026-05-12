package com.company.finance_api.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Whitelist for {@code sort} query param (never concatenate raw user input into queries).
 */
public enum AdminUserDirectorySort {
    CREATED_AT_ASC("u.createdAt asc"),
    CREATED_AT_DESC("u.createdAt desc"),
    USERNAME_ASC("lower(u.username) asc"),
    USERNAME_DESC("lower(u.username) desc"),
    EMAIL_ASC("lower(u.email) asc"),
    EMAIL_DESC("lower(u.email) desc"),
    EMAIL_VERIFIED_ASC("u.emailVerified asc, u.createdAt desc"),
    EMAIL_VERIFIED_DESC("u.emailVerified desc, u.createdAt desc"),
    PORTFOLIO_COUNT_ASC(
            "(select count(ep) from ExternalPortfolio ep where ep.user.id = u.id) asc, u.createdAt desc"),
    PORTFOLIO_COUNT_DESC(
            "(select count(ep) from ExternalPortfolio ep where ep.user.id = u.id) desc, u.createdAt desc");

    private final String jpqlOrder;

    AdminUserDirectorySort(String jpqlOrder) {
        this.jpqlOrder = jpqlOrder;
    }

    public String jpqlOrder() {
        return jpqlOrder;
    }

    public static AdminUserDirectorySort parse(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return CREATED_AT_DESC;
        }
        String[] parts = sortParam.trim().split(",", 2);
        String prop = parts[0].trim().toLowerCase();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";
        boolean asc = dir.equals("asc");
        return switch (prop) {
            case "createdat", "created_at" -> asc ? CREATED_AT_ASC : CREATED_AT_DESC;
            case "username" -> asc ? USERNAME_ASC : USERNAME_DESC;
            case "email" -> asc ? EMAIL_ASC : EMAIL_DESC;
            case "emailverified", "email_verified" -> asc ? EMAIL_VERIFIED_ASC : EMAIL_VERIFIED_DESC;
            case "portfoliocount", "portfolio_count" -> asc ? PORTFOLIO_COUNT_ASC : PORTFOLIO_COUNT_DESC;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort: " + sortParam);
        };
    }
}
