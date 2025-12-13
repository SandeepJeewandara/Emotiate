package com.project.Emotiate.repository;

import com.project.Emotiate.entity.NegotiationSession;
import com.project.Emotiate.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NegotiationSessionRepository extends JpaRepository<NegotiationSession, Long> {

    // Find session by its unique string session ID
    Optional<NegotiationSession> findBySessionId(String sessionId);


    // Retrieve all sessions with a given status
    List<NegotiationSession> findByStatus(SessionStatus status);


    // Check if a session ID already exists
    boolean existsBySessionId(String sessionId);
}