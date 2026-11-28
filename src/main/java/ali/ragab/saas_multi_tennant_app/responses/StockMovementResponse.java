package ali.ragab.saas_multi_tennant_app.responses;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovementResponse {
    private String id;
    private String productId;
    private TypeMvt type;
    private Integer quantity;
    private MovementReason reason;
    private LocalDate dateMvt;
    private Instant occurredAt;
    private String comment;
    private Integer balanceAfter;
    private String sourceType;
    private String sourceId;
    private BigDecimal unitCost;
    private String createdBy;
}
