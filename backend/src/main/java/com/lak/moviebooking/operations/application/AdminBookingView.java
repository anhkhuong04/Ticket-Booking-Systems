package com.lak.moviebooking.operations.application;
import java.time.Instant; import java.util.UUID;
public record AdminBookingView(UUID id, String bookingCode, String customerName, String customerEmail, UUID cinemaId, String cinemaName, String movieTitle, Instant startAt, long totalAmount, String status, Instant createdAt) { }
