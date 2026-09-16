package com.lak.moviebooking.identity.application;

import java.time.LocalDate;

public record CustomerProfileView(String fullName, String email, String phone, LocalDate birthDate) { }
