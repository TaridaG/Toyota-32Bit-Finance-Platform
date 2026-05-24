package com.company.newsservice.shared.web;

/**
 * Standart REST sarmalayıcı: {@code success}, {@code data} ve isteğe bağlı {@link ApiError}.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error
) {
    /** Başarılı yanıt ({@code success=true}, veri dolu). */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Hata yanıtı ({@code success=false}, {@link ApiError} dolu). */
    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}