package com.project.Emotiate.dto.resourceAgt;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestDto {

    private Long       packageId;
    private Long       guestId;
    private String     checkInDate;
    private String     checkOutDate;
    private BigDecimal offeredPrice;
    private Integer    guestCount;
    private String     sessionId;
}