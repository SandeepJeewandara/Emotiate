package com.project.Emotiate.dto.resourceAgt;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponseDto {

    private Long       id;
    private String     reference;
    private Long       guestId;
    private Long       packageId;
    private Long       roomId;
    private String     roomNumber;
    private String     status;
    private BigDecimal offeredPricePerNight;
    private BigDecimal totalPrice;
    private String     checkInDate;
    private String     checkOutDate;
    private int        totalNights;
    private Integer    guestCount;
    private String     sessionId;
    private String     packageName;
    private String     message;
    private LocalDateTime createdAt;
}