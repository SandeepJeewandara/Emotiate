package com.project.Emotiate.service;

import com.project.Emotiate.dto.room.RoomAddRequestDto;
import com.project.Emotiate.dto.room.RoomResponseDto;
import com.project.Emotiate.dto.room.RoomUpdateRequestDto;

import java.util.List;

public interface RoomService {

    // Retrieve rooms
    List<RoomResponseDto> getRooms(Boolean isActive, String roomType, Integer floor);

    // Add a new room
    RoomResponseDto addRoom(RoomAddRequestDto request);

    // Edit an existing room
    RoomResponseDto editRoom(Long id, RoomUpdateRequestDto request);

    // Soft delete a room
    Void removeRoom(Long id);
}