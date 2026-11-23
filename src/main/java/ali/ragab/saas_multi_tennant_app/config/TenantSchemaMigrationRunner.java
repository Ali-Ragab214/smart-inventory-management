package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.services.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaMigrationRunner implements ApplicationRunner {

    private final TenantMigrationService tenantMigrationService;

    @Value("${app.tenant-migrations.migrate-on-startup:true}")
    private boolean migrateOnStartup;

    @Override
    public void run(final ApplicationArguments args) {
        if (!this.migrateOnStartup) {
            log.info("Tenant schema migration on startup is disabled");
            return;
        }

        this.tenantMigrationService.migrateAllTenants();
    }
}
