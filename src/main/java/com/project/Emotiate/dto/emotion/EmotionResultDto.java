package com.project.Emotiate.dto.emotion;

import com.project.Emotiate.enums.EmotionState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class EmotionResultDto {

    private EmotionState emotion;
    private double confidence;
    private String reasoning;
}