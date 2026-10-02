package io.github.balbianoluciano.ljbu.api.checks;

import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.CheckSpec;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.TrapSpec;
import java.util.List;

/** A check of a challenge with its parameters parsed. */
public record Check(CheckSpec spec, CheckParams params, List<Trap> traps) {

  public String id() {
    return spec.id();
  }

  /** A common mistake: when its condition holds, its message replaces the one of the check. */
  public record Trap(TrapSpec spec, CheckParams when) {}
}
