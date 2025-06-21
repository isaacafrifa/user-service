Feature: User Search
  As an API client
  I want to search for users based on various criteria
  So that I can find specific users in the system

  Background:
    Given the following users exist in the system:
      | id | firstName | lastName | email                  | phoneNumber |
      | 1  | John      | Doe      | john.doe@example.com   | 1234567890  |
      | 2  | Jane      | Smith    | jane.smith@example.com | 0987654321  |
      | 3  | John      | Smith    | john.smith@example.com | 1122334455  |
      | 4  | Alice     | Johnson  | alice.j@example.com    | 5566778899  |

  Scenario Outline: Search users by single criterion
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey   | filterValue   |
      | <filterKey> | <filterValue> |
    Then the response status code should be 200
    And the response should contain <count> user(s)
    And the response should include users with ids <userIds>
    Examples:
      | filterKey    | filterValue | count | userIds |
      | userIds      | [3]         | 1     | 3       |
      | firstNames   | ["John"]    | 2     | 1 and 3 |
      | firstNames   | ["A"]       | 2     | 2 and 4 |
      | lastNames    | ["Smith"]   | 2     | 2 and 3 |
      | emails       | ["john"]    | 2     | 1 and 3 |
      | phoneNumbers | ["5566"]    | 1     | 4       |

  Scenario: Search users with multiple criteria
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey  | filterValue |
      | firstNames | ["John"]    |
      | lastNames  | ["Smith"]   |
    Then the response status code should be 200
    And the response should contain 1 user
    And the response should include users with ids 3

  Scenario: Search users with pagination
    When the endpoint "/users/search?pageSize=1&pageNo=0" with pagination params to get users is hit with filters
      | filterKey  | filterValue |
      | firstNames | ["John"]    |
    Then the response status code should be 200
    And the response should contain 1 user
    And the total elements should be 2
    And the total pages should be 2

  Scenario: Search users with sorting
    When the endpoint "/users/search" to get users is hit with filters and sorting
      | filterKey | filterValue |
      | orderBy   | firstName   |
      | direction | asc         |
    Then the response status code should be 200
    And the response should contain 4 users
    And the users should be sorted by "firstNames" in "asc" order

  Scenario: Search with no matching criteria
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey  | filterValue     |
      | firstNames | ["NonExistent"] |
    Then the response status code should be 200
    And the response should contain 0 users

  Scenario Outline: Search users with multiple field-based filtering
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey   | filterValue   |
      | <filterKey> | <filterValue> |
    Then the response status code should be 200
    And the response should contain <count> user(s)
    And the response should include users with ids <userIds>
    Examples:
      | filterKey    | filterValue          | count | userIds    |
      | userIds      | [3, 4]               | 2     | 3 and 4    |
      | firstNames   | ["John", "Jane"]     | 3     | 1, 2 and 3 |
      | lastNames    | ["Smith", "Johnson"] | 3     | 2, 3 and 4 |
      | emails       | ["john", "alice"]    | 3     | 1, 3 and 4 |
      | phoneNumbers | ["1234", "5566"]     | 2     | 1 and 4    |

  Scenario: Search users without filters
    When the endpoint "/users/search" to get users is hit with no filters
    Then the response status code should be 200
    And the response should contain 4 users
    And the response should include all users

  # This scenario tests filter values that should return a 400 Bad Request status
  Scenario Outline: Search users with invalid filter values
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey   | filterValue   |
      | <filterKey> | <filterValue> |
    Then the response status code should be 400
    And the response should contain 0 users
    Examples:
      | filterKey    | filterValue |
      | userIds      | [-1]        |
      | userIds      | ["xyz"]     |
      | userIds      | []          |
      | firstNames   | []          |
      | phoneNumbers | ["abc"]     |
      | phoneNumbers | [""]        |

  # This scenario tests filter values that are accepted but return no results
  Scenario Outline: Search users with filter values that return no results
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey   | filterValue   |
      | <filterKey> | <filterValue> |
    Then the response status code should be 200
    And the response should contain 0 users
    Examples:
      | filterKey    | filterValue              |
      | firstNames   | ["123"]                  |
      | lastNames    | ["123"]                  |
      | userIds      | [""]                     |
      | emails       | ["invalid-email"]        |
      | phoneNumbers | ["12345678901234567890"] |

  # This scenario tests empty string filters that match all users
  Scenario Outline: Search users with empty string filters that match all users
    When the endpoint "/users/search" to get users is hit with filters
      | filterKey   | filterValue   |
      | <filterKey> | <filterValue> |
    Then the response status code should be 200
    And the response should contain 4 users
    Examples:
      | filterKey  | filterValue |
      | firstNames | [""]        |
      | lastNames  | [""]        |
      | emails     | [""]        |