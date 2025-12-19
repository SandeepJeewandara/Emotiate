package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;
import com.project.Emotiate.entity.Booking;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.repository.BookingRepository;
import com.project.Emotiate.service.BookingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    // Method to retrieve all bookings ordered by most recent first
    public List<BookingResponseDto> getBookings() {

        return bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    // Method to delete a booking by database ID
    public Void deleteBooking(Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Booking with Id " + id + " not found",
                        HttpStatus.NOT_FOUND.value()));

        bookingRepository.delete(booking);
        log.info("Booking deleted: id={} reference={}", id, booking.getBookingReference());

        return null;
    }


    // Map booking entity fields to  response DTO
    private BookingResponseDto toResponseDto(Booking booking) {
        return BookingResponseDto.builder()
                .id(booking.getId())
                .reference(booking.getBookingReference())
                .guestId(booking.getGuest() != null ? booking.getGuest().getId() : null)
                .packageId(booking.getHotelPackage() != null ? booking.getHotelPackage().getId() : null)
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : null)
                .status(booking.getStatus().name())
                .offeredPricePerNight(booking.getOfferedPrice())
                .totalPrice(booking.getTotalPrice())
                .checkInDate(booking.getCheckInDate() != null ? booking.getCheckInDate().toString() : null)
                .checkOutDate(booking.getCheckOutDate() != null ? booking.getCheckOutDate().toString() : null)
                .totalNights(booking.getTotalNights() != null ? booking.getTotalNights() : 0)
                .guestCount(booking.getGuestCount())
                .sessionId(booking.getSessionId())
                .packageName(booking.getHotelPackage() != null ? booking.getHotelPackage().getName() : null)
                .createdAt(booking.getCreatedAt())
                .message("Booking retrieved successfully")
                .build();
    }
}