package com.project.Emotiate.dto.chat;

import com.project.Emotiate.enums.MessageType;
import com.project.Emotiate.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponseDto {

    private Long id;
    private String sessionId;
    private SenderType senderType;
    private MessageType messageType;
    private String content;
    private String detectedEmotion;
    private Double offeredPrice;
    private String metadata;
    private Long responseTimeMs;
    private LocalDateTime timestamp;
}