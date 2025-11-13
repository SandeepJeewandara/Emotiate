package com.project.Emotiate.controller;

import com.project.Emotiate.dto.auth.AuthResponseDto;
import com.project.Emotiate.dto.auth.LoginRequestDto;
import com.project.Emotiate.dto.auth.LogoutRequestDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.service.AuthService;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Response<AuthResponseDto>> login(@RequestBody LoginRequestDto request) {

        log.info("Login attempt for user: {}", request.getUsername());
        return ResponseUtil.success(authService.login(request),"User "+ request.getUsername() + " logged in successfully");
    }


    @PostMapping("/logout")
    public ResponseEntity<Response<Void>> logout(@RequestBody LogoutRequestDto logoutRequestDto) {

        log.info("Logout attempt for user: {}", logoutRequestDto.getUsername());
        return ResponseUtil.success(authService.logout(logoutRequestDto.getUsername()), "User " + logoutRequestDto.getUsername() + " logged out successfully");
    }
}