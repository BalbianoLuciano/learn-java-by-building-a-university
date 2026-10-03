package io.github.balbianoluciano.ljbu.api.content;

import java.util.List;
import java.util.Map;

/** Data of the real UTN, from content/domain (docs/DOMAIN.md §6). */
public record Domain(
    Map<String, Rule> rules,
    Map<String, RegionalFaculty> regionalFaculties,
    Map<String, GoverningBody> governingBodies) {

  public record Rule(String id, String statement, String source, String url) {}

  public record RegionalFaculty(
      String id, String name, String city, String province, List<String> sources) {}

  /**
   * @param kind "collegiate" or "unipersonal"
   * @param mandateInYears null when the statute gives the body no fixed term
   */
  public record GoverningBody(
      String id,
      String name,
      String kind,
      String composition,
      Integer mandateInYears,
      String source,
      String url) {}
}
