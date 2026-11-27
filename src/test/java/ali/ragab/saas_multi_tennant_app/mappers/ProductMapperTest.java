package ali.ragab.saas_multi_tennant_app.mappers;

import ali.ragab.saas_multi_tennant_app.entities.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductMapperTest {

    @Test
    void mapsAnUncategorizedProductWithoutThrowing() {
        final Product product = Product.builder()
                .id("product-id")
                .name("Uncategorized")
                .reference("SKU-1")
                .price(BigDecimal.ONE)
                .alertThreshold(0)
                .unitOfMeasure("UNIT")
                .active(true)
                .build();

        final var response = new ProductMapper().toResponse(product);

        assertEquals("product-id", response.getId());
        assertNull(response.getCategoryId());
        assertNull(response.getCategoryName());
    }
}
