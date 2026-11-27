package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.entities.Category;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.exceptions.DuplicateResourceException;
import ali.ragab.saas_multi_tennant_app.mappers.ProductMapper;
import ali.ragab.saas_multi_tennant_app.repositories.CategoryRepository;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockMvtRepository;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;
import ali.ragab.saas_multi_tennant_app.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final StockMvtRepository stockMvtRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse create(final ProductRequest request) {

        checkIfProductAlreadyExistsByReference(request.getReference(), null);
        final Category category = resolveCategory(request.getCategoryId());

        final Product entity = this.productMapper.toEntity(request);
        entity.setCategory(category);
        return this.productMapper.toResponse(this.repository.save(entity));
    }

    @Override
    public ProductResponse update(final String id, final ProductRequest request) {
        final Product product = this.repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Product does not exist"));

        checkIfProductAlreadyExistsByReference(request.getReference(), id);
        final Category category = resolveCategory(request.getCategoryId());

        product.setName(request.getName());
        product.setReference(request.getReference());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setAlertThreshold(request.getAlertThreshold());
        if (request.getUnitOfMeasure() != null && !request.getUnitOfMeasure().isBlank()) {
            product.setUnitOfMeasure(request.getUnitOfMeasure());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setCategory(category);

        return this.productMapper.toResponse(this.repository.save(product));
    }

    @Override
    public PageResponse<ProductResponse> findAll(final int page, final int size, final String search) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<Product> products = search == null || search.isBlank()
                ? this.repository.findAllByDeletedFalse(pageRequest)
                : this.repository.searchActiveProducts(search.trim(), pageRequest);
        final Page<ProductResponse> productResponses = products.map(this.productMapper::toResponse);
        return PageResponse.of(productResponses);
    }

    @Override
    public ProductResponse findById(final String id) {
        return this.repository.findByIdAndDeletedFalse(id)
                .map(this.productMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product does not exist"));
    }

    @Override
    public void delete(final String id) {
        final Product product = this.repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Product does not exist"));

        // Soft delete keeps the row, so the FK cannot cascade for us.
        final int movements = this.stockMvtRepository.softDeleteByProductId(id);
        log.debug("Soft-deleted {} stock movements for product {}", movements, id);

        this.repository.delete(product);
    }

    private void checkIfProductAlreadyExistsByReference(String reference, String excludedId) {
        final Optional<Product> product = this.repository.findByReferenceIgnoreCaseAndDeletedFalse(reference);
        if (product.isPresent() && (excludedId == null || !product.get().getId().equals(excludedId))) {
            log.debug("Product already exists");
            throw new DuplicateResourceException("Product with reference " + reference + " already exists");
        }
    }

    private Category resolveCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        return this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category does not exist"));
    }

    @Override
    public ProductResponse findByReference(final String reference) {
        return this.repository.findByReferenceIgnoreCaseAndDeletedFalse(reference)
                .map(this.productMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product does not exist"));
    }
}