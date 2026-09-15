package com.lak.moviebooking.voucher.application;

import java.util.UUID;

public record VoucherRedemptionCommand(UUID userId, UUID bookingId, String voucherCode, long subtotal) {
}
