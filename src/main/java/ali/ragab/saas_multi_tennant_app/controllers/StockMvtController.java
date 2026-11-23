package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.StockMvtRequest;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import ali.ragab.saas_multi_tennant_app.services.StockMvtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-mvts")
@RequiredArgsConstructor
@Tag(name = "Stock Movement", description = "Stock Movement API")
public class StockMvtController {

    private final StockMvtService service;


    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR')")
    public ResponseEntity<Void> createStockMvt(
            @RequestBody
            @Valid
            final StockMvtRequest request
    ) {
        this.service.create(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{stock-mvt-id}")
    public ResponseEntity<Void> updateStockMvt(
            @RequestBody
            @Valid
            final StockMvtRequest request,
            @PathVariable("stock-mvt-id")
            @NotNull(message = "Stock Mvt ID cannot be null")
            final String id
    ) {
        this.service.update(id, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{stock-mvt-id}")
    @PreAuthorize("hasAnyRole('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR')")
    public ResponseEntity<StockMvtResponse> findStockMvtById(
            @PathVariable("stock-mvt-id")
            @NotNull(message = "Stock Mvt ID cannot be null")
            final String id
    ) {
        return ResponseEntity.ok(this.service.findById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR')")
    public ResponseEntity<PageResponse<StockMvtResponse>> findAllStockMvts(
            @RequestParam(name = "page", defaultValue = "0")
            final int page,
            @RequestParam(name = "size", defaultValue = "10")
            final int size
    ) {
        return ResponseEntity.ok(this.service.findAll(page, size));
    }

    @GetMapping("/product/{product-id}")
    public ResponseEntity<PageResponse<StockMvtResponse>> findAllStockMvtsByProductId(
            @PathVariable("product-id")
            @NotNull(message = "Product ID cannot be null")
            final String productId,
            @RequestParam(name = "page", defaultValue = "0")
            final int page,
            @RequestParam(name = "size", defaultValue = "10")
            final int size
    ) {
        return ResponseEntity.ok(this.service.findAllByProductId(productId, page, size));
    }

    @DeleteMapping("/{stock-mvt-id}")
    public ResponseEntity<Void> deleteStockMvt(
            @PathVariable("stock-mvt-id")
            @NotNull(message = "Stock Mvt ID cannot be null")
            final String id
    ) {
        this.service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
