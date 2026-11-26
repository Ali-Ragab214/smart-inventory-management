package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.TenantResolutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @AfterEach
    void cleanUp() {
        TenantContext.clearCurrentTenant();
    }

    @Test
    void requireTenantReturnsTheCurrentTenant() {
        TenantContext.setCurrentTenant("tenant-a");

        assertEquals("tenant-a", TenantContext.requireTenant());
        assertEquals("tenant-a", TenantContext.requireTenantId());
    }

    @Test
    void requireTenantThrowsWhenContextIsMissing() {
        assertThrows(TenantResolutionException.class, TenantContext::requireTenant);
    }

    @Test
    void clearRemovesTenantAndSchema() {
        TenantContext.setCurrentTenant("tenant-a");
        TenantContext.setCurrentSchema("tenant_a");

        TenantContext.clearCurrentTenant();

        assertNull(TenantContext.getCurrentTenant());
        assertNull(TenantContext.getCurrentSchema());
    }
}
