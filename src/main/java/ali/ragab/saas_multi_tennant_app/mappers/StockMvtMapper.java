package ali.ragab.saas_multi_tennant_app.mappers;

import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import org.springframework.stereotype.Component;

@Component
public class StockMvtMapper {

    public StockMvtResponse toResponse(final StockMvt stockMvt) {
        return StockMvtResponse.builder()
                .id(stockMvt.getId())
                .quantity(stockMvt.getQuantity())
                .typeMvt(stockMvt.getTypeMvt())
                .comment(stockMvt.getComment())
                .dateMvt(stockMvt.getDateMvt())
                .build();
    }

    public StockMovementResponse toMovementResponse(final StockMvt movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProduct() != null ? movement.getProduct().getId() : null)
                .type(movement.getTypeMvt())
                .quantity(movement.getQuantity())
                .reason(movement.getReason())
                .dateMvt(movement.getDateMvt())
                .occurredAt(movement.getOccurredAt())
                .comment(movement.getComment())
                .balanceAfter(movement.getBalanceAfter())
                .sourceType(movement.getSourceType())
                .sourceId(movement.getSourceId())
                .unitCost(movement.getUnitCost())
                .createdBy(movement.getCreatedBy())
                .build();
    }
}
