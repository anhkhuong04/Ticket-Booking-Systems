package com.lak.moviebooking.operations.application;
import java.time.Instant; import java.util.UUID;
public record AdminPaymentView(UUID id, String bookingCode, UUID cinemaId, String cinemaName, String provider, String transactionReference, long amount, String status, Instant createdAt, Instant paidAt) { }
