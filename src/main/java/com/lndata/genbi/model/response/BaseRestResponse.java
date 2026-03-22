package com.lndata.genbi.model.response;

public record BaseRestResponse(boolean success, String message) {

  public static BaseRestResponse success(String message) {
    return new BaseRestResponse(true, message);
  }

  public static BaseRestResponse failure(String message) {
    return new BaseRestResponse(false, message);
  }
}
