package com.lak.moviebooking.refund.application;

/** A provider failure classified by whether the worker can retry it automatically. */
public final class RefundProviderException extends Exception {

    private final String code;
    private final boolean retryable;

    private RefundProviderException(String code, boolean retryable) {
        super(code);
        this.code = code;
        this.retryable = retryable;
    }

    public static RefundProviderException temporary(String code) {
        return new RefundProviderException(code, true);
    }

    public static RefundProviderException permanent(String code) {
        return new RefundProviderException(code, false);
    }

    public String code() { return code; }

    public boolean retryable() { return retryable; }
}
