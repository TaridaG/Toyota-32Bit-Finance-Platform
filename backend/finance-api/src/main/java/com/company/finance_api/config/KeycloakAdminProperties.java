package com.company.finance_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Master-realm admin-cli credentials (dev-friendly). Production: prefer a dedicated
 * confidential client with only {@code manage-users} on the application realm.
 */
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakAdminProperties {

    /**
     * Keycloak base URL without trailing slash (e.g. http://localhost:8085).
     */
    private String serverUrl = "http://localhost:8085";

    private String realm = "finance";

    private Admin admin = new Admin();

    /**
     * Confidential client used only from this API for password grant (custom login UI).
     * Never expose {@code client-secret} to browsers.
     */
    private Portal portal = new Portal();

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Portal getPortal() {
        return portal;
    }

    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    public static class Portal {
        private String clientId = "finance-portal";
        private String clientSecret = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static class Admin {
        private String username = "admin";
        private String password = "";
        /** Always {@code admin-cli} for master-realm password grant. */
        private String clientId = "admin-cli";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
