package com.project.Emotiate.service;

import com.project.Emotiate.dto.session.AgentMessageDto;

public interface AgentManagerService {

    // Method to spawn a paired UserAgent + SellerAgent in the JADE container for a session
    void createSessionAgents(String sessionId, String guestName);


    // Method to pass a guest message to the UserAgent via the O2A channel
    void sendMessageToAgent(String sessionId, AgentMessageDto message);


    // Method to kill both the UserAgent and SellerAgent associated with a session
    void terminateSessionAgents(String sessionId);
}