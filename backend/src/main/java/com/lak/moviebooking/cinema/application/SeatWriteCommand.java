package com.lak.moviebooking.cinema.application;
public record SeatWriteCommand(String rowLabel, int seatNumber, String seatType, String pairKey, String status) { }
