package com.company.notification.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * E-posta şablonlarında kullanılan SMTP gönderici adresi ve portal URL'i ({@code notification.mail.*}).
 */
@ConfigurationProperties(prefix = "notification.mail")
public class NotificationMailProperties {

    private String from = "";
    private String portalPublicUrl = "";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getPortalPublicUrl() {
        return portalPublicUrl;
    }

    public void setPortalPublicUrl(String portalPublicUrl) {
        this.portalPublicUrl = portalPublicUrl;
    }
}
