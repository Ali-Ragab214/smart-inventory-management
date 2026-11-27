package ali.ragab.saas_multi_tennant_app.controllers;


import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.ProductRequest;
import ali.ragab.saas_multi_tennant_app.responses.ProductResponse;
import ali.ragab.saas_multi_tennant_app.services.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product API")
public class ProductController {

    private final ProductService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid
            @RequestBody
            final ProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.service.create(request));
    }

    @PutMapping("/{product-id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<ProductResponse> updateProduct(
      @RequestBody
      @Valid
        final ProductRequest request,
        @PathVariable("product-id")
        final String productId)
    {
        return ResponseEntity.ok(this.service.update(productId, request));
    }

    @GetMapping("/{product-id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR','USER','SALES_OPERATOR')")
    public ResponseEntity<ProductResponse> findProductById(
            @PathVariable("product-id")
            @NotNull(message = "Product ID cannot be null")
            final String id
    ) {
        return ResponseEntity.ok(this.service.findById(id));
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR','USER','SALES_OPERATOR')")
    public ResponseEntity<PageResponse<ProductResponse>> findAllProducts(
            @RequestParam(name = "page", defaultValue = "0")
            final int page,
            @RequestParam(name = "size", defaultValue = "10") final int size,
            @RequestParam(name = "search", required = false) final String search
    )  {
        return ResponseEntity.ok(this.service.findAll(page, size, search));
    }

    @GetMapping("/by-sku/{reference}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR','USER','SALES_OPERATOR')")
    public ResponseEntity<ProductResponse> findProductByReference(
            @PathVariable("reference") final String reference) {
        return ResponseEntity.ok(this.service.findByReference(reference));
    }

    @DeleteMapping("/{product-id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("product-id")
            @NotNull(message = "Product ID cannot be null")
            final String id
    ) {
        this.service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
