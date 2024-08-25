package iam.userservice.service;

import iam.userservice.dto.UserDto;
import iam.userservice.dto.UserRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class UserValidationService {
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String EMAIL_ADDRESS_CANNOT_BE_NULL = "Email address cannot be null";
    public static final String INVALID_EMAIL_ADDRESS_FORMAT = "Invalid email address format";

    public void validateUserRequestDto(UserRequestDto userRequestDto) {
        validateEmail(userRequestDto.getEmail());
        // Add other validation rules as needed
    }

    public void validateUserDto(UserDto userDto) {
        validateEmail(userDto.getEmail());
    }

    private void validateEmail(String email) {
        Assert.notNull(email, EMAIL_ADDRESS_CANNOT_BE_NULL);
        Assert.isTrue(email.matches(EMAIL_PATTERN), INVALID_EMAIL_ADDRESS_FORMAT);
    }

}
