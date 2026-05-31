package com.company.finance_api.profile.infrastructure.persistence;

import com.company.finance_api.profile.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** User entity persistence için Spring Data repository. */
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailIgnoreCase(String email);

  Optional<User> findByUsername(String username);

  Optional<User> findByUsernameIgnoreCase(String username);

  Optional<User> findByAuthUsernameIgnoreCase(String authUsername);

  List<User> findByActiveTrue();

  /** Portal accounts not in deletion workflow (still on roster). */
  @Query("select count(u) from User u where u.deletionRequestedAt is null")
  long countByNotPendingDeletion();

  @Query(
      "select count(u) from User u where u.createdAt >= :from and u.createdAt < :to and u.deletionRequestedAt is null")
  long countCreatedInRangeExcludingPendingDeletion(
      @Param("from") Instant from, @Param("to") Instant to);

  /** Account deletion workflow started in {@code [from, to)} (UTC semantics from callers). */
  @Query(
      "select count(u) from User u where u.deletionRequestedAt is not null and u.deletionRequestedAt >= :from and u.deletionRequestedAt < :to")
  long countDeletionRequestedInRange(@Param("from") Instant from, @Param("to") Instant to);

  /**
   * Roster members with {@code active == true} (may still include accounts pending other
   * workflows).
   */
  @Query("select count(u) from User u where u.deletionRequestedAt is null and u.active = true")
  long countActiveRoster();

  @Query("select u from User u where u.deletionRequestedAt is null order by u.createdAt desc")
  List<User> findRosterByCreatedAtDesc(Pageable pageable);

  /** Roster accounts currently frozen (admin suspend). */
  @Query(
      "select count(u) from User u where u.deletionRequestedAt is null and u.frozenAt is not null")
  long countFrozenRoster();
}
