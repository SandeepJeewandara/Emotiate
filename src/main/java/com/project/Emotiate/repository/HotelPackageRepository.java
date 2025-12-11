package com.project.Emotiate.repository;

import com.project.Emotiate.entity.HotelPackage;
import com.project.Emotiate.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelPackageRepository extends JpaRepository<HotelPackage, Long> {

    // Find all active hotel packages
    List<HotelPackage> findByIsActiveTrue();


    // Find active hotel packages by room type
    @Query("SELECT p FROM HotelPackage p WHERE p.isActive = true AND p.room.roomType = :roomType")
    List<HotelPackage> findActiveByRoomType(@Param("roomType") RoomType roomType);
}