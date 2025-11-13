package com.project.Emotiate.service;

import com.project.Emotiate.dto.user.UserAddRequestDto;
import com.project.Emotiate.dto.user.UserResponseDto;
import com.project.Emotiate.dto.user.UserUpdateRequestDto;

import java.util.List;

public interface UserService {

    // Method to retrieve a list of users based on their active status and role
    List<UserResponseDto> getUsers(Boolean isActive, String role);


    // Method to create a new user and return an authentication response
    UserResponseDto addUser(UserAddRequestDto request);


    // Method to update an existing user and return user response
    UserResponseDto editUser(Long id, UserUpdateRequestDto request);


    // Method to remove an existing user
    Void removeUser(Long id);
}