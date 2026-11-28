package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.requests.StockMovementRequest;
import ali.ragab.saas_multi_tennant_app.requests.StockReceiptRequest;
import ali.ragab.saas_multi_tennant_app.responses.LowStockItemResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;

import java.time.Instant;
import java.util.List;

public interface StockLedgerService {

    StockMovementResponse postMovement(StockMovementRequest request);

    StockMovementResponse receiveStock(StockReceiptRequest request);

    StockBalanceResponse getBalance(String productId);

    StockBalanceResponse recomputeBalance(String productId);

    PageResponse<StockMovementResponse> getMovementHistory(
            String productId, TypeMvt type, Instant from, Instant to, int page, int size);

    List<LowStockItemResponse> getLowStockItems();
}
