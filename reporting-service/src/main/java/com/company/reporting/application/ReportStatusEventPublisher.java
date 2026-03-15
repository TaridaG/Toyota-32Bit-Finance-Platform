package com.company.reporting.application;

import com.company.reporting.event.ReportCompletedEvent;
import com.company.reporting.event.ReportFailedEvent;

public interface ReportStatusEventPublisher {

    void publishCompleted(ReportCompletedEvent event);

    void publishFailed(ReportFailedEvent event);
}