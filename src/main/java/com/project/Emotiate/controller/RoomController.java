package com.project.Emotiate.controller;

import com.project.Emotiate.dto.room.RoomAddRequestDto;
import com.project.Emotiate.dto.room.RoomResponseDto;
import com.project.Emotiate.dto.room.RoomUpdateRequestDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.service.RoomService;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/get")
    public ResponseEntity<Response<List<RoomResponseDto>>> getRooms(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String  roomType,
            @RequestParam(required = false) Integer floor) {

        log.info("Retrieving rooms  isActive={} roomType={} floor={}", isActive, roomType, floor);
        return ResponseUtil.success(roomService.getRooms(isActive, roomType, floor), "Rooms retrieved successfully");
    }


    @PostMapping("/add")
    public ResponseEntity<Response<RoomResponseDto>> addRoom(
            @RequestBody RoomAddRequestDto request) {

        log.info("Adding room  number={}", request.getRoomNumber());
        return ResponseUtil.created(roomService.addRoom(request), "Room added successfully");
    }


    @PutMapping("/edit/{id}")
    public ResponseEntity<Response<RoomResponseDto>> editRoom(
            @PathVariable Long id,
            @RequestBody RoomUpdateRequestDto request) {

        log.info("Editing room  id={}", id);
        return ResponseUtil.success(roomService.editRoom(id, request), "Room updated successfully");
    }


    @DeleteMapping("/remove/{id}")
    public ResponseEntity<Response<Void>> removeRoom(@PathVariable Long id) {

        log.info("Removing room  id={}", id);
        return ResponseUtil.success(roomService.removeRoom(id), "Room removed successfully");
    }
}