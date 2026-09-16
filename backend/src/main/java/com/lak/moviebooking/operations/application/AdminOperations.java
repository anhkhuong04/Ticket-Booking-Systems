package com.lak.moviebooking.operations.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AdminOperations {
    List<AdminBookingView> bookings(String query, String status, UUID cinemaId, LocalDate date, Set<UUID> cinemaScope);
    List<AdminPaymentView> payments(String query, String status, UUID cinemaId, LocalDate date, Set<UUID> cinemaScope);
    List<AdminRefundView> refunds(String status, UUID cinemaId, LocalDate date, Set<UUID> cinemaScope);
    List<AdminUserView> users(String query);
    AdminRefundView refund(UUID refundId, Set<UUID> cinemaScope);
    void retryFailedRefund(UUID actorId, UUID refundId, Set<UUID> cinemaScope);
    void setUserLocked(UUID actorId, UUID userId, boolean locked);
}
