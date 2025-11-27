package com.project.Emotiate.service;

public interface LLMService {

    // Method to call the LLM API with system and user prompts
    String complete (String systemPrompt, String userMessage);
}
