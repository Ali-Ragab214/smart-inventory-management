package ali.ragab.saas_multi_tennant_app.requests;


import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMvtRequest {
    private String comment;
    private Integer quantity;
    private TypeMvt typeMvt;
    private LocalDate dateMvt;
    private String productId;

}
