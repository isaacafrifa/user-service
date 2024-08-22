package iam.userservice.service;

import iam.userservice.entity.User;
import iam.userservice.exception.UserNotFoundException;
import iam.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public record UserService(UserRepository userRepository) {

//    public void createUser() {
//
//    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User not found"));
    }


    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
