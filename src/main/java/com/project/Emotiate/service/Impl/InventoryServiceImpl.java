package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.resourceAgt.AvailabilityRequestDto;
import com.project.Emotiate.dto.resourceAgt.AvailabilityResponseDto;
import com.project.Emotiate.dto.resourceAgt.BookingRequestDto;
import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;
import com.project.Emotiate.entity.Booking;
import com.project.Emotiate.entity.Guest;
import com.project.Emotiate.entity.HotelPackage;
import com.project.Emotiate.enums.BookingStatus;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.repository.BookingRepository;
import com.project.Emotiate.repository.GuestRepository;
import com.project.Emotiate.repository.HotelPackageRepository;
import com.project.Emotiate.repository.RoomRepository;
import com.project.Emotiate.service.InventoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final HotelPackageRepository packageRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;

    @Override
    @Transactional(readOnly = true)
    // Method to check room availability based on requested criteria
    public AvailabilityResponseDto checkAvailability(AvailabilityRequestDto request) {

        // Get the relevant package info
        HotelPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new CustomException(
                        "Package " + request.getPackageId() + " not found",
                        HttpStatus.NOT_FOUND.value()));

        // Validate package availability
        if (!pkg.getIsActive()) {
            return AvailabilityResponseDto.builder()
                    .available(false)
                    .message("Package is not currently active")
                    .build();
        }

        // Get the total days
        LocalDate checkIn  = LocalDate.parse(request.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(request.getCheckOutDate());
        int totalNights    = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        if (totalNights <= 0) {
            return AvailabilityResponseDto.builder()
                    .available(false)
                    .message("Check-out must be after check-in")
                    .build();
        }

        // Validate guest count against package capacity
        if (request.getGuestCount() != null
                && request.getGuestCount() > pkg.getMaxOccupancy()) {
            return AvailabilityResponseDto.builder()
                    .available(false)
                    .message("Guest count exceeds package capacity of " + pkg.getMaxOccupancy())
                    .build();
        }

        // Check for overlapping bookings for the selected room
        long conflicts = bookingRepository.countOverlappingBookings(
                pkg.getRoom().getId(), checkIn, checkOut);

        log.info("Availability check  package={} dates={}/{} conflicts={}",
                pkg.getId(), checkIn, checkOut, conflicts);

        // Build the availability response
        return AvailabilityResponseDto.builder()
                .available(conflicts == 0)
                .packageId(pkg.getId())
                .packageName(pkg.getName())
                .lowerBoundPrice(pkg.getLowerBoundPrice())
                .upperBoundPrice(pkg.getUpperBoundPrice())
                .addOns(pkg.getAddOns().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .totalNights(totalNights)
                .checkInDate(checkIn.toString())
                .checkOutDate(checkOut.toString())
                .imageUrl(pkg.getImageUrl())
                .message(conflicts == 0
                        ? "Room is available"
                        : "Room is already booked for these dates")
                .build();
    }


    @Override
    @Transactional
    // Method to confirm a booking based on the finalized request
    public BookingResponseDto confirmBooking(BookingRequestDto request) {

        // Get the relevant package info
        HotelPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new CustomException(
                        "Package " + request.getPackageId() + " not found",
                        HttpStatus.NOT_FOUND.value()));

        // Validate negotiated price is above the floor
        if (request.getOfferedPrice().compareTo(pkg.getLowerBoundPrice()) < 0) {
            log.warn("Offered price {} is below floor {} for package {}",
                    request.getOfferedPrice(), pkg.getLowerBoundPrice(), pkg.getId());
            return BookingResponseDto.builder()
                    .status("BELOW_FLOOR")
                    .message("Offered price is below minimum acceptable price of "
                            + pkg.getLowerBoundPrice())
                    .build();
        }

        // Get the requested stay dates and duration
        LocalDate checkIn  = LocalDate.parse(request.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(request.getCheckOutDate());
        int totalNights    = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        // Lock the room record before final availability check
        roomRepository.findByIdForUpdate(pkg.getRoom().getId())
                .orElseThrow(() -> new CustomException("Room not found", HttpStatus.NOT_FOUND.value()));


        // Recheck for overlapping bookings after acquiring the lock
        long conflicts = bookingRepository.countOverlappingBookings(
                pkg.getRoom().getId(), checkIn, checkOut);

        if (conflicts > 0) {
            log.warn("Room {} already booked for {}/{}", pkg.getRoom().getId(), checkIn, checkOut);
            return BookingResponseDto.builder()
                    .status("SOLD_OUT")
                    .message("Room is no longer available for these dates")
                    .build();
        }

        // Get the guest by session ID
        Guest guest = null;
        if (request.getSessionId() != null) {
            guest = guestRepository.findBySessionId(request.getSessionId()).orElse(null);
        }

        // Generate a booking reference for the new reservation
        String reference  = "EMT-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        // Calculate the total price for the full stay, rounded to the nearest 100
        BigDecimal totalPrice = request.getOfferedPrice()
                .multiply(BigDecimal.valueOf(totalNights))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Build the booking entity to be persisted
        Booking booking = Booking.builder()
                .bookingReference(reference)
                .guest(guest)
                .hotelPackage(pkg)
                .room(pkg.getRoom())
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .totalNights(totalNights)
                .offeredPrice(request.getOfferedPrice())
                .totalPrice(totalPrice)
                .guestCount(request.getGuestCount())
                .sessionId(request.getSessionId())
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save the booking and log the confirmation
        bookingRepository.save(booking);
        log.info("Booking confirmed  reference={} room={} dates={}/{}",
                reference, pkg.getRoom().getRoomNumber(), checkIn, checkOut);

        return BookingResponseDto.builder()
                .reference(reference)
                .status("CONFIRMED")
                .offeredPricePerNight(request.getOfferedPrice())
                .totalPrice(totalPrice)
                .checkInDate(checkIn.toString())
                .checkOutDate(checkOut.toString())
                .totalNights(totalNights)
                .packageName(pkg.getName())
                .message("Booking confirmed successfully")
                .build();
    }

}