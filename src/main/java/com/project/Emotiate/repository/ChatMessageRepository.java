package com.project.Emotiate.repository;

import com.project.Emotiate.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Retrieve all messages for a session ordered chronologically
    List<ChatMessage> findBySession_SessionIdOrderByTimestampAsc(String sessionId);


    // Count total messages in a session
    long countBySession_SessionId(String sessionId);


    // Average response time across all agent replies that have a recorded response time
    @Query("SELECT AVG(m.responseTimeMs) FROM ChatMessage m WHERE m.senderType = com.project.Emotiate.enums.SenderType.AGENT AND m.responseTimeMs IS NOT NULL")
    Double findAverageResponseTimeMs();


    // Count of agent replies that have a recorded response time
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.senderType = com.project.Emotiate.enums.SenderType.AGENT AND m.responseTimeMs IS NOT NULL")
    Long countRepliesWithResponseTime();
}