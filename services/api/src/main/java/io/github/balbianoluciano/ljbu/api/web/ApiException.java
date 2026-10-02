package io.github.balbianoluciano.ljbu.api.web;

import org.springframework.http.HttpStatus;

/** A request the api cannot serve, with the status and the code the web receives. */
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static ApiException notFound(String code, String message) {
    return new ApiException(HttpStatus.NOT_FOUND, code, message);
  }

  public static ApiException invalid(String code, String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, message);
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }
}
