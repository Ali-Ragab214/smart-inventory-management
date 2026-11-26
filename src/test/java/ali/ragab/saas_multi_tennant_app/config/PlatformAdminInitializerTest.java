package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import ali.ragab.saas_multi_tennant_app.properties.PlatformAdminProperties;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final PlatformAdminProperties properties = new PlatformAdminProperties();
    private PlatformAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        this.initializer = new PlatformAdminInitializer(this.userRepository, this.passwordEncoder, this.properties);
    }

    @Test
    void createsPlatformAdminWhenNoneExists() {
        this.properties.setUsername("platform-admin");
        this.properties.setEmail("platform-admin@example.com");
        this.properties.setPassword("secret123");
        when(this.userRepository.existsByRole(UserRole.ROLE_PLATFORM_ADMIN)).thenReturn(false);
        when(this.passwordEncoder.encode("secret123")).thenReturn("encoded-password");

        this.initializer.run(null);

        final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).save(captor.capture());
        final User created = captor.getValue();
        assertEquals("platform-admin", created.getUsername());
        assertEquals("platform-admin@example.com", created.getEmail());
        assertEquals("encoded-password", created.getPassword());
        assertEquals(UserRole.ROLE_PLATFORM_ADMIN, created.getRole());
        assertEquals(true, created.isEnabled());
    }

    @Test
    void doesNotCreateWhenPlatformAdminExists() {
        when(this.userRepository.existsByRole(UserRole.ROLE_PLATFORM_ADMIN)).thenReturn(true);

        this.initializer.run(null);

        verify(this.userRepository, never()).save(any());
        verify(this.passwordEncoder, never()).encode(any());
    }

    @Test
    void failsWhenUsernameIsMissing() {
        this.properties.setEmail("platform-admin@example.com");
        this.properties.setPassword("secret123");
        when(this.userRepository.existsByRole(UserRole.ROLE_PLATFORM_ADMIN)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> this.initializer.run(null));
        verify(this.userRepository, never()).save(any());
    }

    @Test
    void failsWhenPasswordIsBlank() {
        this.properties.setUsername("platform-admin");
        this.properties.setEmail("platform-admin@example.com");
        this.properties.setPassword("  ");
        when(this.userRepository.existsByRole(UserRole.ROLE_PLATFORM_ADMIN)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> this.initializer.run(null));
        verify(this.passwordEncoder, never()).encode(any());
    }
}
