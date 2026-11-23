package ali.ragab.saas_multi_tennant_app.services;

public interface TenantMigrationService {

    /**
     * Applies all pending tenant migrations to a single tenant schema.
     */
    void migrateSchema(final String schemaName);

    /**
     * Applies pending tenant migrations to every active tenant schema.
     */
    void migrateAllTenants();
}
