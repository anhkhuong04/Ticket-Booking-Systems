package com.lak.moviebooking.refund.application;

/** Provider boundary. Implementations must make the provider reference idempotent for a refund ID. */
public interface RefundProvider {

    String name();

    RefundProviderResult refund(RefundProviderRequest request) throws RefundProviderException;
}
