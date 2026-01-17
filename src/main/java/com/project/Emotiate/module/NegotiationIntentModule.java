package com.project.Emotiate.module;

import com.project.Emotiate.dto.intent.IntentResultDto;
import com.project.Emotiate.service.LLMService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class NegotiationIntentModule {

    @Value("classpath:prompts/negotiation-intent.txt")
    private Resource promptResource;

    private final LLMService llmService;
    private final ObjectMapper objectMapper;
    private String systemPrompt;

    @PostConstruct
    public void init() throws IOException {
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        log.info("Negotiation intent prompt loaded successfully");
    }


    // Classify the guest message as INFORMATIONAL or BARGAINING using the LLM
    public IntentResultDto detectIntent(String userMessage) {
        try {
            log.debug("Detecting negotiation intent for message: {}", userMessage);

            // Call the LLM service with the system prompt and user message
            String response = llmService.complete(
                    systemPrompt,
                    "Guest message: \"" + userMessage + "\""
            );

            log.debug("LLM raw response: {}", response);
            return parseResponse(response);

        } catch (Exception e) {
            log.error("Intent detection failed for message: {}", userMessage, e);
            // Return INFORMATIONAL as the safe default on any error
            return fallbackResult();
        }
    }


    // Parse the LLM JSON response into IntentResultDto
    private IntentResultDto parseResponse(String response) {

        // Strip Markdown code blocks if LLM wraps in ```json ... ```
        String cleaned = response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        // Parse into a JsonNode for safe field extraction
        var node = objectMapper.readTree(cleaned);

        String intent = node.get("intent").asString("INFORMATIONAL").toUpperCase();
        double confidence = node.get("confidence").asDouble(0.5);

        return IntentResultDto.builder()
                .intent(intent)
                .confidence(confidence)
                .build();
    }


    // Return a safe INFORMATIONAL fallback when parsing fails
    private IntentResultDto fallbackResult() {
        return IntentResultDto.builder()
                .intent("INFORMATIONAL")
                .confidence(0.5)
                .build();
    }
}
