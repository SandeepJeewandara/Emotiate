package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.auth.AuthResponseDto;
import com.project.Emotiate.dto.auth.LoginRequestDto;
import com.project.Emotiate.entity.User;
import com.project.Emotiate.repository.UserRepository;
import com.project.Emotiate.security.JwtService;
import com.project.Emotiate.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    // Method to authenticate a user and return an authentication response
    public AuthResponseDto login(LoginRequestDto request) {

        // Normalize the username to lowercase
        String username = request.getUsername().toLowerCase(Locale.ROOT);

        // Authenticate the user using the provided username and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        request.getPassword()
                )
        );

        // Retrieve the user from the database
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate a JWT token for the authenticated user
        String token = jwtService.generateToken(user);

        // Log the successful authentication and token generation
        log.info("User {} authenticated successfully, token generated", username);

        // Return an authentication response
        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .username(username)
                .role(user.getRole())
                .build();
    }


    @Override
    // Method to log out a user based on their username
    public Void logout(String username) {

        // Remove security context
        SecurityContextHolder.clearContext();
        return null;
    }
}