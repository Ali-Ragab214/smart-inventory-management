package ali.ragab.saas_multi_tennant_app.mappers;

import ali.ragab.saas_multi_tennant_app.entities.StockCount;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;
import org.springframework.stereotype.Component;

@Component
public class StockCountMapper {

    public StockCountResponse toResponse(final StockCount count) {
        return StockCountResponse.builder()
                .id(count.getId())
                .productId(count.getProduct() != null ? count.getProduct().getId() : null)
                .countedQty(count.getCountedQty())
                .systemQty(count.getSystemQty())
                .variance(count.getVariance())
                .note(count.getNote())
                .countedBy(count.getCountedBy())
                .countedAt(count.getCountedAt())
                .adjustmentMovementId(
                        count.getAdjustmentMovement() != null
                                ? count.getAdjustmentMovement().getId()
                                : null)
                .build();
    }
}
