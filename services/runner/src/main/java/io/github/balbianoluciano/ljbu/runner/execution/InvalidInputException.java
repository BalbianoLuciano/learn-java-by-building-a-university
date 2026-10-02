package io.github.balbianoluciano.ljbu.runner.execution;

/** The request breaks an input rule of docs/SECURITY.md §3, layer 1. */
public class InvalidInputException extends RuntimeException {

  private final String code;

  public InvalidInputException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
