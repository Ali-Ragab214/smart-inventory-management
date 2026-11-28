package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockBalance;
import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.events.StockMovementPostedEvent;
import ali.ragab.saas_multi_tennant_app.exceptions.InsufficientStockException;
import ali.ragab.saas_multi_tennant_app.mappers.StockBalanceMapper;
import ali.ragab.saas_multi_tennant_app.mappers.StockMvtMapper;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockBalanceRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockMvtRepository;
import ali.ragab.saas_multi_tennant_app.requests.StockMovementRequest;
import ali.ragab.saas_multi_tennant_app.requests.StockReceiptRequest;
import ali.ragab.saas_multi_tennant_app.responses.LowStockItemResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLedgerServiceImplTest {

    @Mock private StockMvtRepository stockMvtRepository;
    @Mock private StockBalanceRepository stockBalanceRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMvtMapper stockMvtMapper;
    @Mock private StockBalanceMapper stockBalanceMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private StockLedgerServiceImpl service;

    private Product testProduct;
    private StockBalance testBalance;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("prod-1")
                .name("Test Product")
                .reference("SKU-001")
                .alertThreshold(10)
                .price(BigDecimal.TEN)
                .build();

        testBalance = StockBalance.builder()
                .productId("prod-1")
                .onHand(50)
                .reserved(0)
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("postMovement")
    class PostMovement {

        @Test
        @DisplayName("IN movement increments balance and records balanceAfter")
        void inMovementIncrementsBalance() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
            when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
                StockMvt m = inv.getArgument(0);
                m.setId("mvt-1");
                return m;
            });
            when(stockBalanceRepository.save(any(StockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(stockMvtMapper.toMovementResponse(any())).thenReturn(
                    StockMovementResponse.builder().id("mvt-1").balanceAfter(60).build());

            final StockMovementRequest request = StockMovementRequest.builder()
                    .productId("prod-1")
                    .type(TypeMvt.IN)
                    .quantity(10)
                    .reason(MovementReason.PURCHASE_RECEIPT)
                    .build();

            final StockMovementResponse response = service.postMovement(request);

            assertNotNull(response);
            // Verify balance was incremented
            final ArgumentCaptor<StockBalance> balCaptor = ArgumentCaptor.forClass(StockBalance.class);
            verify(stockBalanceRepository).save(balCaptor.capture());
            assertEquals(60, balCaptor.getValue().getOnHand());

            // Verify movement was saved with correct balanceAfter
            final ArgumentCaptor<StockMvt> mvtCaptor = ArgumentCaptor.forClass(StockMvt.class);
            verify(stockMvtRepository).save(mvtCaptor.capture());
            assertEquals(60, mvtCaptor.getValue().getBalanceAfter());
            assertEquals(TypeMvt.IN, mvtCaptor.getValue().getTypeMvt());

            // Verify event was published
            verify(eventPublisher).publishEvent(any(StockMovementPostedEvent.class));
        }

        @Test
        @DisplayName("OUT movement decrements balance")
        void outMovementDecrementsBalance() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
            when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
                StockMvt m = inv.getArgument(0);
                m.setId("mvt-2");
                return m;
            });
            when(stockBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(stockMvtMapper.toMovementResponse(any())).thenReturn(
                    StockMovementResponse.builder().id("mvt-2").balanceAfter(45).build());

            final StockMovementRequest request = StockMovementRequest.builder()
                    .productId("prod-1")
                    .type(TypeMvt.OUT)
                    .quantity(5)
                    .reason(MovementReason.SALE)
                    .build();

            service.postMovement(request);

            final ArgumentCaptor<StockBalance> captor = ArgumentCaptor.forClass(StockBalance.class);
            verify(stockBalanceRepository).save(captor.capture());
            assertEquals(45, captor.getValue().getOnHand());
        }

        @Test
        @DisplayName("OUT that would go negative throws InsufficientStockException")
        void outRejectsNegativeBalance() {
            testBalance.setOnHand(3);
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));

            final StockMovementRequest request = StockMovementRequest.builder()
                    .productId("prod-1")
                    .type(TypeMvt.OUT)
                    .quantity(5)
                    .reason(MovementReason.SALE)
                    .build();

            assertThrows(InsufficientStockException.class, () -> service.postMovement(request));
            verify(stockMvtRepository, never()).save(any());
        }

        @Test
        @DisplayName("COUNT_ADJUSTMENT OUT is allowed even when it would go negative")
        void countAdjustmentAllowsNegative() {
            testBalance.setOnHand(3);
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
            when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
                StockMvt m = inv.getArgument(0);
                m.setId("mvt-3");
                return m;
            });
            when(stockBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(stockMvtMapper.toMovementResponse(any())).thenReturn(
                    StockMovementResponse.builder().id("mvt-3").build());

            final StockMovementRequest request = StockMovementRequest.builder()
                    .productId("prod-1")
                    .type(TypeMvt.OUT)
                    .quantity(5)
                    .reason(MovementReason.COUNT_ADJUSTMENT)
                    .build();

            // Should not throw
            assertDoesNotThrow(() -> service.postMovement(request));
            verify(stockMvtRepository).save(any());
        }

        @Test
        @DisplayName("Product not found throws EntityNotFoundException")
        void productNotFound() {
            when(productRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

            final StockMovementRequest request = StockMovementRequest.builder()
                    .productId("missing")
                    .type(TypeMvt.IN)
                    .quantity(1)
                    .reason(MovementReason.OTHER)
                    .build();

            assertThrows(EntityNotFoundException.class, () -> service.postMovement(request));
        }

        @Test
        @DisplayName("Creates balance if none exists")
        void createsBalanceIfNone() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.empty());
            when(stockBalanceRepository.save(any(StockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
                StockMvt m = inv.getArgument(0);
                m.setId("mvt-4");
                return m;
            });
            when(stockMvtMapper.toMovementResponse(any())).thenReturn(
                    StockMovementResponse.builder().id("mvt-4").build());

            final StockMovementRequest request = StockMovementRequest.builder()
                    .productId("prod-1")
                    .type(TypeMvt.IN)
                    .quantity(10)
                    .reason(MovementReason.OPENING_BALANCE)
                    .build();

            service.postMovement(request);

            // save called twice: once for creating balance, once for updating it
            verify(stockBalanceRepository, atLeast(1)).save(any(StockBalance.class));
        }
    }

    @Nested
    @DisplayName("receiveStock")
    class ReceiveStock {

        @Test
        @DisplayName("Receipt creates IN movement with PURCHASE_RECEIPT reason")
        void receiptCreatesInMovement() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
            when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
                StockMvt m = inv.getArgument(0);
                m.setId("mvt-r1");
                return m;
            });
            when(stockBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(stockMvtMapper.toMovementResponse(any())).thenReturn(
                    StockMovementResponse.builder().id("mvt-r1").build());

            final StockReceiptRequest request = StockReceiptRequest.builder()
                    .productId("prod-1")
                    .quantity(20)
                    .unitCost(new BigDecimal("5.50"))
                    .purchaseOrderId("po-1")
                    .build();

            service.receiveStock(request);

            final ArgumentCaptor<StockMvt> captor = ArgumentCaptor.forClass(StockMvt.class);
            verify(stockMvtRepository).save(captor.capture());
            final StockMvt saved = captor.getValue();
            assertEquals(TypeMvt.IN, saved.getTypeMvt());
            assertEquals(MovementReason.PURCHASE_RECEIPT, saved.getReason());
            assertEquals(20, saved.getQuantity());
            assertEquals("PURCHASE_ORDER", saved.getSourceType());
            assertEquals("po-1", saved.getSourceId());
            assertEquals(new BigDecimal("5.50"), saved.getUnitCost());
            assertEquals(70, saved.getBalanceAfter()); // 50 + 20
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("Returns existing balance")
        void returnsExistingBalance() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findById("prod-1")).thenReturn(Optional.of(testBalance));
            when(stockBalanceMapper.toResponse(testBalance, testProduct))
                    .thenReturn(StockBalanceResponse.builder().onHand(50).build());

            final StockBalanceResponse response = service.getBalance("prod-1");
            assertEquals(50, response.getOnHand());
        }

        @Test
        @DisplayName("Returns zero balance when none exists")
        void returnsZeroBalance() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findById("prod-1")).thenReturn(Optional.empty());
            when(stockBalanceMapper.toResponse(any(), eq(testProduct)))
                    .thenReturn(StockBalanceResponse.builder().onHand(0).build());

            final StockBalanceResponse response = service.getBalance("prod-1");
            assertEquals(0, response.getOnHand());
        }
    }

    @Nested
    @DisplayName("recomputeBalance")
    class RecomputeBalance {

        @Test
        @DisplayName("Recomputes balance from ledger sums")
        void recomputesFromLedger() {
            when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
            when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
            when(stockMvtRepository.sumInQuantityByProductId("prod-1")).thenReturn(100);
            when(stockMvtRepository.sumOutQuantityByProductId("prod-1")).thenReturn(30);
            when(stockMvtRepository.findAllByProductIdOrderByOccurredAtAsc("prod-1"))
                    .thenReturn(Collections.emptyList());
            when(stockBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(stockBalanceMapper.toResponse(any(), any()))
                    .thenReturn(StockBalanceResponse.builder().onHand(70).build());

            final StockBalanceResponse response = service.recomputeBalance("prod-1");

            final ArgumentCaptor<StockBalance> captor = ArgumentCaptor.forClass(StockBalance.class);
            verify(stockBalanceRepository).save(captor.capture());
            assertEquals(70, captor.getValue().getOnHand()); // 100 - 30
        }
    }

    @Nested
    @DisplayName("getLowStockItems")
    class GetLowStock {

        @Test
        @DisplayName("Returns products where on_hand <= alertThreshold")
        void returnsLowStockProducts() {
            final StockBalance lowBalance = StockBalance.builder()
                    .productId("prod-1")
                    .onHand(5)
                    .reserved(0)
                    .product(testProduct)
                    .build();
            when(stockBalanceRepository.findLowStockBalances()).thenReturn(List.of(lowBalance));
            when(stockBalanceMapper.toLowStockResponse(lowBalance, testProduct))
                    .thenReturn(LowStockItemResponse.builder()
                            .productId("prod-1")
                            .onHand(5)
                            .alertThreshold(10)
                            .shortfall(5)
                            .build());

            final List<LowStockItemResponse> result = service.getLowStockItems();
            assertEquals(1, result.size());
            assertEquals(5, result.get(0).getShortfall());
        }
    }
}
