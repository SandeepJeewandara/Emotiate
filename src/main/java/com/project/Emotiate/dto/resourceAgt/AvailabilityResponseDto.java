package com.project.Emotiate.dto.resourceAgt;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityResponseDto {

    private boolean      available;
    private Long         packageId;
    private String       packageName;
    private BigDecimal   lowerBoundPrice;
    private BigDecimal   upperBoundPrice;
    private List<String> addOns;
    private int          totalNights;
    private String       checkInDate;
    private String  checkOutDate;
    private String  imageUrl;
    private String       message;
}