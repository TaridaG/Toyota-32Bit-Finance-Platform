package com.company.finance_api.event.listener;

import com.company.finance_api.event.PriceUpdatedEvent;
import com.company.finance_api.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Fiyat güncellendiğinde alarm değerlendirmesini tetikler. */
@Component
@RequiredArgsConstructor
public class PriceUpdatedEventListener {

  private final AlarmService alarmService;

  @EventListener
  public void handle(PriceUpdatedEvent event) {
    alarmService.checkAlarms(event.price().getInstrument(), event.price());
  }
}
