package com.company.marketdataservice.service.historical;

public final class BackfillExecutionContext {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private BackfillExecutionContext() {
    }

    public static void activate() {
        ACTIVE.set(true);
    }

    public static void clear() {
        ACTIVE.remove();
    }

    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }
}
