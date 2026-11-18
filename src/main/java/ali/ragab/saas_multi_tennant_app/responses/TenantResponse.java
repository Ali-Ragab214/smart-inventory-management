package ali.ragab.saas_multi_tennant_app.responses;

import ali.ragab.saas_multi_tennant_app.entities.TenantStatus;
import lombok .*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TenantResponse {
    private String tenantId;
    private String companyName;
    private String companyCode;
    private String email;
    private String adminFullName;
    private String adminEmail;
    private String adminUserName;
    private String adminPassword;
    private LocalDateTime createdAt;
    private TenantStatus status;
}
