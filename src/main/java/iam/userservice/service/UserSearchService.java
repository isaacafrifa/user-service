package iam.userservice.service;

import iam.userservice.mapper.UserDto;
import iam.userservice.mapper.UserMapper;
import iam.userservice.entity.User;
import iam.userservice.repository.UserFilterSpecification;
import iam.userservice.repository.UserSearchTextSpecification;
import iam.userservice.util.Pagination;
import iam.userservice.repository.UserRepository;
import iam.userservice.util.UserFilterCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service to perform searching and filtering of users.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserSearchService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Search for users based on filter criteria with pagination parameters.
     *
     * @return a page of users matching the filter criteria
     */
    public Page<UserDto> searchUsers(UserFilterCriteria userFilterCriteria, Pagination pagination) {
        log.info("Search users with criteria: {}, pagination: {}", userFilterCriteria, pagination);

        return searchUsers(userFilterCriteria, pagination.toPageable());
    }

    /**
     * Search for users based on filter criteria with pagination parameters.
     * 
     * @param userFilterCriteria the filter criteria
     * @param pageNo the page number
     * @param pageSize the page size
     * @param direction the sort direction
     * @param sortBy the field to sort by
     * @return a page of users matching the filter criteria
     */
    public Page<UserDto> searchUsers(UserFilterCriteria userFilterCriteria, int pageNo, int pageSize, String direction, String sortBy) {
        log.info("Search users with criteria: {}, pageNo: {}, pageSize: {}, direction: {}, sortBy: {}",
                userFilterCriteria, pageNo, pageSize, direction, sortBy);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(getSortDirection(direction), sortBy));
        return searchUsers(userFilterCriteria, pageable);
    }


    /**
     * Search for users based on filter criteria with pageable object.
     * 
     * @param userFilterCriteria the filter criteria
     * @param pageable the pageable object
     * @return a page of users matching the filter criteria
     */
    private Page<UserDto> searchUsers(UserFilterCriteria userFilterCriteria, Pageable pageable) {
        Specification<User> spec = buildSpecification(userFilterCriteria);

        return userRepository.findAll(spec, pageable)
                .map(userMapper::toDto);
    }

    /**
     * Converts the direction string to a Sort.Direction enum value.
     * 
     * @param direction the sort direction string
     * @return Sort.Direction.DESC if direction contains "desc", Sort.Direction.ASC otherwise
     */
    private Sort.Direction getSortDirection(String direction) {
        if (direction != null && direction.contains("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }

    private Specification<User> buildSpecification(UserFilterCriteria userFilterCriteria) {
//        return new UserFilterSpecification(userFilterCriteria);
//         return new UserSearchFieldSpecification(userFilterCriteria);

        Specification<User> filterSpec = new UserFilterSpecification(userFilterCriteria);

        // If searchText is provided, combine filter specification with text search specification
        if (userFilterCriteria != null && StringUtils.hasText(userFilterCriteria.getSearchText())) {
            Specification<User> textSearchSpec = new UserSearchTextSpecification(userFilterCriteria);
            // Combine both specifications with AND (user must match both filter criteria and text search)
            return filterSpec.and(textSearchSpec);
        }

        return filterSpec;
    }
}
