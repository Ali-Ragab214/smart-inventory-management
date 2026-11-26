package ali.ragab.saas_multi_tennant_app.auth;

import ali.ragab.saas_multi_tennant_app.auth.service.AuthenticationService;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import ali.ragab.saas_multi_tennant_app.responses.UserResponse;
import ali.ragab.saas_multi_tennant_app.services.TenantService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationControllerTest {

    @Test
    void meReturnsAuthenticatedUsersSafeProfile() throws Exception {
        final AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        final AuthenticationController controller = new AuthenticationController(
                authenticationService, Mockito.mock(TenantService.class));
        final UserResponse user = UserResponse.builder()
                .id("user-id")
                .username("admin")
                .email("admin@acme.test")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ROLE_PLATFORM_ADMIN)
                .build();
        when(authenticationService.me("user-id")).thenReturn(user);

        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user-id", null, List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));

        final ResponseEntity<UserResponse> response = controller.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user-id", response.getBody().getId());
        assertEquals("admin", response.getBody().getUsername());

        final String json = new ObjectMapper().writeValueAsString(response.getBody());
        assertFalse(json.contains("password"));
        verify(authenticationService).me("user-id");
    }
}
