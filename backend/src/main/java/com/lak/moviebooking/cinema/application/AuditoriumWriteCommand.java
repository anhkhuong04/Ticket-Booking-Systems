package com.lak.moviebooking.cinema.application;
public record AuditoriumWriteCommand(String name, String screenFormat, int cleanupMinutes, String status) { }
