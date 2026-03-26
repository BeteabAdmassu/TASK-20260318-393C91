package com.mindflow.security.common;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "default";

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT_TENANT.set(DEFAULT_TENANT);
            return;
        }
        CURRENT_TENANT.set(tenantId.trim().toLowerCase());
    }

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return tenant == null || tenant.isBlank() ? DEFAULT_TENANT : tenant;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
