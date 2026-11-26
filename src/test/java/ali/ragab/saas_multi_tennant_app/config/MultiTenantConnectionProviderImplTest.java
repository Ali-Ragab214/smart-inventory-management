package ali.ragab.saas_multi_tennant_app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiTenantConnectionProviderImplTest {

    @Test
    void resetsSearchPathAndReusesTheSamePhysicalConnection() throws Exception {
        final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        final String schemaA = "step4_a_" + suffix;
        final String schemaB = "step4_b_" + suffix;

        final HikariDataSource dataSource = new HikariDataSource(hikariConfig());
        try {
            execute(dataSource, "CREATE SCHEMA " + schemaA);
            execute(dataSource, "CREATE SCHEMA " + schemaB);

            final MultiTenantConnectionProviderImpl provider =
                    new MultiTenantConnectionProviderImpl(dataSource);

            final int pidA;
            final Connection tenantA = provider.getConnection(schemaA);
            try {
                assertEquals(schemaA, currentSchema(tenantA));
                pidA = backendPid(tenantA);
            } finally {
                provider.releaseConnection(schemaA, tenantA);
            }

            final Connection afterRelease = provider.getAnyConnection();
            try {
                assertEquals("public", currentSchema(afterRelease));
                assertEquals(pidA, backendPid(afterRelease));
            } finally {
                provider.releaseAnyConnection(afterRelease);
            }

            final Connection tenantB = provider.getConnection(schemaB);
            try {
                assertEquals(schemaB, currentSchema(tenantB));
                assertEquals(pidA, backendPid(tenantB));
            } finally {
                provider.releaseConnection(schemaB, tenantB);
            }

            final Connection afterSecondRelease = provider.getAnyConnection();
            try {
                assertEquals("public", currentSchema(afterSecondRelease));
                assertEquals(pidA, backendPid(afterSecondRelease));
            } finally {
                provider.releaseAnyConnection(afterSecondRelease);
            }
        } finally {
            executeQuietly(dataSource, "DROP SCHEMA IF EXISTS " + schemaA + " CASCADE");
            executeQuietly(dataSource, "DROP SCHEMA IF EXISTS " + schemaB + " CASCADE");
            dataSource.close();
        }
    }

    @Test
    void resetsAndClosesConnectionOnSuccessfulRelease() throws Exception {
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        new MultiTenantConnectionProviderImpl(mock(DataSource.class))
                .releaseConnection("tenant_a", connection);

        verify(statement).execute("SET search_path TO public");
        verify(connection).close();
        verify(connection, never()).abort(any(Executor.class));
    }

    @Test
    void abortsConnectionWhenSearchPathResetFails() throws Exception {
        final Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenThrow(new SQLException("reset failed"));

        new MultiTenantConnectionProviderImpl(mock(DataSource.class))
                .releaseConnection("tenant_a", connection);

        verify(connection).abort(any(Executor.class));
        verify(connection, never()).close();
    }

    @Test
    void closesLeasedConnectionWhenSearchPathSetFails() throws Exception {
        final DataSource dataSource = mock(DataSource.class);
        final Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenThrow(new SQLException("set failed"));

        final MultiTenantConnectionProviderImpl provider =
                new MultiTenantConnectionProviderImpl(dataSource);

        assertThrows(SQLException.class, () -> provider.getConnection("tenant_a"));
        verify(connection).close();
    }

    @Test
    void closesLeasedConnectionWhenTenantIdentifierIsInvalid() throws Exception {
        final DataSource dataSource = mock(DataSource.class);
        final Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        final MultiTenantConnectionProviderImpl provider =
                new MultiTenantConnectionProviderImpl(dataSource);

        assertThrows(RuntimeException.class, () -> provider.getConnection(" "));
        verify(connection).close();
    }

    @Test
    void doesNotChangeSearchPathForPublicTenant() throws Exception {
        final DataSource dataSource = mock(DataSource.class);
        final Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        final MultiTenantConnectionProviderImpl provider =
                new MultiTenantConnectionProviderImpl(dataSource);

        assertSame(connection, provider.getConnection("public"));
        verify(connection, never()).createStatement();
    }

    private HikariConfig hikariConfig() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + env("DB_HOST", "localhost") + ":"
                + env("DB_PORT", "5434") + "/" + env("DB_NAME", "saas-app-db"));
        config.setUsername(env("DB_USER", "postgres"));
        config.setPassword(env("DB_PASSWORD", "postgres"));
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setPoolName("step4-provider-test");
        return config;
    }

    private String env(final String name, final String fallback) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private void execute(final DataSource dataSource, final String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void executeQuietly(final DataSource dataSource, final String sql) {
        try {
            execute(dataSource, sql);
        } catch (final SQLException ignored) {
            // best-effort cleanup
        }
    }

    private String currentSchema(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT current_schema()")) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private int backendPid(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT pg_backend_pid()")) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
