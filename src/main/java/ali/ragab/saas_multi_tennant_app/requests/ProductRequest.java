package ali.ragab.saas_multi_tennant_app.requests;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequest {
    @NotBlank(message = "Product name should not be empty")
    @Size(min = 3, max = 255, message = "Product name should be between 3 and 255 characters")
    private String name;
    @NotBlank(message = "Product reference should not be empty")
    @Size(min = 3, max = 255, message = "Product reference should be between 3 and 255 characters")
    private String reference;
    private String description;
    @NotNull(message = "Alert threshold is required")
    @PositiveOrZero(message = "Alert threshold should be zero or a positive number")
    private Integer alertThreshold;
    @NotNull(message = "Price is required")
    @Positive(message = "Price should be a positive number")
    private BigDecimal price;
    @NotNull(message = "Category ID is required")
    private String categoryId;
    @Size(max = 20, message = "Unit of measure must not exceed 20 characters")
    private String unitOfMeasure;
    private Boolean active;

}
