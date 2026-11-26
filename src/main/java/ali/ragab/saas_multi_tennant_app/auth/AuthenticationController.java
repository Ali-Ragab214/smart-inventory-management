package ali.ragab.saas_multi_tennant_app.auth;


import ali.ragab.saas_multi_tennant_app.auth.requests.LoginRequest;
import ali.ragab.saas_multi_tennant_app.auth.responses.LoginResponse;
import ali.ragab.saas_multi_tennant_app.auth.service.AuthenticationService;
import ali.ragab.saas_multi_tennant_app.requests.RegisterTenantRequest;
import ali.ragab.saas_multi_tennant_app.responses.UserResponse;
import ali.ragab.saas_multi_tennant_app.services.TenantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name="Authentication", description = "Authentication API")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final TenantService tenantService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid
            @RequestBody
            final LoginRequest request
    ) {
        final LoginResponse response = this.authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid
            @RequestBody
            final RegisterTenantRequest request
    ) {
        this.tenantService.registerTenant(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(final Authentication authentication) {
        final UserResponse user = this.authenticationService.me(authentication.getPrincipal().toString());
        return ResponseEntity.ok(user);
    }
}
