package iam.userservice.controller;

import iam.userservice.dto.UserDto;
import iam.userservice.dto.UserRequestDto;
import iam.userservice.dto.UsersDto;
import iam.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/v1")
@RestController
@Slf4j
public record UserController(UserService userService) implements UsersApi{

    @Override
    public ResponseEntity<UsersDto> getUsers(Integer pageNo, Integer pageSize, String orderBy, String direction) {
        log.debug("Received request to get all users with pageNo {}, pageSize {}, direction {} and orderBy {}", pageNo, pageSize, direction, orderBy);
        var allUsers = userService.getAllUsers(pageNo, pageSize, direction, orderBy);
        var response = new UsersDto();
                response.setContent(allUsers.getContent());
                response.setTotalElements(allUsers.getTotalElements());
                response.setTotalPages(allUsers.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserDto> getUser(Long id) {
        log.debug("Received request to get user by id {}", id);
        var response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserDto> createUser(@Valid UserRequestDto userRequestDto) {
        log.debug("Received request to create user '{}'", userRequestDto);
        var response = userService.createUser(userRequestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }
}
