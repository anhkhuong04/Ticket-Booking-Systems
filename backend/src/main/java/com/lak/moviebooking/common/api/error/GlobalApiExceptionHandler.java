package com.lak.moviebooking.common.api.error;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.lak.moviebooking.common.api.request.RequestIdFilter;
import com.lak.moviebooking.common.application.error.ApplicationErrorType;
import com.lak.moviebooking.common.application.error.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);
	private static final String GENERIC_BAD_REQUEST = "The request is invalid";
	private final Clock clock;

	public GlobalApiExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(ApplicationException.class)
	ResponseEntity<ApiErrorResponse> handleApplicationException(
			ApplicationException exception,
			HttpServletRequest request) {
		HttpStatus status = statusFor(exception.type());
		return response(status, exception.code(), exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<ApiFieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(error -> new ApiFieldViolation(error.getField(), safeValidationMessage(error)))
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", GENERIC_BAD_REQUEST, request, violations);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		List<ApiFieldViolation> violations = exception.getConstraintViolations().stream()
				.map(violation -> new ApiFieldViolation(
						violation.getPropertyPath().toString(),
						violation.getMessage()))
				.sorted(Comparator.comparing(ApiFieldViolation::field))
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", GENERIC_BAD_REQUEST, request, violations);
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class
	})
	ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", GENERIC_BAD_REQUEST, request, List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
		LOGGER.error(
				"Unhandled API error request_id={} exception_type={}",
				requestId(request),
				exception.getClass().getSimpleName());
		return response(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"An unexpected error occurred",
				request,
				List.of());
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request,
			List<ApiFieldViolation> violations) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(clock),
				status.value(),
				code,
				message,
				request.getRequestURI(),
				requestId(request),
				violations);
		return ResponseEntity.status(status).body(body);
	}

	private HttpStatus statusFor(ApplicationErrorType type) {
		return switch (type) {
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case CONFLICT -> HttpStatus.CONFLICT;
			case EXPIRED -> HttpStatus.GONE;
			case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_CONTENT;
			case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
		};
	}

	private String requestId(HttpServletRequest request) {
		Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
		return requestId instanceof String value ? value : "unknown";
	}

	private String safeValidationMessage(FieldError error) {
		return error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
	}
}
