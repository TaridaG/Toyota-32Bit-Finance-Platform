package com.company.finance_api.alarm.domain.enums;

/** AlarmCondition — domain enum sabitleri. */
public enum AlarmCondition {
  GREATER_THAN, // fiyat büyükse
  LESS_THAN, // fiyat küçükse
  EQUAL, // fiyat eşit mi kontrolü
  PERCENT_CHANGE_UP, // yüzde artış
  PERCENT_CHANGE_DOWN //yüzde olarak azalış
}
