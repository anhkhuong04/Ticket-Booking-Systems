package com.lak.moviebooking.showtime.application;
import java.time.LocalTime;
import java.util.UUID;
public record PriceRuleView(UUID id, UUID profileId, String dayType, LocalTime timeFrom, LocalTime timeTo, String screenFormat, String seatType, long amount, int priority) { }
