package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMvtResponse;
import ali.ragab.saas_multi_tennant_app.services.StockMvtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-mvts")
@RequiredArgsConstructor
@Tag(
        name = "Stock Movements (Legacy Read-Only)",
        description = "Read-only view of ledger rows. To record stock changes use the 'Stock Operations' endpoints under /api/v1/stock/**."
)
public class StockMvtController {

    private final StockMvtService service;

    @GetMapping("/{stock-mvt-id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR')")
    @Operation(
            summary = "Get a stock movement by id (read-only)",
            description = "Returns a single ledger row. Movements cannot be created, modified or deleted through this API."
    )
    public ResponseEntity<StockMvtResponse> findStockMvtById(
            @PathVariable("stock-mvt-id")
            @NotNull(message = "Stock Mvt ID cannot be null")
            final String id
    ) {
        return ResponseEntity.ok(this.service.findById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR')")
    @Operation(
            summary = "List stock movements (read-only)",
            description = "Paginated list of ledger movements across all products. This API is read-only; the ledger is append-only and written only through /api/v1/stock/movements, /receipts and /counts."
    )
    public ResponseEntity<PageResponse<StockMvtResponse>> findAllStockMvts(
            @RequestParam(name = "page", defaultValue = "0")
            final int page,
            @RequestParam(name = "size", defaultValue = "10")
            final int size
    ) {
        return ResponseEntity.ok(this.service.findAll(page, size));
    }

    @GetMapping("/product/{product-id}")
    @Operation(
            summary = "List movements of a product (read-only)",
            description = "Prefer GET /api/v1/stock/products/{id}/movements, which supports type/date filters and returns the full ledger fields (reason, balanceAfter, sourceType...)."
    )
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

}
