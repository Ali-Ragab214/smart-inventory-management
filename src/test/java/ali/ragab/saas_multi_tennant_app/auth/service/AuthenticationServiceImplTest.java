package ali.ragab.saas_multi_tennant_app.auth.service;

import ali.ragab.saas_multi_tennant_app.entities.User;
import ali.ragab.saas_multi_tennant_app.mappers.UserMapper;
import ali.ragab.saas_multi_tennant_app.repositories.UserRepository;
import ali.ragab.saas_multi_tennant_app.responses.UserResponse;
import ali.ragab.saas_multi_tennant_app.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private AuthenticationServiceImpl service;

    @Test
    void meReturnsMappedUserResponseForAuthenticatedUser() {
        final User user = User.builder().id("user-id").username("admin").email("admin@acme.test").build();
        final UserResponse expected = UserResponse.builder()
                .id("user-id").username("admin").email("admin@acme.test").build();
        when(this.userRepository.findByIdAndNotDeleted("user-id")).thenReturn(Optional.of(user));
        when(this.userMapper.toResponse(user)).thenReturn(expected);

        assertEquals(expected, this.service.me("user-id"));
    }

    @Test
    void meThrowsWhenUserIsNotFound() {
        when(this.userRepository.findByIdAndNotDeleted("missing-id")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> this.service.me("missing-id"));
    }
}
