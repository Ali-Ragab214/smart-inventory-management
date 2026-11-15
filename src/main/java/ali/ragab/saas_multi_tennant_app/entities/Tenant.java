package ali.ragab.saas_multi_tennant_app.entities;

import ali.ragab.saas_multi_tennant_app.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tenants")
public class Tenant extends AbstractEntity{

    @Column(name = "name", nullable = false)
    private String companyName;

    @Column(name = "company_code", nullable = false, unique = true)
    private String companyCode;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.PENDING;

    @Column(name = "admin_full_name", nullable = false)
    private String adminFullName;

    @Column(name = "admin_email", nullable = false, unique = true)
    private String adminEmail;

    @Column(name = "admin_user_name", nullable = false, unique = true)
    private String adminUserName;

    @Column(name = "admin_password", nullable = false)
    private String adminPassword;
}
