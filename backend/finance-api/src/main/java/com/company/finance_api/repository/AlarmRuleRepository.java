package com.company.finance_api.repository;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmRuleRepository extends JpaRepository<AlarmRule, Long> {

    // Kullanıcının aktif alarmları (profil ekranı vs.)
    List<AlarmRule> findByUserAndActiveTrue(User user);

    // Bir enstrüman için tüm aktif alarmlar (scheduler burayı kullanır)
    List<AlarmRule> findByInstrumentAndActiveTrue(Instrument instrument);

    List<AlarmRule> findByUserAndInstrumentAndActiveTrue(
            User user,
            Instrument instrument
    );
}
