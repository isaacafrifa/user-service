package iam.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "iam.userservice.repository")
@EntityScan(basePackages = "iam.userservice.entity")
@Import(PostgresConfiguration.class)
public class TestUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestUserServiceApplication.class, args);
    }

}
