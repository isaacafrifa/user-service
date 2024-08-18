package iam.userservice.repository;

import iam.userservice.PostgresConfiguration;
import iam.userservice.entity.Role;
import iam.userservice.entity.User;
import iam.userservice.enums.RoleEnum;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataJpaTest
@Import(PostgresConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository underTest;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        createDefaultRoles();
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
    }

    @Test
    void findByUsername_shouldReturnEmptyOptionalWhenNotFound() {
        // When
        Optional<User> foundUser = underTest.findByUsername("nonexistentUser");
        // Then
        assertTrue(foundUser.isEmpty());
    }

    @Test
    void findByEmail_shouldReturnEmptyOptionalWhenNotFound() {
        // When
        Optional<User> foundUser = underTest.findByEmail("nonexistent@example.com");
        // Then
        assertTrue(foundUser.isEmpty());
    }

    @Test
    void findByUsername_shouldReturnUserWhenFound() {
        // Given
        var user = getUser();
        underTest.save(user);
        // When
        Optional<User> foundUser = underTest.findByUsername("testUser");
        // Then
        assertTrue(foundUser.isPresent());
        assertFalse(foundUser.get().getRoles().isEmpty());
    }

    @Test
    void findByEmail_shouldReturnUserWhenFound() {
        // Given
        var user = getUser();
        underTest.save(user);
        // When
        Optional<User> foundUser = underTest.findByEmail("test@example.com");
        // Then
        assertTrue(foundUser.isPresent());
    }

    private @NotNull User getUser() {
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        Optional<Role> optionalRole = roleRepository.findByName(RoleEnum.USER);
        assertTrue(optionalRole.isPresent());
        var userRole = optionalRole.get();
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setRoles(userRoles);
        return user;
    }

    private void createDefaultRoles() {
        Role userRole = new Role();
        userRole.setName(RoleEnum.USER);
        roleRepository.save(userRole);
    }
}