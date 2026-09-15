package com.lak.moviebooking.payment.application;

public record PaymentCreateCommand(String bookingCode, String provider) {
}
