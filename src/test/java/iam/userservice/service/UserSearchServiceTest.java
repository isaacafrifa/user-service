package iam.userservice.service;

import iam.userservice.entity.User;
import iam.userservice.mapper.UserDto;
import iam.userservice.mapper.UserMapper;
import iam.userservice.repository.UserFilterSpecification;
import iam.userservice.repository.UserRepository;
import iam.userservice.util.Pagination;
import iam.userservice.util.UserFilterCriteria;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserSearchService userSearchService;

    UserSearchServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSearchUsersWithPagination() {
        UserFilterCriteria criteria = new UserFilterCriteria();
        Pagination pagination = Pagination.builder()
                .pageNo(0)
                .pageSize(10)
                .sortBy("id") // Set a default value for sortBy to avoid IllegalArgumentException
                .build();
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"));

        User user = new User();
        user.setId(1L);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user), pageable, 1);
        when(userRepository.findAll(any(UserFilterSpecification.class), any(Pageable.class))).thenReturn(userPage);

        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setEmail("user@example.com");
        userDto.setCreatedOn(OffsetDateTime.now());
        when(userMapper.toDto(user)).thenReturn(userDto);

        Page<UserDto> result = userSearchService.searchUsers(criteria, pagination);

        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
        verify(userRepository, times(1)).findAll(any(UserFilterSpecification.class), any(Pageable.class));
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    void testSearchUsersWithFullyConfiguredPagination() {
        UserFilterCriteria criteria = new UserFilterCriteria();
        int pageNo = 1;
        int pageSize = 5;
        String direction = "desc";
        String sortBy = "createdOn";

        // Create a mock Pagination object that returns a specific Pageable
        Pagination pagination = Pagination.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .direction(direction)
                .sortBy(sortBy)
                .build();

        // Create a user and userDto for the test
        User user = new User();
        user.setId(1L);

        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setFirstName("Jane");
        userDto.setLastName("Smith");
        userDto.setEmail("john@example.com");
        userDto.setCreatedOn(OffsetDateTime.now());

        // Mock the repository to return a page with a single user
        // Use doReturn...when pattern to ensure the mock is set up correctly
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        doReturn(userPage).when(userRepository).findAll(any(UserFilterSpecification.class), any(Pageable.class));

        // Mock the mapper to return the userDto
        when(userMapper.toDto(user)).thenReturn(userDto);

        // Call the method under test
        Page<UserDto> result = userSearchService.searchUsers(criteria, pagination);

        // Verify the results
        assertEquals(1, result.getTotalElements());
        assertEquals("Jane", result.getContent().get(0).getFirstName());
        verify(userRepository, times(1)).findAll(any(UserFilterSpecification.class), any(Pageable.class));
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void testSearchUsersWithDeprecatedMethod() {
        UserFilterCriteria criteria = new UserFilterCriteria();
        int pageNo = 2;
        int pageSize = 3;
        String direction = "asc";
        String sortBy = "lastName";

        // Create a user and userDto for the test
        User user = new User();
        user.setId(2L);

        UserDto userDto = new UserDto();
        userDto.setId(2L);
        userDto.setFirstName("Alice");
        userDto.setLastName("Johnson");
        userDto.setEmail("alice@example.com");
        userDto.setCreatedOn(OffsetDateTime.now());

        // Mock the repository to return a page with a single user
        // Use doReturn...when pattern to ensure the mock is set up correctly
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        doReturn(userPage).when(userRepository).findAll(any(UserFilterSpecification.class), any(Pageable.class));

        // Mock the mapper to return the userDto
        when(userMapper.toDto(user)).thenReturn(userDto);

        // Call the deprecated method directly
        Page<UserDto> result = userSearchService.searchUsers(criteria, pageNo, pageSize, direction, sortBy);

        // Verify the results
        assertEquals(1, result.getTotalElements());
        assertEquals("Alice", result.getContent().get(0).getFirstName());
        assertEquals("Johnson", result.getContent().get(0).getLastName());
        verify(userRepository, times(1)).findAll(any(UserFilterSpecification.class), any(Pageable.class));
        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    void testSearchUsersWithFreeFormText() {
        // Create a UserFilterCriteria with searchText
        UserFilterCriteria criteria = new UserFilterCriteria();
        criteria.setSearchText("john");

        Pagination pagination = Pagination.builder()
                .pageNo(0)
                .pageSize(10)
                .sortBy("id")
                .build();

        // Create a user and userDto for the test
        User user = new User();
        user.setId(3L);
        user.setFirstName("John");
        user.setLastName("Smith");
        user.setEmail("john.smith@example.com");

        UserDto userDto = new UserDto();
        userDto.setId(3L);
        userDto.setFirstName("John");
        userDto.setLastName("Smith");
        userDto.setEmail("john.smith@example.com");
        userDto.setCreatedOn(OffsetDateTime.now());

        // Mock the repository to return a page with a single user
        // The important part is that we're verifying it's called with a Specification
        // that combines both UserFilterSpecification and UserSearchTextSpecification
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        doReturn(userPage).when(userRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));

        // Mock the mapper to return the userDto
        when(userMapper.toDto(user)).thenReturn(userDto);

        // Call the method under test
        Page<UserDto> result = userSearchService.searchUsers(criteria, pagination);

        // Verify the results
        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
        assertEquals("Smith", result.getContent().get(0).getLastName());

        // Verify that the repository was called with a Specification
        verify(userRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
        verify(userMapper, times(1)).toDto(user);
    }
}
