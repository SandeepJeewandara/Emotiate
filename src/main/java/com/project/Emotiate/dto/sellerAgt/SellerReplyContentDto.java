package com.project.Emotiate.dto.sellerAgt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SellerReplyContentDto {

    private String agentReply;
    private String detectedEmotion;
    private String strategy;
    private Double offeredPrice;
    private Long recommendedRoomId;
    private String metadata;
    private Boolean bookingComplete;
}