package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.llm.LLMMessage;
import com.project.Emotiate.dto.llm.LLMRequest;
import com.project.Emotiate.dto.llm.LLMResponse;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Slf4j
@Service
public class LLMServiceImpl implements LLMService {

    @Value("${llm.groq.api-key}")
    private String apiKey;

    @Value("${llm.groq.model:llama3-70b-8192}")
    private String model;

    private final WebClient webClient;


    public LLMServiceImpl(@Value("${llm.groq.url}") String apiUrl) {

         // Configure WebClient with base URL and limit response size to 2MB
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs() .maxInMemorySize(2 * 1024 * 1024))
                .build();
    }


    // Method to call the LLM API with system and user prompts
    public String complete (String systemPrompt, String userMessage) {

        // Build the LLM request payload
        LLMRequest request = LLMRequest.builder()
                .model(model)
                .temperature(0.3)
                .messages(List.of(
                                new LLMMessage("system", systemPrompt),
                                new LLMMessage("user", userMessage)
                )).build();

        try{

            // Call the LLM API and parse the response
            LLMResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LLMResponse.class)
                    .block();

            // Validate response structure and extract content
            if (response == null ||response.getChoices().isEmpty()) {
                log.error("LLM response missing content: {}", response);
                return "";
            }

            // Extract and return the response content
            String content = response.getChoices().getFirst().getMessage().getContent();


            log.info("LLM response received: {}", content);
            return content;

        } catch (WebClientResponseException e) {
            log.error("LLM API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException("LLM API error: " + e.getStatusCode(), e.getStatusCode().value());

        } catch (Exception e) {
            log.error("Unexpected error calling LLM API", e);
            throw new CustomException("Failed to call LLM API: " + e.getMessage(), 500);
        }
    }
}
