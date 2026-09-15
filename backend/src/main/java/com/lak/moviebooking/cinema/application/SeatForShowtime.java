package com.lak.moviebooking.cinema.application;

import java.util.UUID;

public record SeatForShowtime(UUID id, String rowLabel, int seatNumber, String seatType, String pairKey, String status) { }
