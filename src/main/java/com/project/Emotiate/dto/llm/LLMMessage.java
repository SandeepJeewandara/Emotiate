package com.project.Emotiate.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LLMMessage {

    private String role;
    private String content;
}
