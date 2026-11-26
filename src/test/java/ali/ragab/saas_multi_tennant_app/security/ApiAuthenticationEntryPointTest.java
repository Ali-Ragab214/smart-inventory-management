package ali.ragab.saas_multi_tennant_app.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAuthenticationEntryPointTest {

    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(new JsonMapper());

    @Test
    void returnsJson401WhenRequestIsUnauthenticated() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        this.entryPoint.commence(request, response,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Missing credentials"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    void responseBodyContainsNoSensitiveFields() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        this.entryPoint.commence(request, response,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Missing credentials"));

        final String body = response.getContentAsString().toLowerCase();
        assertTrue(body.contains("message"));
        assertTrue(!body.contains("password"));
    }
}
