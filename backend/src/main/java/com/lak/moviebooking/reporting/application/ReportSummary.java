package com.lak.moviebooking.reporting.application;
import java.time.LocalDate; import java.util.List;
public record ReportSummary(long netRevenue, long bookings, long ticketsSold, long occupancyPercent, long pendingBookings, long refundsNeedingAttention, List<DailyMetric> daily) { public record DailyMetric(LocalDate date,long netRevenue,long ticketsSold) { } }
