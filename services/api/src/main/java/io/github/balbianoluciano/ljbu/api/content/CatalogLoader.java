package io.github.balbianoluciano.ljbu.api.content;

import io.github.balbianoluciano.ljbu.api.result.FeedbackCatalogs;
import io.github.balbianoluciano.ljbu.api.result.FeedbackCatalogs.Catalog;
import io.github.balbianoluciano.ljbu.api.result.FeedbackCatalogs.Entry;
import io.github.balbianoluciano.ljbu.api.result.FeedbackCatalogs.Rejections;
import io.github.balbianoluciano.ljbu.api.result.Templates;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Reads the catalogs of content/feedback and checks that every entry can be rendered. */
final class CatalogLoader {

  /** Limits the runner can report (docs/SECURITY.md §3, layer 4), plus a crash of the JVM. */
  private static final Set<String> LIMITS =
      Set.of("timeout", "steps", "objects", "output", "call_depth", "crash");

  private final ObjectMapper yaml;

  CatalogLoader(ObjectMapper yaml) {
    this.yaml = yaml;
  }

  record RawEntry(
      String match, String what, String whatWithoutMatch, String why, String hint, String same) {}

  record RawCatalog(RawEntry fallback, Map<String, RawEntry> entries) {
    @com.fasterxml.jackson.annotation.JsonCreator
    RawCatalog(
        @com.fasterxml.jackson.annotation.JsonProperty("default") RawEntry fallback,
        @com.fasterxml.jackson.annotation.JsonProperty("entries") Map<String, RawEntry> entries) {
      this.fallback = fallback;
      this.entries = entries;
    }
  }

  record RawRule(
      List<String> classes,
      List<String> prefixes,
      List<String> suffixes,
      String what,
      String why,
      String hint) {}

  record RawRejections(RawEntry fallback, List<RawRule> rules) {
    @com.fasterxml.jackson.annotation.JsonCreator
    RawRejections(
        @com.fasterxml.jackson.annotation.JsonProperty("default") RawEntry fallback,
        @com.fasterxml.jackson.annotation.JsonProperty("rules") List<RawRule> rules) {
      this.fallback = fallback;
      this.rules = rules;
    }
  }

  FeedbackCatalogs load(Path directory) {
    Catalog javac = catalog(directory.resolve("javac.es.yaml"), Set.of());
    Catalog exceptions =
        catalog(directory.resolve("exceptions.es.yaml"), Set.of("type", "message"));

    Path limitsFile = directory.resolve("limits.es.yaml");
    RawCatalog rawLimits = read(limitsFile, RawCatalog.class);
    Map<String, Entry> limits = new LinkedHashMap<>();
    for (String limit : LIMITS) {
      if (rawLimits.entries() == null || !rawLimits.entries().containsKey(limit)) {
        throw new ContentException(limitsFile + ": the entry " + limit + " is missing");
      }
      limits.put(limit, entry(limitsFile, limit, rawLimits.entries().get(limit), Set.of()));
    }

    Path rejectionsFile = directory.resolve("rejections.es.yaml");
    RawRejections rawRejections = read(rejectionsFile, RawRejections.class);
    List<Rejections.Rule> rules = new ArrayList<>();
    for (RawRule rule : rawRejections.rules()) {
      RawEntry raw = new RawEntry(null, rule.what(), null, rule.why(), rule.hint(), null);
      rules.add(
          new Rejections.Rule(
              rule.classes() == null ? List.of() : rule.classes(),
              rule.prefixes() == null ? List.of() : rule.prefixes(),
              rule.suffixes() == null ? List.of() : rule.suffixes(),
              entry(rejectionsFile, "rule", raw, Set.of("symbol"))));
    }
    Rejections rejections =
        new Rejections(
            entry(rejectionsFile, "default", rawRejections.fallback(), Set.of("symbol")), rules);
    return new FeedbackCatalogs(javac, exceptions, limits, rejections);
  }

  private Catalog catalog(Path file, Set<String> builtIn) {
    RawCatalog raw = read(file, RawCatalog.class);
    if (raw.fallback() == null || raw.entries() == null) {
      throw new ContentException(file + ": default and entries are required");
    }
    Map<String, Entry> entries = new LinkedHashMap<>();
    for (Map.Entry<String, RawEntry> each : raw.entries().entrySet()) {
      RawEntry source = each.getValue();
      if (source.same() != null) {
        source = raw.entries().get(source.same());
        if (source == null || source.same() != null) {
          throw new ContentException(
              file + ": " + each.getKey() + " reuses an entry that does not exist");
        }
      }
      entries.put(each.getKey(), entry(file, each.getKey(), source, builtIn));
    }
    return new Catalog(entry(file, "default", raw.fallback(), builtIn), entries);
  }

  private static Entry entry(Path file, String key, RawEntry raw, Set<String> builtIn) {
    if (raw == null || isBlank(raw.what()) || isBlank(raw.why()) || isBlank(raw.hint())) {
      throw new ContentException(file + ": " + key + " needs what, why and hint");
    }
    Pattern match = null;
    if (raw.match() != null) {
      try {
        match = Pattern.compile(raw.match());
      } catch (PatternSyntaxException e) {
        throw new ContentException(
            file + ": " + key + " has an invalid match: " + e.getMessage(), e);
      }
      if (isBlank(raw.whatWithoutMatch())) {
        throw new ContentException(file + ": " + key + " has match and needs whatWithoutMatch");
      }
    }
    Set<String> groups = match == null ? Set.of() : match.namedGroups().keySet();
    for (String placeholder : Templates.placeholders(raw.what())) {
      if (!groups.contains(placeholder) && !builtIn.contains(placeholder)) {
        throw new ContentException(
            file + ": " + key + " uses {" + placeholder + "}, which nothing fills");
      }
    }
    return new Entry(match, raw.what(), raw.whatWithoutMatch(), raw.why(), raw.hint());
  }

  private <T> T read(Path file, Class<T> type) {
    try {
      return yaml.readValue(Files.readString(file, StandardCharsets.UTF_8), type);
    } catch (IOException | JacksonException e) {
      throw new ContentException("Cannot read " + file + ": " + e.getMessage(), e);
    }
  }

  private static boolean isBlank(String text) {
    return text == null || text.isBlank();
  }
}
