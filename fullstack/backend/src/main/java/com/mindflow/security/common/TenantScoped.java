package com.mindflow.security.common;

public interface TenantScoped {
    String getTenantId();
    void setTenantId(String tenantId);
}
