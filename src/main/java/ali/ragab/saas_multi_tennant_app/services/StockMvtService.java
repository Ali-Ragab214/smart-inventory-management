package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.StockMvtRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;

public interface StockMvtService extends BasicService<StockMvtRequest, StockMvtResponse>{
    PageResponse<StockMvtResponse> findAllByProductId(final String productId, final int page, final int size);

}
