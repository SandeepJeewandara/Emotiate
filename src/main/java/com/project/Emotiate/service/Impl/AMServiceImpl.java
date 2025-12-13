package com.project.Emotiate.service.Impl;

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

    // Thread-safe map of sessionId -> AgentController for lifecycle management
    private final ConcurrentHashMap<String, AgentController> agentControllerMap = new ConcurrentHashMap<>();

    @Override
    // Method to spawn a new UserAgent in the JADE container for a negotiation session
    public void createUserAgent(String sessionId, String guestName) {

        try{
            // Agent name is derived from the sessionId so it can be looked up later
            String agentName = "UserAgent-" + sessionId;

            // Pass sessionId and guestName as arguments to the agent constructor
            Object[] args = new Object[]{sessionId, guestName};

            AgentController userAgent = container.createNewAgent(
                    agentName,
                    UserAgent.class.getName(),
                    args
            );

            // Start user agent and set into map
            userAgent.start();
            agentControllerMap.put(sessionId, userAgent);
            log.info("UserAgent spawned: {} for session: {}", agentName, sessionId);

        } catch (StaleProxyException e) {

            log.error("Failed to create UserAgent for session: {}", sessionId, e);
            throw new CustomException("Failed to spawn agent for session: " + sessionId, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    @Override
    // Method to pass a guest message to the UserAgent via the O2A channel
    public void sendMessageToAgent(String sessionId, AgentMessageDto message) {

        AgentController agentController = agentControllerMap.get(sessionId);
        if (agentController == null) {

            // Check for active agent session
            log.error("No active agent found for session : {}", sessionId);
            throw new CustomException("No active agent found for session: " + sessionId, HttpStatus.NOT_FOUND.value());
        }
        try {

            // O2A (Object-to-Agent) channel passes the DTO directly
            agentController.putO2AObject(message, false);
            log.info("Message passed via O2A to agent for session: {}", sessionId);

        } catch (StaleProxyException e) {

            // Exception handling
            log.error("Failed to send message to agent for session: {}", sessionId, e);
            throw new CustomException("Failed to send message to agent for session: " + sessionId, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    @Override
    // Method to kill the UserAgent associated with a session and remove it from the registry
    public void terminateUserAgent(String sessionId) {

        AgentController agentController = agentControllerMap.remove(sessionId);
        if (agentController == null) {
            log.error("No active agent found for session: {}", sessionId);
            throw new CustomException("No active agent found for session: " + sessionId, HttpStatus.NOT_FOUND.value());
        }
        try {

            // Kill agent instance
            agentController.kill();
            agentControllerMap.remove(sessionId);
            log.info("UserAgent terminated for session: {}", sessionId);

        } catch (StaleProxyException e) {

            // Exception handling
            log.error("Failed to terminate UserAgent for session: {}", sessionId, e);
            throw new CustomException("Failed to terminate agent for session: " + sessionId, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}