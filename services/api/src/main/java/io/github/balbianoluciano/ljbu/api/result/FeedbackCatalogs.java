package io.github.balbianoluciano.ljbu.api.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The catalogs of content/feedback: how to explain compilation errors, exceptions, limits and
 * rejected code (docs/FEEDBACK.md §4–5).
 */
public record FeedbackCatalogs(
    Catalog javac, Catalog exceptions, Map<String, Entry> limits, Rejections rejections) {

  /** What happened, why, and a hint: a message ready to show. */
  public record Message(String what, String why, String hint) {}

  /**
   * An entry of a catalog.
   *
   * @param match regular expression applied to the original message; its named groups fill the
   *     placeholders of {@code what}
   * @param whatWithoutMatch used when {@code match} does not apply
   */
  public record Entry(
      Pattern match, String what, String whatWithoutMatch, String why, String hint) {

    /**
     * @param original message of javac or of the exception; may be null
     * @param builtIn placeholders available besides the groups of {@code match}
     */
    public Message explain(String original, Map<String, String> builtIn) {
      Map<String, String> values = new LinkedHashMap<>(builtIn);
      boolean matched = match == null;
      if (match != null && original != null) {
        Matcher matcher = match.matcher(original);
        if (matcher.find()) {
          matched = true;
          match.namedGroups().keySet().forEach(group -> values.put(group, matcher.group(group)));
        }
      }
      String title = matched ? what : whatWithoutMatch;
      return new Message(Templates.render(title, values), why, hint);
    }
  }

  /** Entries by code, with a default for the codes nobody wrote an entry for. */
  public record Catalog(Entry fallback, Map<String, Entry> entries) {

    public boolean knows(String code) {
      return entries.containsKey(code);
    }

    public Message explain(String code, String original, Map<String, String> builtIn) {
      return entries.getOrDefault(code, fallback).explain(original, builtIn);
    }
  }

  public record Rejections(Entry fallback, List<Rule> rules) {

    /**
     * @param classes the symbol is one of these classes, or a member of one of them
     */
    public record Rule(
        List<String> classes, List<String> prefixes, List<String> suffixes, Entry entry) {
      boolean applies(String symbol) {
        return classes.stream()
                .anyMatch(name -> symbol.equals(name) || symbol.startsWith(name + "."))
            || prefixes.stream().anyMatch(symbol::startsWith)
            || suffixes.stream().anyMatch(symbol::endsWith);
      }
    }

    public Message explain(String symbol) {
      Entry entry =
          rules.stream()
              .filter(rule -> rule.applies(symbol))
              .map(Rule::entry)
              .findFirst()
              .orElse(fallback);
      return entry.explain(null, Map.of("symbol", symbol));
    }
  }
}
