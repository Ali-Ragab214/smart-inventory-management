package ali.ragab.saas_multi_tennant_app.responses;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String reference;
    private String name;
    private String description;
    private String categoryId;
    private Integer alertThreshold;
    private String categoryName;
    private BigDecimal price;
    private String unitOfMeasure;
    private Boolean active;

}
