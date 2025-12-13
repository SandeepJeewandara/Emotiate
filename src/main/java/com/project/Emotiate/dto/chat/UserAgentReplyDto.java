package com.project.Emotiate.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAgentReplyDto {

    private String sessionId;
    private String agentReply;
    private String detectedEmotion;
    private Double offeredPrice;
    private String metadata;
}