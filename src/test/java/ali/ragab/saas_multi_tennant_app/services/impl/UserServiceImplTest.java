package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.entities.Tenant;
import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import ali.ragab.saas_multi_tennant_app.exceptions.ConflictException;
import ali.ragab.saas_multi_tennant_app.exceptions.InvalidRequestException;
import ali.ragab.saas_multi_tennant_app.mappers.UserMapper;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import ali.ragab.saas_multi_tennant_app.requests.UserRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;
    @Mock
    private UserMapper mapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void clearTenant() {
        TenantContext.clearCurrentTenant();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserByIdFailsWhenTenantIsMissing() {
        TenantContext.clearCurrentTenant();

        assertThrows(IllegalStateException.class, () -> this.userService.getUserById("user-id"));
    }

    @Test
    void createUserFailsWhenTenantIsMissing() {
        TenantContext.clearCurrentTenant();
        final UserRequest request = UserRequest.builder()
                .username("new-user")
                .email("new-user@acme.test")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .build();

        assertThrows(IllegalStateException.class, () -> this.userService.createUser(request));
    }

    @Test
    void getUserByIdFailsForUserFromAnotherTenant() {
        TenantContext.setCurrentTenant("tenant-a");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("other-user", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> this.userService.getUserById("other-user"));
    }

    @Test
    void updateUserFailsForUserFromAnotherTenant() {
        TenantContext.setCurrentTenant("tenant-a");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("other-user", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> this.userService.updateUser("other-user", userRequest()));
    }

    @Test
    void deleteUserFailsForUserFromAnotherTenant() {
        TenantContext.setCurrentTenant("tenant-a");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("other-user", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> this.userService.deleteUser("other-user"));
    }

    @Test
    void enableUserFailsForUserFromAnotherTenant() {
        TenantContext.setCurrentTenant("tenant-a");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("other-user", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> this.userService.enableUser("other-user"));
    }

    @Test
    void disableUserFailsForUserFromAnotherTenant() {
        TenantContext.setCurrentTenant("tenant-a");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("other-user", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> this.userService.disableUser("other-user"));
    }

    @Test
    void adminCannotDisableSelf() {
        TenantContext.setCurrentTenant("tenant-a");
        authenticate("admin-id");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("admin-id", "tenant-a"))
                .thenReturn(Optional.of(companyAdmin("admin-id")));

        assertThrows(ConflictException.class, () -> this.userService.disableUser("admin-id"));
    }

    @Test
    void adminCannotDeleteSelf() {
        TenantContext.setCurrentTenant("tenant-a");
        authenticate("admin-id");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("admin-id", "tenant-a"))
                .thenReturn(Optional.of(companyAdmin("admin-id")));

        assertThrows(ConflictException.class, () -> this.userService.deleteUser("admin-id"));
    }

    @Test
    void cannotDisableLastActiveCompanyAdmin() {
        TenantContext.setCurrentTenant("tenant-a");
        authenticate("other-admin-id");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("admin-id", "tenant-a"))
                .thenReturn(Optional.of(companyAdmin("admin-id")));
        when(this.repository.countActiveByTenantIdAndRole("tenant-a", UserRole.ROLE_COMPANY_ADMIN))
                .thenReturn(1L);

        assertThrows(ConflictException.class, () -> this.userService.disableUser("admin-id"));
    }

    @Test
    void cannotDeleteLastActiveCompanyAdmin() {
        TenantContext.setCurrentTenant("tenant-a");
        authenticate("other-admin-id");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("admin-id", "tenant-a"))
                .thenReturn(Optional.of(companyAdmin("admin-id")));
        when(this.repository.countActiveByTenantIdAndRole("tenant-a", UserRole.ROLE_COMPANY_ADMIN))
                .thenReturn(1L);

        assertThrows(ConflictException.class, () -> this.userService.deleteUser("admin-id"));
    }

    @Test
    void cannotDemoteLastActiveCompanyAdmin() {
        TenantContext.setCurrentTenant("tenant-a");
        authenticate("other-admin-id");
        when(this.repository.findByIdAndTenantIdAndDeletedFalse("admin-id", "tenant-a"))
                .thenReturn(Optional.of(companyAdmin("admin-id")));
        when(this.repository.countActiveByTenantIdAndRole("tenant-a", UserRole.ROLE_COMPANY_ADMIN))
                .thenReturn(1L);

        assertThrows(ConflictException.class, () -> this.userService.updateUser("admin-id", userRequest()));
    }

    @Test
    void platformAdminRoleIsStillRejectedOnCreate() {
        TenantContext.setCurrentTenant("tenant-a");
        final UserRequest request = UserRequest.builder()
                .username("new-user")
                .email("new-user@acme.test")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.ROLE_PLATFORM_ADMIN)
                .build();

        assertThrows(InvalidRequestException.class, () -> this.userService.createUser(request));
    }

    @Test
    void platformAdminRoleIsStillRejectedOnUpdate() {
        TenantContext.setCurrentTenant("tenant-a");

        assertThrows(InvalidRequestException.class, () -> this.userService.updateUser("user-id",
                UserRequest.builder()
                        .username("new-user")
                        .email("new-user@acme.test")
                        .password("password123")
                        .firstName("New")
                        .lastName("User")
                        .role(UserRole.ROLE_PLATFORM_ADMIN)
                        .build()));
    }

    private void authenticate(final String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private User companyAdmin(final String id) {
        return User.builder()
                .id(id)
                .username("admin")
                .email("admin@acme.test")
                .password("encoded")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ROLE_COMPANY_ADMIN)
                .enabled(true)
                .deleted(false)
                .tenant(Tenant.builder().id("tenant-a").build())
                .build();
    }

    private UserRequest userRequest() {
        return UserRequest.builder()
                .username("updated-user")
                .email("updated-user@acme.test")
                .password("password123")
                .firstName("Updated")
                .lastName("User")
                .role(UserRole.ROLE_USER)
                .build();
    }
}
