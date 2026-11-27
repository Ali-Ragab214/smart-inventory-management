package ali.ragab.saas_multi_tennant_app.integration;

import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.entities.Category;
import ali.ragab.saas_multi_tennant_app.exceptions.DuplicateResourceException;
import ali.ragab.saas_multi_tennant_app.repositories.CategoryRepository;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import ali.ragab.saas_multi_tennant_app.services.ProductService;
import ali.ragab.saas_multi_tennant_app.services.TenantMigrationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.platform-admin.username=platform-admin",
        "app.platform-admin.email=platform-admin@example.com",
        "app.platform-admin.password=platform-admin",
        "app.tenant-migrations.migrate-on-startup=false"
})
@Testcontainers(disabledWithoutDocker = true)
class ProductTenantIsolationIntegrationTest {

    private static final String SCHEMA_A = "tenant_product_test_a";
    private static final String SCHEMA_B = "tenant_product_test_b";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @Autowired
    private TenantMigrationService tenantMigrationService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void provisionTenantSchemas() {
        dropSchemas();
        this.jdbcTemplate.execute("create schema " + SCHEMA_A);
        this.jdbcTemplate.execute("create schema " + SCHEMA_B);
        this.tenantMigrationService.migrateSchema(SCHEMA_A);
        this.tenantMigrationService.migrateSchema(SCHEMA_B);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clearCurrentTenant();
        dropSchemas();
    }

    @Test
    void productCrudIsTenantScopedAndSoftDeletedProductsAreInvisible() {
        useSchema(SCHEMA_A);
        final String categoryId = createCategory("A category");
        final var created = this.productService.create(request("SKU-A", categoryId));

        final var updated = this.productService.update(created.getId(), request("SKU-A", categoryId));
        assertEquals(created.getId(), updated.getId());
        assertEquals("SKU-A", updated.getReference());

        useSchema(SCHEMA_B);
        assertTrue(this.productService.findAll(0, 10, null).getContent().isEmpty());
        assertTrue(this.productService.findAll(0, 10, "SKU-A").getContent().isEmpty());
        assertThrows(EntityNotFoundException.class, () -> this.productService.findById(created.getId()));
        assertThrows(EntityNotFoundException.class, () -> this.productService.findByReference("SKU-A"));
        assertThrows(EntityNotFoundException.class,
                () -> this.productService.update(created.getId(), request("SKU-A", categoryId)));
        assertThrows(EntityNotFoundException.class, () -> this.productService.delete(created.getId()));

        useSchema(SCHEMA_A);
        assertThrows(DuplicateResourceException.class,
                () -> this.productService.create(request("SKU-A", categoryId)));
        assertEquals(1, this.productService.findAll(0, 10, "SKU-A").getContent().size());
        this.productService.delete(created.getId());
        assertThrows(EntityNotFoundException.class, () -> this.productService.findById(created.getId()));
        assertThrows(EntityNotFoundException.class, () -> this.productService.findByReference("SKU-A"));
        assertTrue(this.productService.findAll(0, 10, null).getContent().isEmpty());
        assertTrue(this.productService.findAll(0, 10, "SKU-A").getContent().isEmpty());
    }

    private String createCategory(final String name) {
        return this.categoryRepository.save(Category.builder()
                .name(name)
                .description(name)
                .build()).getId();
    }

    private ProductRequest request(final String reference, final String categoryId) {
        return ProductRequest.builder()
                .name("Rice")
                .reference(reference)
                .description("Basmati rice")
                .price(new BigDecimal("10.00"))
                .alertThreshold(0)
                .categoryId(categoryId)
                .unitOfMeasure("KG")
                .active(true)
                .build();
    }

    private void useSchema(final String schema) {
        TenantContext.setCurrentTenant(schema);
        TenantContext.setCurrentSchema(schema);
    }

    private void dropSchemas() {
        this.jdbcTemplate.execute("drop schema if exists " + SCHEMA_A + " cascade");
        this.jdbcTemplate.execute("drop schema if exists " + SCHEMA_B + " cascade");
    }
}
