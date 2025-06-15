package iam.userservice.cucumber.steps;

import iam.userservice.mapper.UserDto;
import iam.userservice.mapper.UsersDto;
import iam.userservice.util.UserFilterCriteria;
import iam.userservice.entity.User;
import iam.userservice.repository.UserRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
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

    public UserSearchSteps(WebTestClient webTestClient, UserRepository userRepository) {
        this.webTestClient = webTestClient;
        this.userRepository = userRepository;
    }

    private WebTestClient.ResponseSpec response;
    private UsersDto usersResponse;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public void theEndpointToGetUsersIsHitWithFilters(String endpoint, DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        UserFilterCriteria filterDto = new UserFilterCriteria();

        // Default pagination parameters
        int pageNo = 0;
        int pageSize = 10;
        String orderBy = "id";
        String direction = "asc";

        // Process each filter
        for (Map<String, String> row : rows) {
            String filterKey = row.get("filterKey");
            String filterValue = row.get("filterValue");

            switch (filterKey) {
                case "userIds":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<Long> values = parseJsonArrayToLong(filterValue);
                        if (values != null && !values.isEmpty()) {
                            // Use the parsed list directly
                            filterDto.setUserIds(values);
                            filterDto.setExactUserIdsFlag(true);
                        } else {
                            try {
                                // Try to parse a single value
                                Long userId = Long.parseLong(filterValue.replaceAll("[\\[\\]\"]", ""));
                                filterDto.setUserIds(Collections.singletonList(userId));
                                filterDto.setExactUserIdsFlag(true);
                            } catch (NumberFormatException e) {
                                System.err.println("Error parsing userId: " + e.getMessage());
                            }
                        }
                    } else {
                        try {
                            Long userId = Long.parseLong(filterValue);
                            filterDto.setUserIds(Collections.singletonList(userId));
                            filterDto.setExactUserIdsFlag(true);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing userId: " + e.getMessage());
                        }
                    }
                    break;
                case "firstNames":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            // Use the parsed list directly
                            filterDto.setFirstNames(values);
                        } else {
                            filterDto.setFirstNames(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setFirstNames(Collections.singletonList(filterValue));
                    }
                    break;
                case "lastNames":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            // Use the parsed list directly
                            filterDto.setLastNames(values);
                        } else {
                            filterDto.setLastNames(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setLastNames(Collections.singletonList(filterValue));
                    }
                    break;
                case "emails":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            // Use the parsed list directly
                            filterDto.setEmails(values);
                        } else {
                            filterDto.setEmails(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setEmails(Collections.singletonList(filterValue));
                    }
                    break;
                case "phoneNumbers":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            // Use the parsed list directly
                            filterDto.setPhoneNumbers(values);
                        } else {
                            filterDto.setPhoneNumbers(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setPhoneNumbers(Collections.singletonList(filterValue));
                    }
                    break;
                default:
                    // Ignore other keys for now
                    break;
            }
        }

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
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @When("the endpoint {string} to get users is hit with filters and pagination")
    public void theEndpointToGetUsersIsHitWithFiltersAndPagination(String endpoint, DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        UserFilterCriteria filterDto = new UserFilterCriteria();

        // Default pagination parameters
        int pageNo = 0;
        int pageSize = 10;
        String orderBy = "id";
        String direction = "asc";

        // Process each filter
        for (Map<String, String> row : rows) {
            String filterKey = row.get("filterKey");
            String filterValue = row.get("filterValue");

            switch (filterKey) {
                case "userIds":
                    try {
                        if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                            List<Long> values = parseJsonArrayToLong(filterValue);
                            if (values != null && !values.isEmpty()) {
                                filterDto.setUserIds(values);
                                filterDto.setExactUserIdsFlag(true);
                            } else {
                                Long userId = Long.parseLong(filterValue.replaceAll("[\\[\\]\"]", ""));
                                filterDto.setUserIds(Collections.singletonList(userId));
                                filterDto.setExactUserIdsFlag(true);
                            }
                        } else {
                            Long userId = Long.parseLong(filterValue);
                            filterDto.setUserIds(Collections.singletonList(userId));
                            filterDto.setExactUserIdsFlag(true);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing userId: " + e.getMessage());
                    }
                    break;
                case "firstNames":
                    filterDto.setFirstNames(Collections.singletonList(filterValue));
                    break;
                case "lastNames":
                    filterDto.setLastNames(Collections.singletonList(filterValue));
                    break;
                case "emails":
                    filterDto.setEmails(Collections.singletonList(filterValue));
                    break;
                case "phoneNumbers":
                    filterDto.setPhoneNumbers(Collections.singletonList(filterValue));
                    break;
                case "pageNo":
                    pageNo = Integer.parseInt(filterValue);
                    break;
                case "pageSize":
                    pageSize = Integer.parseInt(filterValue);
                    break;
                case "orderBy":
                    orderBy = filterValue;
                    break;
                case "direction":
                    direction = filterValue;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter key: " + filterKey);
            }
        }

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
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @When("the endpoint {string} with pagination params to get users is hit with filters")
    public void theEndpointWithPaginationToGetUsersIsHitWithFilters(String endpointWithParams, DataTable dataTable) {
        // Extract the base endpoint and query parameters
        String[] parts = endpointWithParams.split("\\?", 2);
        String endpoint = parts[0];
        String queryParams = parts.length > 1 ? parts[1] : "";

        // Parse query parameters
        int pageNo = 0;
        int pageSize = 10;
        String orderBy = "id";
        String direction = "asc";

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

        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        UserFilterCriteria filterDto = new UserFilterCriteria();

        // Process each filter
        for (Map<String, String> row : rows) {
            String filterKey = row.get("filterKey");
            String filterValue = row.get("filterValue");

            switch (filterKey) {
                case "userIds":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<Long> values = parseJsonArrayToLong(filterValue);
                        if (values != null && !values.isEmpty()) {
                            // Use the parsed list directly
                            filterDto.setUserIds(values);
                            filterDto.setExactUserIdsFlag(true);
                        } else {
                            try {
                                // Try to parse a single value
                                Long userId = Long.parseLong(filterValue.replaceAll("[\\[\\]\"]", ""));
                                filterDto.setUserIds(Collections.singletonList(userId));
                                filterDto.setExactUserIdsFlag(true);
                            } catch (NumberFormatException e) {
                                System.err.println("Error parsing userId: " + e.getMessage());
                            }
                        }
                    } else {
                        try {
                            Long userId = Long.parseLong(filterValue);
                            filterDto.setUserIds(Collections.singletonList(userId));
                            filterDto.setExactUserIdsFlag(true);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing userId: " + e.getMessage());
                        }
                    }
                    break;
                case "firstNames":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            filterDto.setFirstNames(values);
                        } else {
                            filterDto.setFirstNames(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setFirstNames(Collections.singletonList(filterValue));
                    }
                    break;
                case "lastNames":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            filterDto.setLastNames(values);
                        } else {
                            filterDto.setLastNames(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setLastNames(Collections.singletonList(filterValue));
                    }
                    break;
                case "emails":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            filterDto.setEmails(values);
                        } else {
                            filterDto.setEmails(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setEmails(Collections.singletonList(filterValue));
                    }
                    break;
                case "phoneNumbers":
                    if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                        List<String> values = parseJsonArray(filterValue);
                        if (values != null && !values.isEmpty()) {
                            filterDto.setPhoneNumbers(values);
                        } else {
                            filterDto.setPhoneNumbers(Collections.singletonList(filterValue));
                        }
                    } else {
                        filterDto.setPhoneNumbers(Collections.singletonList(filterValue));
                    }
                    break;
                default:
                    // Ignore other keys for now
                    break;
            }
        }

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
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }


    @When("the endpoint {string} to get users is hit with no filters")
    public void theEndpointToGetUsersIsHitWithNoFilters(String endpoint) {
        // Create empty filter criteria
        UserFilterCriteria filterDto = new UserFilterCriteria();

        // Default pagination parameters
        int pageNo = 0;
        int pageSize = 10;
        String orderBy = "id";
        String direction = "asc";

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
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @When("the endpoint {string} to get users is hit with filters and sorting")
    public void theEndpointToGetUsersIsHitWithFiltersAndSorting(String endpoint, DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        UserFilterCriteria filterDto = new UserFilterCriteria();

        // Default pagination parameters
        int pageNo = 0;
        int pageSize = 10;
        String orderBy = "id";
        String direction = "asc";

        // Process each filter
        for (Map<String, String> row : rows) {
            String filterKey = row.get("filterKey");
            String filterValue = row.get("filterValue");

            switch (filterKey) {
                case "userIds":
                    try {
                        if (filterValue.startsWith("[") && filterValue.endsWith("]")) {
                            List<Long> values = parseJsonArrayToLong(filterValue);
                            if (values != null && !values.isEmpty()) {
                                filterDto.setUserIds(values);
                                filterDto.setExactUserIdsFlag(true);
                            } else {
                                Long userId = Long.parseLong(filterValue.replaceAll("[\\[\\]\"]", ""));
                                filterDto.setUserIds(Collections.singletonList(userId));
                                filterDto.setExactUserIdsFlag(true);
                            }
                        } else {
                            Long userId = Long.parseLong(filterValue);
                            filterDto.setUserIds(Collections.singletonList(userId));
                            filterDto.setExactUserIdsFlag(true);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing userId: " + e.getMessage());
                    }
                    break;
                case "firstNames":
                    filterDto.setFirstNames(Collections.singletonList(filterValue));
                    break;
                case "lastNames":
                    filterDto.setLastNames(Collections.singletonList(filterValue));
                    break;
                case "emails":
                    filterDto.setEmails(Collections.singletonList(filterValue));
                    break;
                case "phoneNumbers":
                    filterDto.setPhoneNumbers(Collections.singletonList(filterValue));
                    break;
                case "orderBy":
                    orderBy = filterValue;
                    break;
                case "direction":
                    direction = filterValue;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported filter key: " + filterKey);
            }
        }

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
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @When("I search for users with first name {string} and last name {string}")
    public void iSearchForUsersWithFirstNameAndLastName(String firstName, String lastName) {
        UserFilterCriteria filterDto = new UserFilterCriteria();
        filterDto.setFirstNames(Collections.singletonList(firstName));
        filterDto.setLastNames(Collections.singletonList(lastName));

        response = webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/users/search")
                .queryParam("pageNo", 0)
                .queryParam("pageSize", 10)
                .queryParam("orderBy", "id")
                .queryParam("direction", "asc")
                .build())
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @When("I search for users with first name {string} and page size {int} and page number {int}")
    public void iSearchForUsersWithFirstNameAndPageSizeAndPageNumber(String firstName, int pageSize, int pageNo) {
        UserFilterCriteria filterDto = new UserFilterCriteria();
        filterDto.setFirstNames(Collections.singletonList(firstName));

        response = webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/users/search")
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize)
                .queryParam("orderBy", "id")
                .queryParam("direction", "asc")
                .build())
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @When("I search for users with sorting by {string} in {string} order")
    public void iSearchForUsersWithSortingByInOrder(String sortBy, String direction) {
        UserFilterCriteria filterDto = new UserFilterCriteria();

        response = webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/users/search")
                .queryParam("pageNo", 0)
                .queryParam("pageSize", 10)
                .queryParam("orderBy", sortBy)
                .queryParam("direction", direction)
                .build())
            .bodyValue(filterDto)
            .exchange();

        usersResponse = response.expectStatus().isOk()
            .expectBody(UsersDto.class)
            .returnResult()
            .getResponseBody();
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) {
        response.expectStatus().isEqualTo(statusCode);
    }

    @Then("the response should contain {int} user\\(s)")
    public void theResponseShouldContainUserS(int count) {
        assertEquals(count, usersResponse.getContent().size());
    }

    @Then("the response should contain {int} user")
    public void theResponseShouldContainUser(int count) {
        assertEquals(count, usersResponse.getContent().size());
    }

    @Then("the response should contain {int} users")
    public void theResponseShouldContainUsers(int count) {
        assertEquals(count, usersResponse.getContent().size());
    }

    @Then("the response should include users with emails {string} and {string}")
    public void theResponseShouldIncludeUsersWithEmailsAnd(String email1, String email2) {
        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        assertTrue(userEmails.contains(email1), "Response should contain user with email " + email1);
        assertTrue(userEmails.contains(email2), "Response should contain user with email " + email2);
    }

    @Then("the response should include users with emails {string}")
    public void theResponseShouldIncludeUsersWithEmails(String email) {
        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        assertTrue(userEmails.contains(email), "Response should contain user with email " + email);
    }

    @Then("the response should include users with ids {int} and {int}")
    public void theResponseShouldIncludeUsersWithIdsAnd(int id1, int id2) {
        // Map IDs to emails based on the test data
        String email1 = getEmailForId(id1);
        String email2 = getEmailForId(id2);

        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        assertTrue(userEmails.contains(email1), "Response should contain user with email " + email1);
        assertTrue(userEmails.contains(email2), "Response should contain user with email " + email2);
    }

    @Then("the response should include users with ids {int}")
    public void theResponseShouldIncludeUsersWithIds(int id) {
        // Map ID to email based on the test data
        String email = getEmailForId(id);

        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        assertTrue(userEmails.contains(email), "Response should contain user with email " + email);
    }

    @Then("the response should include users with ids {int}, {int} and {int}")
    public void theResponseShouldIncludeUsersWithIdsAndAnd(int id1, int id2, int id3) {
        // Map IDs to emails based on the test data
        String email1 = getEmailForId(id1);
        String email2 = getEmailForId(id2);
        String email3 = getEmailForId(id3);

        List<String> userEmails = usersResponse.getContent().stream()
            .map(UserDto::getEmail)
            .toList();

        assertTrue(userEmails.contains(email1), "Response should contain user with email " + email1);
        assertTrue(userEmails.contains(email2), "Response should contain user with email " + email2);
        assertTrue(userEmails.contains(email3), "Response should contain user with email " + email3);
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
     * Parse a JSON array string into a List of strings.
     * 
     * @param jsonArrayString the JSON array string to parse
     * @return a List of strings, or null if parsing fails
     */
    private List<String> parseJsonArray(String jsonArrayString) {
        try {
            if (jsonArrayString.startsWith("[") && jsonArrayString.endsWith("]")) {
                return Arrays.asList(objectMapper.readValue(jsonArrayString, String[].class));
            }
        } catch (JsonProcessingException e) {
            // Log the error or handle it as appropriate
            System.err.println("Error parsing JSON array: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse a JSON array string into a List of Long values.
     * 
     * @param jsonArrayString the JSON array string to parse
     * @return a List of Long values, or null if parsing fails
     */
    private List<Long> parseJsonArrayToLong(String jsonArrayString) {
        try {
            if (jsonArrayString.startsWith("[") && jsonArrayString.endsWith("]")) {
                return Arrays.asList(objectMapper.readValue(jsonArrayString, Long[].class));
            }
        } catch (JsonProcessingException e) {
            // Log the error or handle it as appropriate
            System.err.println("Error parsing JSON array to Long: " + e.getMessage());
        }
        return null;
    }
}
