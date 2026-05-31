package com.company.finance_api.alarm.domain.enums;

/** AlarmCondition — domain enum sabitleri. */
public enum AlarmCondition {
  GREATER_THAN, // fiyat >
  LESS_THAN, // fiyat <
  EQUAL, // fiyat ==
  PERCENT_CHANGE_UP, // % artış
  PERCENT_CHANGE_DOWN
}
