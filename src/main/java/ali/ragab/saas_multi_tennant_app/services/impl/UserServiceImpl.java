package ali.ragab.saas_multi_tennant_app.services.impl;


import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.entities.Tenant;
import ali.ragab.saas_multi_tennant_app.entities.TenantStatus;
import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import ali.ragab.saas_multi_tennant_app.exceptions.DuplicateResourceException;
import ali.ragab.saas_multi_tennant_app.exceptions.InvalidRequestException;
import ali.ragab.saas_multi_tennant_app.mappers.UserMapper;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import ali.ragab.saas_multi_tennant_app.requests.UserRequest;
import ali.ragab.saas_multi_tennant_app.responses.UserResponse;
import ali.ragab.saas_multi_tennant_app.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final UserMapper  mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createUser(UserRequest request) {

        final String tenantId = TenantContext.requireTenantId();
        log.info("Creating user for tenant: {}", tenantId);
        if (this.repository.existsByUsername(request.getUsername()))
            throw new DuplicateKeyException("User with username " + request.getUsername() + "already exists");

        if (this.repository.existsByEmail(request.getEmail()))
            throw new DuplicateKeyException("User with email " + request.getEmail() + " already exists");

        if (request.getRole() == UserRole.ROLE_PLATFORM_ADMIN)
            throw new InvalidRequestException("platform admin cannot be selected as a role");

        final User user = this.mapper.toEntity(request);
        user.setTenant(Tenant.builder().id(tenantId).build());
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));
         this.repository.save(user);
         log.info("User with username {} created successfully for tenant {}", user.getUsername(), tenantId);

    }

    @Override
    public void updateUser(String userId, UserRequest request) {
        final String tenantId = TenantContext.requireTenantId();
        log.info("Updating user for tenant: {}", tenantId);

        // validate role (cannot be PLATFORM_ADMIN)
        if (request.getRole() == UserRole.ROLE_PLATFORM_ADMIN) {
            throw new InvalidRequestException("Platform admin role cannot be assigned");
        }

        final User user = loadOwned(userId);

        // check if username is being changed and if it is already taken
        if (!user.getUsername().equals(request.getUsername()) && this.repository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        // check if email is being changed and if it is already taken
        if (!user.getEmail().equals(request.getEmail()) && this.repository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // update user details
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        this.repository.save(user);
        log.info("User updated successfully");

    }

    @Override
    public void deleteUser(String userId) {
        final String tenantId = TenantContext.requireTenantId();
        log.info("Deleting user for tenant: {}", tenantId);

        final User user = loadOwned(userId);

        user.setDeleted(true);
        this.repository.save(user);
        log.info("User deleted successfully");
    }

    @Override
    public UserResponse getUserById(String userId) {
        final String tenantId = TenantContext.requireTenantId();
        log.info("fetching user for tenant: {}", tenantId);

        final User user = loadOwned(userId);
        return this.mapper.toResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        final String tenantId = TenantContext.requireTenantId();
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<User> userPage = this.repository.findAllByTenantId(tenantId, pageRequest);
        final Page<UserResponse> userResponses = userPage.map(this.mapper::toResponse);
        return PageResponse.of(userResponses);
    }

    @Override
    public void enableUser(String userId) {
     final String tenantId = TenantContext.requireTenantId();
        log.info("Enabling user for tenant: {}", tenantId);

        final User user = loadOwned(userId);

        user.setEnabled(true);
        this.repository.save(user);
        log.info("User enabled successfully");
    }

    @Override
    public void disableUser(String userId) {
        final String tenantId = TenantContext.requireTenantId();
        log.info("Disabling user for tenant: {}", tenantId);

        final User user = loadOwned(userId);

        user.setEnabled(false);
        this.repository.save(user);
        log.info("User disabled successfully");

    }

    private User loadOwned(final String userId) {
        final String tenantId = TenantContext.requireTenantId();
        return this.repository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user = this.repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Platform admins have no tenant. Tenant users may only log in while their tenant is ACTIVE.
        if (user.getTenant() != null && user.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw new DisabledException("Tenant is not active");
        }

        return user;
    }
}

