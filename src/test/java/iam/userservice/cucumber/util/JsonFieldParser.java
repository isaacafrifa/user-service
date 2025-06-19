package iam.userservice.cucumber.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for parsing JSON-like data fields into a nested map structure.
 * 
 * <p>This class provides functionality to convert a list of field-value pairs into a
 * hierarchical map structure, supporting:</p>
 * <ul>
 *   <li>Dot notation for nested fields (e.g., "user.address.city")</li>
 *   <li>Array notation for indexed elements (e.g., "users[0].name")</li>
 *   <li>Special value placeholders like "null" and "empty"</li>
 *   <li>Automatic parsing of JSON array strings</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * List<List<String>> fieldValuePairs = new ArrayList<>();
 * fieldValuePairs.add(Arrays.asList("user.firstName", "John"));
 * fieldValuePairs.add(Arrays.asList("user.lastName", "Doe"));
 * fieldValuePairs.add(Arrays.asList("user.addresses[0].city", "New York"));
 * 
 * JsonFieldParser parser = new JsonFieldParser();
 * Map<String, Object> result = parser.convertToMap(fieldValuePairs);
 * </pre>
 */
public class JsonFieldParser {
    private static final Logger LOGGER = Logger.getLogger(JsonFieldParser.class.getName());
    private static final String ARRAY_START_DELIMITER = "[";
    private static final String ARRAY_END_DELIMITER = "]";
    private static final String PATH_DELIMITER = "\\.";
    private static final String NULL_VALUE = "null";
    private static final String EMPTY_VALUE = "empty";

    private final ObjectMapper objectMapper;

    /**
     * Creates a new JsonFieldParser with default configuration.
     */
    public JsonFieldParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new JsonFieldParser with a custom ObjectMapper.
     * 
     * @param objectMapper The custom ObjectMapper to use for JSON parsing
     */
    public JsonFieldParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    }

    /**
     * Converts a list of field-value pairs to a nested map structure.
     * 
     * <p>Each field-value pair should be a List containing exactly two elements:
     * the field path (which may use dot notation and array notation) and the value.</p>
     * 
     * @param input A list of field-value pairs, where each pair is a List with two elements
     * @return A map representing the nested structure defined by the input pairs
     * @throws IllegalArgumentException If any input pair is invalid or cannot be processed
     */
    public Map<String, Object> convertToMap(List<List<String>> input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < input.size(); i++) {
            List<String> pair = input.get(i);
            if (pair == null || pair.size() < 2) {
                LOGGER.warning("Skipping invalid field-value pair at index " + i + ": " + pair);
                continue;
            }

            String fieldPath = pair.get(0).trim();
            String rawValue = pair.get(1);

            if (fieldPath.isEmpty()) {
                LOGGER.warning("Skipping empty field path at index " + i);
                continue;
            }

            try {
                Object processedValue = processValue(rawValue);
                String[] pathSegments = fieldPath.split(PATH_DELIMITER);
                setValue(result, pathSegments, processedValue);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Failed to process field-value pair at index %d: %s", i, pair), e);
                // Continue processing other pairs instead of failing completely
            }
        }

        return result;
    }

    /**
     * Sets a value into the map at the given path.
     * 
     * @param map The main map structure to populate
     * @param pathSegments The split path representing the field hierarchy
     * @param value The value to set at the specified path
     */
    private void setValue(Map<String, Object> map, String[] pathSegments, Object value) {
        Map<String, Object> current = map;

        for (int i = 0; i < pathSegments.length; i++) {
            String segment = pathSegments[i];
            boolean isLastSegment = (i == pathSegments.length - 1);

            if (isArrayNotation(segment)) {
                handleArraySegment(current, segment, value, isLastSegment);
            } else {
                handleSimpleSegment(current, segment, value, isLastSegment);
            }

            // Traverse down into the structure, but only if it's not the last segment
            if (!isLastSegment) {
                String fieldName = extractFieldName(segment);
                Object nextLevel = current.get(fieldName);

                if (!(nextLevel instanceof Map)) {
                    // This should not happen with proper handling in handleArraySegment and handleSimpleSegment
                    throw new IllegalStateException("Expected a Map at path segment: " + segment);
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> nextMap = (Map<String, Object>) nextLevel;
                current = nextMap;
            }
        }
    }

    /**
     * Handles a path segment with array notation (e.g., "fieldName[0]").
     * 
     * @param current The current map being populated
     * @param segment The path segment with array notation
     * @param value The value to set if this is the last segment
     * @param isLastSegment Whether this is the last segment in the path
     */
    private void handleArraySegment(Map<String, Object> current, String segment, Object value, boolean isLastSegment) {
        String fieldName = extractFieldName(segment);
        int index = extractArrayIndex(segment);

        // Initialize the array if it does not exist
        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>) current.computeIfAbsent(fieldName, k -> new ArrayList<>());

        // Ensure the array is large enough to accommodate the index
        while (array.size() <= index) {
            // For non-terminal segments, we need maps for further traversal
            // For terminal segments, we'll replace with the actual value later
            array.add(isLastSegment ? null : new HashMap<String, Object>());
        }

        // Set the value if this is the last segment
        if (isLastSegment) {
            array.set(index, value);
        }
    }

    /**
     * Handles a simple (non-array) path segment.
     * 
     * @param current The current map being populated
     * @param segment The simple path segment
     * @param value The value to set if this is the last segment
     * @param isLastSegment Whether this is the last segment in the path
     */
    private void handleSimpleSegment(Map<String, Object> current, String segment, Object value, boolean isLastSegment) {
        if (isLastSegment) {
            current.put(segment, value);
        } else {
            current.computeIfAbsent(segment, k -> new HashMap<String, Object>());
        }
    }

    /**
     * Processes the raw value string to handle special placeholders and JSON arrays.
     * 
     * @param value The raw value string to process
     * @return The processed value object
     */
    private Object processValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        // Handle special values
        if (NULL_VALUE.equals(trimmedValue)) {
            return null;
        } else if (EMPTY_VALUE.equals(trimmedValue)) {
            return "";
        }

        // Check if it's a JSON array string
        if (isJsonArrayString(trimmedValue)) {
            try {
                return parseJsonArray(trimmedValue);
            } catch (JsonProcessingException e) {
                LOGGER.log(Level.WARNING, e, () -> "Failed to parse JSON array for value: " + trimmedValue + " due to: " + e.getMessage());
                // Fall through to return the original string
            }
        }

        // Return as a regular string
        return trimmedValue;
    }

    /**
     * Parses a JSON array string into a List.
     * 
     * @param jsonString The JSON array string to parse
     * @return A List containing the parsed array elements
     * @throws JsonProcessingException If the string cannot be parsed as a JSON array
     */
    private List<?> parseJsonArray(String jsonString) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, List.class);
    }

    /**
     * Checks if a string appears to be a JSON array.
     * 
     * @param value The string to check
     * @return true if the string looks like a JSON array, false otherwise
     */
    private boolean isJsonArrayString(String value) {
        return value.startsWith("[") && value.endsWith("]");
    }

    /**
     * Checks if a path segment uses array notation.
     * 
     * @param segment The path segment to check
     * @return true if the segment uses array notation, false otherwise
     */
    private boolean isArrayNotation(String segment) {
        return segment.contains(ARRAY_START_DELIMITER) && segment.contains(ARRAY_END_DELIMITER);
    }

    /**
     * Extracts the field name from a path segment with array notation.
     * 
     * @param segment The path segment, which may use array notation
     * @return The field name without array notation
     */
    private String extractFieldName(String segment) {
        if (!isArrayNotation(segment)) {
            return segment;
        }

        int bracketIndex = segment.indexOf(ARRAY_START_DELIMITER);
        return segment.substring(0, bracketIndex);
    }

    /**
     * Extracts the array index from a path segment with array notation.
     * 
     * @param segment The path segment with array notation
     * @return The extracted array index
     * @throws NumberFormatException If the index is not a valid integer
     * @throws IllegalArgumentException If the segment does not contain a valid array index
     */
    private int extractArrayIndex(String segment) {
        if (!isArrayNotation(segment)) {
            throw new IllegalArgumentException("Segment does not contain array notation: " + segment);
        }

        int startIndex = segment.indexOf(ARRAY_START_DELIMITER) + 1;
        int endIndex = segment.indexOf(ARRAY_END_DELIMITER);

        if (startIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid array notation in segment: " + segment);
        }

        String indexStr = segment.substring(startIndex, endIndex);
        try {
            return Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid array index in segment: " + segment, e);
        }
    }
}
