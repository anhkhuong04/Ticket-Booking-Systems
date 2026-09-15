package com.lak.moviebooking.cinema.application;
import java.util.UUID;
public record SeatView(UUID id, String rowLabel, int seatNumber, String seatType, String pairKey, String status) { }
