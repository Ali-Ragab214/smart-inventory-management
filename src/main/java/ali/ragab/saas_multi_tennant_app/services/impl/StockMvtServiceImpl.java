package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.StockMvtRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import ali.ragab.saas_multi_tennant_app.services.StockMvtService;

public class StockMvtServiceImpl implements StockMvtService {
    @Override
    public void create(StockMvtRequest request) {

    }

    @Override
    public void update(String id, StockMvtRequest request) {

    }

    @Override
    public StockMvtResponse findById(String id) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public PageResponse<StockMvtResponse> findAll(int page, int size) {
        return null;
    }
}
