package com.company.finance_api.portfolio.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioGoalRepository extends JpaRepository<PortfolioGoal, Long> {

    @Query("""
            select g from PortfolioGoal g
            left join fetch g.externalPortfolio
            where g.user.id = :userId
              and ((:portfolioId is null and g.externalPortfolio is null)
                   or (g.externalPortfolio.id = :portfolioId))
            """)
    List<PortfolioGoal> findAllForScope(@Param("userId") UUID userId, @Param("portfolioId") Long portfolioId);

    @Query("""
            select g from PortfolioGoal g
            where g.user.id = :userId
              and g.goalType = :goalType
              and ((:portfolioId is null and g.externalPortfolio is null)
                   or (g.externalPortfolio.id = :portfolioId))
            """)
    Optional<PortfolioGoal> findForScopeAndType(
            @Param("userId") UUID userId,
            @Param("portfolioId") Long portfolioId,
            @Param("goalType") GoalType goalType
    );
}
