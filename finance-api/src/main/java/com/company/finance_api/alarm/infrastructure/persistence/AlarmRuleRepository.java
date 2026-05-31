package com.company.finance_api.alarm.infrastructure.persistence;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.profile.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** AlarmRule entity persistence için Spring Data repository. */
public interface AlarmRuleRepository extends JpaRepository<AlarmRule, Long> {

  // Kullanıcının aktif alarmları (profil ekranı vs.)
  List<AlarmRule> findByUserAndActiveTrue(User user);

  // Bir enstrüman için tüm aktif alarmlar (scheduler burayı kullanır)
  List<AlarmRule> findByInstrumentAndActiveTrue(Instrument instrument);

  List<AlarmRule> findByUserAndInstrumentAndActiveTrue(User user, Instrument instrument);
}
