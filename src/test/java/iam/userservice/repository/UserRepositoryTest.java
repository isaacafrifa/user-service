package iam.userservice.repository;

import iam.userservice.PostgresConfiguration;
import iam.userservice.entity.User;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataJpaTest
@Import(PostgresConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Running userRepository tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository underTest;
    /// This pattern (XXX) includes the 3-digit zone offset (e.g. +05:30 for India Standard Time).
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    public static final String PHONE_NUMBER = "1234567890";
    public static final String LAST_NAME = "Doe";
    public static final String FIRST_NAME = "John";
    public static final String EMAIL = "test@example.com";

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
    }

    @Test
    void findByEmail_shouldReturnEmptyOptionalWhenNotFound() {
        // When
        Optional<User> foundUser = underTest.findByEmail("nonexistent@example.com");
        // Then
        assertTrue(foundUser.isEmpty());
    }

    @Test
    void findByEmail_shouldReturnUserWhenFound() {
        // Given
        var user = getUser();
        underTest.save(user);
        // When
        Optional<User> foundUser = underTest.findByEmail(EMAIL);
        // Then
        assertTrue(foundUser.isPresent());
        var actualUser = foundUser.get();
        assertEquals(PHONE_NUMBER, actualUser.getPhoneNumber());
        assertEquals(FIRST_NAME, actualUser.getFirstName());
        assertEquals(LAST_NAME, actualUser.getLastName());
        assertEquals(EMAIL, actualUser.getEmail());
    }

    private @NotNull User getUser() {
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setEmail(EMAIL);
        user.setPhoneNumber(PHONE_NUMBER);
        user.setCreatedOn(OffsetDateTime.parse("2022-08-01T10:00:00+00:00", formatter));
        user.setUpdatedOn(OffsetDateTime.parse("2022-08-01T10:00:00+00:00", formatter));
        return user;
    }

}