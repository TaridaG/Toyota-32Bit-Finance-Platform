package com.company.finance_api.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** AdminPortalRosterCounters — JPA domain entity (admin portal roster counters). */
@Entity
@Table(name = "admin_portal_roster_counters")
public class AdminPortalRosterCounters {

  public static final short SINGLETON_ID = 1;

  @Id private Short id = SINGLETON_ID;

  @Column(name = "deleted_accounts_total", nullable = false)
  private long deletedAccountsTotal;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AdminPortalRosterCounters() {}

  public static AdminPortalRosterCounters initial() {
    AdminPortalRosterCounters row = new AdminPortalRosterCounters();
    row.id = SINGLETON_ID;
    row.deletedAccountsTotal = 0L;
    row.updatedAt = Instant.now();
    return row;
  }

  public long getDeletedAccountsTotal() {
    return deletedAccountsTotal;
  }

  public void incrementDeletedAccounts() {
    this.deletedAccountsTotal++;
    this.updatedAt = Instant.now();
  }
}
