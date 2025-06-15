package iam.userservice.cucumber.config;

import iam.userservice.TestUserServiceApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Configuration class for Cucumber tests.
 * This class configures the Spring context for the Cucumber tests.
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = TestUserServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    /**
     * Creates a WebTestClient bean for making HTTP requests to the API.
     * 
     * @return a configured WebTestClient
     */
    @Bean
    public WebTestClient webTestClient() {
        return WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    }
}
