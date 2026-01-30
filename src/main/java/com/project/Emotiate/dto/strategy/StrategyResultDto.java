package com.project.Emotiate.dto.strategy;

import com.project.Emotiate.enums.EmotionTone;
import com.project.Emotiate.enums.NegotiationStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyResultDto {

    private NegotiationStrategy strategy;
    private double eus;
    private double dus;
    private double rdf;
    private double discountRate;
    private double newPrice;
    private double lowerBound;
    private double upperBound;
    private boolean boundaryHit;
    private EmotionTone tone;
    private String reasoning;
}