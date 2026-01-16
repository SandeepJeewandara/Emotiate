package com.project.Emotiate.service;


import com.project.Emotiate.dto.resourceAgt.AvailabilityRequestDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityResponseDto;
import com.project.Emotiate.dto.resourceAgt.BookingRequestDto;
import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;

public interface InventoryService {

    // Method to check room availability based on requested criteria
    AvailabilityResponseDto checkAvailability(AvailabilityRequestDto request);

    // Method to confirm a booking based on the finalized request
    BookingResponseDto confirmBooking(BookingRequestDto request);
}