package com.company.finance_api.repository;

import com.company.finance_api.domain.EurobondInstrument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** EurobondInstrument entity persistence için Spring Data repository. */
public interface EurobondInstrumentRepository extends JpaRepository<EurobondInstrument, String> {

  List<EurobondInstrument> findAllByActiveIsTrueOrderByMaturityDateAsc();
}
