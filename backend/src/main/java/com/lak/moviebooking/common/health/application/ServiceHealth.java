package com.lak.moviebooking.common.health.application;

public record ServiceHealth(String status) {

	public static ServiceHealth up() {
		return new ServiceHealth("UP");
	}

	public static ServiceHealth down() {
		return new ServiceHealth("DOWN");
	}

	public boolean matchesUpStatus() {
		return "UP".equals(status);
	}
}
