package io.github.balbianoluciano.ljbu.api.content;

/** The content folder breaks a rule of docs/specs/challenge-format.md. The api does not start. */
public class ContentException extends RuntimeException {

  public ContentException(String message) {
    super(message);
  }

  public ContentException(String message, Throwable cause) {
    super(message, cause);
  }
}
