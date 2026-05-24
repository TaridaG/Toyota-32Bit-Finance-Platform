package com.company.finance_api.admin.application;

import com.company.finance_api.admin.domain.AdminUserDirectorySort;
import com.company.finance_api.admin.infrastructure.http.dto.AdminUserDirectoryPageDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminUserListItemDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin portal kullanıcı dizinini filtre, sıralama ve sayfalama ile JPQL projection ile listeler.
 */
@Service
public class AdminUserDirectoryService {

  private static final String SELECT_ROW =
      """
            select new com.company.finance_api.admin.infrastructure.http.dto.AdminUserListItemDto(
                u.id, u.username, u.email, u.emailVerified, u.createdAt,
                case when u.profileAvatarUpdatedAt is not null then true else false end,
                (select count(ep) from ExternalPortfolio ep where ep.user.id = u.id),
                case when u.frozenAt is not null then true else false end
            )
            from User u
            where u.deletionRequestedAt is null
            """;

  private static final String COUNT_BASE =
      """
            select count(u)
            from User u
            where u.deletionRequestedAt is null
            """;

  private final EntityManager entityManager;

  public AdminUserDirectoryService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Filtrelenmiş ve sıralanmış kullanıcı dizin sayfasını döner. */
  @Transactional(readOnly = true)
  public AdminUserDirectoryPageDto list(
      int page,
      int size,
      AdminUserDirectorySort sort,
      String sortParamEcho,
      Instant registeredFrom,
      Instant registeredToExclusive,
      Boolean emailVerifiedFilter,
      Integer portfolioExact,
      String search) {
    if (page < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
    }
    if (size != 10 && size != 20 && size != 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be 10, 20, or 50");
    }
    if (portfolioExact != null && (portfolioExact < 1 || portfolioExact > 5)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "portfolioCount must be 1..5 when set");
    }
    if (registeredFrom != null
        && registeredToExclusive != null
        && !registeredToExclusive.isAfter(registeredFrom)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "registeredTo must be after registeredFrom");
    }

    StringBuilder filters = new StringBuilder();
    Map<String, Object> params = new HashMap<>();
    if (registeredFrom != null) {
      filters.append(" and u.createdAt >= :registeredFrom");
      params.put("registeredFrom", registeredFrom);
    }
    if (registeredToExclusive != null) {
      filters.append(" and u.createdAt < :registeredToExclusive");
      params.put("registeredToExclusive", registeredToExclusive);
    }
    if (emailVerifiedFilter != null) {
      filters.append(" and u.emailVerified = :emailVerifiedFilter");
      params.put("emailVerifiedFilter", emailVerifiedFilter);
    }
    if (portfolioExact != null) {
      filters.append(
          " and (select count(ep) from ExternalPortfolio ep where ep.user.id = u.id) = :portfolioExact");
      params.put("portfolioExact", portfolioExact.longValue());
    }
    if (StringUtils.hasText(search)) {
      String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
      filters.append(
          " and (lower(u.username) like :search or lower(u.email) like :search or lower(u.authUsername) like :search)");
      params.put("search", term);
    }

    String orderBy = " order by " + sort.jpqlOrder();
    String listJpql = SELECT_ROW + filters + orderBy;
    String countJpql = COUNT_BASE + filters;

    TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
    params.forEach(countQuery::setParameter);
    long total = countQuery.getSingleResult();

    TypedQuery<AdminUserListItemDto> listQuery =
        entityManager.createQuery(listJpql, AdminUserListItemDto.class);
    params.forEach(listQuery::setParameter);
    listQuery.setFirstResult(page * size);
    listQuery.setMaxResults(size);
    List<AdminUserListItemDto> content = listQuery.getResultList();

    int totalPages = (int) Math.ceil(total / (double) size);
    if (totalPages == 0) {
      totalPages = 0;
    }
    return new AdminUserDirectoryPageDto(content, total, totalPages, page, size, sortParamEcho);
  }
}
