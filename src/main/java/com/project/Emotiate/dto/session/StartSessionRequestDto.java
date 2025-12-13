package com.project.Emotiate.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StartSessionRequestDto {

    private String guestName;
    private String initialMessage;
}