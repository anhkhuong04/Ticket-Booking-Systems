package com.lak.moviebooking.common.application.error;

import java.util.Objects;

public final class ApplicationException extends RuntimeException {

	private final ApplicationErrorType type;
	private final String code;

	private ApplicationException(ApplicationErrorType type, String code, String safeMessage) {
		super(Objects.requireNonNull(safeMessage));
		this.type = Objects.requireNonNull(type);
		this.code = requireCode(code);
	}

	public static ApplicationException unauthenticated(String code, String safeMessage) {
		return new ApplicationException(ApplicationErrorType.UNAUTHENTICATED, code, safeMessage);
	}

	public static ApplicationException forbidden(String code, String safeMessage) {
		return new ApplicationException(ApplicationErrorType.FORBIDDEN, code, safeMessage);
	}

	public static ApplicationException conflict(String code, String safeMessage) {
		return new ApplicationException(ApplicationErrorType.CONFLICT, code, safeMessage);
	}

	public static ApplicationException expired(String code, String safeMessage) {
		return new ApplicationException(ApplicationErrorType.EXPIRED, code, safeMessage);
	}

	public static ApplicationException businessRule(String code, String safeMessage) {
		return new ApplicationException(ApplicationErrorType.BUSINESS_RULE, code, safeMessage);
	}

	public static ApplicationException rateLimited(String code, String safeMessage) {
		return new ApplicationException(ApplicationErrorType.RATE_LIMITED, code, safeMessage);
	}

	public ApplicationErrorType type() {
		return type;
	}

	public String code() {
		return code;
	}

	private static String requireCode(String code) {
		String value = Objects.requireNonNull(code).trim();
		if (value.isEmpty()) {
			throw new IllegalArgumentException("Error code must not be blank");
		}
		return value;
	}
}
