package com.lak.moviebooking.operations.application;
import java.time.Instant; import java.util.UUID;
public record AdminRefundView(UUID id, String bookingCode, UUID cinemaId, String cinemaName, long amount, String reason, String status, int attemptCount, Instant requestedAt, Instant refundedAt) { }
