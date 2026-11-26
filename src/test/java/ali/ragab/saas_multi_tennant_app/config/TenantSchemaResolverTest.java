package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.TenantNotActiveException;
import ali.ragab.saas_multi_tennant_app.exceptions.TenantResolutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSchemaResolverTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TenantSchemaResolver resolver;

    @Test
    void resolvesActiveTenantToItsSchema() {
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenReturn(Map.<String, Object>of("company_code", "acme", "status", "ACTIVE"));

        assertEquals("tenant_acme", resolver.resolveTenantSchema("tenant-id"));
    }

    @Test
    void throwsWhenTenantIsUnknownOrDeleted() {
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(TenantResolutionException.class,
                () -> resolver.resolveTenantSchema("unknown-id"));
    }

    @Test
    void throwsWhenTenantIsInactive() {
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenReturn(Map.<String, Object>of("company_code", "acme", "status", "INACTIVE"));

        assertThrows(TenantNotActiveException.class,
                () -> resolver.resolveTenantSchema("tenant-id"));
    }

    @Test
    void throwsWhenTenantIsSuspended() {
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenReturn(Map.<String, Object>of("company_code", "acme", "status", "SUSPENDED"));

        assertThrows(TenantNotActiveException.class,
                () -> resolver.resolveTenantSchema("tenant-id"));
    }

    @Test
    void throwsWhenDatabaseLookupFails() {
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(TenantResolutionException.class,
                () -> resolver.resolveTenantSchema("tenant-id"));
    }

    @Test
    void throwsWhenTenantIdIsMissing() {
        assertThrows(TenantResolutionException.class, () -> resolver.resolveTenantSchema(null));
        assertThrows(TenantResolutionException.class, () -> resolver.resolveTenantSchema(" "));
    }

    @Test
    void throwsWhenStoredCompanyCodeIsInvalid() {
        when(jdbcTemplate.queryForMap(anyString(), any()))
                .thenReturn(Map.<String, Object>of("company_code", "bad-code!", "status", "ACTIVE"));

        assertThrows(TenantResolutionException.class,
                () -> resolver.resolveTenantSchema("tenant-id"));
    }
}
