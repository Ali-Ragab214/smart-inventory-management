package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.requests.StockCountRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;

public interface StockCountService {
    StockCountResponse recordCount(StockCountRequest request);
}
