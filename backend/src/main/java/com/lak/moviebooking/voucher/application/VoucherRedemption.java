package com.lak.moviebooking.voucher.application;

import java.time.Instant;
import java.util.UUID;

public interface VoucherRedemption {

    AppliedVoucher redeem(VoucherRedemptionCommand command);

    void restoreForRefund(UUID bookingId, Instant now);
}
