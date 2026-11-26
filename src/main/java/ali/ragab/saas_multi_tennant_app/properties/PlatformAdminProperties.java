package ali.ragab.saas_multi_tennant_app.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.platform-admin")
public class PlatformAdminProperties {

    private String username;
    private String email;
    private String password;
}
