package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.entities.Tenant;
import ali.ragab.saas_multi_tennant_app.entities.TenantStatus;
import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import ali.ragab.saas_multi_tennant_app.mappers.TenantMapper;
import ali.ragab.saas_multi_tennant_app.repositories.TenantRepository;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import ali.ragab.saas_multi_tennant_app.requests.RegisterTenantRequest;
import ali.ragab.saas_multi_tennant_app.responses.TenantResponse;
import ali.ragab.saas_multi_tennant_app.services.ProvisioningService;
import ali.ragab.saas_multi_tennant_app.services.TenantService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {
     private final TenantRepository repository;
     private final TenantMapper mapper;
     private final PasswordEncoder passwordEncoder;
     private final UserRepository userRepository;
     private final ProvisioningService provisioningService;



    @Override
    public void registerTenant(RegisterTenantRequest request) {
        //first checking if the tenant already exist by companyCode
        if(this.repository.ExistsByCompanyCode(request.getCompanyCode())){
            throw new DuplicateKeyException("Tenant with company code " + request.getCompanyCode() + " already exists");
        }
        if(this.repository.existsByEmail(request.getEmail())){
            throw new DuplicateKeyException("Tenant with email " + request.getEmail() + " already exists");
        }

        final Tenant tenant = this.mapper.toEntity(request);
        tenant.setAdminPassword(this.passwordEncoder.encode(request.getAdminPassword()));
        tenant.setStatus(TenantStatus.PENDING);


    }

    @Override
    public void approveTenant(String tenantId) {
        //check if the tenant exists
        final Tenant tenant = this.repository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant with id " + tenantId + " not found"));

        tenant.setStatus(TenantStatus.ACTIVE);
        this.repository.save(tenant);

        try {
            //provision the schema for the tenant
            this.provisioningService.provisionTenant(tenant);
        }
        catch (final Exception e ) {
        rollBackTenantStatus(tenant);
    }

    }

    private void rollBackTenantStatus(Tenant tenant) {
        tenant.setStatus(TenantStatus.PENDING);
        this.repository.save(tenant);
    }

    @Override
    public void activateTenant(String tenantId) {
        final Tenant tenant = this.repository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant with id " + tenantId + " not found"));
    if(tenant.getStatus() != TenantStatus.PENDING)
        throw new IllegalStateException("Tenant with id " + tenantId + " is not in PENDING state");

    tenant.setStatus(TenantStatus.ACTIVE);
    this.repository.save(tenant);

    }

    @Override
    public void deactivateTenant(String tenantId) {
        final Tenant tenant = this.repository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant with id " + tenantId + " not found"));
        if(tenant.getStatus() != TenantStatus.ACTIVE)
            throw new IllegalStateException("Tenant with id " + tenantId + " is not in ACTIVE state");

        tenant.setStatus(TenantStatus.INACTIVE);
        this.repository.save(tenant);

    }

    @Override
    public void suspendTenant(String tenantId) {
        final Tenant tenant = this.repository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant with id " + tenantId + " not found"));
        if(tenant.getStatus() != TenantStatus.ACTIVE)
            throw new IllegalStateException("Tenant with id " + tenantId + " is not in ACTIVE state");

        tenant.setStatus(TenantStatus.SUSPENDED);
        this.repository.save(tenant);

    }

    @Override
    public PageResponse<TenantResponse> findAll(final int page, final int size) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<Tenant> tenants = this.repository.findAll(pageRequest);
        final Page<TenantResponse> tenantResponses = tenants.map(this.mapper::toResponse);
        return PageResponse.of(tenantResponses);
    }


    private void createAdminUserForTenant(final Tenant tenant) {
        final var adminUser = userRepository.findByUsername(tenant.getAdminUserName());
        if (adminUser.isPresent()) {
            throw new DuplicateKeyException("Admin user with username " + tenant.getAdminUserName() + " already exists");
        }

        final var user = User.builder()
                .username(tenant.getAdminUserName())
                .email(tenant.getAdminEmail())
                .firstName((extractFirstName(tenant.getAdminFullName())))
                .lastName((extractLastName(tenant.getAdminFullName())))
                .password(tenant.getAdminPassword())
                .role(UserRole.ROLE_COMPANY_ADMIN)
                .tenant(tenant)
                .deleted(false)
                .build();

        userRepository.save(user);
        log.info("Admin user with username {} created successfully for tenant {}", user.getUsername(), tenant.getId());
    }
    private String extractFirstName(final String adminFullName)
    {
        return adminFullName.split(" ")[0];
    }

    private String extractLastName(final String adminFullName)
    {
        return adminFullName.split(" ").length > 1 ? adminFullName.split(" ")[1] : adminFullName;
    }
}
