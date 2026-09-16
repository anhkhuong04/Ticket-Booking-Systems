package com.lak.moviebooking.identity.application;

public record BillingPreferencesView(String recipientType, String recipientName, String taxCode,
                                     String address, String email) { }
