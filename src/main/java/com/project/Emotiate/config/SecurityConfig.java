package com.project.Emotiate.config;

import com.project.Emotiate.security.JwtAuthenticationFilter;
import com.project.Emotiate.security.JwtUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtUserDetailsService userDetailsService;

    @Bean
    // Configure security filters
    public SecurityFilterChain securityFilterChain(HttpSecurity http){
        http
                // Disable CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Chat endpoints
                        .requestMatchers(HttpMethod.GET, "/api/chat/sessions").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/chat/sessions/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Role-based endpoints
                        .requestMatchers("/api/emotion/**").permitAll()
                        .requestMatchers("/api/package/get").permitAll()
                        .requestMatchers("/api/room/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/package/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/booking/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/user/**").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Configure authentication provider
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Handle unauthorized requests
                .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {

                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");

                    String message;
                    if (authException.getCause() instanceof DisabledException ||
                            authException instanceof DisabledException) {
                        message = "Account is disabled. Please contact support";
                    } else {
                        message = "Unauthorized access";
                    }

                    response.getWriter().write("""
                    {
                        "data": null,
                        "message": "%s",
                        "status": 401
                    }
                    """.formatted(message));
                }) .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");

                    response.getWriter().write("""
                    {
                        "data": null,
                        "message": "Access denied. You do not have permission to access this resource",
                        "status": 403
                    }
                    """);
                })
        );

        return http.build();
    }


    @Bean
    // Configure authentication provider
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }


    @Bean
    // Configure password encoder
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    // Configure authentication manager
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
