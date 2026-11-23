package ali.ragab.saas_multi_tennant_app.responses;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
}