package com.project.Emotiate.dto.sellerAgt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuestDetailsResultDto {

    private String  guestName;
    private String  email;
    private String  contactNumber;
    private boolean allCollected;
    private String  reply;
}