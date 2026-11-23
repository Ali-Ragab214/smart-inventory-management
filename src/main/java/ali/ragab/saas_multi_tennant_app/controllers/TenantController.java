package ali.ragab.saas_multi_tennant_app.controllers;

import ali.ragab.saas_multi_tennant_app.common.PageResponse;
import ali.ragab.saas_multi_tennant_app.responses.TenantResponse;
import ali.ragab.saas_multi_tennant_app.services.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PLATFORM_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<PageResponse<TenantResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "10") final int size) {
        return ResponseEntity.ok(this.tenantService.findAll(page, size));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable("id") final String id) {
        this.tenantService.approveTenant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable("id") final String id) {
        this.tenantService.activateTenant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable("id") final String id) {
        this.tenantService.deactivateTenant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable("id") final String id) {
        this.tenantService.suspendTenant(id);
        return ResponseEntity.noContent().build();
    }
}
