Feature: User Search with Free Text
  As an API client
  I want to search for users using free form text
  So that I can find specific users across multiple fields

  Background:
    Given the following users exist in the system:
      | id | firstName | lastName        | email                  | phoneNumber |
      | 1  | John      | Doe             | john.doe@example.com   | 1234567890  |
      | 2  | Jane      | Smith           | jane.smith@example.com | 0987654321  |
      | 3  | John      | Smith           | john.smith@example.com | 1122334455  |
      | 4  | Alice     | Johnson         | alice.j@example.com    | 5566778899  |
      | 5  | O'Hara    | Jean-Luc Picard | m.jack@example.com     | 2206878643  |

  Scenario Outline: Search users with searchText matching names
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value   |
      | searchText | <value> |
    Then the response status code should be 200
    And the response should contain <count> user(s)
    And the response should include users with ids <userIds>
    Examples:
      | value           | count | userIds    |
      | john            | 3     | 1, 3 and 4 |
      | Smith           | 2     | 2 and 3    |
      | Jean-Luc Picard | 1     | 5          |
      | O'Hara          | 1     | 5          |


  Scenario Outline: Search users with searchText matching partial text
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value   |
      | searchText | <value> |
    Then the response status code should be 200
    And the response should contain <count> user(s)
    And the response should include users with ids <userIds>
    Examples:
      | value     | count | userIds    |
      | alice.j   | 1     | 4          |
      | h@example | 2     | 2 and 3    |
      | jo        | 3     | 1, 3 and 4 |

  Scenario: Search users with searchText matching phone number
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value |
      | searchText | 5566  |
    Then the response status code should be 200
    And the response should contain 1 user
    And the response should include users with ids 4


  Scenario: Search users with searchText and additional filter
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value     |
      | searchText | jo        |
      | lastNames  | ["Smith"] |
    Then the response status code should be 200
    And the response should contain 1 user
    And the response should include users with ids 3

  Scenario: Search users with searchText that matches no users
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value       |
      | searchText | NonExistent |
    Then the response status code should be 200
    And the response should contain 0 users

  Scenario: Search users with searchText and pagination
    When the endpoint "/users/search?pageSize=1&pageNo=0" with pagination params to get users is hit with filters
      | field      | value |
      | searchText | john  |
    Then the response status code should be 200
    And the response should contain 1 user
    And the total elements should be 3
    And the total pages should be 3

  Scenario: Search users with searchText and sorting
    When the endpoint "/users/search" to get users is hit with filters and sorting
      | field      | value     |
      | searchText | john      |
      | orderBy    | firstName |
      | direction  | asc       |
    Then the response status code should be 200
    And the response should contain 3 users
    And the users should be sorted by "firstNames" in "asc" order

  Scenario: Search with searchText exceeding maximum length
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value                                                               |
      | searchText | ThisSearchTextIsWayTooLongAndShouldBeRejectedByTheValidationService |
    Then the response status code should be 400
    And the response should contain 0 users

    # Security-related tests
  Scenario Outline: Search with searchText containing SQL injection attempts
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value   |
      | searchText | <value> |
    Then the response status code should be 400
    And the response should contain 0 users
    Examples:
      | value                               |
      | john'; DROP TABLE users; SELECT '1  |
      | ' OR 1=1 --                         |
      | " OR 1=1 --                         |
      | '; DROP TABLE orders; --            |
      | admin' UNION SELECT * FROM users;-- |

  Scenario Outline: Search with searchText containing invalid or special characters
    When the endpoint "/users/search" to get users is hit with filters
      | field      | value   |
      | searchText | <value> |
    Then the response status code should be 400
    And the response should contain 0 users
    Examples:
      | value                              |
      | <script>alert('xss')</script>      |
      # Encoded SQL injection
      | %27%3B--                           |
      # Encoded XSS
      | &#60;script&#62;                   |
      # Double encoding
      | <<SCRIPT>alert("XSS");//<</SCRIPT> |
      # XSS with img tag
      | <img src=1 onerror=alert('hack')>  |
      # Obfuscated SQL Injection
      | \'; --                             |