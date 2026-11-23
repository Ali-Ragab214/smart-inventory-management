package ali.ragab.saas_multi_tennant_app.auth.service;

import ali.ragab.saas_multi_tennant_app.auth.requests.LoginRequest;
import ali.ragab.saas_multi_tennant_app.auth.responses.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(final LoginRequest request);

}
