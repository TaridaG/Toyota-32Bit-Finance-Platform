package com.company.reporting.application;

import com.company.reporting.event.ReportRequestedEvent;

public interface ReportRequestPublisher {

    void publish(ReportRequestedEvent event);
}