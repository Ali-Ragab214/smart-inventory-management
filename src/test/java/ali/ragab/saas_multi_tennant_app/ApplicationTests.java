package ali.ragab.saas_multi_tennant_app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.platform-admin.username=platform-admin",
        "app.platform-admin.email=platform-admin@example.com",
        "app.platform-admin.password=platform-admin"
})
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
