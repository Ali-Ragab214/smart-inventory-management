package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.config.TenantContext;
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
import ali.ragab.saas_multi_tennant_app.services.StockLedgerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockLedgerServiceImpl implements StockLedgerService {

    private final StockMvtRepository stockMvtRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ProductRepository productRepository;
    private final StockMvtMapper stockMvtMapper;
    private final StockBalanceMapper stockBalanceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public StockMovementResponse postMovement(final StockMovementRequest request) {
        final Product product = findProductOrThrow(request.getProductId());

        // Lock the balance row (or create one)
        final StockBalance balance = getOrCreateBalanceLocked(product.getId());

        // Validate insufficient stock for OUT movements
        if (request.getType() == TypeMvt.OUT) {
            final int newOnHand = balance.getOnHand() - request.getQuantity();
            if (newOnHand < 0 && request.getReason() != MovementReason.COUNT_ADJUSTMENT) {
                throw new InsufficientStockException(
                        "Insufficient stock for product " + product.getReference()
                                + ": on_hand=" + balance.getOnHand()
                                + ", requested=" + request.getQuantity());
            }
        }

        // Update balance
        if (request.getType() == TypeMvt.IN) {
            balance.setOnHand(balance.getOnHand() + request.getQuantity());
        } else {
            balance.setOnHand(balance.getOnHand() - request.getQuantity());
        }

        // Create movement
        final StockMvt movement = StockMvt.builder()
                .product(product)
                .typeMvt(request.getType())
                .quantity(request.getQuantity())
                .dateMvt(LocalDate.now())
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now())
                .reason(request.getReason())
                .comment(request.getComment())
                .balanceAfter(balance.getOnHand())
                .deleted(false)
                .build();

        final StockMvt saved = this.stockMvtRepository.save(movement);
        this.stockBalanceRepository.save(balance);

        // Publish event (will be delivered after commit via @TransactionalEventListener)
        publishMovementEvent(saved, product.getId());

        return this.stockMvtMapper.toMovementResponse(saved);
    }

    @Override
    @Transactional
    public StockMovementResponse receiveStock(final StockReceiptRequest request) {
        final Product product = findProductOrThrow(request.getProductId());

        // Lock the balance row
        final StockBalance balance = getOrCreateBalanceLocked(product.getId());

        // Update balance (receipt is always IN)
        balance.setOnHand(balance.getOnHand() + request.getQuantity());

        // Create movement
        final StockMvt movement = StockMvt.builder()
                .product(product)
                .typeMvt(TypeMvt.IN)
                .quantity(request.getQuantity())
                .dateMvt(LocalDate.now())
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now())
                .reason(MovementReason.PURCHASE_RECEIPT)
                .sourceType(request.getPurchaseOrderId() != null ? "PURCHASE_ORDER" : null)
                .sourceId(request.getPurchaseOrderId())
                .unitCost(request.getUnitCost())
                .balanceAfter(balance.getOnHand())
                .deleted(false)
                .build();

        final StockMvt saved = this.stockMvtRepository.save(movement);
        this.stockBalanceRepository.save(balance);

        publishMovementEvent(saved, product.getId());

        return this.stockMvtMapper.toMovementResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBalanceResponse getBalance(final String productId) {
        final Product product = findProductOrThrow(productId);
        final StockBalance balance = this.stockBalanceRepository.findById(productId)
                .orElse(StockBalance.builder()
                        .productId(productId)
                        .onHand(0)
                        .reserved(0)
                        .build());
        return this.stockBalanceMapper.toResponse(balance, product);
    }

    @Override
    @Transactional
    public StockBalanceResponse recomputeBalance(final String productId) {
        final Product product = findProductOrThrow(productId);

        // Lock the balance row
        final StockBalance balance = getOrCreateBalanceLocked(productId);

        // Recompute from ledger
        final int totalIn = this.stockMvtRepository.sumInQuantityByProductId(productId);
        final int totalOut = this.stockMvtRepository.sumOutQuantityByProductId(productId);
        final int computedOnHand = totalIn - totalOut;

        log.info("Recomputing balance for product {}: IN={}, OUT={}, computed={}, was={}",
                productId, totalIn, totalOut, computedOnHand, balance.getOnHand());

        balance.setOnHand(computedOnHand);
        this.stockBalanceRepository.save(balance);

        // Also update balance_after on each movement in order
        final var movements = this.stockMvtRepository.findAllByProductIdOrderByOccurredAtAsc(productId);
        int runningBalance = 0;
        for (final StockMvt m : movements) {
            if (m.getTypeMvt() == TypeMvt.IN) {
                runningBalance += m.getQuantity();
            } else {
                runningBalance -= m.getQuantity();
            }
            m.setBalanceAfter(runningBalance);
        }
        this.stockMvtRepository.saveAll(movements);

        return this.stockBalanceMapper.toResponse(balance, product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getMovementHistory(
            final String productId, final TypeMvt type,
            final Instant from, final Instant to,
            final int page, final int size) {
        findProductOrThrow(productId);
        final PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        final Specification<StockMvt> spec = (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("product").get("id"), productId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (type != null) {
                predicates.add(cb.equal(root.get("typeMvt"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        final Page<StockMvt> movements = this.stockMvtRepository.findAll(spec, pageRequest);
        final Page<StockMovementResponse> responsePage = movements.map(this.stockMvtMapper::toMovementResponse);
        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockItemResponse> getLowStockItems() {
        return this.stockBalanceRepository.findLowStockBalances().stream()
                .map(balance -> {
                    final Product product = balance.getProduct();
                    return this.stockBalanceMapper.toLowStockResponse(balance, product);
                })
                .toList();
    }


    private Product findProductOrThrow(final String productId) {
        return this.productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
    }

    private StockBalance getOrCreateBalanceLocked(final String productId) {
        return this.stockBalanceRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> {
                    final StockBalance newBalance = StockBalance.builder()
                            .productId(productId)
                            .onHand(0)
                            .reserved(0)
                            .build();
                    return this.stockBalanceRepository.save(newBalance);
                });
    }

    private void publishMovementEvent(final StockMvt movement, final String productId) {
        this.eventPublisher.publishEvent(StockMovementPostedEvent.builder()
                .movementId(movement.getId())
                .productId(productId)
                .type(movement.getTypeMvt())
                .quantity(movement.getQuantity())
                .reason(movement.getReason())
                .balanceAfter(movement.getBalanceAfter())
                .actor(movement.getCreatedBy())
                .occurredAt(movement.getOccurredAt())
                .tenantSchema(TenantContext.getCurrentSchema())
                .build());
    }
}
