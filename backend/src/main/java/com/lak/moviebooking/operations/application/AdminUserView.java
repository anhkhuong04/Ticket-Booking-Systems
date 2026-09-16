package com.lak.moviebooking.operations.application;
import java.time.Instant; import java.util.List; import java.util.UUID;
public record AdminUserView(UUID id, String fullName, String email, String phone, String status, List<String> roles, List<String> cinemaNames, Instant createdAt) { }
