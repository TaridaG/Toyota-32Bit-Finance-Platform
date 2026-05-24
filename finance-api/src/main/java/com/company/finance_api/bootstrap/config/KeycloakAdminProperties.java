package com.company.finance_api.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Master-realm admin-cli kimlik bilgileri (dev-friendly). Production'da yalnızca {@code
 * manage-users} yetkili ayrı confidential client tercih edilir.
 */
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakAdminProperties {

  /** Sondaki slash olmadan Keycloak base URL (örn. http://localhost:8085). */
  private String serverUrl = "http://localhost:8085";

  private String realm = "finance";

  private Admin admin = new Admin();

  /**
   * Yalnızca bu API'den password grant için kullanılan confidential client. {@code client-secret}
   * browser'a asla verilmemeli.
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

  /** Portal password grant client kimlik bilgileri. */
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

  /** Master-realm admin-cli password grant kimlik bilgileri. */
  public static class Admin {
    private String username = "admin";
    private String password = "";

    /** Master-realm password grant için her zaman {@code admin-cli}. */
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
