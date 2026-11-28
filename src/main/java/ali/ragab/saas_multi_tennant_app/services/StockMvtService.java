package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;

public interface StockMvtService {
    StockMvtResponse findById(final String id);
    PageResponse<StockMvtResponse> findAll(final int page, final int size);
    PageResponse<StockMvtResponse> findAllByProductId(final String productId, final int page, final int size);
}
