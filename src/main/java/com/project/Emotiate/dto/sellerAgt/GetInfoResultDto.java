package com.project.Emotiate.dto.sellerAgt;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetInfoResultDto {

    private String reply;

    // Booking Info
    private String checkInDate;
    private String checkOutDate;
    private Integer guestCount;
    private Integer nights;

    private boolean allCollected;
}