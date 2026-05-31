package com.company.finance_api.market.infrastructure.persistence;

import com.company.finance_api.market.domain.EurobondQuote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** EurobondQuote entity persistence için Spring Data repository. */
public interface EurobondQuoteRepository extends JpaRepository<EurobondQuote, Long> {

  Optional<EurobondQuote> findFirstByIsinOrderByQuoteTimeDesc(String isin);

  @Query(
      """
      select q
      from EurobondQuote q
      where q.isin in :isins
        and q.quoteTime = (
          select max(q2.quoteTime)
          from EurobondQuote q2
          where q2.isin = q.isin
        )
      """)
  List<EurobondQuote> findLatestByIsinIn(@Param("isins") Collection<String> isins);
}
