package com.project.Emotiate.dto.sellerAgt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NegotiationReplyResultDto {

    private String selectedPackage;
    private String reply;
    private boolean bookingRequested;
    private boolean abortRequested;
}
