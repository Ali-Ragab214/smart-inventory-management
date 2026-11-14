package ali.ragab.saas_multi_tennant_app.requests;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequest {
    private String reference;
    private String name;
    private String description;
    private String categoryId;
    private Integer alertThreshold;
    private BigDecimal price;

}
