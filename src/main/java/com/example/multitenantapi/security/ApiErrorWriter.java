package com.example.multitenantapi.security;

import com.example.multitenantapi.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes {@link ApiError} bodies from servlet filters and security handlers, which run
 * before Spring MVC and therefore cannot rely on {@code GlobalExceptionHandler}.
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError err = ApiError.builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .path(request.getRequestURI())
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(err));
    }
}
