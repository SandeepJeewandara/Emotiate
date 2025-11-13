package com.project.Emotiate.service;

import com.project.Emotiate.dto.auth.AuthResponseDto;
import com.project.Emotiate.dto.auth.LoginRequestDto;

public interface AuthService {

    // Method to authenticate a user and return an authentication response
    AuthResponseDto login(LoginRequestDto request);

    // Method to log out a user based on their username
    Void logout(String username);
}