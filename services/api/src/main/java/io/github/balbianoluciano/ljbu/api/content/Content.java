package io.github.balbianoluciano.ljbu.api.content;

import io.github.balbianoluciano.ljbu.api.result.FeedbackCatalogs;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Everything loaded from the content folder. Immutable once the api has started. */
public record Content(
    List<ModuleSpec> modules,
    Map<String, Challenge> challenges,
    Domain domain,
    FeedbackCatalogs feedback) {

  public Optional<Challenge> challenge(String id) {
    return Optional.ofNullable(challenges.get(id));
  }

  /** The challenges of a module, in order. */
  public List<Challenge> challengesOf(String moduleId) {
    return challenges.values().stream()
        .filter(challenge -> challenge.spec().module().equals(moduleId))
        .sorted((a, b) -> Integer.compare(a.spec().order(), b.spec().order()))
        .toList();
  }
}
