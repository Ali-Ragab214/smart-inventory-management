package ali.ragab.saas_multi_tennant_app.security;

import ali.ragab.saas_multi_tennant_app.responses.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;

// This class is used to handle authentication errors in a Spring Security application. It implements the AuthenticationEntryPoint interface, which is called when a user tries to access a secured REST resource without providing valid credentials. The class constructs an ErrorResponse object containing details about the error, such as the timestamp, HTTP status code, error message, and the requested URI. It then sets the response status to 401 (Unauthorized), specifies the content type as JSON, and writes the serialized ErrorResponse to the response body.
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    //spring security call this method when user try to access a secured REST resource(endpoint) without supplying any credentials
    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException authException) throws IOException, ServletException {

        final ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("UNAUTHORIZED")
                .message("Full authentication is required to access this resource")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(this.jsonMapper.writeValueAsString(error));
    }
}
