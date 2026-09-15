package com.lak.moviebooking.refund.application;

public record RefundProviderResult(String providerRefundId) {

    public RefundProviderResult {
        if (providerRefundId == null || providerRefundId.isBlank()) {
            throw new IllegalArgumentException("Provider refund ID is required");
        }
    }
}
