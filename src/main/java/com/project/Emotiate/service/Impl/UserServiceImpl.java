package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.user.UserAddRequestDto;
import com.project.Emotiate.dto.user.UserResponseDto;
import com.project.Emotiate.dto.user.UserUpdateRequestDto;
import com.project.Emotiate.entity.User;
import com.project.Emotiate.enums.UserRole;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.repository.UserRepository;
import com.project.Emotiate.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    // Method to retrieve a list of users based on their active status and role
    public List<UserResponseDto> getUsers(Boolean isActive, String role) {

        // Declare a list to store retrieved users
        java.util.List<User> users;

        // Retrieve users based on active status and role
        if (isActive != null && role != null) {
            users = userRepository.findByIsActiveAndRole(isActive, UserRole.valueOf(role.toUpperCase(Locale.ROOT)));
        } else if (isActive != null) {
            users = userRepository.findByIsActive(isActive);
        } else if (role != null) {
            users = userRepository.findByRole(UserRole.valueOf(role.toUpperCase(Locale.ROOT)));
        } else {
            users = userRepository.findAll();
        }

        // Return the user list as UserResponseDto list
        return users.stream()
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .toList();
    }


    @Override
    // Method to create a new user and return an authentication response
    public UserResponseDto addUser(UserAddRequestDto request) {

        // Check if the username already exists in the database
        if (userRepository.existsByUsernameAndIsActiveTrue(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Get the user role from the request, defaulting to GUEST if not provided
        UserRole role = request.getRole() != null ? UserRole.valueOf(request.getRole()) : UserRole.GUEST;

        // Create User entity from the request data
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername().toLowerCase(Locale.ROOT))
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Return a UserResponseDto containing the user's information

        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }


    @Override
    // Method to update an existing user and return user response
    public UserResponseDto editUser(Long id, UserUpdateRequestDto request) {

        // Retrieve the user from the database
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with Id "+id+" not found"));

        // Normalize the username to lowercase if provided
        String username = request.getUsername() != null
                ? request.getUsername().toLowerCase(Locale.ROOT)
                : user.getUsername();

        // Check if the updated username already exists for another user
        userRepository.findByUsernameAndIsActiveTrue(username).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(id)) {
                throw new CustomException("Username already exists", HttpStatus.BAD_REQUEST.value());
            }
        });

        // Update the user fields
        user.setFirstName(request.getFirstName() != null ? request.getFirstName() : user.getFirstName());
        user.setLastName(request.getLastName() != null ? request.getLastName() : user.getLastName());
        user.setUsername(username);
        user.setEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());
        user.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : user.getPhoneNumber());

        // Update the password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update the role if provided
        if (request.getRole() != null) {
            user.setRole(UserRole.valueOf(request.getRole()));
        }

        // Update the active status if provided
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        // Update the updatedAt timestamp and save the user to the database
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Return a UserResponseDto containing the updated user's information
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }


    @Override
    // Method to remove an existing user
    public Void removeUser(Long id) {

        // Retrieve the user from the database
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User with Id " +id +" not found", HttpStatus.NOT_FOUND.value()));

        // Check if the user is already inactive
        if (!user.getIsActive()) {
            throw new CustomException("User with Id " +id +" is already inactive", HttpStatus.BAD_REQUEST.value());
        }

        // Soft delete user from the database
        user.setIsActive(false);
        userRepository.save(user);

        return null;
    }
}