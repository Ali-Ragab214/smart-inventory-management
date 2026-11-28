package ali.ragab.saas_multi_tennant_app.requests;

import ali.ragab.saas_multi_tennant_app.entities.MovementReason;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovementRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Movement type is required")
    private TypeMvt type;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Reason is required")
    private MovementReason reason;

    private Instant occurredAt;

    private String comment;
}
