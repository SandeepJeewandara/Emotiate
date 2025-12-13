package com.project.Emotiate.repository;

import com.project.Emotiate.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Retrieve all messages for a session ordered chronologically
    List<ChatMessage> findBySession_SessionIdOrderByTimestampAsc(String sessionId);


    // Count total messages in a session
    long countBySession_SessionId(String sessionId);
}