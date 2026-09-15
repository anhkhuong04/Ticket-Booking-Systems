package com.lak.moviebooking.voucher.application;

import java.util.UUID;

public record AppliedVoucher(UUID voucherId, String code, long discountAmount) {
}
