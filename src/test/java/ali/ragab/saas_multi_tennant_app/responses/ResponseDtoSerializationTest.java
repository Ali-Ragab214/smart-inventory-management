package ali.ragab.saas_multi_tennant_app.responses;

import ali.ragab.saas_multi_tennant_app.entities.TenantStatus;
import ali.ragab.saas_multi_tennant_app.entities.UserRole;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ResponseDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void responsesDoNotExposePasswordOrHashFields() throws Exception {
        final UserResponse userResponse = UserResponse.builder()
                .id("user-id")
                .username("admin")
                .email("admin@acme.test")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ROLE_COMPANY_ADMIN)
                .build();

        final TenantResponse tenantResponse = TenantResponse.builder()
                .tenantId("tenant-id")
                .companyName("Acme")
                .companyCode("ACME")
                .email("info@acme.test")
                .adminFullName("Admin User")
                .adminEmail("admin@acme.test")
                .adminUserName("admin")
                .createdAt(LocalDateTime.now())
                .status(TenantStatus.ACTIVE)
                .build();

        assertNoSecrets(this.objectMapper.writeValueAsString(userResponse));
        assertNoSecrets(this.objectMapper.writeValueAsString(tenantResponse));
    }

    private void assertNoSecrets(final String json) {
        final String lowerCaseJson = json.toLowerCase();
        assertFalse(lowerCaseJson.contains("password"), json);
        assertFalse(lowerCaseJson.contains("hash"), json);
    }
}
