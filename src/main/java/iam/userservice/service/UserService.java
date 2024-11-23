package iam.userservice.service;

import iam.userservice.dto.UserDto;
import iam.userservice.dto.UserMapper;
import iam.userservice.dto.UserRequestDto;
import iam.userservice.entity.User;
import iam.userservice.exception.ResourceAlreadyExistsException;
import iam.userservice.exception.ResourceNotFoundException;
import iam.userservice.exception.UserOptimisticLockException;
import iam.userservice.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserService{
     private final UserRepository userRepository;
     private final UserMapper userMapper;
     private final UserValidationService userValidationService;

    public static final String USER_NOT_FOUND_MESSAGE = "User not found";
    public static final String USER_ALREADY_EXISTS_MESSAGE = "User already exists";
    public static final String USERS = "users";

    public UserService(UserRepository userRepository, UserMapper userMapper, UserValidationService userValidationService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userValidationService = userValidationService;
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
    public UserDto updateUser(Long userId, UserRequestDto userRequestDto) {
        log.info("Update user with id '{}'", userId);

        userValidationService.validateUserRequestDto(userRequestDto);
        var existingUser = getExistingUser(userId);
        existingUser.setFirstName(userRequestDto.getFirstName());
        existingUser.setLastName(userRequestDto.getLastName());
        existingUser.setEmail(userRequestDto.getEmail());
        existingUser.setPhoneNumber(userRequestDto.getPhoneNumber());

        try {
            log.info("User [id: {}] updated successfully", userId);
            return userMapper.toDto(userRepository.save(existingUser));
        } catch (OptimisticLockException e) {
            log.error("Optimistic lock exception for user with id '{}'", userId);
            throw new UserOptimisticLockException("Concurrent modification detected. Please ty again");
        }
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
}
