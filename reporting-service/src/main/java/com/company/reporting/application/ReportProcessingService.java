package com.company.reporting.application;

import com.company.reporting.event.ReportRequestedEvent;

public interface ReportProcessingService {

    void process(ReportRequestedEvent event);
}