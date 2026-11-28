package ali.ragab.saas_multi_tennant_app.events;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class StockMovementPostedEvent {
    private final String movementId;
    private final String productId;
    private final TypeMvt type;
    private final Integer quantity;
    private final MovementReason reason;
    private final Integer balanceAfter;
    private final String actor;
    private final Instant occurredAt;
    private final String tenantSchema;
}
