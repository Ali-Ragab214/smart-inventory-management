package ali.ragab.saas_multi_tennant_app.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PgvectorConfigTest {

    @Test
    void composeUsesPgvectorImage() throws Exception {
        final String compose = Files.readString(Path.of("docker-compose.yml"));
        assertTrue(compose.contains("image: pgvector/pgvector:pg17"),
                "docker-compose.yml must run pgvector/pgvector:pg17");
        assertTrue(compose.contains("- \"${DB_PORT:-5434}:5432\""),
                "docker-compose.yml must keep port 5434 by default");
    }

    @Test
    void commonV2MigrationEnablesVectorExtension() throws Exception {
        final String migration = Files.readString(
                Path.of("src/main/resources/db/migration/common/V2__enable_pgvector.sql"));
        assertTrue(migration.toLowerCase().contains("create extension if not exists vector"),
                "common/V2 must create the vector extension idempotently");
    }
}
