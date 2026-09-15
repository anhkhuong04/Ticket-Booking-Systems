package com.lak.moviebooking.showtime.application;
import java.time.LocalTime;
public record PriceRuleCommand(String dayType, LocalTime timeFrom, LocalTime timeTo, String screenFormat, String seatType, long amount, int priority) { }
