package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.entities.Tenant;

public interface ProvisioningService {
    void provisionTenant(final Tenant tenant);
}
