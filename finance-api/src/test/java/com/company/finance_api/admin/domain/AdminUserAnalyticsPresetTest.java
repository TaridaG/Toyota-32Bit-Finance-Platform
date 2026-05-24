package com.company.finance_api.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminUserAnalyticsPresetTest {

  @Test
  void last7Days_inclusiveDayCountIsSeven() {
    assertThat(AdminUserAnalyticsPreset.LAST_7_DAYS.inclusiveDayCount()).isEqualTo(7);
    assertThat(AdminUserAnalyticsPreset.LAST_7_DAYS.queryParam()).isEqualTo("7d");
  }

  @Test
  void last30Days_inclusiveDayCountIsThirty() {
    assertThat(AdminUserAnalyticsPreset.LAST_30_DAYS.inclusiveDayCount()).isEqualTo(30);
    assertThat(AdminUserAnalyticsPreset.LAST_30_DAYS.queryParam()).isEqualTo("30d");
  }

  @Test
  void parse_resolvesKnownPreset() {
    assertThat(AdminUserAnalyticsPreset.parse("7d"))
        .isEqualTo(AdminUserAnalyticsPreset.LAST_7_DAYS);
    assertThat(AdminUserAnalyticsPreset.parse("unknown"))
        .isEqualTo(AdminUserAnalyticsPreset.LAST_7_DAYS);
    assertThat(AdminUserAnalyticsPreset.parse(null))
        .isEqualTo(AdminUserAnalyticsPreset.LAST_7_DAYS);
  }
}
