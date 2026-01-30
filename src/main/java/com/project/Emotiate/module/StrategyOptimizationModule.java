package com.project.Emotiate.module;

import com.project.Emotiate.dto.emotion.EmotionResultDto;
import com.project.Emotiate.dto.strategy.NegotiationBounds;
import com.project.Emotiate.dto.strategy.StrategyResultDto;
import com.project.Emotiate.enums.EmotionState;
import com.project.Emotiate.enums.EmotionTone;
import com.project.Emotiate.enums.NegotiationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StrategyOptimizationModule {

    // VAD values from NRC VAD Lexicon
    private static final Map<EmotionState, double[]> VAD_MAP = Map.of(
        EmotionState.FRUSTRATED, new double[]{0.080, 0.651, 0.255},
        EmotionState.HESITANT,   new double[]{0.333, 0.542, 0.321},
        EmotionState.NEUTRAL,    new double[]{0.469, 0.184, 0.357},
        EmotionState.INTERESTED, new double[]{0.750, 0.529, 0.725},
        EmotionState.SATISFIED,  new double[]{0.959, 0.510, 0.685},
        EmotionState.EXCITED,    new double[]{0.908, 0.931, 0.709}
    );

    // Map emotion states to communication tone
    private static final Map<EmotionState, EmotionTone> TONE_MAP = Map.of(
        EmotionState.FRUSTRATED, EmotionTone.EMPATHETIC,
        EmotionState.HESITANT,   EmotionTone.REASSURING,
        EmotionState.NEUTRAL,    EmotionTone.PROFESSIONAL,
        EmotionState.INTERESTED, EmotionTone.ENTHUSIASTIC,
        EmotionState.SATISFIED,  EmotionTone.CONFIRMATORY,
        EmotionState.EXCITED,    EmotionTone.CELEBRATORY
    );

    // Stores thresholds for strategy selection
    private final double[] derivedThresholds;

    public StrategyOptimizationModule() {

        // Compute thresholds values during initialization
        this.derivedThresholds = computeThresholds();
        log.info("Derived DUS thresholds — t1={}, t2={}, t3={}, t4={}",
            derivedThresholds[0], derivedThresholds[1], derivedThresholds[2], derivedThresholds[3]);
    }

    public StrategyResultDto optimize(EmotionResultDto emotion, int negotiationRound,
                                      double remainingBudget, NegotiationBounds bounds) {

        // Validate price bounds
        bounds.validate();

        // Get detected emotion from EDM
        EmotionState emotionState = emotion.getEmotion();

        // Compute Emotional Utility Score (EUS)
        double eus = computeEUS(emotionState);
        log.trace("EUS [emotion={}]: {}", emotionState, eus);

        // Compute Round Decay Factor  (RDF)
        double rdf = computeRDF(negotiationRound);
        log.trace("RDF [negotiationRound={}]: {}", negotiationRound, rdf);

        // Compute Budget Factor (BF)
        double maxDiscount = bounds.maxDiscountRate();
        double bf = maxDiscount > 0.0 ? Math.min(1.0, Math.max(0.0, remainingBudget / maxDiscount)) : 0.0;
        log.trace("BF [remainingBudget={}, maxDiscount={}]: {}", remainingBudget, maxDiscount, bf);

        // Compute Dynamic Utility Score  (DUS)
        double dus = computeDUS(eus, rdf, bf);
        log.trace("DUS: {}", dus);

        // Strategy selection via derived thresholds
        NegotiationStrategy strategy = selectStrategy(dus);
        log.trace("Strategy selected: {}", strategy);

        // Discount calculation
        double discount = computeDiscount(strategy, dus, rdf, remainingBudget, maxDiscount);
        log.trace("Discount rate (pre-ZOPA): {}", discount);

        // ZOPA boundary enforcement
        double candidatePrice = bounds.getBasePrice() * (1.0 - discount);
        double offeredPrice = bounds.clamp(candidatePrice);

        // Check if price was adjusted due to limits
        boolean boundaryHit = Math.abs(offeredPrice - candidatePrice) > 1e-9;

        // Calculate actual discount after clamping
        double actualDiscount = (bounds.getBasePrice() - offeredPrice) / bounds.getBasePrice();

        // Check if final offer make boundary hit
        if (boundaryHit) {
            log.warn("ZOPA boundary hit: candidatePrice={} clamped to offeredPrice={}",
                candidatePrice, offeredPrice);
        }
        log.trace("OfferedPrice: {}, actualDiscountRate: {}", offeredPrice, actualDiscount);

        // Get tone based on emotion
        EmotionTone tone = TONE_MAP.get(emotionState);
        log.info("Tone: {}", tone);

        // Return comprehensive strategy result
        return StrategyResultDto.builder()
            .strategy(strategy)
            .eus(eus)
            .dus(dus)
            .rdf(rdf)
            .discountRate(actualDiscount)
            .newPrice(offeredPrice)
            .lowerBound(bounds.getLowerBound())
            .upperBound(bounds.getUpperBound())
            .boundaryHit(boundaryHit)
            .tone(tone)
            .reasoning(emotion.getReasoning())
            .build();
    }


    // Function to compute Emotional Utility Score (EUS) based on VAD values
    private double computeEUS(EmotionState state) {

        double[] vad = VAD_MAP.get(state);
        double v = vad[0], a = vad[1], d = vad[2];

        // Formula: ((1 - V) + A + (1 - D)) / 3
        return ((1.0 - v) + a + (1.0 - d)) / 3.0;
    }


    // Function to compute Round Decay Factor (RDF)
    private double computeRDF(int negotiationRound) {

        if (negotiationRound <= 0) return 1.0;
        return 1.0 - Math.pow(negotiationRound / 10.0, 0.7);
    }


    // Function to compute Dynamic Utility Score (DUS) by combining EUS, RDF, BF
    private double computeDUS(double eus, double rdf, double bf) {
        double dus = 0.50 * eus + 0.30 * (1.0 - rdf) + 0.20 * (1.0 - bf);

        return Math.min(1.0, Math.max(0.0, dus));
    }


    // Function to select negotiation strategy based on DUS and derived thresholds
    private NegotiationStrategy selectStrategy(double dus) {
        double t1 = derivedThresholds[0];
        double t2 = derivedThresholds[1];
        double t3 = derivedThresholds[2];
        double t4 = derivedThresholds[3];

        if (dus < t1) return NegotiationStrategy.HOLD_FIRM;
        if (dus < t2) return NegotiationStrategy.FEATURE_FOCUS;
        if (dus < t3) return NegotiationStrategy.GENTLE_CONCEDE;
        if (dus < t4) return NegotiationStrategy.AGGRESSIVE_CONCEDE;
        return NegotiationStrategy.CLOSE_DEAL;
    }


    // Function to compute discount rate based on selected strategy, DUS, RDF, and remaining budget
    private double computeDiscount(NegotiationStrategy strategy, double dus, double rdf, double remainingBudget, double maxDiscount) {
        double discount = switch (strategy) {
            case HOLD_FIRM          -> 0.0;
            case FEATURE_FOCUS      -> dus * maxDiscount * 0.20;
            case GENTLE_CONCEDE     -> dus * maxDiscount * 0.50;
            case AGGRESSIVE_CONCEDE -> dus * maxDiscount * 0.80;
            case CLOSE_DEAL         -> Math.min(remainingBudget, dus * maxDiscount);
        };

        discount *= (1.0 - rdf * 0.4);
        return Math.min(discount, remainingBudget);
    }


    // Function to compute DUS thresholds by simulating a wide range of scenarios and taking percentiles
    private double[] computeThresholds() {

        double[] bfLevels = {0.0, 0.25, 0.50, 0.75, 1.0};
        List<Double> samples = new ArrayList<>(300);

        // Generate DUS values for all combinations
        for (EmotionState state : EmotionState.values()) {
            double eus = computeEUS(state);

            for (int round = 1; round <= 10; round++) {
                double rdf = computeRDF(round);

                for (double bf : bfLevels) {
                    double dus = 0.50 * eus + 0.30 * (1.0 - rdf) + 0.20 * (1.0 - bf);
                    dus = Math.min(1.0, Math.max(0.0, dus));
                    samples.add(dus);
                }
            }
        }

        // Sort values to compute percentiles
        Collections.sort(samples);


        // Return threshold values
        return new double[]{
            percentile(samples, 20),
            percentile(samples, 40),
            percentile(samples, 60),
            percentile(samples, 80)
        };
    }


    // Helper method to set percentiles
    private double percentile(List<Double> sorted, double p) {
        double index = (p / 100.0) * (sorted.size() - 1);
        int lo = (int) Math.floor(index);
        int hi = (int) Math.ceil(index);
        if (lo == hi) return sorted.get(lo);
        double fraction = index - lo;
        return sorted.get(lo) * (1.0 - fraction) + sorted.get(hi) * fraction;
    }
}