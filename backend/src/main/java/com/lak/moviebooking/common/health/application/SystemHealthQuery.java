package com.lak.moviebooking.common.health.application;

@FunctionalInterface
public interface SystemHealthQuery {

	SystemHealthResponse check();
}
