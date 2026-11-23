package ali.ragab.saas_multi_tennant_app.security;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // --- public ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/error").permitAll()

                        // --- platform ---
                        .requestMatchers("/api/v1/tenants/**").hasRole("PLATFORM_ADMIN")

                        // --- users ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole("COMPANY_ADMIN", "ADMINISTRATOR")
                        .requestMatchers("/api/v1/users/**").hasRole("COMPANY_ADMIN")

                        // --- stock ledger: append-only ---
                        .requestMatchers(HttpMethod.PUT, "/api/v1/stock-mvts/**").denyAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/stock-mvts/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock-mvts/*/reverse").hasAnyRole("COMPANY_ADMIN", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock-mvts").hasAnyRole("COMPANY_ADMIN", "ADMINISTRATOR", "SALES_OPERATOR")

                        // --- tenant business data: قراءة ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/products/**", "/api/v1/stock-mvts/**")
                        .hasAnyRole("COMPANY_ADMIN", "ADMINISTRATOR", "SALES_OPERATOR", "USER")
                        // --- catalog: كتابة ---
                        .requestMatchers("/api/v1/categories/**", "/api/v1/products/**")
                        .hasAnyRole("COMPANY_ADMIN", "ADMINISTRATOR")
                        .anyRequest().denyAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Configure properly in production
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}