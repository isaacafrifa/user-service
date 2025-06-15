package iam.userservice.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Utility interface for creating reusable SQL predicates using the Criteria API.
 * Provides methods to handle special character escaping and construct SQL LIKE clauses.
 */
public interface Predicates {
    String ESCAPE_CHAR_PERCENT = "%";
    String ESCAPE_CHAR_UNDERSCORE = "_";
    char DEFAULT_ESCAPE_CHAR = '\\';

    default String escapeSearchField(String searchField) {
        return searchField.replace(ESCAPE_CHAR_PERCENT, "\\%").replace(ESCAPE_CHAR_UNDERSCORE, "\\_");
    }

    default Predicate like(final CriteriaBuilder criteriaBuilder, final Expression<String> field, final String matchPattern) {
        return criteriaBuilder.like(criteriaBuilder.lower(field), constructLikePattern(matchPattern.toLowerCase()), DEFAULT_ESCAPE_CHAR);
    }

    private String constructLikePattern(String matchPattern) {
        return "%" + escapeSearchField(matchPattern) + "%";
    }
}
