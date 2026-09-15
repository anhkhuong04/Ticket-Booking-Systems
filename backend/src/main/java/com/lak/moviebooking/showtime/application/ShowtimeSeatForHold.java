package com.lak.moviebooking.showtime.application;

import java.util.UUID;

public record ShowtimeSeatForHold(UUID id, String pairKey, String status, UUID currentHoldId) {
}
