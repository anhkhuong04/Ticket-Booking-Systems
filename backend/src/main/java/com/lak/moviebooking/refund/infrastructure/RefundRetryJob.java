package com.lak.moviebooking.refund.infrastructure;

import java.time.Clock;

import com.lak.moviebooking.refund.application.RefundManagement;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RefundRetryJob {

    private final RefundManagement refunds;
    private final Clock clock;

    RefundRetryJob(RefundManagement refunds, Clock clock) {
        this.refunds = refunds;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.refund.retry-poll-interval:30s}")
    void retry() {
        refunds.processRequestedRefunds(clock.instant());
    }
}
