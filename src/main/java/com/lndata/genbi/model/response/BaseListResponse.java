package com.lndata.genbi.model.response;

import java.util.List;

public record BaseListResponse<T>(boolean success, String message, long total, List<T> data) {

  public static <T> BaseListResponse<T> success(String message, List<T> data) {
    return new BaseListResponse<>(true, message, data.size(), data);
  }

  public static <T> BaseListResponse<T> failure(String message, List<T> data) {
    return new BaseListResponse<>(false, message, data.size(), data);
  }
}
