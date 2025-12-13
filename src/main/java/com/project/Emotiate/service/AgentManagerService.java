package com.project.Emotiate.service;

import com.project.Emotiate.dto.session.AgentMessageDto;

public interface AgentManagerService {

    // Method to spawn a new UserAgent in the JADE container for a negotiation session
    void createUserAgent(String sessionId, String guestName);


    // Method to pass a guest message to the UserAgent via the O2A channel
    void sendMessageToAgent(String sessionId, AgentMessageDto message);


    // Method to kill the UserAgent associated with a session
    void terminateUserAgent(String sessionId);
}