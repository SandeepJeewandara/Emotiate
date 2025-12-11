package com.project.Emotiate.repository;

import com.project.Emotiate.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find a booking by its unique booking reference
    Optional<Booking> findByBookingReference(String bookingReference);


    // Find all bookings linked to a negotiation session
    List<Booking> findBySessionId(String sessionId);


    // Count confirmed bookings that overlap a given date range for a room
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.room.id = :roomId
          AND b.status = 'CONFIRMED'
          AND b.checkInDate  < :checkOutDate
          AND b.checkOutDate > :checkInDate
    """)
    long countOverlappingBookings(
            @Param("roomId")       Long      roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
}