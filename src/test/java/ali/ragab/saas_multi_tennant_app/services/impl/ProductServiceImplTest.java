package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.entities.Category;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.exceptions.DuplicateResourceException;
import ali.ragab.saas_multi_tennant_app.mappers.ProductMapper;
import ali.ragab.saas_multi_tennant_app.repositories.CategoryRepository;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void updateAllowsTheProductToKeepItsOwnReference() {
        final Product product = product("product-id", "SKU-1");
        final Category category = Category.builder().id("category-id").name("Food").build();
        final ProductServiceImpl service = service();

        when(this.productRepository.findByIdAndDeletedFalse("product-id")).thenReturn(Optional.of(product));
        when(this.productRepository.findByReferenceIgnoreCaseAndDeletedFalse("SKU-1"))
                .thenReturn(Optional.of(product));
        when(this.categoryRepository.findById("category-id")).thenReturn(Optional.of(category));
        when(this.productRepository.save(product)).thenReturn(product);

        final var response = service.update("product-id", request("SKU-1"));

        assertEquals("product-id", response.getId());
        assertEquals("SKU-1", response.getReference());
        assertEquals("category-id", response.getCategoryId());
    }

    @Test
    void updatePreservesOmittedUnitOfMeasureAndActiveFlag() {
        final Product product = product("product-id", "SKU-1");
        product.setActive(false);
        final Category category = Category.builder().id("category-id").name("Food").build();
        final ProductRequest request = request("SKU-1");
        request.setUnitOfMeasure(null);
        request.setActive(null);

        when(this.productRepository.findByIdAndDeletedFalse("product-id")).thenReturn(Optional.of(product));
        when(this.productRepository.findByReferenceIgnoreCaseAndDeletedFalse("SKU-1"))
                .thenReturn(Optional.of(product));
        when(this.categoryRepository.findById("category-id")).thenReturn(Optional.of(category));
        when(this.productRepository.save(product)).thenReturn(product);

        final var response = service().update("product-id", request);

        assertEquals("KG", response.getUnitOfMeasure());
        assertEquals(false, response.getActive());
    }

    @Test
    void createRejectsAnExistingActiveReference() {
        when(this.productRepository.findByReferenceIgnoreCaseAndDeletedFalse("SKU-1"))
                .thenReturn(Optional.of(product("other-id", "SKU-1")));

        assertThrows(DuplicateResourceException.class, () -> service().create(request("SKU-1")));
    }

    @Test
    void softDeletedProductCannotBeFetched() {
        when(this.productRepository.findByIdAndDeletedFalse("deleted-product"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service().findById("deleted-product"));
    }

    @Test
    void deleteSoftDeletesProductWithoutModifyingStockMovements() {
        final Product product = product("product-id", "SKU-1");
        when(this.productRepository.findByIdAndDeletedFalse("product-id"))
                .thenReturn(Optional.of(product));

        service().delete("product-id");

        verify(this.productRepository).delete(product);
    }

    private ProductServiceImpl service() {
        return new ProductServiceImpl(this.productRepository, this.categoryRepository, new ProductMapper());
    }

    private ProductRequest request(final String reference) {
        return ProductRequest.builder()
                .name("Rice")
                .reference(reference)
                .description("Basmati rice")
                .price(new BigDecimal("10.00"))
                .alertThreshold(0)
                .categoryId("category-id")
                .unitOfMeasure("KG")
                .active(true)
                .build();
    }

    private Product product(final String id, final String reference) {
        return Product.builder()
                .id(id)
                .name("Rice")
                .reference(reference)
                .description("Basmati rice")
                .price(new BigDecimal("10.00"))
                .alertThreshold(0)
                .unitOfMeasure("KG")
                .active(true)
                .build();
    }
}
