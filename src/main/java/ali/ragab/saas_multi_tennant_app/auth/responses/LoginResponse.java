package ali.ragab.saas_multi_tennant_app.auth.responses;
import lombok .*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
}