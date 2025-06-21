package iam.userservice.cucumber.steps;

import iam.userservice.mapper.UserDto;
import iam.userservice.mapper.UsersDto;
import iam.userservice.entity.User;
import iam.userservice.repository.UserRepository;
import iam.userservice.cucumber.util.JsonFieldParser;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for the user search feature.
 */
@ScenarioScope
public class UserSearchSteps {

    private final WebTestClient webTestClient;
    private final UserRepository userRepository;
    private final JsonFieldParser jsonFieldParser;

    public UserSearchSteps(WebTestClient webTestClient, UserRepository userRepository) {
        this.webTestClient = webTestClient;
        this.userRepository = userRepository;
        this.jsonFieldParser = new JsonFieldParser();
    }

    private WebTestClient.ResponseSpec response;
    private UsersDto usersResponse;

    // Default pagination parameters
    private static final int DEFAULT_PAGE_NO = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_ORDER_BY = "id";
    private static final String DEFAULT_DIRECTION = "asc";

    // List of known header rows
    private static final List<List<String>> HEADERS = List.of(
            List.of("filterKey", "filterValue"), // Header 1
            List.of("field", "value")             // Alternate Header
    );

    @Before
    public void setup() {
        // Clear the database before each scenario
        userRepository.deleteAll();
    }

    @After
    public void cleanup() {
        // Clean up after each scenario
        userRepository.deleteAll();
    }


    @Given("the following users exist in the system:")
    public void theFollowingUsersExistInTheSystem(DataTable dataTable) {
        // Clear the database before creating new users
        userRepository.deleteAll();

        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            User user = new User();
            // Don't set ID, let the database generate it
            user.setFirstName(row.get("firstName"));
            user.setLastName(row.get("lastName"));
            user.setEmail(row.get("email"));
            user.setPhoneNumber(row.get("phoneNumber"));

            // Set creation and update timestamps
            OffsetDateTime now = OffsetDateTime.now();
            user.setCreatedOn(now);
            user.setUpdatedOn(now);

            // Set version for optimistic locking
            user.setVersion(0L);

            userRepository.save(user);
        }
    }

    @When("the endpoint {string} to get users is hit with filters")
    public void theEndpointToGetUsersIsHitWithFilters(String endpoint, List<List<String>> rows) {
        // Process rows to field-value pairs
        List<List<String>> fieldValuePairs = processRowsToFieldValuePairs(rows);

        // Convert to a map structure
        Map<String, Object> filterMap = jsonFieldParser.convertToMap(fieldValuePairs);

        // Make API request with default pagination parameters
        makeApiRequest(endpoint, filterMap, DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE, DEFAULT_ORDER_BY, DEFAULT_DIRECTION);
    }

    @When("the endpoint {string} to get users is hit with filters and pagination")
    public void theEndpointToGetUsersIsHitWithFiltersAndPagination(String endpoint, List<List<String>> rows) {
        // Process rows to field-value pairs
        List<List<String>> fieldValuePairs = processRowsToFieldValuePairs(rows);

        // Extract pagination parameters
        Object[] params = extractPaginationParams(fieldValuePairs);
        List<List<String>> filteredPairs = (List<List<String>>) params[0];
        int pageNo = (int) params[1];
        int pageSize = (int) params[2];
        String orderBy = (String) params[3];
        String direction = (String) params[4];

        // Convert to a map structure
        Map<String, Object> filterMap = jsonFieldParser.convertToMap(filteredPairs);

        // Make API request with extracted pagination parameters
        makeApiRequest(endpoint, filterMap, pageNo, pageSize, orderBy, direction);
    }

    @When("the endpoint {string} with pagination params to get users is hit with filters")
    public void theEndpointWithPaginationToGetUsersIsHitWithFilters(String endpointWithParams, List<List<String>> rows) {
        // Extract the base endpoint and query parameters
        String[] parts = endpointWithParams.split("\\?", 2);
        String endpoint = parts[0];
        String queryParams = parts.length > 1 ? parts[1] : "";

        // Parse query parameters
        Object[] params = parseQueryParams(queryParams);
        int pageNo = (int) params[0];
        int pageSize = (int) params[1];
        String orderBy = (String) params[2];
        String direction = (String) params[3];

        // Process rows to field-value pairs
        List<List<String>> fieldValuePairs = processRowsToFieldValuePairs(rows);

        // Convert to a map structure
        Map<String, Object> filterMap = jsonFieldParser.convertToMap(fieldValuePairs);

        // Make API request with extracted pagination parameters
        makeApiRequest(endpoint, filterMap, pageNo, pageSize, orderBy, direction);
    }


    @When("the endpoint {string} to get users is hit with no filters")
    public void theEndpointToGetUsersIsHitWithNoFilters(String endpoint) {
        // Create empty filter map
        Map<String, Object> filterMap = new HashMap<>();

        // Make API request with default pagination parameters
        makeApiRequest(endpoint, filterMap, DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE, DEFAULT_ORDER_BY, DEFAULT_DIRECTION);
    }

    @When("the endpoint {string} to get users is hit with filters and sorting")
    public void theEndpointToGetUsersIsHitWithFiltersAndSorting(String endpoint, List<List<String>> rows) {
        // Process rows to field-value pairs
        List<List<String>> fieldValuePairs = processRowsToFieldValuePairs(rows);

        // Extract pagination parameters (in this case, just sorting parameters)
        Object[] params = extractPaginationParams(fieldValuePairs);
        List<List<String>> filteredPairs = (List<List<String>>) params[0];
        int pageNo = (int) params[1];
        int pageSize = (int) params[2];
        String orderBy = (String) params[3];
        String direction = (String) params[4];

        // Convert to a map structure
        Map<String, Object> filterMap = jsonFieldParser.convertToMap(filteredPairs);

        // Make API request with extracted pagination parameters
        makeApiRequest(endpoint, filterMap, pageNo, pageSize, orderBy, direction);
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) {
        response.expectStatus().isEqualTo(statusCode);
    }

    /**
     * Verifies that the response contains the expected number of users.
     * This method handles all variations of the step definition.
     *
     * @param count Expected number of users
     */
    @Then("the response should contain {int} user\\(s)")
    @Then("the response should contain {int} user")
    @Then("the response should contain {int} users")
    public void theResponseShouldContainUsers(int count) {
        // Handle case where usersResponse or its content might be null
        if (usersResponse == null) {
            assertEquals(0, count, "Expected 0 users for null response");
            return;
        }

        List<UserDto> content = usersResponse.getContent();
        if (content == null) {
            assertEquals(0, count, "Expected 0 users for null content");
            return;
        }

        assertEquals(count, content.size());
    }

    /**
     * Helper method to verify that the response includes users with the specified emails.
     *
     * @param emails List of emails to check
     */
    private void verifyResponseContainsEmails(String... emails) {
        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        for (String email : emails) {
            assertTrue(userEmails.contains(email), "Response should contain user with email " + email);
        }
    }

    @Then("the response should include users with emails {string} and {string}")
    public void theResponseShouldIncludeUsersWithEmailsAnd(String email1, String email2) {
        verifyResponseContainsEmails(email1, email2);
    }

    @Then("the response should include users with emails {string}")
    public void theResponseShouldIncludeUsersWithEmails(String email) {
        verifyResponseContainsEmails(email);
    }

    /**
     * Helper method to verify that the response includes users with the specified IDs.
     * Maps IDs to emails based on the test data and checks if the emails are in the response.
     *
     * @param ids Array of user IDs to check
     */
    private void verifyResponseContainsIds(int... ids) {
        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        for (int id : ids) {
            String email = getEmailForId(id);
            assertTrue(userEmails.contains(email), "Response should contain user with email " + email);
        }
    }

    @Then("the response should include users with ids {int} and {int}")
    public void theResponseShouldIncludeUsersWithIdsAnd(int id1, int id2) {
        verifyResponseContainsIds(id1, id2);
    }

    @Then("the response should include users with ids {int}")
    public void theResponseShouldIncludeUsersWithIds(int id) {
        verifyResponseContainsIds(id);
    }

    @Then("the response should include users with ids {int}, {int} and {int}")
    public void theResponseShouldIncludeUsersWithIdsAndAnd(int id1, int id2, int id3) {
        verifyResponseContainsIds(id1, id2, id3);
    }

    /**
     * Maps an ID to the corresponding email based on the test data.
     * 
     * @param id the ID to map
     * @return the corresponding email
     */
    private String getEmailForId(int id) {
        return switch (id) {
            case 1 -> "john.doe@example.com";
            case 2 -> "jane.smith@example.com";
            case 3 -> "john.smith@example.com";
            case 4 -> "alice.j@example.com";
            default -> throw new IllegalArgumentException("Unknown ID: " + id);
        };
    }

    @Then("the total elements should be {int}")
    public void theTotalElementsShouldBe(int totalElements) {
        assertEquals(totalElements, usersResponse.getTotalElements());
    }

    @Then("the total pages should be {int}")
    public void theTotalPagesShouldBe(int totalPages) {
        assertEquals(totalPages, usersResponse.getTotalPages());
    }

    @Then("the response should include all users")
    public void theResponseShouldIncludeAllUsers() {
        // Check that all users are included in the response
        // We don't check specific IDs because they might be different in each test run
        // Instead, we check that the response contains 4 users, which is already done by the previous step
        // We also check that each user has the expected properties
        List<UserDto> users = usersResponse.getContent();

        assertEquals(4, users.size(), "Response should contain all users");

        // Check that the response includes users with the expected properties
        assertTrue(users.stream().anyMatch(u -> "John".equals(u.getFirstName()) && "Doe".equals(u.getLastName())), 
                "Response should include John Doe");
        assertTrue(users.stream().anyMatch(u -> "Jane".equals(u.getFirstName()) && "Smith".equals(u.getLastName())), 
                "Response should include Jane Smith");
        assertTrue(users.stream().anyMatch(u -> "John".equals(u.getFirstName()) && "Smith".equals(u.getLastName())), 
                "Response should include John Smith");
        assertTrue(users.stream().anyMatch(u -> "Alice".equals(u.getFirstName()) && "Johnson".equals(u.getLastName())), 
                "Response should include Alice Johnson");
    }

    @Then("the users should be sorted by {string} in {string} order")
    public void theUsersShouldBeSortedByInOrder(String field, String direction) {
        List<UserDto> users = usersResponse.getContent();

        // Check if the users are sorted by the specified field in the specified order
        if ("firstNames".equals(field)) {
            if ("asc".equals(direction)) {
                for (int i = 0; i < users.size() - 1; i++) {
                    assertTrue(users.get(i).getFirstName().compareTo(users.get(i + 1).getFirstName()) <= 0);
                }
            } else {
                for (int i = 0; i < users.size() - 1; i++) {
                    assertTrue(users.get(i).getFirstName().compareTo(users.get(i + 1).getFirstName()) >= 0);
                }
            }
        } else if ("lastNames".equals(field)) {
            if ("asc".equals(direction)) {
                for (int i = 0; i < users.size() - 1; i++) {
                    assertTrue(users.get(i).getLastName().compareTo(users.get(i + 1).getLastName()) <= 0);
                }
            } else {
                for (int i = 0; i < users.size() - 1; i++) {
                    assertTrue(users.get(i).getLastName().compareTo(users.get(i + 1).getLastName()) >= 0);
                }
            }
        }
    }


    /**
     * Helper method to process rows from a DataTable into field-value pairs.
     * Skips the header row if it matches the expected header content.
     *
     * @param rows List of rows from the DataTable
     * @return List of field-value pairs
     */
    private List<List<String>> processRowsToFieldValuePairs(List<List<String>> rows) {
        // Process rows excluding any known header rows
        List<List<String>> fieldValuePairs = new ArrayList<>();
        for (List<String> row : rows) {
            // Skip the row if it matches any known header
            if (HEADERS.contains(row)) {
                continue;
            }
            if (row.size() >= 2) {
                String filterKey = row.get(0);
                String filterValue = row.get(1);
                fieldValuePairs.add(Arrays.asList(filterKey, filterValue));
            }
        }

        return fieldValuePairs;
    }

    /**
     * Helper method to extract pagination parameters from field-value pairs.
     * Removes pagination parameters from the list and returns them separately.
     *
     * @param fieldValuePairs List of field-value pairs
     * @return Object array containing [updatedFieldValuePairs, pageNo, pageSize, orderBy, direction]
     */
    private Object[] extractPaginationParams(List<List<String>> fieldValuePairs) {
        List<List<String>> updatedPairs = new ArrayList<>();
        int pageNo = DEFAULT_PAGE_NO;
        int pageSize = DEFAULT_PAGE_SIZE;
        String orderBy = DEFAULT_ORDER_BY;
        String direction = DEFAULT_DIRECTION;

        for (List<String> pair : fieldValuePairs) {
            String key = pair.get(0);
            String value = pair.get(1);

            switch (key) {
                case "pageNo":
                    try {
                        pageNo = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing pageNo: " + value);
                    }
                    break;
                case "pageSize":
                    try {
                        pageSize = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing pageSize: " + value);
                    }
                    break;
                case "orderBy":
                    orderBy = value;
                    break;
                case "direction":
                    direction = value;
                    break;
                default:
                    updatedPairs.add(pair);
                    break;
            }
        }

        return new Object[] { updatedPairs, pageNo, pageSize, orderBy, direction };
    }

    /**
     * Helper method to parse query parameters from a URL string.
     *
     * @param queryParams Query parameters string
     * @return Object array containing [pageNo, pageSize, orderBy, direction]
     */
    private Object[] parseQueryParams(String queryParams) {
        int pageNo = DEFAULT_PAGE_NO;
        int pageSize = DEFAULT_PAGE_SIZE;
        String orderBy = DEFAULT_ORDER_BY;
        String direction = DEFAULT_DIRECTION;

        if (!queryParams.isEmpty()) {
            String[] params = queryParams.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];

                    switch (key) {
                        case "pageNo":
                            pageNo = Integer.parseInt(value);
                            break;
                        case "pageSize":
                            pageSize = Integer.parseInt(value);
                            break;
                        case "orderBy":
                            orderBy = value;
                            break;
                        case "direction":
                            direction = value;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + key);
                    }
                }
            }
        }

        return new Object[] { pageNo, pageSize, orderBy, direction };
    }

    /**
     * Helper method to make an API request with the given endpoint, filter map, and pagination parameters.
     *
     * @param endpoint API endpoint
     * @param filterMap Map of filter criteria
     * @param pageNo Page number
     * @param pageSize Page size
     * @param orderBy Order by field
     * @param direction Sort direction
     */
    private void makeApiRequest(String endpoint, Map<String, Object> filterMap, int pageNo, int pageSize, String orderBy, String direction) {
        // Create final copies of the variables for use in the lambda
        final int finalPageNo = pageNo;
        final int finalPageSize = pageSize;
        final String finalOrderBy = orderBy;
        final String finalDirection = direction;

        response = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1" + endpoint)
                        .queryParam("pageNo", finalPageNo)
                        .queryParam("pageSize", finalPageSize)
                        .queryParam("orderBy", finalOrderBy)
                        .queryParam("direction", finalDirection)
                        .build())
                .bodyValue(filterMap)
                .exchange();

        // Don't assert status code here, it will be checked in the specific step
        try {
            usersResponse = response.expectBody(UsersDto.class)
                    .returnResult()
                    .getResponseBody();
        } catch (Exception e) {
            // For error responses, usersResponse might be null
            // This is expected for 4xx/5xx responses
            usersResponse = new UsersDto();
            usersResponse.setContent(Collections.emptyList());
        }
    }

}
