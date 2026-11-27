package ali.ragab.saas_multi_tennant_app.mappers;


import ali.ragab.saas_multi_tennant_app.entities.Category;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(final ProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .reference(request.getReference())
                .description(request.getDescription())
                .price(request.getPrice())
                .alertThreshold(request.getAlertThreshold())
                .unitOfMeasure(request.getUnitOfMeasure() == null || request.getUnitOfMeasure().isBlank()
                        ? "UNIT"
                        : request.getUnitOfMeasure())
                .active(request.getActive() == null || request.getActive())
                .category(request.getCategoryId() == null
                        ? null
                        : Category.builder().id(request.getCategoryId()).build())
                .build();
    }

    public ProductResponse toResponse(final Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .reference(product.getReference())
                .description(product.getDescription())
                .price(product.getPrice())
                .alertThreshold(product.getAlertThreshold())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                .categoryName(product.getCategory() == null
                        ? null
                        : product.getCategory().getName())
                .unitOfMeasure(product.getUnitOfMeasure())
                .active(product.getActive())
                // .availableQuantity() to be later implemented
                .build();
    }

}