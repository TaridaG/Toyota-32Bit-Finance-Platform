package com.company.analytics.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/** REST API standart yanıt sarmalayıcısı. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ApiError error;

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /** Başarılı yanıt oluşturur. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Hata yanıtı oluşturur. */
    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }

    /** İşlemin başarılı olup olmadığını döner. */
    public boolean isSuccess() {
        return success;
    }

    /** Yanıt verisini döner. */
    public T getData() {
        return data;
    }

    /** Hata detayını döner. */
    public ApiError getError() {
        return error;
    }
}