package com.lak.moviebooking.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReportSummary(
        long netRevenue,
        long bookings,
        long seatsSold,
        long occupancyPercent,
        OperationalAlerts alerts,
        Comparison previousPeriod,
        List<DailyMetric> daily,
        List<TopMovie> topMovies,
        Instant asOf
) {
    public record DailyMetric(LocalDate date, long netRevenue, long bookings, long seatsSold) { }

    public record Comparison(long netRevenue, long bookings, long seatsSold) { }

    public record OperationalAlerts(long totalBookings, long paymentReview, long overduePayments,
                                    long overdueRefunds, long failedRefunds, long cancelledShowtimeBookings,
                                    long paidWithoutTicket) { }

    public record TopMovie(UUID movieId, String title, long netRevenue, long seatsSold) { }
}
