package iam.userservice.repository;

import iam.userservice.entity.User;
import iam.userservice.util.UserFilterCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static iam.userservice.config.AppConstants.EMAIL_FIELD;
import static iam.userservice.config.AppConstants.FIRST_NAME_FIELD;
import static iam.userservice.config.AppConstants.LAST_NAME_FIELD;
import static iam.userservice.config.AppConstants.PHONE_NUMBER_FIELD;

/**
 * Specification class for User entity to support open search text for user fields.
 * This specification searches across firstName, lastName, email, and phoneNumber fields
 * for the given search text.
 */
public class UserSearchTextSpecification implements Specification<User>, Predicates {

    private final UserFilterCriteria criteria;

    public UserSearchTextSpecification(UserFilterCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (criteria == null || !StringUtils.hasText(criteria.getSearchText())) {
            return criteriaBuilder.conjunction(); // Return always true predicate if no search text
        }

        String searchText = criteria.getSearchText().trim();

        // Create a list to hold individual field predicates
        List<Predicate> predicates = new ArrayList<>();

        // Add predicates for each field we want to search
        predicates.add(like(criteriaBuilder, root.get(FIRST_NAME_FIELD), searchText));
        predicates.add(like(criteriaBuilder, root.get(LAST_NAME_FIELD), searchText));
        predicates.add(like(criteriaBuilder, root.get(EMAIL_FIELD), searchText));
        predicates.add(like(criteriaBuilder, root.get(PHONE_NUMBER_FIELD), searchText));

        // Combine all predicates with OR (match any field)
        return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
    }
}
