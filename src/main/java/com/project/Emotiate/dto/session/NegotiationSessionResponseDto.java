package com.project.Emotiate.dto.session;

import com.project.Emotiate.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NegotiationSessionResponseDto {

    private Long id;
    private String sessionId;
    private String guestName;
    private SessionStatus status;
    private Integer currentRound;
    private Double offeredPrice;
    private Long recommendedPackageId;
    private String bookingReference;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime endedAt;
}