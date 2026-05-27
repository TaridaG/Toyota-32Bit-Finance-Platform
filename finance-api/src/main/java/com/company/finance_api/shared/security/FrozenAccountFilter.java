package com.company.finance_api.shared.security;

import com.company.finance_api.shared.web.ApiError;
import com.company.finance_api.shared.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** JWT geçerli olsa bile dondurulmuş veya silinmiş portal hesaplarının isteklerini reddeder. */
@Component
public class FrozenAccountFilter extends OncePerRequestFilter {

  private final PortalAccountGuardService portalAccountGuardService;
  private final ObjectMapper objectMapper;

  public FrozenAccountFilter(
      PortalAccountGuardService portalAccountGuardService, ObjectMapper objectMapper) {
    this.portalAccountGuardService = portalAccountGuardService;
    this.objectMapper = objectMapper;
  }

  /** Health, public auth ve admin path'lerinde filter devre dışı. */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) {
      return false;
    }
    if ("/health".equals(path)) {
      return true;
    }
    if (path.startsWith("/api/v1/public/")) {
      return true;
    }
    if (path.startsWith("/api/v1/admin/")) {
      return true;
    }
    return false;
  }

  /** Frozen veya silinmiş hesap için HTTP 403 JSON yanıtı yazar. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !portalAccountGuardService.isAdmin(authentication)) {
      var user = portalAccountGuardService.resolvePortalUser(authentication);
      if (user.isPresent()) {
        if (user.get().isFrozen()) {
          writeErrorResponse(
              response,
              PortalAccountGuardService.ACCOUNT_FROZEN_ERROR_CODE,
              PortalAccountGuardService.ACCOUNT_FROZEN_MESSAGE);
          return;
        }
      } else if (authentication instanceof JwtAuthenticationToken jwtAuth
          && portalAccountGuardService.hasPortalIdentityClaims(jwtAuth.getToken())) {
        writeErrorResponse(
            response,
            PortalAccountGuardService.ACCOUNT_REMOVED_ERROR_CODE,
            PortalAccountGuardService.ACCOUNT_REMOVED_MESSAGE);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private void writeErrorResponse(HttpServletResponse response, String code, String message)
      throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error = new ApiError(code, message);
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
  }
}
