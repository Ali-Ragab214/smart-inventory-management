package ali.ragab.saas_multi_tennant_app.services.impl;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.entities.Product;
import ali.ragab.saas_multi_tennant_app.entities.StockMvt;
import ali.ragab.saas_multi_tennant_app.mappers.StockMvtMapper;
import ali.ragab.saas_multi_tennant_app.repositories.ProductRepository;
import ali.ragab.saas_multi_tennant_app.repositories.StockMvtRepository;
import ali.ragab.saas_multi_tennant_app.requests.StockMvtRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import ali.ragab.saas_multi_tennant_app.services.StockMvtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMvtServiceImpl implements StockMvtService {
    private final StockMvtRepository repository;
    private final ProductRepository productRepository;
    private final StockMvtMapper stockMvtMapper;

    @Override
    public void create(StockMvtRequest request) {

        checkIfProductExistsById(request.getProductId());
        final StockMvt entity = this.stockMvtMapper.toEntity(request);
        entity.setDateMvt(LocalDate.now());
        this.repository.save(entity);
    }

    @Override
    public void update(String id, StockMvtRequest request) {

        //check if the stock movement exists
        final Optional<StockMvt> stockMvt = this.repository.findById(id);
        if (stockMvt.isEmpty()) {
            log.debug("StockMvt does not exist");
            throw new EntityNotFoundException("StockMvt does not exist");
        }
        //check if the product exists
         checkIfProductExistsById(request.getProductId());
        final StockMvt stockMvtToUpdate = this.stockMvtMapper.toEntity(request);
        stockMvtToUpdate.setDateMvt(LocalDate.now());
        stockMvtToUpdate.setId(id);
        this.repository.save(stockMvtToUpdate);

    }

    @Override
    public StockMvtResponse findById(String id) {
        return this.repository.findById(id)
                .map(this.stockMvtMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("StockMvt does not exist"));
    }

    @Override
    public void delete(String id) {
        final StockMvt stockMvt = this.repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("StockMvt does not exist"));
        stockMvt.setDeleted(true);
        this.repository.save(stockMvt);
    }

    @Override
    public PageResponse<StockMvtResponse> findAll(int page, int size) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<StockMvt> stockMvts = this.repository.findAll(pageRequest);
        final Page<StockMvtResponse> stockMvtResponses = stockMvts.map(this.stockMvtMapper::toResponse);
        return PageResponse.of(stockMvtResponses);
    }

    @Override
    public PageResponse<StockMvtResponse> findAllByProductId(String productId, int page, int size) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        final Page<StockMvt> stockMvts = this.repository.findAllByProductId(productId, pageRequest);
        final Page<StockMvtResponse> stockMvtResponses = stockMvts.map(this.stockMvtMapper::toResponse);
        return PageResponse.of(stockMvtResponses);    }


    private void checkIfProductExistsById(final String productId) {
        final Optional<Product> product = this.productRepository.findById(productId);
        if (product.isEmpty()) {
            log.debug("Product does not exist");
            throw new EntityNotFoundException("Product does not exist");
        }
    }
}
