package com.lak.moviebooking.authorization.infrastructure;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.lak.moviebooking.common.api.error.ApiErrorResponse;
import com.lak.moviebooking.common.api.request.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class ApiSecurityResponseWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    ApiSecurityResponseWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(clock), status, code, message, request.getRequestURI(), requestId(request), List.of()));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        return value instanceof String requestId ? requestId : "unknown";
    }
}
