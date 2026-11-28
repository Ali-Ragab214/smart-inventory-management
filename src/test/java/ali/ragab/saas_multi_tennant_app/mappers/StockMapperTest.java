package ali.ragab.saas_multi_tennant_app.mappers;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockBalance;
import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.responses.LowStockItemResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StockMapperTest {

    private final StockMvtMapper stockMvtMapper = new StockMvtMapper();
    private final StockBalanceMapper stockBalanceMapper = new StockBalanceMapper();

    @Test
    @DisplayName("toMovementResponse maps all fields correctly")
    void toMovementResponseMapsAllFields() {
        final Product product = Product.builder().id("prod-1").build();
        final StockMvt movement = StockMvt.builder()
                .id("mvt-1")
                .product(product)
                .typeMvt(TypeMvt.IN)
                .quantity(10)
                .reason(MovementReason.PURCHASE_RECEIPT)
                .dateMvt(LocalDate.of(2026, 11, 27))
                .occurredAt(Instant.parse("2026-11-27T10:00:00Z"))
                .comment("Test receipt")
                .balanceAfter(60)
                .sourceType("PURCHASE_ORDER")
                .sourceId("po-1")
                .unitCost(new BigDecimal("5.50"))
                .build();
        movement.setCreatedBy("sam");

        final StockMovementResponse response = stockMvtMapper.toMovementResponse(movement);

        assertEquals("mvt-1", response.getId());
        assertEquals("prod-1", response.getProductId());
        assertEquals(TypeMvt.IN, response.getType());
        assertEquals(10, response.getQuantity());
        assertEquals(MovementReason.PURCHASE_RECEIPT, response.getReason());
        assertEquals(LocalDate.of(2026, 11, 27), response.getDateMvt());
        assertEquals(Instant.parse("2026-11-27T10:00:00Z"), response.getOccurredAt());
        assertEquals("Test receipt", response.getComment());
        assertEquals(60, response.getBalanceAfter());
        assertEquals("PURCHASE_ORDER", response.getSourceType());
        assertEquals("po-1", response.getSourceId());
        assertEquals(new BigDecimal("5.50"), response.getUnitCost());
        assertEquals("sam", response.getCreatedBy());
    }

    @Test
    @DisplayName("toMovementResponse handles null product safely")
    void toMovementResponseNullProduct() {
        final StockMvt movement = StockMvt.builder()
                .id("mvt-2")
                .typeMvt(TypeMvt.OUT)
                .quantity(5)
                .reason(MovementReason.SALE)
                .dateMvt(LocalDate.now())
                .occurredAt(Instant.now())
                .build();

        final StockMovementResponse response = stockMvtMapper.toMovementResponse(movement);

        assertNull(response.getProductId());
        assertEquals("mvt-2", response.getId());
    }

    @Test
    @DisplayName("StockBalanceMapper.toResponse computes available correctly")
    void toResponseComputesAvailable() {
        final StockBalance balance = StockBalance.builder()
                .productId("prod-1")
                .onHand(100)
                .reserved(15)
                .build();
        final Product product = Product.builder()
                .id("prod-1")
                .name("Test Product")
                .reference("SKU-001")
                .build();

        final StockBalanceResponse response = stockBalanceMapper.toResponse(balance, product);

        assertEquals("prod-1", response.getProductId());
        assertEquals("Test Product", response.getProductName());
        assertEquals("SKU-001", response.getProductReference());
        assertEquals(100, response.getOnHand());
        assertEquals(15, response.getReserved());
        assertEquals(85, response.getAvailable()); // 100 - 15
    }

    @Test
    @DisplayName("StockBalanceMapper.toLowStockResponse computes shortfall correctly")
    void toLowStockResponseComputesShortfall() {
        final StockBalance balance = StockBalance.builder()
                .productId("prod-1")
                .onHand(3)
                .reserved(0)
                .build();
        final Product product = Product.builder()
                .id("prod-1")
                .name("Test Product")
                .reference("SKU-001")
                .alertThreshold(10)
                .price(BigDecimal.TEN)
                .build();

        final LowStockItemResponse response = stockBalanceMapper.toLowStockResponse(balance, product);

        assertEquals("prod-1", response.getProductId());
        assertEquals(3, response.getOnHand());
        assertEquals(10, response.getAlertThreshold());
        assertEquals(7, response.getShortfall()); // 10 - 3
    }
}
