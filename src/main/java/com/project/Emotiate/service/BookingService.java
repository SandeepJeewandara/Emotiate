package com.project.Emotiate.service;

import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;

import java.util.List;

public interface BookingService {

    // Method to retrieve all bookings for booking management
    List<BookingResponseDto> getBookings();


    // Method to delete a booking by database ID
    Void deleteBooking(Long id);
}