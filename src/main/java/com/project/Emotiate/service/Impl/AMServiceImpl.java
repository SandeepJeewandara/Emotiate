package com.project.Emotiate.service.Impl;

import com.project.Emotiate.agent.SellerAgent;
import com.project.Emotiate.agent.UserAgent;
import com.project.Emotiate.dto.session.AgentMessageDto;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.service.AgentManagerService;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AMServiceImpl implements AgentManagerService {

    private final AgentContainer container;

    // Thread-safe map of sessionId -> UserAgent controller
    private final ConcurrentHashMap<String, AgentController> userAgentMap = new ConcurrentHashMap<>();

    // Thread-safe map of sessionId -> SellerAgent controller
    private final ConcurrentHashMap<String, AgentController> sellerAgentMap = new ConcurrentHashMap<>();


    @Override
    // Method to spawn a paired UserAgent + SellerAgent in the JADE container for a session
    public void createSessionAgents(String sessionId, String guestName) {

        spawnSellerAgent(sessionId);
        spawnUserAgent(sessionId, guestName);
        log.info("Session agent pair created for session: {}", sessionId);
    }


    // Spawns a UserAgent instance scoped to this session and registers it in userAgentMap
    private void spawnUserAgent(String sessionId, String guestName) {

        try {
            // Naming convention: UserAgent-{sessionId} ensures uniqueness per session
            String agentName = "UserAgent-" + sessionId;

            // Pass sessionId and guestName so the UserAgent can build its context
            Object[] args = new Object[]{sessionId, guestName};

            AgentController userAgent = container.createNewAgent(
                    agentName,
                    UserAgent.class.getName(),
                    args
            );

            userAgent.start();
            userAgentMap.put(sessionId, userAgent);
            log.info("UserAgent spawned: {}", agentName);

        } catch (Exception e) {
            log.error("Failed to create UserAgent for session: {}", sessionId, e);

            // SellerAgent was already started — clean it up to avoid orphaned agents
            terminateAgent(sessionId, sellerAgentMap, "SellerAgent");

            throw new CustomException("Failed to spawn UserAgent for session: " + sessionId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    // Spawns a SellerAgent instance scoped to this session and registers it in sellerAgentMap
    private void spawnSellerAgent(String sessionId) {

        try {
            // Naming convention: SellerAgent-{sessionId} ensures uniqueness per session
            String agentName = "SellerAgent-" + sessionId;

            // Pass sessionId as an argument so the SellerAgent knows which session it belongs to
            Object[] args = new Object[]{sessionId};

            AgentController sellerAgent = container.createNewAgent(
                    agentName,
                    SellerAgent.class.getName(),
                    args
            );

            sellerAgent.start();
            sellerAgentMap.put(sessionId, sellerAgent);
            log.info("SellerAgent spawned: {}", agentName);

        } catch (Exception e) {
            log.error("Failed to create SellerAgent for session: {}", sessionId, e);
            throw new CustomException("Failed to spawn SellerAgent for session: " + sessionId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    @Override
    // Method to pass a guest message to the UserAgent via the O2A channel
    public void sendMessageToAgent(String sessionId, AgentMessageDto message) {

        AgentController userAgentController = userAgentMap.get(sessionId);
        if (userAgentController == null) {

            // Check for active agent session
            log.error("No active agent found for session : {}", sessionId);
            throw new CustomException("No active agent found for session: " + sessionId, HttpStatus.NOT_FOUND.value());
        }
        try {

            // O2A (Object-to-Agent) channel passes the DTO directly
            userAgentController.putO2AObject(message, false);
            log.info("Message passed via O2A to agent for session: {}", sessionId);

        } catch (StaleProxyException e) {

            // Exception handling
            log.error("Failed to send message to agent for session: {}", sessionId, e);
            throw new CustomException("Failed to send message to agent for session: " + sessionId, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    @Override
    // Method to kill both the UserAgent and SellerAgent associated with a session
    public void terminateSessionAgents(String sessionId) {

        terminateAgent(sessionId, userAgentMap, "UserAgent");
        terminateAgent(sessionId, sellerAgentMap, "SellerAgent");
        log.info("Session agent pair terminated for session: {}", sessionId);
    }


    // Shared helper to safely kill an agent and remove it from its registry map
    private void terminateAgent(String sessionId, ConcurrentHashMap<String, AgentController> map, String agentType) {

        AgentController controller = map.get(sessionId);

        // Check for session
        if (controller == null) {
            log.warn("Attempted to terminate non-existent {} for session: {}", agentType, sessionId);
            return;
        }

        try {
            // Stop the controller process
            controller.kill();
            map.remove(sessionId);
            log.info("{} terminated for session: {}", agentType, sessionId);

        } catch (Exception e) {
            log.warn("Error while terminating {} for session: {}", agentType, sessionId, e);
        }
    }
}