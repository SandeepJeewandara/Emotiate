package com.project.Emotiate.repository;

import com.project.Emotiate.entity.Room;
import com.project.Emotiate.enums.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Check whether an active room already exists for the given room number
    boolean existsByRoomNumberAndIsActiveTrue(String roomNumber);


    // Find rooms by active status
    List<Room> findByIsActive(Boolean isActive);


    // Find rooms by room type
    List<Room> findByRoomType(RoomType roomType);


    // Find rooms by active status and room type
    List<Room> findByIsActiveAndRoomType(Boolean isActive, RoomType roomType);


    // Find rooms located on a specific floor
    List<Room> findByFloor(Integer floor);


    // Find rooms by active status on a specific floor
    List<Room> findByIsActiveAndFloor(Boolean isActive, Integer floor);


    // Find rooms by room type on a specific floor
    List<Room> findByRoomTypeAndFloor(RoomType roomType, Integer floor);


    // Find all active rooms by room type
    List<Room> findByRoomTypeAndIsActiveTrue(RoomType roomType);


    // Find an active room by room number
    Optional<Room> findByRoomNumberAndIsActiveTrue(String roomNumber);


    // Find an active room by room number by type and floor
    List<Room> findByIsActiveAndRoomTypeAndFloor(Boolean isActive, RoomType roomType, Integer floor);


    // Lock a room row by ID for update operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}