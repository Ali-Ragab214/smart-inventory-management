package ali.ragab.saas_multi_tennant_app.responses;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockBalanceResponse {
    private String productId;
    private String productName;
    private String productReference;
    private Integer onHand;
    private Integer reserved;
    private Integer available;
}
