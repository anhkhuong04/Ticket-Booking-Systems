package com.lak.moviebooking.payment.infrastructure;

import java.time.Clock;

import com.lak.moviebooking.payment.application.PaymentManagement;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class PaymentReconciliationJob {

    private final PaymentManagement payments;
    private final Clock clock;

    PaymentReconciliationJob(PaymentManagement payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.payment.reconciliation-poll-interval:30s}")
    void reconcile() {
        payments.expireDuePayments(clock.instant());
        payments.reconcilePendingPayments(clock.instant());
    }
}
