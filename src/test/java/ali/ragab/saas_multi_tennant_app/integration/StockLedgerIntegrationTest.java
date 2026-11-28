package ali.ragab.saas_multi_tennant_app.integration;

import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.events.StockMovementPostedEvent;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockMvtRepository;
import ali.ragab.saas_multi_tennant_app.requests.StockCountRequest;
import ali.ragab.saas_multi_tennant_app.requests.StockMovementRequest;
import ali.ragab.saas_multi_tennant_app.requests.StockReceiptRequest;
import ali.ragab.saas_multi_tennant_app.responses.LowStockItemResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;
import ali.ragab.saas_multi_tennant_app.services.ProductService;
import ali.ragab.saas_multi_tennant_app.services.StockCountService;
import ali.ragab.saas_multi_tennant_app.services.StockLedgerService;
import ali.ragab.saas_multi_tennant_app.services.TenantMigrationService;
import ali.ragab.saas_multi_tennant_app.exceptions.InsufficientStockException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.platform-admin.username=platform-admin",
        "app.platform-admin.email=platform-admin@example.com",
        "app.platform-admin.password=platform-admin",
        "app.tenant-migrations.migrate-on-startup=false"
})
@Testcontainers(disabledWithoutDocker = true)
@Import(StockLedgerIntegrationTest.AfterCommitEventConfig.class)
class StockLedgerIntegrationTest {

    private static final String SCHEMA_A = "tenant_stock_test_a";
    private static final String SCHEMA_B = "tenant_stock_test_b";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @Autowired private TenantMigrationService tenantMigrationService;
    @Autowired private StockLedgerService stockLedgerService;
    @Autowired private StockCountService stockCountService;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockMvtRepository stockMvtRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AfterCommitEventRecorder eventRecorder;

    private String productIdA;
    private String productIdB;

    @DynamicPropertySource
    static void databaseProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUp() {
        eventRecorder.clear();
        dropSchemas();
        jdbcTemplate.execute("CREATE SCHEMA " + SCHEMA_A);
        jdbcTemplate.execute("CREATE SCHEMA " + SCHEMA_B);
        tenantMigrationService.migrateSchema(SCHEMA_A);
        tenantMigrationService.migrateSchema(SCHEMA_B);

        // Create a product in schema A
        useSchema(SCHEMA_A);
        final Product productA = Product.builder()
                .name("Widget A")
                .reference("WID-A")
                .description("Test widget for schema A")
                .alertThreshold(10)
                .price(new BigDecimal("25.00"))
                .unitOfMeasure("UNIT")
                .active(true)
                .build();
        productIdA = productRepository.save(productA).getId();

        // Create a product in schema B
        useSchema(SCHEMA_B);
        final Product productB = Product.builder()
                .name("Widget B")
                .reference("WID-B")
                .description("Test widget for schema B")
                .alertThreshold(5)
                .price(new BigDecimal("15.00"))
                .unitOfMeasure("UNIT")
                .active(true)
                .build();
        productIdB = productRepository.save(productB).getId();
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clearCurrentTenant();
        dropSchemas();
    }

    @Test
    @DisplayName("Receipt increments balance and records movement")
    void receiptIncrementsBalance() {
        useSchema(SCHEMA_A);

        final StockMovementResponse mvt = stockLedgerService.receiveStock(
                StockReceiptRequest.builder()
                        .productId(productIdA)
                        .quantity(100)
                        .unitCost(new BigDecimal("5.00"))
                        .build());

        assertNotNull(mvt.getId());
        assertEquals(TypeMvt.IN, mvt.getType());
        assertEquals(100, mvt.getQuantity());
        assertEquals(100, mvt.getBalanceAfter());

        final StockBalanceResponse balance = stockLedgerService.getBalance(productIdA);
        assertEquals(100, balance.getOnHand());
    }

    @Test
    @DisplayName("OUT movement decrements balance")
    void outDecrementsBalance() {
        useSchema(SCHEMA_A);

        // First receive some stock
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(50).build());

        // Now sell 20
        final StockMovementResponse outMvt = stockLedgerService.postMovement(
                StockMovementRequest.builder()
                        .productId(productIdA)
                        .type(TypeMvt.OUT)
                        .quantity(20)
                        .reason(MovementReason.SALE)
                        .build());

        assertEquals(30, outMvt.getBalanceAfter());
        assertEquals(30, stockLedgerService.getBalance(productIdA).getOnHand());
    }

    @Test
    @DisplayName("OUT that exceeds on_hand throws InsufficientStockException (409)")
    void insufficientStockThrows409() {
        useSchema(SCHEMA_A);

        // Start with 10
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(10).build());

        // Try to sell 20
        assertThrows(InsufficientStockException.class, () ->
                stockLedgerService.postMovement(StockMovementRequest.builder()
                        .productId(productIdA)
                        .type(TypeMvt.OUT)
                        .quantity(20)
                        .reason(MovementReason.SALE)
                        .build()));

        // Balance should remain at 10
        assertEquals(10, stockLedgerService.getBalance(productIdA).getOnHand());
    }

    @Test
    @DisplayName("Physical count creates adjustment movement and updates balance")
    void physicalCountCreatesAdjustment() {
        useSchema(SCHEMA_A);

        // Start with 50
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(50).build());

        // Physical count reveals 45 (shrinkage of 5)
        final StockCountResponse countResponse = stockCountService.recordCount(
                StockCountRequest.builder()
                        .productId(productIdA)
                        .countedQty(45)
                        .note("Monthly audit")
                        .build());

        assertEquals(50, countResponse.getSystemQty());
        assertEquals(45, countResponse.getCountedQty());
        assertEquals(-5, countResponse.getVariance());
        assertNotNull(countResponse.getAdjustmentMovementId());

        // Balance should now be 45
        assertEquals(45, stockLedgerService.getBalance(productIdA).getOnHand());
    }

    @Test
    @DisplayName("Balance equals ledger sum after a sequence of operations")
    void balanceEqualsLedgerSum() {
        useSchema(SCHEMA_A);

        // IN 100
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(100).build());
        // OUT 30
        stockLedgerService.postMovement(StockMovementRequest.builder()
                .productId(productIdA).type(TypeMvt.OUT).quantity(30)
                .reason(MovementReason.SALE).build());
        // IN 20
        stockLedgerService.postMovement(StockMovementRequest.builder()
                .productId(productIdA).type(TypeMvt.IN).quantity(20)
                .reason(MovementReason.CUSTOMER_RETURN).build());
        // OUT 10
        stockLedgerService.postMovement(StockMovementRequest.builder()
                .productId(productIdA).type(TypeMvt.OUT).quantity(10)
                .reason(MovementReason.DAMAGE).build());

        // Expected: 100 - 30 + 20 - 10 = 80
        assertEquals(80, stockLedgerService.getBalance(productIdA).getOnHand());

        // Recompute and verify consistency
        final StockBalanceResponse recomputed = stockLedgerService.recomputeBalance(productIdA);
        assertEquals(80, recomputed.getOnHand());
    }

    @Test
    @DisplayName("Movement history is paginated and filterable")
    void movementHistoryPaginationAndFiltering() {
        useSchema(SCHEMA_A);

        // Create several movements
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(100).build());
        stockLedgerService.postMovement(StockMovementRequest.builder()
                .productId(productIdA).type(TypeMvt.OUT).quantity(10)
                .reason(MovementReason.SALE).build());
        stockLedgerService.postMovement(StockMovementRequest.builder()
                .productId(productIdA).type(TypeMvt.OUT).quantity(5)
                .reason(MovementReason.SALE).build());

        // Get all movements
        var allHistory = stockLedgerService.getMovementHistory(
                productIdA, null, null, null, 0, 10);
        assertEquals(3, allHistory.getTotalElements());

        // Filter by type OUT
        var outHistory = stockLedgerService.getMovementHistory(
                productIdA, TypeMvt.OUT, null, null, 0, 10);
        assertEquals(2, outHistory.getTotalElements());

        // Filter by type IN
        var inHistory = stockLedgerService.getMovementHistory(
                productIdA, TypeMvt.IN, null, null, 0, 10);
        assertEquals(1, inHistory.getTotalElements());
    }

    @Test
    @DisplayName("Low-stock query returns products where on_hand <= alertThreshold")
    void lowStockQuery() {
        useSchema(SCHEMA_A);

        // Product A has alertThreshold=10, give it 5 stock (below threshold)
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(5).build());

        final List<LowStockItemResponse> lowStock = stockLedgerService.getLowStockItems();
        assertFalse(lowStock.isEmpty());
        assertTrue(lowStock.stream().anyMatch(item ->
                item.getProductId().equals(productIdA) && item.getShortfall() == 5));
    }

    @Test
    @DisplayName("Cross-tenant stock isolation: movements in schema A are invisible in schema B")
    void crossTenantIsolation() {
        // Create stock in schema A
        useSchema(SCHEMA_A);
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(100).build());
        assertEquals(100, stockLedgerService.getBalance(productIdA).getOnHand());

        // Schema B should have no stock for its own product
        useSchema(SCHEMA_B);
        assertEquals(0, stockLedgerService.getBalance(productIdB).getOnHand());

        // Schema B history should be empty
        var history = stockLedgerService.getMovementHistory(productIdB, null, null, null, 0, 10);
        assertEquals(0, history.getTotalElements());
    }

    @Test
    @DisplayName("Append-only: service has no update/delete path for movements")
    void appendOnlyNoUpdateDelete() {
        // StockLedgerService interface has no update/delete methods — this is a compile-time guarantee.
        // Verify that StockLedgerService only exposes: postMovement, receiveStock, getBalance,
        // recomputeBalance, getMovementHistory, getLowStockItems — no update/delete.
        final var methods = StockLedgerService.class.getDeclaredMethods();
        for (final var method : methods) {
            final String name = method.getName().toLowerCase();
            assertFalse(name.contains("update"), "StockLedgerService must not have an update method");
            assertFalse(name.contains("delete"), "StockLedgerService must not have a delete method");
        }
    }

    @Test
    @DisplayName("Concurrency: two parallel OUTs cannot both succeed against a balance of 1")
    void concurrentOutCannotProduceNegativeBalance() throws Exception {
        useSchema(SCHEMA_A);
        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(1).build());

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger successes = new AtomicInteger(0);
        final AtomicInteger insufficientFailures = new AtomicInteger(0);

        final Runnable task = () -> {
            useSchema(SCHEMA_A);
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                stockLedgerService.postMovement(StockMovementRequest.builder()
                        .productId(productIdA)
                        .type(TypeMvt.OUT)
                        .quantity(1)
                        .reason(MovementReason.SALE)
                        .build());
                successes.incrementAndGet();
            } catch (InsufficientStockException e) {
                insufficientFailures.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                TenantContext.clearCurrentTenant();
            }
        };

        try {
            final Future<?> f1 = executor.submit(task);
            final Future<?> f2 = executor.submit(task);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            f1.get(15, TimeUnit.SECONDS);
            f2.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, successes.get(), "Exactly one parallel OUT should succeed");
        assertEquals(1, insufficientFailures.get(), "Exactly one parallel OUT should fail with InsufficientStockException");
        assertEquals(0, stockLedgerService.getBalance(productIdA).getOnHand());
    }

    @Test
    @DisplayName("StockMovementPostedEvent is delivered after commit and not on rollback")
    void eventPublishedAfterCommitOnly() {
        useSchema(SCHEMA_A);
        eventRecorder.clear();

        final StockMovementResponse committed = stockLedgerService.receiveStock(
                StockReceiptRequest.builder().productId(productIdA).quantity(5).build());

        assertEquals(1, eventRecorder.events().size());
        assertEquals(committed.getId(), eventRecorder.events().get(0).getMovementId());
        assertEquals(SCHEMA_A, eventRecorder.events().get(0).getTenantSchema());

        eventRecorder.clear();
        assertThrows(InsufficientStockException.class, () ->
                stockLedgerService.postMovement(StockMovementRequest.builder()
                        .productId(productIdA)
                        .type(TypeMvt.OUT)
                        .quantity(999)
                        .reason(MovementReason.SALE)
                        .build()));

        assertTrue(eventRecorder.events().isEmpty(), "No event should be delivered when transaction rolls back");
    }

    @Test
    @DisplayName("Soft-deleting a product preserves historical stock movements unchanged")
    void softDeletingProductPreservesHistoricalStockMovements() {
        useSchema(SCHEMA_A);

        stockLedgerService.receiveStock(StockReceiptRequest.builder()
                .productId(productIdA).quantity(30).build());
        stockLedgerService.postMovement(StockMovementRequest.builder()
                .productId(productIdA)
                .type(TypeMvt.OUT)
                .quantity(12)
                .reason(MovementReason.SALE)
                .build());

        final List<StockMovementSnapshot> before = jdbcTemplate.query(
                "SELECT id, quantity, type_mvt, reason, balance_after, deleted " +
                        "FROM " + SCHEMA_A + ".stock_mvts WHERE product_id = ? ORDER BY occurred_at ASC",
                (rs, rowNum) -> new StockMovementSnapshot(
                        rs.getString("id"),
                        rs.getInt("quantity"),
                        rs.getString("type_mvt"),
                        rs.getString("reason"),
                        rs.getInt("balance_after"),
                        rs.getBoolean("deleted")),
                productIdA);
        assertEquals(2, before.size());

        // Soft-delete the product
        productService.delete(productIdA);
        assertThrows(EntityNotFoundException.class, () -> productService.findById(productIdA));

        // Historical stock_mvts rows in the tenant schema must be completely untouched
        final List<StockMovementSnapshot> after = jdbcTemplate.query(
                "SELECT id, quantity, type_mvt, reason, balance_after, deleted " +
                        "FROM " + SCHEMA_A + ".stock_mvts WHERE product_id = ? ORDER BY occurred_at ASC",
                (rs, rowNum) -> new StockMovementSnapshot(
                        rs.getString("id"),
                        rs.getInt("quantity"),
                        rs.getString("type_mvt"),
                        rs.getString("reason"),
                        rs.getInt("balance_after"),
                        rs.getBoolean("deleted")),
                productIdA);

        assertEquals(before.size(), after.size());
        assertEquals(before, after);
        assertTrue(after.stream().noneMatch(StockMovementSnapshot::deleted));

        // Direct ledger repository query still includes all historical movements
        final var persistedMovements = stockMvtRepository.findAll();
        assertEquals(2, persistedMovements.size());

        // Under current business rules, StockLedgerService requires an active (non-deleted) product
        assertThrows(EntityNotFoundException.class,
                () -> stockLedgerService.getMovementHistory(productIdA, null, null, null, 0, 10));
        assertThrows(EntityNotFoundException.class,
                () -> stockLedgerService.recomputeBalance(productIdA));
    }

    private record StockMovementSnapshot(
            String id,
            int quantity,
            String typeMvt,
            String reason,
            int balanceAfter,
            boolean deleted) {
    }

    @TestConfiguration
    static class AfterCommitEventConfig {
        @Bean
        AfterCommitEventRecorder afterCommitEventRecorder() {
            return new AfterCommitEventRecorder();
        }
    }

    static class AfterCommitEventRecorder {
        private final List<StockMovementPostedEvent> recorded = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onMovementPosted(final StockMovementPostedEvent event) {
            recorded.add(event);
        }

        List<StockMovementPostedEvent> events() {
            return recorded;
        }

        void clear() {
            recorded.clear();
        }
    }

    private void useSchema(final String schema) {
        TenantContext.setCurrentTenant(schema);
        TenantContext.setCurrentSchema(schema);
    }

    private void dropSchemas() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA_A + " CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCHEMA_B + " CASCADE");
    }
}
