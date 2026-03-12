package com.company.reporting.api;

import com.company.reporting.common.ApiError;
import com.company.reporting.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {

        log.warn("Bad request: {}", ex.getMessage());

        return ApiResponse.error(
                new ApiError("BAD_REQUEST", ex.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneric(Exception ex) {

        log.error("Unhandled error", ex);

        return ApiResponse.error(
                new ApiError("INTERNAL_ERROR", "Unexpected server error")
        );
    }
}