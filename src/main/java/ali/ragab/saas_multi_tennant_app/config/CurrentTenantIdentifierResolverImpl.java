package ali.ragab.saas_multi_tennant_app.config;

import ali.ragab.saas_multi_tennant_app.exceptions.TenantResolutionException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    private static final String PLATFORM_SCHEMA = "public";
    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    @Override
    public String resolveCurrentTenantIdentifier() {
        final String schema = TenantContext.getCurrentSchema();
        if (schema != null && !schema.isBlank()) {
            log.trace("Resolving Current tenant schema identifier: {}", schema);
            return schema;
        }

        final String tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null && !tenantId.isBlank()) {
            throw new TenantResolutionException(
                    "Tenant context has no resolved schema for tenant: " + tenantId);
        }

        if (isPlatformContext()) {
            log.trace("No tenant context; using public schema for platform operation");
            return PLATFORM_SCHEMA;
        }

        throw new TenantResolutionException("Tenant context is missing for a business operation");
    }

    private boolean isPlatformContext() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> PLATFORM_ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);

    }
}
