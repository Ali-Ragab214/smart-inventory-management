package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockBalance;
import ali.ragab.saas_multi_tennant_app.entities.StockCount;
import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.mappers.StockCountMapper;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockBalanceRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockCountRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockMvtRepository;
import ali.ragab.saas_multi_tennant_app.requests.StockCountRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockCountServiceImplTest {

    @Mock private StockCountRepository stockCountRepository;
    @Mock private StockMvtRepository stockMvtRepository;
    @Mock private StockBalanceRepository stockBalanceRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockCountMapper stockCountMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditorAware<String> auditorAware;

    @InjectMocks private StockCountServiceImpl service;

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

    @Test
    @DisplayName("Count with positive variance creates IN adjustment")
    void positiveVarianceCreatesInAdjustment() {
        when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
        when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("sam"));
        when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
            StockMvt m = inv.getArgument(0);
            m.setId("mvt-adj-1");
            return m;
        });
        when(stockBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> {
            StockCount c = inv.getArgument(0);
            c.setId("count-1");
            return c;
        });
        when(stockCountMapper.toResponse(any())).thenReturn(
                StockCountResponse.builder()
                        .id("count-1")
                        .systemQty(50)
                        .countedQty(55)
                        .variance(5)
                        .build());

        final StockCountRequest request = StockCountRequest.builder()
                .productId("prod-1")
                .countedQty(55) // system is 50, so +5
                .note("Found extra items")
                .build();

        final StockCountResponse response = service.recordCount(request);

        // Verify adjustment movement was created as IN
        final ArgumentCaptor<StockMvt> mvtCaptor = ArgumentCaptor.forClass(StockMvt.class);
        verify(stockMvtRepository).save(mvtCaptor.capture());
        assertEquals(TypeMvt.IN, mvtCaptor.getValue().getTypeMvt());
        assertEquals(5, mvtCaptor.getValue().getQuantity());
        assertEquals(MovementReason.COUNT_ADJUSTMENT, mvtCaptor.getValue().getReason());
        assertEquals(55, mvtCaptor.getValue().getBalanceAfter());

        // Verify balance was updated
        final ArgumentCaptor<StockBalance> balCaptor = ArgumentCaptor.forClass(StockBalance.class);
        verify(stockBalanceRepository).save(balCaptor.capture());
        assertEquals(55, balCaptor.getValue().getOnHand());

        // Verify count was saved
        verify(stockCountRepository).save(any(StockCount.class));
        assertEquals(5, response.getVariance());
    }

    @Test
    @DisplayName("Count with negative variance creates OUT adjustment")
    void negativeVarianceCreatesOutAdjustment() {
        when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
        when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("sam"));
        when(stockMvtRepository.save(any(StockMvt.class))).thenAnswer(inv -> {
            StockMvt m = inv.getArgument(0);
            m.setId("mvt-adj-2");
            return m;
        });
        when(stockBalanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> {
            StockCount c = inv.getArgument(0);
            c.setId("count-2");
            return c;
        });
        when(stockCountMapper.toResponse(any())).thenReturn(
                StockCountResponse.builder()
                        .id("count-2")
                        .systemQty(50)
                        .countedQty(45)
                        .variance(-5)
                        .build());

        final StockCountRequest request = StockCountRequest.builder()
                .productId("prod-1")
                .countedQty(45) // system is 50, so -5
                .build();

        service.recordCount(request);

        final ArgumentCaptor<StockMvt> mvtCaptor = ArgumentCaptor.forClass(StockMvt.class);
        verify(stockMvtRepository).save(mvtCaptor.capture());
        assertEquals(TypeMvt.OUT, mvtCaptor.getValue().getTypeMvt());
        assertEquals(5, mvtCaptor.getValue().getQuantity());
    }

    @Test
    @DisplayName("Count with zero variance creates no adjustment movement")
    void zeroVarianceNoAdjustment() {
        when(productRepository.findByIdAndDeletedFalse("prod-1")).thenReturn(Optional.of(testProduct));
        when(stockBalanceRepository.findByProductIdForUpdate("prod-1")).thenReturn(Optional.of(testBalance));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("sam"));
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> {
            StockCount c = inv.getArgument(0);
            c.setId("count-3");
            return c;
        });
        when(stockCountMapper.toResponse(any())).thenReturn(
                StockCountResponse.builder()
                        .id("count-3")
                        .systemQty(50)
                        .countedQty(50)
                        .variance(0)
                        .build());

        final StockCountRequest request = StockCountRequest.builder()
                .productId("prod-1")
                .countedQty(50) // same as system
                .build();

        service.recordCount(request);

        // No movement should be created
        verify(stockMvtRepository, never()).save(any());
        // Count should still be saved
        verify(stockCountRepository).save(any(StockCount.class));
    }

    @Test
    @DisplayName("Product not found throws EntityNotFoundException")
    void productNotFoundThrows() {
        when(productRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        final StockCountRequest request = StockCountRequest.builder()
                .productId("missing")
                .countedQty(10)
                .build();

        assertThrows(EntityNotFoundException.class, () -> service.recordCount(request));
    }
}
