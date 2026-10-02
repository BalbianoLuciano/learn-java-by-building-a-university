package io.github.balbianoluciano.ljbu.api.health;

public record HealthResponse(String status) {

  public static HealthResponse ok() {
    return new HealthResponse("ok");
  }
}
