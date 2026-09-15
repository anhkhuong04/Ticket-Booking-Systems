package com.lak.moviebooking.showtime.application;
import java.util.UUID;
public record ShowtimeSeatView(UUID id, String rowLabel, int seatNumber, String seatType, String pairKey, String status, long price) { }
