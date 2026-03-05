package com.project.Emotiate.dto.sellerAgt;

import com.project.Emotiate.dto.resourceAgt.AvailabilityResponseDto;
import com.project.Emotiate.enums.NegotiationPhase;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class SellerNegotiationContext {

    // Session Identity
    private String sessionId;
    private String guestName;

    // Phase Tracking
    private NegotiationPhase phase;

    // Guest Info
    private String checkInDate;
    private String checkOutDate;
    private Integer nights;
    private Integer guestCount;

    // Package Info
    private double basePrice;
    private double lowerBound;
    private double upperBound;
    private double currentOfferedPrice;
    private double remainingBudget;
    private double lastDiscountRate;
    private boolean boundaryHit;

    // Negotiation State
    private List<AvailabilityResponseDto> availablePackages;
    private AvailabilityResponseDto selectedPackage;
    private boolean negotiationStarted;
    private int totalTurns;
    private int negotiationRound;
    private boolean abortEligible;

    // Guest Contact Details
    private String  guestEmail;
    private String  guestContactNumber;
    private boolean guestDetailsCollected;

    // Booking Context
    private boolean bookingComplete;
    private String bookingReference;
    private Long selectedPackageId;

    // Function to initialize a new negotiation context for a session
    public static SellerNegotiationContext newSession(String sessionId) {
        return SellerNegotiationContext.builder()
                .sessionId(sessionId)
                .guestName(null)
                .guestEmail(null)
                .guestContactNumber(null)
                .guestDetailsCollected(false)
                .phase(NegotiationPhase.GET_INFO)
                .totalTurns(0)
                .selectedPackageId(null)
                .negotiationStarted(false)
                .negotiationRound(0)
                .abortEligible(false)
                .bookingComplete(false)
                .bookingReference(null)
                .selectedPackageId(null)
                .checkInDate(null)
                .checkOutDate(null)
                .guestCount(null)
                .basePrice(0.0)
                .lowerBound(0.0)
                .currentOfferedPrice(0.0)
                .remainingBudget(0.0)
                .lastDiscountRate(0.0)
                .boundaryHit(false)
                .build();
    }
}
