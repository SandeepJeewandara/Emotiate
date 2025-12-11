package com.project.Emotiate.dto.resourceAgt;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityRequestDto {

    private Long    packageId;
    private String  checkInDate;
    private String  checkOutDate;
    private Integer guestCount;
}