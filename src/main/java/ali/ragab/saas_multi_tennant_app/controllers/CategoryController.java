package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.requests.CategoryRequest;
import ali.ragab.saas_multi_tennant_app.responses.CategoryResponse;
import ali.ragab.saas_multi_tennant_app.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
    private final CategoryService categoryService;


    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR')")
    public ResponseEntity<Void> createCategory(
            @Valid
            @RequestBody final CategoryRequest request) {
        categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR')")
    public ResponseEntity<Void> updateCategory(
            @PathVariable("id") final String id,
            @Valid
            @RequestBody final CategoryRequest request) {
        categoryService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_USER','ROLE_SALES_OPERATOR')")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @PathVariable("id") final String id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR','ROLE_USER','ROLE_SALES_OPERATOR')")
    public ResponseEntity<PageResponse<CategoryResponse>> findAllCategories(
            @RequestParam(name = "page", defaultValue = "0")
            final int page,
            @RequestParam(name = "size", defaultValue = "10")
            final int size
    ) {
        return ResponseEntity.ok(this.categoryService.findAll(page, size));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COMPANY_ADMIN','ROLE_ADMINISTRATOR')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable("id") final String id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

}