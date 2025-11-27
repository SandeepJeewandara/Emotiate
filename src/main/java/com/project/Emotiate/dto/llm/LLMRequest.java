package com.project.Emotiate.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LLMRequest {

    private String model;
    private double temperature;
    private List<LLMMessage> messages;
}
