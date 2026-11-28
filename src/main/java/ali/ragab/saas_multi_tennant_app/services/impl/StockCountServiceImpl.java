package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.config.TenantContext;
import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockBalance;
import ali.ragab.saas_multi_tennant_app.entities.StockCount;
import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.events.StockMovementPostedEvent;
import ali.ragab.saas_multi_tennant_app.mappers.StockCountMapper;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockBalanceRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockCountRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockMvtRepository;
import ali.ragab.saas_multi_tennant_app.requests.StockCountRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;
import ali.ragab.saas_multi_tennant_app.services.StockCountService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockCountServiceImpl implements StockCountService {

    private final StockCountRepository stockCountRepository;
    private final StockMvtRepository stockMvtRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ProductRepository productRepository;
    private final StockCountMapper stockCountMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditorAware<String> auditorAware;

    @Override
    @Transactional
    public StockCountResponse recordCount(final StockCountRequest request) {
        final Product product = this.productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getProductId()));

        // Lock balance row
        final StockBalance balance = this.stockBalanceRepository
                .findByProductIdForUpdate(product.getId())
                .orElseGet(() -> {
                    final StockBalance newBalance = StockBalance.builder()
                            .productId(product.getId())
                            .onHand(0)
                            .reserved(0)
                            .build();
                    return this.stockBalanceRepository.save(newBalance);
                });

        final int systemQty = balance.getOnHand();
        final int countedQty = request.getCountedQty();
        final int variance = countedQty - systemQty;

        final String actor = this.auditorAware.getCurrentAuditor().orElse("SYSTEM");

        StockMvt adjustmentMovement = null;

        if (variance != 0) {
            // Create adjustment movement
            final TypeMvt adjustmentType = variance > 0 ? TypeMvt.IN : TypeMvt.OUT;
            final int adjustmentQty = Math.abs(variance);

            // Update balance to match counted quantity
            balance.setOnHand(countedQty);

            adjustmentMovement = StockMvt.builder()
                    .product(product)
                    .typeMvt(adjustmentType)
                    .quantity(adjustmentQty)
                    .dateMvt(LocalDate.now())
                    .occurredAt(Instant.now())
                    .reason(MovementReason.COUNT_ADJUSTMENT)
                    .comment("Physical count adjustment" + (request.getNote() != null ? ": " + request.getNote() : ""))
                    .balanceAfter(countedQty)
                    .deleted(false)
                    .build();

            adjustmentMovement = this.stockMvtRepository.save(adjustmentMovement);
            this.stockBalanceRepository.save(balance);

            // Publish event
            this.eventPublisher.publishEvent(StockMovementPostedEvent.builder()
                    .movementId(adjustmentMovement.getId())
                    .productId(product.getId())
                    .type(adjustmentType)
                    .quantity(adjustmentQty)
                    .reason(MovementReason.COUNT_ADJUSTMENT)
                    .balanceAfter(countedQty)
                    .actor(actor)
                    .occurredAt(adjustmentMovement.getOccurredAt())
                    .tenantSchema(TenantContext.getCurrentSchema())
                    .build());
        }

        // Create count record
        final StockCount count = StockCount.builder()
                .product(product)
                .countedQty(countedQty)
                .systemQty(systemQty)
                .variance(variance)
                .note(request.getNote())
                .countedBy(actor)
                .countedAt(Instant.now())
                .adjustmentMovement(adjustmentMovement)
                .deleted(false)
                .build();

        final StockCount savedCount = this.stockCountRepository.save(count);

        log.info("Physical count recorded for product {}: system={}, counted={}, variance={}",
                product.getReference(), systemQty, countedQty, variance);

        return this.stockCountMapper.toResponse(savedCount);
    }
}
