package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.entities.Tenant;
import ali.ragab.saas_multi_tennant_app.exceptions.TenantProvisioningException;
import ali.ragab.saas_multi_tennant_app.services.ProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;



@Service
@RequiredArgsConstructor
@Slf4j
public class  ProvisioningServiceImpl implements ProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void provisionTenant(Tenant tenant) {
        final String schemaName = "tenant_" + tenant.getCompanyCode().toLowerCase();
        try {
           log.info("Provisioning tenant: {} (schema: {})",tenant.getCompanyName(),schemaName);
            // Create schema for the tenant
            createSchema(schemaName);
            log.info("Schema created for tenant: {}", schemaName);
            //run the flyway migration for the new schema
            runTenantMigration(schemaName);
            //initialize the def data
            initializeDefaultData(schemaName,tenant);

        }
        catch(final Exception e) {
            log.error("Error provisioning tenant: {} (schema: {})", tenant.getCompanyName(), schemaName, e);
            //rollback to drop the schema creation


            try {
                dropSchema(schemaName);
            } catch (final Exception exp) {
                log.error("Error dropping schema: {}", schemaName, exp);
            }
            throw new TenantProvisioningException("Error provisioning tenant: " + tenant.getCompanyName());
        }

    }

    private void dropSchema(String schemaName) {
        final String sql = String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName);
        this.jdbcTemplate.execute(sql);
    }


    private void initializeDefaultData(String schemaName, Tenant tenant) {


    }

    private void runTenantMigration(String schemaName) {
        log.info("Running Flyway migration for schema: {}", schemaName);
        final Flyway tenantFlyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
        log.info("Flyway migration configured for schema: {}", schemaName);
        tenantFlyway.migrate();
        log.info("Flyway migration completed for schema: {}", schemaName);

    }

    private void createSchema(String schemaName) {
        final String sql = String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName);
      this.jdbcTemplate.execute(sql);
    }
}
