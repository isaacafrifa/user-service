package iam.userservice.repository;

import iam.userservice.entity.User;
import iam.userservice.util.UserFilterCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserFilterSpecification Tests")
class UserFilterSpecificationTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @DisplayName("toPredicate should create predicates for non-empty criteria with exact user IDs")
    void toPredicate_shouldCreatePredicatesForNonEmptyCriteriaWithExactUserIds() {
        // Given
        UserFilterCriteria criteria = new UserFilterCriteria();
        criteria.setUserIds(List.of(1L));
        criteria.setFirstNames(List.of("John"));
        criteria.setLastNames(List.of("Doe"));
        criteria.setEmails(List.of("john.doe@example.com"));
        criteria.setPhoneNumbers(List.of("1234567890"));
        criteria.setExactUserIdsFlag(true);

        // Mock objects
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        // Mock predicates
        Predicate idPredicate = mock(Predicate.class);
        Predicate firstNameInPredicate = mock(Predicate.class);
        Predicate lastNameInPredicate = mock(Predicate.class);
        Predicate emailInPredicate = mock(Predicate.class);
        Predicate phoneNumberInPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        // Mock paths
        Path idPath = mock(Path.class);
        Path firstNamePath = mock(Path.class);
        Path lastNamePath = mock(Path.class);
        Path emailPath = mock(Path.class);
        Path phoneNumberPath = mock(Path.class);

        // Setup mocks for the test
        when(root.get("id")).thenReturn(idPath);
        when(root.get("firstName")).thenReturn(firstNamePath);
        when(root.get("lastName")).thenReturn(lastNamePath);
        when(root.get("email")).thenReturn(emailPath);
        when(root.get("phoneNumber")).thenReturn(phoneNumberPath);

        when(criteriaBuilder.equal(any(), eq(1L))).thenReturn(idPredicate);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(idPredicate);

        when(firstNamePath.in(any(List.class))).thenReturn(firstNameInPredicate);
        when(lastNamePath.in(any(List.class))).thenReturn(lastNameInPredicate);
        when(emailPath.in(any(List.class))).thenReturn(emailInPredicate);
        when(phoneNumberPath.in(any(List.class))).thenReturn(phoneNumberInPredicate);

        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

        // When
        UserFilterSpecification specification = new UserFilterSpecification(criteria);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Then
        assertNotNull(result);
        assertEquals(finalPredicate, result);

        // Verify interactions
        verify(criteriaBuilder).equal(any(), eq(1L));
        verify(criteriaBuilder).or(any(Predicate[].class));
        verify(firstNamePath).in(any(List.class));
        verify(lastNamePath).in(any(List.class));
        verify(emailPath).in(any(List.class));
        verify(phoneNumberPath).in(any(List.class));
        verify(criteriaBuilder).and(any(Predicate[].class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @DisplayName("toPredicate should create predicates for non-empty criteria with non-exact user IDs")
    void toPredicate_shouldCreatePredicatesForNonEmptyCriteriaWithNonExactUserIds() {
        // Given
        UserFilterCriteria criteria = new UserFilterCriteria();
        criteria.setUserIds(List.of(1L));
        criteria.setExactUserIdsFlag(false);

        // Mock objects
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        // Mock predicates
        Predicate idLikePredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        // Mock paths and expressions
        Path idPath = mock(Path.class);
        Expression stringExpression = mock(Expression.class);

        // Setup mocks for the test
        when(root.get("id")).thenReturn(idPath);
        when(criteriaBuilder.function(anyString(), eq(String.class), any())).thenReturn(stringExpression);
        when(criteriaBuilder.like(any(), anyString(), anyChar())).thenReturn(idLikePredicate);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(idLikePredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

        // When
        UserFilterSpecification specification = new UserFilterSpecification(criteria);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Then
        assertNotNull(result);
        assertEquals(finalPredicate, result);

        // Verify interactions
        verify(criteriaBuilder).function(anyString(), eq(String.class), any());
        verify(criteriaBuilder).like(any(), anyString(), anyChar());
        verify(criteriaBuilder).or(any(Predicate[].class));
        verify(criteriaBuilder).and(any(Predicate[].class));
    }

    @Test
    @DisplayName("toPredicate should not create predicates for empty criteria")
    void toPredicate_shouldNotCreatePredicatesForEmptyCriteria() {
        // Given
        UserFilterCriteria criteria = new UserFilterCriteria();
        // All fields are null or empty

        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Predicate finalPredicate = mock(Predicate.class);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

        // When
        UserFilterSpecification specification = new UserFilterSpecification(criteria);
        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Then
        assertNotNull(result);
        assertEquals(finalPredicate, result);

        // Verify only the final and operation
        verify(criteriaBuilder).and(any(Predicate[].class));
    }
}
