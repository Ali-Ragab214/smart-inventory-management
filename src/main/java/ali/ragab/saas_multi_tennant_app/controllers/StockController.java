package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.entities.TypeMvt;
import ali.ragab.saas_multi_tennant_app.requests.StockCountRequest;
import ali.ragab.saas_multi_tennant_app.requests.StockMovementRequest;
import ali.ragab.saas_multi_tennant_app.requests.StockReceiptRequest;
import ali.ragab.saas_multi_tennant_app.responses.LowStockItemResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockBalanceResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockCountResponse;
import ali.ragab.saas_multi_tennant_app.responses.StockMovementResponse;
import ali.ragab.saas_multi_tennant_app.services.StockCountService;
import ali.ragab.saas_multi_tennant_app.services.StockLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock Operations", description = "Stock ledger, balances, receipts, counts, and history")
public class StockController {

    private final StockLedgerService stockLedgerService;
    private final StockCountService stockCountService;

    @PostMapping("/movements")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR','ROLE_USER')")
    @Operation(summary = "Post a stock movement (IN or OUT)")
    public ResponseEntity<StockMovementResponse> postMovement(
            @RequestBody @Valid final StockMovementRequest request) {
        return ResponseEntity.status(201).body(this.stockLedgerService.postMovement(request));
    }

    @PostMapping("/receipts")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR','ROLE_USER')")
    @Operation(summary = "Receive stock (goods arrived)")
    public ResponseEntity<StockMovementResponse> receiveStock(
            @RequestBody @Valid final StockReceiptRequest request) {
        return ResponseEntity.status(201).body(this.stockLedgerService.receiveStock(request));
    }

    @PostMapping("/counts")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR','ROLE_USER')")
    @Operation(summary = "Record a physical stock count")
    public ResponseEntity<StockCountResponse> recordCount(
            @RequestBody @Valid final StockCountRequest request) {
        return ResponseEntity.status(201).body(this.stockCountService.recordCount(request));
    }

    @GetMapping("/products/{id}/movements")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR','ROLE_USER')")
    @Operation(summary = "Get movement history for a product")
    public ResponseEntity<PageResponse<StockMovementResponse>> getMovementHistory(
            @PathVariable("id") final String productId,
            @RequestParam(value = "type", required = false) final TypeMvt type,
            @RequestParam(value = "from", required = false) final Instant from,
            @RequestParam(value = "to", required = false) final Instant to,
            @RequestParam(value = "page", defaultValue = "0") final int page,
            @RequestParam(value = "size", defaultValue = "20") final int size) {
        return ResponseEntity.ok(
                this.stockLedgerService.getMovementHistory(productId, type, from, to, page, size));
    }

    @GetMapping("/products/{id}/balance")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_SALES_OPERATOR','ROLE_USER')")
    @Operation(summary = "Get current stock balance for a product")
    public ResponseEntity<StockBalanceResponse> getBalance(
            @PathVariable("id") final String productId) {
        return ResponseEntity.ok(this.stockLedgerService.getBalance(productId));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR')")
    @Operation(summary = "Get products with low stock (on_hand <= alert_threshold)")
    public ResponseEntity<List<LowStockItemResponse>> getLowStock() {
        return ResponseEntity.ok(this.stockLedgerService.getLowStockItems());
    }

    @PostMapping("/products/{id}/recompute-balance")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR')")
    @Operation(summary = "Recompute stock balance from ledger (manager only)")
    public ResponseEntity<StockBalanceResponse> recomputeBalance(
            @PathVariable("id") final String productId) {
        return ResponseEntity.ok(this.stockLedgerService.recomputeBalance(productId));
    }
}
