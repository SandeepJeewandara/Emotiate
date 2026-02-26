
package com.project.Emotiate.controller;

import com.project.Emotiate.dto.chat.ChatMessageResponseDto;
import com.project.Emotiate.dto.chat.SendMessageRequestDto;
import com.project.Emotiate.dto.session.NegotiationSessionResponseDto;
import com.project.Emotiate.dto.session.StartSessionRequestDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.service.NegotiationService;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationService negotiationService;

    @PostMapping("/session/start")
    public ResponseEntity<Response<NegotiationSessionResponseDto>> startSession(@RequestBody StartSessionRequestDto request) {

        log.info("New negotiation session requested for guest: {}", request.getGuestName());
        return ResponseUtil.created(negotiationService.startSession(request), "Negotiation session started");
    }


    @PostMapping("/session/message")
    public ResponseEntity<Response<ChatMessageResponseDto>> sendMessage(@RequestBody SendMessageRequestDto request) {

        log.debug("Guest message received for session: {}", request.getSessionId());
        return ResponseUtil.success(negotiationService.sendMessage(request), "Message sent successfully");
    }


    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Response<NegotiationSessionResponseDto>> getSession(@PathVariable String sessionId) {

        log.info("Retrieving session details for: {}", sessionId);
        return ResponseUtil.success(negotiationService.getSession(sessionId), "Session retrieved successfully");
    }


    @GetMapping("/sessions")
    public ResponseEntity<Response<List<NegotiationSessionResponseDto>>> getAllSessions() {

        log.info("Retrieving all negotiation sessions");
        return ResponseUtil.success(negotiationService.getAllSessions(), "All sessions retrieved successfully");
    }


    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<Response<List<ChatMessageResponseDto>>> getSessionMessages(@PathVariable String sessionId) {

        log.info("Retrieving message history for session: {}", sessionId);
        return ResponseUtil.success(negotiationService.getSessionMessages(sessionId), "Messages retrieved successfully");
    }


    @GetMapping("/sessions/active")
    public ResponseEntity<Response<List<NegotiationSessionResponseDto>>> getActiveSessions() {

        log.info("Retrieving all active negotiation sessions");
        return ResponseUtil.success(negotiationService.getActiveSessions(), "Active sessions retrieved successfully");
    }


    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Response<Void>> deleteSession(@PathVariable Long id) {

        log.info("Deleting session with id: {}", id);
        return ResponseUtil.success(negotiationService.deleteSession(id), "Session deleted successfully");
    }


    @PutMapping("/session/{sessionId}/abort")
    public ResponseEntity<Response<NegotiationSessionResponseDto>> abortSession(@PathVariable String sessionId) {

        log.info("Aborting session: {}", sessionId);
        return ResponseUtil.success(negotiationService.abortSession(sessionId), "Session aborted successfully");
    }


    @PutMapping("/session/{sessionId}/complete")
    public ResponseEntity<Response<NegotiationSessionResponseDto>> completeSession(@PathVariable String sessionId) {

        log.info("Completing session: {}", sessionId);
        return ResponseUtil.success(negotiationService.completeSession(sessionId), "Session completed successfully");
    }


    @GetMapping("/stats/response-time")
    public ResponseEntity<Response<Map<String, Object>>> getResponseTimeStats() {

        log.info("Retrieving response time statistics");
        return ResponseUtil.success(negotiationService.getResponseTimeStats(), "Response time statistics retrieved");
    }
}
