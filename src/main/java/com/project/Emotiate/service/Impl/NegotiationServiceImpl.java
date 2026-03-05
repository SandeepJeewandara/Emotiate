package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.chat.ChatMessageResponseDto;
import com.project.Emotiate.dto.chat.SendMessageRequestDto;
import com.project.Emotiate.dto.chat.UserAgentReplyDto;
import com.project.Emotiate.dto.session.AgentMessageDto;
import com.project.Emotiate.dto.session.NegotiationSessionResponseDto;
import com.project.Emotiate.dto.session.StartSessionRequestDto;
import com.project.Emotiate.entity.ChatMessage;
import com.project.Emotiate.entity.NegotiationSession;
import com.project.Emotiate.enums.MessageType;
import com.project.Emotiate.enums.SenderType;
import com.project.Emotiate.enums.SessionStatus;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.repository.ChatMessageRepository;
import com.project.Emotiate.repository.NegotiationSessionRepository;
import com.project.Emotiate.service.AgentManagerService;
import com.project.Emotiate.service.NegotiationService;
import com.project.Emotiate.util.ResponseTimeTracker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class NegotiationServiceImpl implements NegotiationService {

    private final NegotiationSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentManagerService agentManagerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ResponseTimeTracker responseTimeTracker;

    @Override
    // Method to create a new negotiation session and spawn a UserAgent for it
    public NegotiationSessionResponseDto startSession(StartSessionRequestDto request) {

        // Generate a unique session ID prefixed for easy identification in logs
        String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);

        // Build and persist the new session with ACTIVE status
        NegotiationSession session = NegotiationSession.builder()
                .sessionId(sessionId)
                .guestId(request.getGuestId())
                .guestName(request.getGuestName())
                .status(SessionStatus.ACTIVE)
                .currentRound(0)
                .startedAt(LocalDateTime.now())
                .build();


        sessionRepository.save(session);
        log.info("Negotiation session created: {}", sessionId);

        // Spawn a dedicated UserAgent in the JADE container for this session
        agentManagerService.createSessionAgents(sessionId, request.getGuestName());

        // Map to Dto and return
        return mapSessionToDto(session);
    }


    @Override
    // Method to persist a guest message and forward it to the UserAgent via O2A
    public ChatMessageResponseDto sendMessage(SendMessageRequestDto request) {

        // Retrieve and validate the session is still active
        NegotiationSession session = getActiveSession(request.getSessionId());

        // Persist the guest message before dispatching to the agent
        ChatMessage guestMessage = ChatMessage.builder()
                .session(session)
                .senderType(SenderType.GUEST)
                .messageType(MessageType.TEXT)
                .content(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        messageRepository.save(guestMessage);

        // Increment the negotiation round counter
        session.setCurrentRound(session.getCurrentRound() + 1);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Build the full conversation history to give the LLM proper context
        List<String> conversationHistory = buildConversationHistory(request.getSessionId());

        // Wrap the message in an AgentMessageDto and pass via the O2A channel
        AgentMessageDto agentMessage = AgentMessageDto.builder()
                .sessionId(request.getSessionId())
                .guestMessage(request.getMessage())
                .guestName(session.getGuestName())
                .conversationHistory(conversationHistory)
                .round(session.getCurrentRound())
                .build();

        // Start tracking time
        responseTimeTracker.recordMessageDispatched(request.getSessionId());

        agentManagerService.sendMessageToAgent(request.getSessionId(), agentMessage);
        return mapMessageToDto(guestMessage);
    }


    @Override
    // Method to retrieve the full message history for a session ordered by time
    public List<ChatMessageResponseDto> getSessionMessages(String sessionId) {

        return messageRepository.findBySession_SessionIdOrderByTimestampAsc(sessionId)
                .stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());
    }


    @Override
    // Method to retrieve session details by session ID
    public NegotiationSessionResponseDto getSession(String sessionId) {

        NegotiationSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException("Session not found: " + sessionId, HttpStatus.NOT_FOUND.value()));

        return mapSessionToDto(session);
    }


    @Override
    // Method to retrieve all sessions for session history management
    public List<NegotiationSessionResponseDto> getAllSessions() {

        return sessionRepository.findAll()
                .stream()
                .map(this::mapSessionToDto)
                .collect(Collectors.toList());
    }


    @Override
    // Method to retrieve all currently active sessions for admin monitoring
    public List<NegotiationSessionResponseDto> getActiveSessions() {

        return sessionRepository.findByStatus(SessionStatus.ACTIVE)
                .stream()
                .map(this::mapSessionToDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    // Method to delete a session and its chat history by database ID
    public Void deleteSession(Long id) {

        // Find the session by database
        NegotiationSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new CustomException("Session not found with id: " + id, HttpStatus.NOT_FOUND.value()));

        // Stop the running agent first if the session is still active
        if (session.getStatus() == SessionStatus.ACTIVE) {
            agentManagerService.terminateSessionAgents(session.getSessionId());
        }

        // Delete the session record
        sessionRepository.delete(session);
        log.info("Session deleted: id={} sessionId={}", id, session.getSessionId());

        return null;
    }


    @Override
    // Method to abort a session without agreement and terminate its UserAgent
    public NegotiationSessionResponseDto abortSession(String sessionId) {

        NegotiationSession session = getActiveSession(sessionId);

        // Update status and record end time
        session.setStatus(SessionStatus.ABORTED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Kill the UserAgent in the JADE container for this session
        agentManagerService.terminateSessionAgents(sessionId);
        log.info("Session aborted: {}", sessionId);

        return mapSessionToDto(session);
    }


    @Override
    // Method to mark a session as completed after booking confirmation
    public NegotiationSessionResponseDto completeSession(String sessionId) {

        NegotiationSession session = getActiveSession(sessionId);

        // Update status and record end time
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Kill the UserAgent in the JADE container for this session
        agentManagerService.terminateSessionAgents(sessionId);
        log.info("Session completed: {}", sessionId);

        return mapSessionToDto(session);
    }


    @Override
    // Method called by the UserAgent to persist its reply and push it via WebSocket
    public void saveAgentReply(UserAgentReplyDto request) {

        NegotiationSession session = sessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new CustomException("Session not found: " + request.getSessionId(), HttpStatus.NOT_FOUND.value()));

        // Update the session with the latest negotiated values coming from the agent pipeline.
        if (request.getOfferedPrice() != null) {
            session.setOfferedPrice(request.getOfferedPrice());
        }
        if (request.getRecommendedPackageId() != null) {
            session.setRecommendedPackageId(request.getRecommendedPackageId());
        }
        if (request.getBookingReference() != null && !request.getBookingReference().isBlank()) {
            session.setBookingReference(request.getBookingReference());
        }
        if (Boolean.TRUE.equals(request.getBookingComplete())) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndedAt(LocalDateTime.now());
        }
        sessionRepository.save(session);

        // Determine message type based on content
        MessageType type;
        if (Boolean.TRUE.equals(request.getBookingComplete())) {
            type = MessageType.BOOKING_CARD;
        } else if (request.getMetadata() != null && !request.getMetadata().isBlank()) {
            type = MessageType.PACKAGE_CARD;
        } else {
            type = MessageType.TEXT;
        }

        // Stop tracking time
        Long responseTimeMs = responseTimeTracker.computeAndClear(request.getSessionId());

        // Persist the agent reply message
        ChatMessage agentMessage = ChatMessage.builder()
                .session(session)
                .senderType(SenderType.AGENT)
                .messageType(type)
                .content(request.getAgentReply())
                .detectedEmotion(request.getDetectedEmotion())
                .offeredPrice(request.getOfferedPrice())
                .metadata(request.getMetadata())
                .responseTimeMs(responseTimeMs)
                .timestamp(LocalDateTime.now())
                .build();

        messageRepository.save(agentMessage);
        ChatMessageResponseDto responseDto = mapMessageToDto(agentMessage);

        // Push the agent reply to the guest chat in real time via WebSocket
        messagingTemplate.convertAndSend("/topic/session/" + request.getSessionId(), responseDto);
    }


    @Override
    // Method to retrieve average agent reply time across all recorded messages
    public Map<String, Object> getResponseTimeStats() {
        Double average = messageRepository.findAverageResponseTimeMs();
        Long count = messageRepository.countRepliesWithResponseTime();
        return Map.of(
                "averageResponseTimeMs", average != null ? average : 0.0,
                "totalReplies", count != null ? count : 0L
        );
    }


    // Helper: retrieve a session by ID and verify it is currently ACTIVE
    private NegotiationSession getActiveSession(String sessionId) {
        NegotiationSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException("Session not found: " + sessionId, HttpStatus.NOT_FOUND.value()));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new CustomException("Session " + sessionId + " is not active", HttpStatus.BAD_REQUEST.value());
        }
        return session;
    }


    // Helper: build an ordered list of "SENDER: message" strings for LLM conversation context
    private List<String> buildConversationHistory(String sessionId) {
        return messageRepository.findBySession_SessionIdOrderByTimestampAsc(sessionId)
                .stream()
                .map(msg -> msg.getSenderType() + ": " + msg.getContent())
                .collect(Collectors.toList());
    }


    // Helper: map NegotiationSession entity to response DTO
    private NegotiationSessionResponseDto mapSessionToDto(NegotiationSession session) {
        return NegotiationSessionResponseDto.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .guestId(session.getGuestId())
                .guestName(session.getGuestName())
                .status(session.getStatus())
                .currentRound(session.getCurrentRound())
                .offeredPrice(session.getOfferedPrice())
                .recommendedPackageId(session.getRecommendedPackageId())
                .bookingReference(session.getBookingReference())
                .startedAt(session.getStartedAt())
                .updatedAt(session.getUpdatedAt())
                .endedAt(session.getEndedAt())
                .build();
    }


    // Helper: map ChatMessage entity to response DTO
    private ChatMessageResponseDto mapMessageToDto(ChatMessage message) {
        return ChatMessageResponseDto.builder()
                .id(message.getId())
                .sessionId(message.getSession().getSessionId())
                .senderType(message.getSenderType())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .detectedEmotion(message.getDetectedEmotion())
                .offeredPrice(message.getOfferedPrice())
                .metadata(message.getMetadata())
                .responseTimeMs(message.getResponseTimeMs())
                .timestamp(message.getTimestamp())
                .build();
    }
}
