package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.room.RoomAddRequestDto;
import com.project.Emotiate.dto.room.RoomResponseDto;
import com.project.Emotiate.dto.room.RoomUpdateRequestDto;
import com.project.Emotiate.entity.Room;
import com.project.Emotiate.enums.RoomType;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.repository.RoomRepository;
import com.project.Emotiate.service.RoomService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@AllArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    // Retrieve rooms by condition
    public List<RoomResponseDto> getRooms(Boolean isActive, String roomType, Integer floor) {

        // Resolve roomType enum once
        RoomType type = roomType != null
                ? RoomType.valueOf(roomType.toUpperCase(Locale.ROOT))
                : null;

        // Build the filtered query based on params
        List<Room> rooms;

        if (isActive != null && type != null && floor != null) {
            rooms = roomRepository.findByIsActiveAndRoomTypeAndFloor(isActive, type, floor);

        } else if (isActive != null && type != null) {
            rooms = roomRepository.findByIsActiveAndRoomType(isActive, type);

        } else if (isActive != null && floor != null) {
            rooms = roomRepository.findByIsActiveAndFloor(isActive, floor);

        } else if (type != null && floor != null) {
            rooms = roomRepository.findByRoomTypeAndFloor(type, floor);

        } else if (isActive != null) {
            rooms = roomRepository.findByIsActive(isActive);

        } else if (type != null) {
            rooms = roomRepository.findByRoomType(type);

        } else if (floor != null) {
            rooms = roomRepository.findByFloor(floor);

        } else {
            rooms = roomRepository.findAll();
        }
        return rooms.stream()
                .map(this::toResponseDto)
                .toList();
    }


    @Override
    // Add a new room
    public RoomResponseDto addRoom(RoomAddRequestDto request) {

        // Prevent duplicate room numbers
        if (roomRepository.existsByRoomNumberAndIsActiveTrue(request.getRoomNumber())) {
            throw new CustomException(
                    "Room number " + request.getRoomNumber() + " already exists",
                    HttpStatus.BAD_REQUEST.value());
        }

        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .floor(request.getFloor())
                .roomType(RoomType.valueOf(request.getRoomType().toUpperCase(Locale.ROOT)))
                .description(request.getDescription())
                .maxOccupancy(request.getMaxOccupancy() != null ? request.getMaxOccupancy() : 2)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        roomRepository.save(room);
        log.info("Room added — number={} type={}", room.getRoomNumber(), room.getRoomType());

        return toResponseDto(room);
    }


    @Override
    // Edit an existing room
    public RoomResponseDto editRoom(Long id, RoomUpdateRequestDto request) {

        // Fetch the room
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Room with Id " + id + " not found",
                        HttpStatus.NOT_FOUND.value()));

        // If room number is being changed, check it is not already taken
        if (request.getRoomNumber() != null
                && !request.getRoomNumber().equals(room.getRoomNumber())
                && roomRepository.existsByRoomNumberAndIsActiveTrue(request.getRoomNumber())) {
            throw new CustomException(
                    "Room number " + request.getRoomNumber() + " is already in use",
                    HttpStatus.BAD_REQUEST.value());
        }

        // Apply only the fields that were provided
        if (request.getRoomNumber()  != null) room.setRoomNumber(request.getRoomNumber());
        if (request.getFloor()       != null) room.setFloor(request.getFloor());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getMaxOccupancy()!= null) room.setMaxOccupancy(request.getMaxOccupancy());
        if (request.getIsActive()    != null) room.setIsActive(request.getIsActive());

        if (request.getRoomType() != null) {
            room.setRoomType(RoomType.valueOf(request.getRoomType().toUpperCase(Locale.ROOT)));
        }

        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
        log.info("Room updated — id={}", id);

        return toResponseDto(room);
    }


    @Override
    // Soft delete a room
    public Void removeRoom(Long id) {

        // Fetch the room
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Room with Id " + id + " not found",
                        HttpStatus.NOT_FOUND.value()));


        if (!room.getIsActive()) {
            throw new CustomException(
                    "Room with Id " + id + " is already inactive",
                    HttpStatus.BAD_REQUEST.value());
        }

        room.setIsActive(false);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
        log.info("Room soft-deleted — id={}", id);

        return null;
    }


    // Mapper to map Room entities to RoomResponseDto
    private RoomResponseDto toResponseDto(Room room) {
        return RoomResponseDto.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .roomType(room.getRoomType().name())
                .description(room.getDescription())
                .maxOccupancy(room.getMaxOccupancy())
                .isActive(room.getIsActive())
                .build();
    }
}