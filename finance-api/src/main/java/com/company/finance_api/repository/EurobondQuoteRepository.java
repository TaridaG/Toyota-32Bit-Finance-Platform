package com.company.finance_api.repository;

import com.company.finance_api.domain.EurobondQuote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** EurobondQuote entity persistence için Spring Data repository. */
public interface EurobondQuoteRepository extends JpaRepository<EurobondQuote, Long> {

  Optional<EurobondQuote> findFirstByIsinOrderByQuoteTimeDesc(String isin);
}
