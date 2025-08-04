package de.resume.inventory.management.system.productservice;

import de.resume.inventory.management.system.productservice.config.TestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
class ProductserviceApplicationTests {

    @Test
    void contextLoads() {
    }

}
