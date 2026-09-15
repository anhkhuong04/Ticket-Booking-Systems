package com.lak.moviebooking.cinema.application;

import java.util.UUID;

public record CinemaDetail(UUID id, String name, String address, String city, String timezone, String status) {
}
