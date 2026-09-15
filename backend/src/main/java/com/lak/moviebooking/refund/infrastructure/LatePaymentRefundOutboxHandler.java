package com.lak.moviebooking.refund.infrastructure;

import java.util.UUID;

import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventHandler;
import com.lak.moviebooking.refund.application.RefundManagement;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Turns the payment module's at-least-once late-payment event into one refund request. */
@Component
public class LatePaymentRefundOutboxHandler implements OutboxEventHandler {

    private static final String EVENT_TYPE = "refund.late_payment_requested";
    private final ObjectMapper objectMapper;
    private final RefundManagement refunds;

    public LatePaymentRefundOutboxHandler(ObjectMapper objectMapper, RefundManagement refunds) {
        this.objectMapper = objectMapper;
        this.refunds = refunds;
    }

    @Override
    public boolean supports(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            LatePaymentPayload payload = objectMapper.readValue(event.payload(), LatePaymentPayload.class);
            refunds.requestLatePaymentRefund(payload.bookingId(), payload.paymentId(), payload.amount());
        }
        catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid late payment refund event", exception);
        }
    }

    private record LatePaymentPayload(UUID bookingId, UUID paymentId, long amount) { }
}
