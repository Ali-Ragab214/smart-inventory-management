package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.config.TenantSchemaName;
import ali.ragab.saas_multi_tennant_app.services.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

//this class is responsible for managing database migrations for tenant schemas in a multi-tenant application.
// It uses Flyway to apply migrations to each tenant's schema, ensuring that the database structure is up-to-date and consistent across all tenants.
//عندنا كل تينانت ليه سكيما وكل سكيما فيها نفس الجداول فلما نعدل علي الجداول لازم التعديل يسمع في كل السكيماز بتاعه التينانتس
//Flyway بطبيعته بيشتغل على schema واحد، فالكلاس دا بيشغّله مرة لكل شركة.
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationServiceImpl implements TenantMigrationService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void migrateSchema(final String schemaName) {
        log.info("Running tenant migrations for schema: {}", schemaName);

        Flyway.configure()
                .dataSource(this.dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load()
                .migrate();

        log.info("Tenant migrations completed for schema: {}", schemaName);
    }

    @Override
    public void migrateAllTenants() {
        final List<String> companyCodes;
        try {
            companyCodes = this.jdbcTemplate.queryForList(
                    "select company_code from public.tenants where deleted = false and status = 'ACTIVE'",
                    String.class);
        } catch (final Exception e) {
            log.error("Could not list active tenants for schema migration", e);
            return;
        }

        for (final String companyCode : companyCodes) {
            try {
                this.migrateSchema(TenantSchemaName.fromCompanyCode(companyCode));
            } catch (final Exception e) {
                log.error("Tenant schema migration failed for company code: {}", companyCode, e);
            }
        }
    }
}
