package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.TenantNotActiveException;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantSchemaResolverCacheTest {

    @Test
    void cachesResolvedSchemaUntilTheEntryIsEvicted() {
        final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenReturn(Map.<String, Object>of("company_code", "acme", "status", "ACTIVE"));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(JdbcTemplate.class, () -> jdbcTemplate);
            context.register(CacheConfig.class, TenantSchemaResolver.class);
            context.refresh();

            final TenantSchemaResolver resolver = context.getBean(TenantSchemaResolver.class);
            final CacheManager cacheManager = context.getBean(CacheManager.class);

            assertEquals("tenant_acme", resolver.resolveTenantSchema("tenant-id"));
            assertEquals("tenant_acme", resolver.resolveTenantSchema("tenant-id"));
            verify(jdbcTemplate, times(1)).queryForMap(anyString(), any());

            when(jdbcTemplate.queryForMap(anyString(), any()))
                    .thenReturn(Map.<String, Object>of("company_code", "acme", "status", "SUSPENDED"));

            assertEquals("tenant_acme", resolver.resolveTenantSchema("tenant-id"));
            verify(jdbcTemplate, times(1)).queryForMap(anyString(), any());

            cacheManager.getCache("tenantSchemas").evict("tenant-id");

            assertThrows(TenantNotActiveException.class,
                    () -> resolver.resolveTenantSchema("tenant-id"));
            verify(jdbcTemplate, times(2)).queryForMap(anyString(), any());
        }
    }
}
