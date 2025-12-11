package com.project.Emotiate.dto.resourceAgt;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponseDto {

    private String     reference;
    private String     status;
    private BigDecimal offeredPricePerNight;
    private BigDecimal totalPrice;
    private String     checkInDate;
    private String     checkOutDate;
    private int        totalNights;
    private String     packageName;
    private String     message;
}