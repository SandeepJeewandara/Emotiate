package com.project.Emotiate.dto.auth;

import com.project.Emotiate.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;
    private Long userId;
    private String username;
    private UserRole role;
}