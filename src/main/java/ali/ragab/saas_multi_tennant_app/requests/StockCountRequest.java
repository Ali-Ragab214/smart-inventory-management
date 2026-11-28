package ali.ragab.saas_multi_tennant_app.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockCountRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Counted quantity is required")
    @PositiveOrZero(message = "Counted quantity must be zero or positive")
    private Integer countedQty;

    private String note;
}
