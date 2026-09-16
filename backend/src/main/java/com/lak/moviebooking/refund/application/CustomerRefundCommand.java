package com.lak.moviebooking.refund.application;

import java.util.UUID;

public record CustomerRefundCommand(UUID userId, UUID bookingId, String idempotencyKey) {
}
