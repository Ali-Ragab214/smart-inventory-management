package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.config.TenantSchemaName;
import ali.ragab.saas_multi_tennant_app.entities.Tenant;
import ali.ragab.saas_multi_tennant_app.exceptions.TenantProvisioningException;
import ali.ragab.saas_multi_tennant_app.services.ProvisioningService;
import ali.ragab.saas_multi_tennant_app.services.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisioningServiceImpl implements ProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final TenantMigrationService tenantMigrationService;

    @Override
    public void provisionTenant(final Tenant tenant) {
        final String schemaName = TenantSchemaName.fromCompanyCode(tenant.getCompanyCode());
        boolean schemaCreated = false;

        try {
            log.info("Provisioning tenant: {} (schema: {})", tenant.getCompanyName(), schemaName);

            schemaCreated = createSchema(schemaName);
            log.info("Schema {} for tenant: {}", schemaCreated ? "created" : "already exists", schemaName);

            this.tenantMigrationService.migrateSchema(schemaName);
            initializeDefaultData(schemaName, tenant);
        } catch (final Exception e) {
            log.error("Error provisioning tenant: {} (schema: {})", tenant.getCompanyName(), schemaName, e);

            if (schemaCreated) {
                try {
                    dropSchema(schemaName);
                } catch (final Exception exp) {
                    log.error("Error dropping schema: {}", schemaName, exp);
                }
            }

            throw new TenantProvisioningException("Error provisioning tenant: " + tenant.getCompanyName());
        }
    }

    /**
     * @return {@code true} only when this call created the schema. A pre-existing
     * schema must never be dropped by the failure handler.
     */
    private boolean createSchema(final String schemaName) {
        final Integer existing = this.jdbcTemplate.queryForObject(
                "select count(*) from pg_namespace where nspname = ?",
                Integer.class,
                schemaName);

        if (existing != null && existing > 0) {
            return false;
        }

        this.jdbcTemplate.execute("CREATE SCHEMA " + TenantSchemaName.quote(schemaName));
        return true;
    }

    private void dropSchema(final String schemaName) {
        this.jdbcTemplate.execute(
                "DROP SCHEMA IF EXISTS " + TenantSchemaName.quote(schemaName) + " CASCADE");
    }

    private void initializeDefaultData(final String schemaName, final Tenant tenant) {
        // Default tenant data can be inserted here after the tenant migrations have run.
    }
}
