package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.mappers.UserMapper;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import ali.ragab.saas_multi_tennant_app.requests.UserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
