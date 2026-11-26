package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.TenantResolutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentTenantIdentifierResolverImplTest {

    private final CurrentTenantIdentifierResolverImpl resolver = new CurrentTenantIdentifierResolverImpl();

    @AfterEach
    void cleanUp() {
        TenantContext.clearCurrentTenant();
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsResolvedSchemaWhenPresent() {
        TenantContext.setCurrentTenant("tenant-id");
        TenantContext.setCurrentSchema("tenant_acme");

        assertEquals("tenant_acme", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void throwsWhenTenantContextHasNoResolvedSchema() {
        TenantContext.setCurrentTenant("tenant-id");

        assertThrows(TenantResolutionException.class, resolver::resolveCurrentTenantIdentifier);
    }

    @Test
    void returnsPublicForAnonymousPlatformOperation() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertEquals("public", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void returnsPublicForPlatformAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))));

        assertEquals("public", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void throwsForAuthenticatedTenantUserWithoutTenantContext() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThrows(TenantResolutionException.class, resolver::resolveCurrentTenantIdentifier);
    }
}
