package ali.ragab.saas_multi_tennant_app.mappers;

import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockBalance;
import ali.ragab.saas_multi_tennant_app.responses.LowStockItemResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import org.springframework.stereotype.Component;

@Component
public class StockBalanceMapper {

    public StockBalanceResponse toResponse(final StockBalance balance, final Product product) {
        return StockBalanceResponse.builder()
                .productId(balance.getProductId())
                .productName(product != null ? product.getName() : null)
                .productReference(product != null ? product.getReference() : null)
                .onHand(balance.getOnHand())
                .reserved(balance.getReserved())
                .available(balance.getOnHand() - balance.getReserved())
                .build();
    }

    public LowStockItemResponse toLowStockResponse(final StockBalance balance, final Product product) {
        final int threshold = product.getAlertThreshold();
        return LowStockItemResponse.builder()
                .productId(balance.getProductId())
                .productName(product.getName())
                .productReference(product.getReference())
                .onHand(balance.getOnHand())
                .alertThreshold(threshold)
                .shortfall(threshold - balance.getOnHand())
                .build();
    }
}
