package com.project.Emotiate.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AgentMessageDto {

    private String sessionId;
    private String guestMessage;
    private String guestName;
    private List<String> conversationHistory;
    private Integer round;
}