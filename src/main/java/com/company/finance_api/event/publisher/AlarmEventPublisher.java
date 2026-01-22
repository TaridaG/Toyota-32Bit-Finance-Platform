package com.company.finance_api.event.publisher;

import com.company.finance_api.event.AlarmTriggeredEvent;

public interface AlarmEventPublisher {

    void publish(AlarmTriggeredEvent event);
}
