package iam.userservice.repository;

import iam.userservice.entity.User;
import iam.userservice.util.UserFilterCriteria;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Specification class for User entity to support dynamic filtering.
 */
@RequiredArgsConstructor
public class UserFilterSpecification implements Specification<User>, Predicates {

    private static final String FIRST_NAME_FIELD = "firstName";
    private static final String LAST_NAME_FIELD = "lastName";
    private static final String EMAIL_FIELD = "email";
    private static final String PHONE_NUMBER_FIELD = "phoneNumber";

    private final UserFilterCriteria criteria;

    @Override
    public Predicate toPredicate( final Root<User> root,
                                 @Nullable final CriteriaQuery<?> query,
                                 final CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();

        // Add user ID predicates based on the exact flag
        addPredicateIfValuesPresent(
                criteria.getUserIds(),
                () -> criteria.isExactUserIdsFlag()
                        ? buildExactUserIdsPredicate(root, criteriaBuilder)
                        : buildUserIdsPredicate(root, criteriaBuilder),
                predicates
        );
        // Add predicates for string fields using LIKE for partial matching
        addStringFieldPredicate(criteria.getFirstNames(), FIRST_NAME_FIELD, root, criteriaBuilder, predicates);
        addStringFieldPredicate(criteria.getLastNames(), LAST_NAME_FIELD, root, criteriaBuilder, predicates);
        addStringFieldPredicate(criteria.getEmails(), EMAIL_FIELD, root, criteriaBuilder, predicates);
        addStringFieldPredicate(criteria.getPhoneNumbers(), PHONE_NUMBER_FIELD, root, criteriaBuilder, predicates);

        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    /**
     * Adds a predicate for a string field using LIKE for partial matching.
     */
    private void addStringFieldPredicate(List<?> values,
                                        String field,
                                        Root<User> root,
                                        CriteriaBuilder criteriaBuilder,
                                        List<Predicate> predicates) {
        addPredicateIfValuesPresent(
                values,
                () -> {
                    Predicate[] fieldPredicates = values.stream()
                            .map(value -> like(criteriaBuilder, root.get(field).as(String.class), value.toString()))
                            .toArray(Predicate[]::new);
                    return criteriaBuilder.or(fieldPredicates);
                },
                predicates
        );
    }

    /**
     * Creates a predicate for exact ID matching.
     */
    private Predicate buildExactUserIdsPredicate(Root<User> root, CriteriaBuilder builder) {
        final Predicate[] ids = criteria.getUserIds().stream()
                .map(userId -> builder.equal(root.get("id"), userId))
                .toArray(Predicate[]::new);
        return builder.or(ids);
    }

    /**
     * Creates a predicate for partial ID matching (using like).
     */
    private Predicate buildUserIdsPredicate(Root<User> root, CriteriaBuilder builder) {
        var idsAsString = builder.function("str", String.class, root.get("id"));
        final Predicate[] ids = criteria.getUserIds().stream()
                .map(userId -> like(builder, idsAsString, String.valueOf(userId)))
                .toArray(Predicate[]::new);
        return builder.or(ids);
    }

    /**
     * Adds a predicate to the list if the criteria values are not null or empty.
     */
    private void addPredicateIfValuesPresent(@Nullable List<?> values,
                                             Supplier<Predicate> predicateSupplier,
                                             List<Predicate> predicates) {
        if (values != null && !values.isEmpty()) {
            predicates.add(predicateSupplier.get());
        }
    }
}
