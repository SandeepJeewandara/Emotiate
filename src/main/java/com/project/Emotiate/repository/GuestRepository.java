package com.project.Emotiate.repository;

import com.project.Emotiate.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    // Find a guest by their session ID
    Optional<Guest> findBySessionId(String sessionId);
}