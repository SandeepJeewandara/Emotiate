package com.project.Emotiate.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAgentReplyDto {

    private String sessionId;
    private String agentReply;
    private String detectedEmotion;
    private Double offeredPrice;
    private String metadata;
    private Boolean bookingComplete; // signals BOOKING_CARD message type
}