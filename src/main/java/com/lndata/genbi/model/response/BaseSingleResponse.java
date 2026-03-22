package com.lndata.genbi.model.response;

public record BaseSingleResponse<T>(boolean success, String message, T data) {

  public static <T> BaseSingleResponse<T> success(String message, T data) {
    return new BaseSingleResponse<>(true, message, data);
  }

  public static <T> BaseSingleResponse<T> failure(String message, T data) {
    return new BaseSingleResponse<>(false, message, data);
  }
}
