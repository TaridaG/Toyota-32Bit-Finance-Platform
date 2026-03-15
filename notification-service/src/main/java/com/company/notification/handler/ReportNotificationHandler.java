package com.company.notification.handler;

import com.company.notification.event.ReportCompletedEvent;
import com.company.notification.event.ReportFailedEvent;

public interface ReportNotificationHandler {

    void handleCompleted(ReportCompletedEvent event);

    void handleFailed(ReportFailedEvent event);
}