package com.lak.moviebooking.voucher.application;

public interface VoucherRedemption {

    AppliedVoucher redeem(VoucherRedemptionCommand command);
}
