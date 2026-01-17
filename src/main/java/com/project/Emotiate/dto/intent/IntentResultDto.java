package com.project.Emotiate.dto.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntentResultDto {

    private String intent;
    private double confidence;

    // Returns true when the guest is negotiating price or terms
    public boolean isBargaining() {
        return "BARGAINING".equals(intent);
    }
}