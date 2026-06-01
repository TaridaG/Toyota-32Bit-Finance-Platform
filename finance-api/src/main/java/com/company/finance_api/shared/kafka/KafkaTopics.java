package com.company.finance_api.shared.kafka;

/** Servisler arası paylaşılan Kafka topic ad sabitleri. */
public final class KafkaTopics {

  private KafkaTopics() {}

  /** Alarm tetiklendi event topic'i. */
  public static final String ALARM_TRIGGERED = "alarm-triggered";

  /** İşlem gerçekleşti event topic'i. */
  public static final String TRANSACTION_EXECUTED = "transaction-executed";

  /** Watchlist'e instrument eklendi event topic'i. */
  public static final String WATCHLIST_ITEM_ADDED = "watchlist.item.added";

  /** Watchlist'ten instrument kaldırıldı event topic'i. */
  public static final String WATCHLIST_ITEM_REMOVED = "watchlist.item.removed";

  /** Dahili kullanıcı silme saga isteği topic'i. */
  public static final String INTERNAL_USER_DELETE_REQUESTED = "internal.user.delete.requested";

  /** Login güvenlik uyarısı e-postası topic'i. */
  public static final String LOGIN_SECURITY_ALERT = "login-security.alert";

  /** Portal bildirim kutusuna sistem mesajı (watchlist digest vb.). */
  public static final String NOTIFICATION_PORTAL_INBOX = "notification.portal.inbox";
}
