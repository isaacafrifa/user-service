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

    /**
     * Creates a `LIKE` predicate for a field, ensuring case insensitivity
     * and escaping special SQL wildcard and special characters appropriately.
     *
     * @param criteriaBuilder The CriteriaBuilder instance for building predicates.
     * @param field           The database field to apply the `LIKE` operation on.
     * @param matchPattern    The pattern to search for (before escaping).
     * @return A `Predicate` for the `LIKE` clause.
     */
    default Predicate like(final CriteriaBuilder criteriaBuilder, final Expression<String> field, final String matchPattern) {
        return criteriaBuilder.like(criteriaBuilder.lower(field), constructLikePattern(matchPattern.toLowerCase()), DEFAULT_ESCAPE_CHAR);
    }

    /**
     * Constructs the final `LIKE` pattern by wrapping the escaped search input
     * in wildcard characters (e.g., `%search%`).
     *
     * @param matchPattern The raw search input.
     * @return The escaped and formatted `LIKE` pattern.
     */
    private String constructLikePattern(String matchPattern) {
        return "%" + escapeSearchField(matchPattern) + "%";
    }

    /**
     * Escapes special SQL wildcard and other reserved characters in the given search field.
     *
     * @param searchField The input search string.
     * @return The escaped search field, safe for use in SQL `LIKE` operations.
     */
    default String escapeSearchField(String searchField) {
        if (searchField == null) {
            return "";
        }

        // Escape backslash first to avoid double escaping
        String escaped = searchField.replace("\\", "\\\\");

        // Escape SQL LIKE wildcards
        escaped = escaped.replace(ESCAPE_CHAR_PERCENT, "\\%");
        escaped = escaped.replace(ESCAPE_CHAR_UNDERSCORE, "\\_");

        // Escape additional special characters
        escaped = escaped.replace("'", "\\'")   // Single quote
                .replace("\"", "\\\"") // Double quote
                .replace(";", "\\;")   // Semicolon
                .replace("--", "\\--") // SQL comment
                .replace("/*", "\\/*") // SQL block comment start
                .replace("*/", "\\*/") // SQL block comment end
                .replace("[", "\\[")   // Left square bracket
                .replace("]", "\\]")   // Right square bracket
                .replace("^", "\\^");  // Caret symbol

        return escaped;
    }
}
