package com.company.finance_api.portfolio.goal.infrastructure.persistence;

import com.company.finance_api.portfolio.goal.domain.GoalType;
import com.company.finance_api.portfolio.goal.domain.PortfolioGoal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@link PortfolioGoal} kayıtları için Spring Data JPA repository. */
public interface PortfolioGoalRepository extends JpaRepository<PortfolioGoal, Long> {

  /** Belirtilen kullanıcı ve portfolio kapsamındaki tüm hedefleri döner. */
  @Query(
      """
            select g from PortfolioGoal g
            left join fetch g.externalPortfolio
            where g.user.id = :userId
              and ((:portfolioId is null and g.externalPortfolio is null)
                   or (g.externalPortfolio.id = :portfolioId))
            """)
  List<PortfolioGoal> findAllForScope(
      @Param("userId") UUID userId, @Param("portfolioId") Long portfolioId);

  /** Kapsam ve hedef türüne göre tek bir hedef kaydını arar. */
  @Query(
      """
            select g from PortfolioGoal g
            where g.user.id = :userId
              and g.goalType = :goalType
              and ((:portfolioId is null and g.externalPortfolio is null)
                   or (g.externalPortfolio.id = :portfolioId))
            """)
  Optional<PortfolioGoal> findForScopeAndType(
      @Param("userId") UUID userId,
      @Param("portfolioId") Long portfolioId,
      @Param("goalType") GoalType goalType);
}
