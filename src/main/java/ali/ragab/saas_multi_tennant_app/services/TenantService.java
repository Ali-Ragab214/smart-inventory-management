package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.RegisterTenantRequest;
import ali.ragab.saas_multi_tennant_app.responses.TenantResponse;

public interface TenantService {
    void registerTenant(final RegisterTenantRequest request);

    void approveTenant(final String tenantId);

    void activateTenant(final String tenantId);

    void deactivateTenant(final String tenantId);

    void suspendTenant(final String tenantId);

    PageResponse<TenantResponse> findAll(final int page, final int size);
}
