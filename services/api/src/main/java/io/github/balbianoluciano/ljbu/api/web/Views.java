package io.github.balbianoluciano.ljbu.api.web;

import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import java.util.List;

/**
 * What the api answers about content. Each record is a contract of packages/contracts: module-list,
 * challenge-view, hint and solution.
 */
public final class Views {

  private Views() {}

  public record ModuleList(List<ModuleSummary> modules) {}

  public record ModuleSummary(
      String id, int order, String title, String goal, List<ChallengeSummary> challenges) {}

  public record ChallengeSummary(
      String id, int order, String title, String concept, List<PieceSummary> pieces) {}

  public record PieceSummary(String archetype, String label) {}

  public record ChallengeView(
      String id,
      String module,
      int order,
      String title,
      String concept,
      String brief,
      List<Criterion> criteria,
      List<StarterFile> files,
      int hintCount,
      List<Rule> rules,
      RealReference realReference,
      Analogy analogy) {}

  public record Analogy(String passed, String incomplete, String failed) {}

  public record Criterion(List<String> checks, String text) {}

  public record StarterFile(String path, String content, boolean editable) {}

  public record Rule(String id, String statement, String source, String url) {}

  public record RealReference(
      List<RegionalFaculty> regionalFaculties, List<GoverningBody> governingBodies) {}

  public record RegionalFaculty(String id, String name, String city, String province) {}

  public record GoverningBody(
      String id, String name, String kind, String composition, Integer mandateInYears) {}

  public record Hint(int level, String text) {}

  public record Solution(List<SourceFile> files, String explanation) {}

  /** Body of POST /api/v1/runs (run-request.schema.json). */
  public record RunRequest(String challengeId, List<SourceFile> files) {}
}
