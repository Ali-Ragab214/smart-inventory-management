package ali.ragab.saas_multi_tennant_app.services;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;

public interface ProductService {

    ProductResponse create(ProductRequest request);
    ProductResponse update(String id, ProductRequest request);
    ProductResponse findById(String id);
    void delete(String id);
    PageResponse<ProductResponse> findAll(int page, int size, String search);
    ProductResponse findByReference(String reference);
}
