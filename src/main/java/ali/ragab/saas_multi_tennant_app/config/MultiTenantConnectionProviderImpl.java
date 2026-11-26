package ali.ragab.saas_multi_tennant_app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider<String>, HibernatePropertiesCustomizer {
   //MultiTenantConnectionProvider : an interface from hibernate responsible for  provide and manage database connections for different tenants in a multi-tenant application.

private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.debug("Getting connection for tenant: {}", tenantIdentifier);
        final Connection connection = getAnyConnection();
        try {
            if (tenantIdentifier != null && !tenantIdentifier.equals("public") ) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET search_path TO " + TenantSchemaName.quote(tenantIdentifier) + ", public");
                }
                log.trace("Set search_path to: {}", tenantIdentifier);
            }
            return connection;
        } catch (final SQLException | RuntimeException e) {
            log.error("Error setting search_path for tenant: {}. Closing the leased connection.", tenantIdentifier, e);
            closeQuietly(connection);
            throw e;
        }
    }

    @Override
    public void releaseConnection(final String tenantIdentifier, final Connection connection) throws SQLException {
        boolean reset = false;
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO public");
            reset = true;
        } catch (final SQLException | RuntimeException e) {
            log.error("Failed to reset search_path for tenant: {}. "
                    + "Aborting the connection instead of returning it to the pool.", tenantIdentifier, e);
        }

        if (reset) {
            connection.close();
            return;
        }

        abortQuietly(connection);
    }

    private void closeQuietly(final Connection connection) {
        try {
            connection.close();
        } catch (final SQLException | RuntimeException e) {
            log.error("Failed to close connection after a failed search_path change", e);
        }
    }

    private void abortQuietly(final Connection connection) {
        try {
            connection.abort(Runnable::run);
        } catch (final SQLException | RuntimeException e) {
            log.error("Failed to abort dirty connection; it must not be reused", e);
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(final Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> unwrapType) {
        return null;
    }

    @Override
    public void customize(final Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}
