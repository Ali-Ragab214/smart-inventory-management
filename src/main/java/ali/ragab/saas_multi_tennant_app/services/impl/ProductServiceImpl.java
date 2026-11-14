package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;
import ali.ragab.saas_multi_tennant_app.services.ProductService;

public class ProductServiceImpl implements ProductService {

    @Override
    public void create(ProductRequest request) {

    }

    @Override
    public void update(String id, ProductRequest request) {

    }

    @Override
    public ProductResponse findById(String id) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public PageResponse<ProductResponse> findAll(int page, int size) {
        return null;
    }
}
