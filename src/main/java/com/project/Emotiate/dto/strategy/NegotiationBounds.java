package com.project.Emotiate.dto.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NegotiationBounds {

    private double upperBound;
    private double basePrice;
    private double lowerBound;

    // Method to validate that the price bounds
    public void validate() {
        if (lowerBound >= basePrice) {
            throw new IllegalArgumentException(
                "lowerBound must be less than basePrice: lowerBound=" + lowerBound + ", basePrice=" + basePrice);
        }
        if (basePrice > upperBound) {
            throw new IllegalArgumentException(
                "basePrice must not exceed upperBound: basePrice=" + basePrice + ", upperBound=" + upperBound);
        }
    }

    // Method to get the max discount rate based on the bounds
    public double maxDiscountRate() {
        return (basePrice - lowerBound) / basePrice;
    }

    // Method to clamp a price within the bounds
    public double clamp(double price) {
        return Math.max(lowerBound, Math.min(upperBound, price));
    }
}