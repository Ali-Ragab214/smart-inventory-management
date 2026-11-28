package ali.ragab.saas_multi_tennant_app.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockCountResponse {
    private String id;
    private String productId;
    private Integer countedQty;
    private Integer systemQty;
    private Integer variance;
    private String note;
    private String countedBy;
    private Instant countedAt;
    private String adjustmentMovementId;
}
