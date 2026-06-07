package com.company.marketdataservice.history.infrastructure.orchestration;
/**
 * `geçmiş veri ve backfill` backfill/ingestion orchestration bileşeni.
 */
public final class BackfillExecutionContext {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> BOOTSTRAP = ThreadLocal.withInitial(() -> false);

    private BackfillExecutionContext() {
    }

    /**
     * Mevcut thread'de backfill execution context'ini aktifleştirir.
         */
    public static void activate() {
        ACTIVE.set(true);
    }

    public static void activateBootstrap() {
        ACTIVE.set(true);
        BOOTSTRAP.set(true);
    }

    /**
     * Thread-local backfill execution bayraklarını temizler.
         */
    public static void clear() {
        ACTIVE.remove();
        BOOTSTRAP.remove();
    }

    /**
     * Durum kontrolü yapar.
         * @return işlem sonucu
         */
    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    public static boolean isBootstrap() {
        return Boolean.TRUE.equals(BOOTSTRAP.get());
    }
}
