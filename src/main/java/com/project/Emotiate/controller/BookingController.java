package com.project.Emotiate.controller;

import com.project.Emotiate.dto.resourceAgt.BookingResponseDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.service.BookingService;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/get")
    public ResponseEntity<Response<List<BookingResponseDto>>> getBookings() {

        log.info("Retrieving all bookings");
        return ResponseUtil.success(bookingService.getBookings(), "Bookings retrieved successfully");
    }


    @DeleteMapping("/remove/{id}")
    public ResponseEntity<Response<Void>> deleteBooking(@PathVariable Long id) {

        log.info("Deleting booking with id={}", id);
        return ResponseUtil.success(bookingService.deleteBooking(id), "Booking deleted successfully");
    }
}
