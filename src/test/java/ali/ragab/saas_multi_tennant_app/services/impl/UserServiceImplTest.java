package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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
