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

    // Find all active rooms by room type
    List<Room> findByRoomTypeAndIsActiveTrue(RoomType roomType);


    // Find an active room by room number
    Optional<Room> findByRoomNumberAndIsActiveTrue(String roomNumber);


    // Lock a room row by ID for update operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}