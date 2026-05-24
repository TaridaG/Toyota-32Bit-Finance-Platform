package com.company.notification.report.application;

import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;

/**
 * Rapor export sonuçları hakkında kullanıcıları bilgilendirme sözleşmesi.
 */
public interface ReportNotificationUseCase {

    /** Rapor dosyası hazır olduğunda başarı e-postası gönderir. */
    void handleCompleted(ReportCompletedMessage event);

    /** Rapor üretimi başarısız olduğunda hata e-postası gönderir. */
    void handleFailed(ReportFailedMessage event);
}