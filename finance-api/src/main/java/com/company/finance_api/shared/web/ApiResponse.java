package com.company.finance_api.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Standart API yanıt zarfı: {@code success}, {@code data} ve opsiyonel {@link ApiError}. */
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

  public boolean isSuccess() {
    return success;
  }

  public T getData() {
    return data;
  }

  public ApiError getError() {
    return error;
  }
}
