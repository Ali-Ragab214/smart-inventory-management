package ali.ragab.saas_multi_tennant_app.requests;

import lombok .*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterTenantRequest {

    @NotBlank(message = "Company name should not be empty")
    private String companyName;
    @NotBlank(message = "Company code should not be empty")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{1,50}$",
            message = "Company code must contain only letters, digits and underscores (max 50 characters)")
    private String companyCode;
    @NotBlank(message = "Email should not be empty")
    private String email;
    @NotBlank(message = "Admin full name should not be empty")
    private String adminFullName;
    @NotBlank(message = "Admin email should not be empty")
    private String adminEmail;
    @NotBlank(message = "Admin username should not be empty")
    private String adminUserName;
    @NotBlank(message = "Admin password should not be empty")
    private String adminPassword;
}
