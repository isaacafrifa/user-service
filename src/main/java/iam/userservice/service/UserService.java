package iam.userservice.service;

import iam.userservice.mapper.UserDto;
import iam.userservice.mapper.UserFilterDto;
import iam.userservice.mapper.UserFilterMapper;
import iam.userservice.mapper.UserMapper;
import iam.userservice.mapper.UserRequestDto;
import iam.userservice.entity.User;
import iam.userservice.events.UserEmailUpdatedEvent;
import iam.userservice.exception.EventPublishingException;
import iam.userservice.exception.ResourceAlreadyExistsException;
import iam.userservice.exception.ResourceNotFoundException;
import iam.userservice.exception.UserOptimisticLockException;
import iam.userservice.util.Pagination;
import iam.userservice.repository.UserRepository;
import iam.userservice.util.UserFilterCriteria;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class UserService{
     private final UserRepository userRepository;
     private final UserMapper userMapper;
     private final UserValidationService userValidationService;
     private final RabbitTemplate rabbitTemplate;
     private final UserSearchService userSearchService;
    private final UserFilterMapper userFilterMapper;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public static final String USER_NOT_FOUND_MESSAGE = "User not found";
    public static final String USER_ALREADY_EXISTS_MESSAGE = "User already exists";
    public static final String USERS = "users";

    public UserService(UserRepository userRepository, UserMapper userMapper, UserValidationService userValidationService, RabbitTemplate rabbitTemplate, UserSearchService userSearchService, UserFilterMapper userFilterMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userValidationService = userValidationService;
        this.rabbitTemplate = rabbitTemplate;
        this.userSearchService = userSearchService;
        this.userFilterMapper = userFilterMapper;
    }

    /*
    Not caching getAllUsers() because getAllUsers()'s dataset could be large and will consume significant memory.
    I want to keep the memory consumption to a minimum.
     */
    public Page<UserDto> getAllUsers(int pageNo, int pageSize, String direction, String sortBy) {
        log.info("Get all users with pageNo '{}', pageSize '{}', direction '{}' and orderBy '{}'", pageNo, pageSize, direction, sortBy);

        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(getSortDirection(direction), sortBy));
        return userRepository.findAll(paging)
                .map(userMapper::toDto);
    }

    @Cacheable(value = USERS, key = "#userId")
    public UserDto getUserById(Long userId) {
        log.info("Get user by id '{}'", userId);

        return userRepository.findById(userId)
                .map(userMapper::toDto)
                .orElseThrow(
                        () -> {
                            log.info("User with id {} not found", userId);
                            return new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE);
                        });
    }

    @Cacheable(value = USERS, key = "#userEmail")
    public UserDto getUserByEmail(String userEmail) {
        log.info("Get user by userEmail '{}'", userEmail);

        userValidationService.validateUserEmail(userEmail);
        Optional<User> userOptional =  userRepository.findByEmail(userEmail.toLowerCase());
        return userOptional.map(userMapper::toDto).orElse(null);
    }

    public UserDto createUser(UserRequestDto userRequestDto) {
        log.info("Create user '{}'", userRequestDto);

        userValidationService.validateUserRequestDto(userRequestDto);
        if (Boolean.TRUE.equals(userRepository.existsByEmailIgnoreCase(userRequestDto.getEmail()))) {
            log.info("User [user email: {}] already exists", userRequestDto.getEmail());
            throw new ResourceAlreadyExistsException(USER_ALREADY_EXISTS_MESSAGE);
        }
        User toBeSaved = userMapper.toEntity(userRequestDto);
        var saved = userRepository.save(toBeSaved);
        log.info("User [id: {}] created successfully", saved.getId());
        return userMapper.toDto(saved);
    }

    /**
     * Updates user information and refreshes the cache.
     * The 'unless' condition prevents caching when the operation fails due to
     * optimistic locking (concurrent modification) or returns null.
     * This ensures the cache is only updated with successful operations.
     *
     * @param userId user identifier
     * @param userRequestDto updated user information
     * @return the updated UserDto
     */
    @CachePut(value = USERS, key = "#userId", unless = "#result == null")
    @Transactional
    public UserDto updateUser(Long userId, UserRequestDto userRequestDto) {
        log.info("Update user with id '{}'", userId);

        userValidationService.validateUserRequestDto(userRequestDto);
        var existingUser = getExistingUser(userId);

        // Only publish event if email actually changed
        String oldEmail = existingUser.getEmail();
        boolean emailChanged = !oldEmail.equals(userRequestDto.getEmail());

        // Update user fields
        existingUser.setFirstName(userRequestDto.getFirstName());
        existingUser.setLastName(userRequestDto.getLastName());
        existingUser.setEmail(userRequestDto.getEmail());
        existingUser.setPhoneNumber(userRequestDto.getPhoneNumber());

        User updatedUser;
        try {
            updatedUser = userRepository.save(existingUser);
            log.info("User [id: {}] updated successfully", userId);
        } catch (OptimisticLockException e) {
            log.error("Optimistic lock exception for user with id '{}'", userId);
            throw new UserOptimisticLockException("Concurrent modification detected. Please try again");
        }

        // Only publish event if email changed
        if (emailChanged) {
            try {
                publishEmailUpdateEvent(existingUser.getId(), oldEmail, userRequestDto.getEmail());
            } catch (AmqpException e) {
                log.error("Failed to publish email update event for user '{}': {}", userId, e.getMessage());
                throw new EventPublishingException("Failed to publish email update event: " + e.getMessage());
            }
        }

        return userMapper.toDto(updatedUser);
    }

    /**
     * Deletes user and refreshes the cache.
     * @param userId user identifier
     */
    @CacheEvict(value = USERS, key = "#userId")
    public void deleteUser(Long userId) {
        log.info("Delete user with id '{}'", userId);

        getExistingUser(userId);
        userRepository.deleteById(userId);
        log.info("User with id '{}' deleted successfully", userId);
    }

    private User getExistingUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> {
                    log.info("User with id '{}' not found", userId);
                    return new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE);
                });
    }

    private Sort.Direction getSortDirection(String direction) {
        assert direction != null;
        if (direction.contains("desc")) return Sort.Direction.DESC;
        return Sort.Direction.ASC;
    }

    /* Create and publish event
     */
    private void publishEmailUpdateEvent(Long userId, String oldEmail, String newEmail) {
        UserEmailUpdatedEvent event = UserEmailUpdatedEvent.builder()
                .userId(userId)
                .oldEmail(oldEmail)
                .newEmail(newEmail)
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Publishing email update event for user: {}", userId);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        log.info("Email update event published successfully");
    }

    /**
     * Search users based on the provided criteria.
     * 
     * @param filterDto the filter criteria
     * @param pageNo the page number
     * @param pageSize the page size
     * @param direction the sort direction
     * @param sortBy the field to sort by
     * @return a page of users matching the filter criteria
     */
    public Page<UserDto> searchUsers(UserFilterDto filterDto, int pageNo, int pageSize, String direction, String sortBy) {
       final Pagination pagination = Pagination.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .direction(direction)
                .sortBy(sortBy)
                .build();
       final UserFilterCriteria userFilterCriteria = userFilterMapper.toCriteria(filterDto);
        return userSearchService.searchUsers(userFilterCriteria, pagination);
    }

}
