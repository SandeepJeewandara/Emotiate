package com.project.Emotiate.service;

import com.project.Emotiate.dto.chat.ChatMessageResponseDto;
import com.project.Emotiate.dto.chat.UserAgentReplyDto;
import com.project.Emotiate.dto.chat.SendMessageRequestDto;
import com.project.Emotiate.dto.session.NegotiationSessionResponseDto;
import com.project.Emotiate.dto.session.StartSessionRequestDto;

import java.util.List;

public interface NegotiationService {

    // Method to create a new negotiation session and spawn a UserAgent for it
    NegotiationSessionResponseDto startSession(StartSessionRequestDto request);


    // Method to persist a guest message and forward it to the UserAgent via O2A
    ChatMessageResponseDto sendMessage(SendMessageRequestDto request);


    // Method to retrieve the full message history for a session
    List<ChatMessageResponseDto> getSessionMessages(String sessionId);


    // Method to retrieve session details by session ID
    NegotiationSessionResponseDto getSession(String sessionId);


    // Method to retrieve all currently active sessions for admin monitoring
    List<NegotiationSessionResponseDto> getActiveSessions();


    // Method to abort a session without agreement and terminate its UserAgent
    NegotiationSessionResponseDto abortSession(String sessionId);


    // Method to mark a session as completed after booking confirmation
    NegotiationSessionResponseDto completeSession(String sessionId);


    // Method called by the UserAgent to persist its reply and push it via WebSocket
    ChatMessageResponseDto saveAgentReply(UserAgentReplyDto request);
}