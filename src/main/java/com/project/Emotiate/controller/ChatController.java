package com.project.Emotiate.controller;

import com.project.Emotiate.dto.chat.SendMessageRequestDto;
import com.project.Emotiate.service.NegotiationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final NegotiationService negotiationService;

    // Handles guest messages sent via WebSocket to /app/session/message
    // The agent reply is pushed back to the guest via /topic/session/{sessionId}
    @MessageMapping("/session/message")
    public void handleGuestMessage(@Payload SendMessageRequestDto request) {

        log.info("WebSocket message received for session: {}", request.getSessionId());
        negotiationService.sendMessage(request);
        log.info("Message dispatched to agent for session: {}", request.getSessionId());
    }


    // Handles session abort requests sent via WebSocket to /app/session/abort
    @MessageMapping("/session/abort")
    public void handleSessionAbort(@Payload String sessionId) {

        log.info("WebSocket abort request for session: {}", sessionId);
        negotiationService.abortSession(sessionId);
    }
}