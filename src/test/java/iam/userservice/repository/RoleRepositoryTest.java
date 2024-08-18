package iam.userservice.repository;

import iam.userservice.PostgresConfiguration;
import iam.userservice.entity.Role;
import iam.userservice.enums.RoleEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataJpaTest
@Import(PostgresConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RoleRepositoryTest {
    @Autowired
    private RoleRepository underTest;

    @BeforeEach
    void setUp() {
        createDefaultRoles();
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
    }

    @Test
    void findByName_shouldReturnRoleWhenFound() {
        // Given
        String roleName = RoleEnum.USER.name();
        // When
        Optional<Role> foundRole = underTest.findByName(RoleEnum.USER);
        // Then
        assertTrue(foundRole.isPresent());
        assertEquals(roleName, foundRole.get().getName().name());
    }
    @Test
    void findByName_shouldReturnEmptyWhenNotFound() {
       // When
        Optional<Role> foundRole = underTest.findByName(RoleEnum.ADMIN);
        // Then
        assertTrue(foundRole.isEmpty());
    }

    @Test
    void findByName_shouldThrowExceptionForInvalidRoleName() {
        // Given
        String invalidRoleName = "nonexistentRole";
        // When
        assertThrows(IllegalArgumentException.class,
                () -> underTest.findByName(RoleEnum.valueOf(invalidRoleName))
        );
    }

    private void createDefaultRoles() {
        Role userRole = new Role();
        userRole.setName(RoleEnum.USER);
        underTest.save(userRole);
    }
}