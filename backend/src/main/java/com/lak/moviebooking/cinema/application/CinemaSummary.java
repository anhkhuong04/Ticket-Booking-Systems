package com.lak.moviebooking.cinema.application;

import java.util.UUID;

public record CinemaSummary(UUID id, String name, String address, String city, String timezone) {
}
