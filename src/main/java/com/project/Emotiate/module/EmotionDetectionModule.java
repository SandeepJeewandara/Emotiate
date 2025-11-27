package com.project.Emotiate.module;

import com.project.Emotiate.dto.emotion.EmotionResultDto;
import com.project.Emotiate.enums.EmotionState;
import com.project.Emotiate.service.LLMService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionDetectionModule {

    @Value("classpath:prompts/emotion-detection.txt")
    private Resource promptResource;

    private final LLMService llmService;
    private final ObjectMapper objectMapper;
    private String systemPrompt;

    @PostConstruct
    public void init() throws IOException {
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        log.info("Emotion detection prompt loaded successfully");
    }


    // Function to detect emotion from user message using LLM
    public EmotionResultDto detectEmotion(String userMessage) {
        try{
            log.debug("Detecting emotion for message: {}", userMessage);

            // Call the LLM service to get the emotion detection result
            String response = llmService.complete(systemPrompt, userMessage);

            log.debug("LLM raw response: {}", response);
            return parseResponse(response);

        } catch (Exception e) {

            log.error("Emotion detection failed for message: {}", userMessage, e);
            // Return a default NEUTRAL emotion result in case of any error
            return fallbackResult();
        }
    }


    // Helper method to parse the LLM response into EmotionResultDto
    private EmotionResultDto parseResponse(String response){

        // Strip Markdown code blocks if LLM wraps in ```json ... ```
        String cleaned = response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        // Parse into a raw map first for safe enum handling
        var node = objectMapper.readTree(cleaned);

        // Convert the raw map to an EmotionResultDto object
        EmotionState emotion = EmotionState.valueOf(
                node.get("emotion").asString("NEUTRAL").toUpperCase()
        );
        double confidence = node.get("confidence").asDouble(0.5);
        String reasoning = node.get("reasoning").asString("");

        return EmotionResultDto.builder()
                .emotion(emotion)
                .confidence(confidence)
                .reasoning(reasoning)
                .build();
    }


    // Helper method to parse the LLM response into EmotionResultDto
    private EmotionResultDto fallbackResult() {
        return EmotionResultDto.builder()
                .emotion(EmotionState.NEUTRAL)
                .confidence(0.0)
                .reasoning("Detection failed — defaulting to NEUTRAL")
                .build();
    }
}
