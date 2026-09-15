package com.lak.moviebooking.reservation.infrastructure;

import java.time.Clock;

import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SeatHoldExpirationJob {

    private final SeatHoldManagement seatHoldManagement;
    private final Clock clock;

    SeatHoldExpirationJob(SeatHoldManagement seatHoldManagement, Clock clock) {
        this.seatHoldManagement = seatHoldManagement;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.reservation.expiry-poll-interval:20s}")
    void expireDueHolds() {
        seatHoldManagement.expireDueHolds(clock.instant());
    }
}
