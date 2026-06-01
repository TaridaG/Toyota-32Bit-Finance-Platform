package com.company.notification.insight.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Paylaşılan {@code finance} veritabanındaki {@code users} satırından bildirim alanlarını okur.
 */
@Repository
public class FinanceUserNotificationReader {

    private final JdbcTemplate jdbcTemplate;

    public FinanceUserNotificationReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<FinanceUserNotificationProfile> findById(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        List<FinanceUserNotificationProfile> rows =
                jdbcTemplate.query(
                        """
                                SELECT id, email, preferred_locale, active, notify_watchlist_alerts
                                FROM users
                                WHERE id = ?
                                """,
                        (rs, rowNum) -> mapRow(rs),
                        userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private static FinanceUserNotificationProfile mapRow(ResultSet rs) throws SQLException {
        return new FinanceUserNotificationProfile(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("preferred_locale"),
                rs.getBoolean("active"),
                rs.getBoolean("notify_watchlist_alerts"));
    }
}
