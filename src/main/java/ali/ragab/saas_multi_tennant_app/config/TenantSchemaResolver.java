package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.entities.TenantStatus;
import ali.ragab.saas_multi_tennant_app.exceptions.TenantNotActiveException;
import ali.ragab.saas_multi_tennant_app.exceptions.TenantResolutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaResolver {

    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = "tenantSchemas", key = "#tenantId")
    public String resolveTenantSchema(final String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantResolutionException("Tenant id is required to resolve a schema");
        }

        final Map<String, Object> tenant;
        try {
            tenant = this.jdbcTemplate.queryForMap(
                    "SELECT company_code, status FROM public.tenants WHERE id = ? AND deleted = false",
                    tenantId);
        } catch (final EmptyResultDataAccessException e) {
            throw new TenantResolutionException("Tenant not found or deleted: " + tenantId, e);
        } catch (final DataAccessException e) {
            throw new TenantResolutionException("Failed to resolve tenant: " + tenantId, e);
        }

        final String companyCode = (String) tenant.get("company_code");
        final String status = (String) tenant.get("status");

        if (companyCode == null || companyCode.isBlank()) {
            throw new TenantResolutionException("Tenant has no company code: " + tenantId);
        }
        if (!TenantStatus.ACTIVE.name().equals(status)) {
            throw new TenantNotActiveException("Tenant is not active: " + tenantId);
        }

        try {
            final String schemaName = TenantSchemaName.fromCompanyCode(companyCode);
            log.debug("Resolved Tenant schema: {} for tenant: {}", schemaName, tenantId);
            return schemaName;
        } catch (final RuntimeException e) {
            throw new TenantResolutionException("Invalid tenant schema for tenant: " + tenantId, e);
        }
    }
}
