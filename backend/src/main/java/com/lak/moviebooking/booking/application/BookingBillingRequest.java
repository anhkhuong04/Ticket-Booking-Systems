package com.lak.moviebooking.booking.application;

public record BookingBillingRequest(String recipientType, String recipientName, String taxCode,
                                    String address, String email) { }
