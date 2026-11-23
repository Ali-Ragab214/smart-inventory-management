package ali.ragab.saas_multi_tennant_app.mappers;

import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.requests.StockMvtRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import org.springframework.stereotype.Component;

@Component
public class StockMvtMapper {

    public StockMvt toEntity(final StockMvtRequest request) {
        return StockMvt.builder()
                .dateMvt(request.getDateMvt())
                .comment(request.getComment())
                .typeMvt(request.getTypeMvt())
                .quantity(request.getQuantity())
                .product(Product.builder()
                        .id(request.getProductId())
                        .build())
                .deleted(false)
                .build();
    }

    public StockMvtResponse toResponse(final StockMvt stockMvt)
    {
        return StockMvtResponse.builder()
                .id(stockMvt.getId())
                .quantity(stockMvt.getQuantity())
                .typeMvt(stockMvt.getTypeMvt())
                .comment(stockMvt.getComment())
                .dateMvt(stockMvt.getDateMvt())
                .build();
    }

}
