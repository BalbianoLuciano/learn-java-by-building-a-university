package io.github.balbianoluciano.ljbu.runner.health;

public record HealthResponse(String status) {

  public static HealthResponse ok() {
    return new HealthResponse("ok");
  }
}
