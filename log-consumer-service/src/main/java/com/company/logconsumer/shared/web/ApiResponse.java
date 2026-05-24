package com.company.logconsumer.shared.web;

/**
 * Standart REST sarmalayıcı: {@code success}, {@code data} ve isteğe bağlı {@link ApiError}.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
