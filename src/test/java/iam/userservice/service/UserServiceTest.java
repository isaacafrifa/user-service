package iam.userservice.service;

import iam.userservice.mapper.UserDto;
import iam.userservice.mapper.UserMapper;
import iam.userservice.mapper.UserRequestDto;
import iam.userservice.entity.User;
import iam.userservice.events.UserEmailUpdatedEvent;
import iam.userservice.exception.EventPublishingException;
import iam.userservice.exception.ResourceAlreadyExistsException;
import iam.userservice.exception.ResourceNotFoundException;
import iam.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@DisplayName("Running userService tests")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    @InjectMocks
    private UserService underTest;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Captor
    ArgumentCaptor<User> userArgumentCaptor;
    @Captor
    private ArgumentCaptor<UserEmailUpdatedEvent> eventArgumentCaptor;

    private User user;
    private UserDto userDto;
    private UserRequestDto userRequestDto;

    public static final Long USER_ID = 1L;
    public static final String PHONE_NUMBER = "1234567890";
    public static final String LAST_NAME = "Doe";
    public static final String FIRST_NAME = "John";
    public static final String EMAIL = "john.doe@example.com";
    public static final long NON_EXISTENT_ID = 33L;
    // This pattern (XXX) includes the 3-digit zone offset (e.g. +05:30 for India Standard Time).
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final String EXCHANGE_NAME = "user.exchange";
    private static final String ROUTING_KEY = "user.email.update";

    @BeforeEach
    void setUp() {
        user = createDefaultUser();
        userDto = createDefaultUserDto();
        // Set the exchange name and routing key using reflection
        ReflectionTestUtils.setField(underTest, "exchangeName", EXCHANGE_NAME);
        ReflectionTestUtils.setField(underTest, "routingKey", ROUTING_KEY);

    }

    @Test
    void getAllBookings_shouldReturnEmptyPageWhenNoBookings() {
        // given
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));
        given(userRepository.findAll(pageable)).willReturn(Page.empty());
        // when
        var actual = underTest.getAllUsers(0, 5, "desc", "id");
        // then
        verify(userRepository).findAll(pageable);
        assertEquals(0, actual.getTotalElements(), "Expected no users");
    }

    @Test
    void getAllUsers_shouldGetAllUsers() {
        //given
        Page<User> page = new PageImpl<>(Collections.singletonList(user));
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        given(userRepository.findAll(pageable)).willReturn(page);
        //when
        var actual = underTest.getAllUsers(0, 5, "desc", "id");
        //then
        verify(userRepository).findAll(pageable);
        verify(userRepository).findAll(pageableCaptor.capture());
        var captorValue = pageableCaptor.getValue();
        assertEquals(5, captorValue.getPageSize());
        assertEquals(1, actual.getTotalElements(), "Expected to find one user");
    }

    @Test
    void getUserById_shouldReturnUserDto() {
        // given
        given(userMapper.toDto(any())).willReturn(userDto);
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        // when
        var actualDto = underTest.getUserById(user.getId());
        // then
        verify(userMapper).toDto(user);
        verify(userRepository).findById(user.getId());
        assertNotNull(actualDto, "Expected a UserDto to be returned");
    }

    @Test
    void getUserById_shouldReturnNotFoundException() {
        //given
        given(userRepository.findById(USER_ID)).willThrow(ResourceNotFoundException.class);
        //when + then
        assertThrows(ResourceNotFoundException.class,
                () -> underTest.getUserById(USER_ID),
                "Should throw user not found exception"
        );
        verify(userMapper, never()).toDto(user);
    }

    @Test
    void getUserById_shouldThrowExceptionForNullId() {
        // when + then
        assertThrows(ResourceNotFoundException.class, () -> underTest.getUserById(null));
    }

    @Test
    void getUserById_shouldThrowExceptionForInvalidIdType() {
        // Given
        String invalidId = "invalid-string-id";
        // When + Then
        try {
            underTest.getUserById(Long.valueOf(invalidId));
        } catch (Exception e) {
            assertInstanceOf(IllegalArgumentException.class, e, "Unexpected exception type: " + e.getClass().getName());
        }
    }

    @Test
    void getUserByUserEmail_shouldReturnUserDto() {
        // given
        given(userMapper.toDto(any())).willReturn(userDto);
        given(userRepository.findByEmail(any())).willReturn(Optional.of(user));
        // when
        var actualDto = underTest.getUserByEmail(user.getEmail());
        // then
        verify(userMapper).toDto(user);
        verify(userRepository).findByEmail(user.getEmail());
        assertNotNull(actualDto, "Expected a UserDto to be returned");
    }

    @Test
    void getUserByUserEmail_shouldThrowExceptionWhenUserNotFound() {
        // given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        // when
        var actual = underTest.getUserByEmail(EMAIL);
        // then
        verify(userMapper, never()).toDto(user);
        assertNull(actual);
    }

    @Test
    void getUserByUserEmail_shouldThrowExceptionWhenIdIsNull() {
        // when + then
        assertThrows(NullPointerException.class, () -> underTest.getUserByEmail(null));
    }

    @Test
    void saveUser_shouldSaveBooking() {
        // given
         userRequestDto = createUserRequestDto();

        given(userMapper.toEntity(userRequestDto)).willReturn(user);
        given(userRepository.save(user)).willReturn(user);
        // when
        underTest.createUser(userRequestDto);
        // then
        verify(userMapper).toEntity(userRequestDto);
        verify(userValidationService).validateUserRequestDto(userRequestDto);
        verify(userRepository).save(userArgumentCaptor.capture());
        verify(userMapper, times(1)).toDto(user);

        User capturedUser = userArgumentCaptor.getValue();
        assertEquals(user.getEmail(), capturedUser.getEmail());
        assertEquals(user.getFirstName(), capturedUser.getFirstName());
        assertEquals(user.getLastName(), capturedUser.getLastName());
        assertEquals(user.getPhoneNumber(), capturedUser.getPhoneNumber());
    }

    @Test
    void saveBooking_shouldThrowExceptionWhenBookingAlreadyExists() {
        // given
         userRequestDto = createUserRequestDto();

        given(userRepository.existsByEmailIgnoreCase(userRequestDto.getEmail())).willReturn(true);
        // when + then
        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> underTest.createUser(userRequestDto),
                "Should throw exception"
        );
        verify(userMapper, never()).toEntity(userRequestDto);
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
        verify(userValidationService).validateUserRequestDto(any());
    }

    @Test
    void updateUser_shouldUpdateUser() {
        // given
        userRequestDto = createUserRequestDto();
        // Set the same email to ensure no event is published
        userRequestDto.setEmail(user.getEmail()); // Make sure emails match
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(userMapper.toDto(any())).willReturn(new UserDto());

        // when
        underTest.updateUser(USER_ID, userRequestDto);

        // then
        verify(userRepository).save(userArgumentCaptor.capture());
        verify(rabbitTemplate, never()).convertAndSend(
                eq("exchangeName"),
                eq("routingKey"),
                any(UserEmailUpdatedEvent.class)
        );

        User capturedUser = userArgumentCaptor.getValue();
        assertEquals(user.getEmail(), capturedUser.getEmail());
        assertEquals(user.getFirstName(), capturedUser.getFirstName());
        assertEquals(user.getLastName(), capturedUser.getLastName());
        assertEquals(user.getPhoneNumber(), capturedUser.getPhoneNumber());
    }

    @Test
    void updateUser_withEmailChange_shouldPublishEvent() {
        // given
        String oldEmail = "old@email.com";
        String newEmail = "new@email.com";

        // Set up user with ID
        user.setId(USER_ID);
        user.setEmail(oldEmail);

        // Set up request DTO
        userRequestDto = createUserRequestDto();
        userRequestDto.setEmail(newEmail);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userMapper.toDto(any())).willReturn(new UserDto());
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        underTest.updateUser(USER_ID, userRequestDto);

        // then
        verify(userRepository).save(userArgumentCaptor.capture());
        verify(rabbitTemplate).convertAndSend(
                eq(EXCHANGE_NAME),
                eq(ROUTING_KEY),
                eventArgumentCaptor.capture()
        );

        // Verify user was updated correctly
        User capturedUser = userArgumentCaptor.getValue();
        assertEquals(USER_ID, capturedUser.getId());
        assertEquals(newEmail, capturedUser.getEmail());
        assertEquals(userRequestDto.getFirstName(), capturedUser.getFirstName());
        assertEquals(userRequestDto.getLastName(), capturedUser.getLastName());
        assertEquals(userRequestDto.getPhoneNumber(), capturedUser.getPhoneNumber());

        // Verify event was published with correct data
        UserEmailUpdatedEvent capturedEvent = eventArgumentCaptor.getValue();
        assertEquals(oldEmail, capturedEvent.getOldEmail());
        assertEquals(newEmail, capturedEvent.getNewEmail());
        assertEquals(USER_ID, capturedEvent.getUserId());
        assertNotNull(capturedEvent.getUpdatedAt());
    }


    @Test
    void updateUser_shouldThrowExceptionForNonexistentId() {
        // given
        given(userRepository.findById(NON_EXISTENT_ID)).willReturn(Optional.empty());

        // when + then
        assertThrows(ResourceNotFoundException.class,
                () -> underTest.updateUser(NON_EXISTENT_ID, userRequestDto),
                "Should throw an exception");

        verify(rabbitTemplate, never()).convertAndSend(
                eq(EXCHANGE_NAME),
                eq(ROUTING_KEY),
                any(UserEmailUpdatedEvent.class)
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenEventPublishingFails_shouldThrowEventPublishingException() {
        // given
        String oldEmail = "old@email.com";
        String newEmail = "new@email.com";
        user.setEmail(oldEmail);
        userRequestDto = createUserRequestDto();
        userRequestDto.setEmail(newEmail);

        given(userRepository.findById(any())).willReturn(Optional.of(user));

        doThrow(new AmqpException("Failed to publish"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(EXCHANGE_NAME),
                        eq(ROUTING_KEY),
                        any(UserEmailUpdatedEvent.class)
                );

        // when + then
        EventPublishingException exception = assertThrows(EventPublishingException.class,
                () -> underTest.updateUser(USER_ID, userRequestDto));

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Failed to publish email update event"));

        // Verify that the user was saved but event publishing failed
        verify(userRepository).save(any(User.class));
        verify(rabbitTemplate).convertAndSend(
                eq(EXCHANGE_NAME),
                eq(ROUTING_KEY),
                any(UserEmailUpdatedEvent.class)
        );
    }



    @Test
    void deleteUser_shouldDeleteUser() {
        // given
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        // when
        underTest.deleteUser(USER_ID);
        // then
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void deleteUser_shouldThrowExceptionForNonexistentId() {
        // given
        given(userRepository.findById(NON_EXISTENT_ID)).willThrow(ResourceNotFoundException.class);
        // when + then
        assertThrows(ResourceNotFoundException.class,
                () -> underTest.deleteUser(NON_EXISTENT_ID),
                "Should throw an exception");
        verify(userRepository, never()).deleteById(NON_EXISTENT_ID);
    }

    private User createDefaultUser() {
        var defaultUser = new User();
        defaultUser.setFirstName(FIRST_NAME);
        defaultUser.setLastName(LAST_NAME);
        defaultUser.setEmail(EMAIL);
        defaultUser.setPhoneNumber(PHONE_NUMBER);
        defaultUser.setCreatedOn(OffsetDateTime.parse("2022-08-01T10:00:00+00:00", formatter));
        defaultUser.setUpdatedOn(OffsetDateTime.parse("2022-08-01T10:00:00+00:00", formatter));

        return defaultUser;
    }

    private UserDto createDefaultUserDto() {
        var defaultUserDto = new UserDto();
        defaultUserDto.setFirstName(user.getFirstName());
        defaultUserDto.setLastName(user.getLastName());
        defaultUserDto.setEmail(user.getEmail());
        defaultUserDto.setPhoneNumber(user.getPhoneNumber());
        defaultUserDto.setId(USER_ID);
        defaultUserDto.setCreatedOn(user.getCreatedOn());
        defaultUserDto.setUpdatedOn(user.getUpdatedOn());
        return defaultUserDto;
    }

    private UserRequestDto createUserRequestDto() {
        var defaultUserRequestDto = new UserRequestDto();
        defaultUserRequestDto.setFirstName(user.getFirstName());
        defaultUserRequestDto.setLastName(user.getLastName());
        defaultUserRequestDto.setEmail(user.getEmail());
        defaultUserRequestDto.setPhoneNumber(user.getPhoneNumber());
        return defaultUserRequestDto;
    }
}