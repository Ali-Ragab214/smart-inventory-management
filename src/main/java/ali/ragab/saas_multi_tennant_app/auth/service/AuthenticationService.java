package ali.ragab.saas_multi_tennant_app.auth.service;

import ali.ragab.saas_multi_tennant_app.auth.requests.LoginRequest;
import ali.ragab.saas_multi_tennant_app.auth.responses.LoginResponse;
import ali.ragab.saas_multi_tennant_app.responses.UserResponse;

public interface AuthenticationService {

    LoginResponse login(final LoginRequest request);

    UserResponse me(final String userId);

}
