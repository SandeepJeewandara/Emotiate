package com.project.Emotiate.dto.room;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomAddRequestDto {

    private String  roomNumber;
    private Integer floor;
    private String  roomType;
    private String  description;
    private Integer maxOccupancy;
}