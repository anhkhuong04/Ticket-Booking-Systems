package com.lak.moviebooking.reporting.application;
import java.time.LocalDate; import java.util.Set; import java.util.UUID;
public interface ReportingQuery { ReportSummary summary(LocalDate from, LocalDate to, UUID cinemaId, Set<UUID> cinemaScope); }
