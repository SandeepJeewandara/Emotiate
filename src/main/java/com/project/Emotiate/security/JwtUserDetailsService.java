package com.project.Emotiate.security;

import com.project.Emotiate.entity.User;
import com.project.Emotiate.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    // Method to load user details by username for authentication
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

        // Check if the user exists in the database, if not throw an exception
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new BadCredentialsException("User not found with username: " + username));

        // Return a UserDetails object containing user info
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getIsActive(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}