package com.project.Emotiate.repository;

import com.project.Emotiate.entity.User;
import com.project.Emotiate.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username or email
    Optional<User> findByUsernameAndIsActiveTrue(String username);


    // Check if a user exists by username or email
    boolean existsByUsernameAndIsActiveTrue(String username);


    // Find all active users
    List<User> findByIsActive(Boolean isActive);


    // Find all users by role
    List<User> findByRole(UserRole role);


    // Find all active users by role
    List<User> findByIsActiveAndRole(Boolean isActive, UserRole role);
}