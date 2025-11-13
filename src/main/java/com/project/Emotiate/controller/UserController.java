package com.project.Emotiate.controller;

import com.project.Emotiate.dto.user.UserAddRequestDto;
import com.project.Emotiate.dto.user.UserResponseDto;
import com.project.Emotiate.dto.user.UserUpdateRequestDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.service.UserService;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/get")
    public ResponseEntity<Response<List<UserResponseDto>>> getUsers(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String role) {

        log.info("Retrieving users with isActive: {} and role: {}", isActive, role);
        return ResponseUtil.success(userService.getUsers(isActive, role), "Users retrieved successfully");
    }


    @PostMapping("/add")
    public ResponseEntity<Response<UserResponseDto>> addUser(@RequestBody UserAddRequestDto request) {

        log.info("Adding new user with username: {}", request.getUsername());
        return ResponseUtil.created(userService.addUser(request), "User added successfully");
    }


    @PutMapping("/edit/{id}")
    public ResponseEntity<Response<UserResponseDto>> editUser(@PathVariable Long id, @RequestBody UserUpdateRequestDto request) {

        log.info("Editing user with ID: {}", id);
        return ResponseUtil.success(userService.editUser(id, request), "User updated successfully");
    }


    @DeleteMapping("/remove/{id}")
    public ResponseEntity<Response<Void>> removeUser(@PathVariable Long id) {

        log.info("Request to remove user with ID: {}", id);
        return ResponseUtil.success(userService.removeUser(id), "User removed successfully");
    }
}