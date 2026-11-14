package ali.ragab.saas_multi_tennant_app.responses;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMvtResponse {

    private Integer quantity;
    private String typeMvt;
    private String comment;
    private String dateMvt;
}
