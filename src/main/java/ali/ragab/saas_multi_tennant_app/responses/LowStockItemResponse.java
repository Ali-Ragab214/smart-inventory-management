package ali.ragab.saas_multi_tennant_app.responses;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LowStockItemResponse {
    private String productId;
    private String productName;
    private String productReference;
    private Integer onHand;
    private Integer alertThreshold;
    private Integer shortfall;
}
