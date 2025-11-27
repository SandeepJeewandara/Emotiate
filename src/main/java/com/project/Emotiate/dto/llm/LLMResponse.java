package com.project.Emotiate.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LLMResponse {

    private String id;
    private List<Choice> choices;

    @Data
    public static class Choice {
        private LLMMessage  message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }
}
