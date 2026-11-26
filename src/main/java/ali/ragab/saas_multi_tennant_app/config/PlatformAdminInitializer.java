package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import ali.ragab.saas_multi_tennant_app.properties.PlatformAdminProperties;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAdminProperties properties;

    @Override
    public void run(final ApplicationArguments args) {
        if (this.userRepository.existsByRole(UserRole.ROLE_PLATFORM_ADMIN)) {
            log.debug("A platform admin already exists; nothing to initialize");
            return;
        }

        final String username = nonBlank(this.properties.getUsername(), "app.platform-admin.username");
        final String email = nonBlank(this.properties.getEmail(), "app.platform-admin.email");
        final String password = nonBlank(this.properties.getPassword(), "app.platform-admin.password");

        final User platformAdmin = User.builder()
                .username(username)
                .email(email)
                .password(this.passwordEncoder.encode(password))
                .firstName(username)
                .lastName("Admin")
                .role(UserRole.ROLE_PLATFORM_ADMIN)
                .enabled(true)
                .build();
        this.userRepository.save(platformAdmin);
        log.info("Created initial platform admin with username: {}", username);
    }

    private static String nonBlank(final String value, final String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing or invalid platform admin configuration: " + property
                            + " must be set to a non-blank value");
        }
        return value.trim();
    }
}
