package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.TenantResolutionException;

public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private static final ThreadLocal<String> CURRENT_SCHEMA  = new ThreadLocal<>();


    public static void setCurrentTenant(final String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static String requireTenant() {
        final String tenantId = CURRENT_TENANT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantResolutionException("Tenant context is missing");
        }
        return tenantId;
    }

    public static String requireTenantId() {
        return requireTenant();
    }

    public static void setCurrentSchema(final String tenant) {
        CURRENT_SCHEMA.set(tenant);
    }

    public static String getCurrentSchema() {
        return CURRENT_SCHEMA.get();
    }


    public static void clearCurrentTenant() {

        CURRENT_TENANT.remove();
        CURRENT_SCHEMA.remove();
    }



}
