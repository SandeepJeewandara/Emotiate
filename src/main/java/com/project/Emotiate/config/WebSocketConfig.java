package com.project.Emotiate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    // Configures broker paths for subscriptions and application-handled messages.
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // In memory broker for session-scoped topics
        // Clients subscribe to /topic/session/{sessionId} to receive agent replies.
        registry.enableSimpleBroker("/topic");

        // Prefix for messages handled by @MessageMapping controllers.
        // Clients send to /app/chat/{sessionId}.
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    // Registers the WebSocket/STOMP endpoint clients use to connect.
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // Primary STOMP-over-WebSocket endpoint.
        registry.addEndpoint("/ws/negotiation")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}