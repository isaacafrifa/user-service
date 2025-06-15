package iam.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PostgresConfiguration.class)
public class TestUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestUserServiceApplication.class, args);
    }

}
