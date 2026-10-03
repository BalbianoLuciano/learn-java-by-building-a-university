package io.github.balbianoluciano.ljbu.api.content;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.balbianoluciano.ljbu.api.checks.Check;
import io.github.balbianoluciano.ljbu.api.checks.CheckParams;
import io.github.balbianoluciano.ljbu.api.checks.CheckType;
import io.github.balbianoluciano.ljbu.api.content.Challenge.Expected;
import io.github.balbianoluciano.ljbu.api.content.Challenge.Variant;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.CheckSpec;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec.Message;
import io.github.balbianoluciano.ljbu.api.result.FeedbackCatalogs;
import io.github.balbianoluciano.ljbu.api.result.Templates;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Reads the content folder and checks every rule of docs/specs/challenge-format.md. One invalid
 * challenge stops the api from starting: a broken challenge must never reach a learner.
 */
public final class ContentLoader {

  private static final Pattern MODULE_DIRECTORY = Pattern.compile("^(m[1-9][0-9]*)-[a-z0-9-]+$");
  private static final Pattern CHALLENGE_DIRECTORY = Pattern.compile("^([0-9]{2})-[a-z0-9-]+$");
  private static final Pattern JAVA_FILE = Pattern.compile("^[A-Z][A-Za-z0-9_]*\\.java$");
  private static final String MAIN_FILE = "Main.java";

  /** An interface has no objects, so it needs no binding. */
  private static final Pattern INTERFACE =
      Pattern.compile("^\\s*(public\\s+)?interface\\s", Pattern.MULTILINE);

  /** docs/FEEDBACK.md §3: what happened and why fit in 280 characters. */
  private static final int MAX_MESSAGE_LENGTH = 280;

  private final ObjectMapper yaml = YAMLMapper.builder().build();
  private final ObjectMapper json = JsonMapper.builder().build();
  private final ObjectMapper strict =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  private final Schema challengeSchema = schema("challenge.schema.json");
  private final Schema moduleSchema = schema("module.schema.json");

  public Content load(Path root) {
    if (!Files.isDirectory(root)) {
      throw new ContentException("The content folder does not exist: " + root.toAbsolutePath());
    }
    Domain domain = loadDomain(root.resolve("domain"));
    FeedbackCatalogs feedback = new CatalogLoader(yaml).load(root.resolve("feedback"));

    List<ModuleSpec> modules = new ArrayList<>();
    Map<String, Challenge> challenges = new LinkedHashMap<>();
    for (Path moduleDirectory : directories(root.resolve("challenges"))) {
      Matcher name = MODULE_DIRECTORY.matcher(moduleDirectory.getFileName().toString());
      if (!name.matches()) {
        throw invalid(moduleDirectory, "module folders are named m<n>-<slug>");
      }
      ModuleSpec module = loadModule(moduleDirectory, name.group(1));
      if (modules.stream().anyMatch(other -> other.id().equals(module.id()))) {
        throw invalid(moduleDirectory, "module " + module.id() + " is defined twice");
      }
      modules.add(module);

      Set<Integer> orders = new HashSet<>();
      for (Path challengeDirectory : directories(moduleDirectory)) {
        Challenge challenge = loadChallenge(challengeDirectory, module, domain);
        if (challenges.putIfAbsent(challenge.id(), challenge) != null) {
          throw invalid(challengeDirectory, "challenge id " + challenge.id() + " is used twice");
        }
        if (!orders.add(challenge.spec().order())) {
          throw invalid(challengeDirectory, "order " + challenge.spec().order() + " is used twice");
        }
      }
    }
    modules.sort((a, b) -> Integer.compare(a.order(), b.order()));
    return new Content(List.copyOf(modules), challenges, domain, feedback);
  }

  // --- Domain

  private Domain loadDomain(Path directory) {
    Map<String, Domain.Rule> rules = new LinkedHashMap<>();
    for (Domain.Rule rule :
        readJson(directory.resolve("rules.json"), new TypeReference<List<Domain.Rule>>() {})) {
      requireText(
          directory.resolve("rules.json"),
          "every rule needs id, statement, source and url",
          rule.id(),
          rule.statement(),
          rule.source(),
          rule.url());
      rules.put(rule.id(), rule);
    }
    Map<String, Domain.RegionalFaculty> faculties = new LinkedHashMap<>();
    Path facultiesFile = directory.resolve("regional-faculties.json");
    for (Domain.RegionalFaculty faculty :
        readJson(facultiesFile, new TypeReference<List<Domain.RegionalFaculty>>() {})) {
      requireText(
          facultiesFile,
          "every regional faculty needs id, name, city and province",
          faculty.id(),
          faculty.name(),
          faculty.city(),
          faculty.province());
      if (faculty.sources() == null || faculty.sources().isEmpty()) {
        // docs/DOMAIN.md §1: only facts with a source.
        throw invalid(facultiesFile, faculty.id() + " has no source");
      }
      faculties.put(faculty.id(), faculty);
    }
    Map<String, Domain.GoverningBody> bodies = new LinkedHashMap<>();
    Path bodiesFile = directory.resolve("governing-bodies.json");
    for (Domain.GoverningBody body :
        readJson(bodiesFile, new TypeReference<List<Domain.GoverningBody>>() {})) {
      requireText(
          bodiesFile,
          "every governing body needs id, name, kind, composition, source and url",
          body.id(),
          body.name(),
          body.kind(),
          body.composition(),
          body.source(),
          body.url());
      if (!Set.of("collegiate", "unipersonal").contains(body.kind())) {
        throw invalid(bodiesFile, body.id() + " has an unknown kind " + body.kind());
      }
      bodies.put(body.id(), body);
    }
    return new Domain(rules, faculties, bodies);
  }

  // --- Modules and challenges

  private ModuleSpec loadModule(Path directory, String idInFolderName) {
    Path file = directory.resolve("module.yaml");
    ModuleSpec module = read(file, moduleSchema, ModuleSpec.class);
    if (!module.id().equals(idInFolderName)) {
      throw invalid(file, "id " + module.id() + " does not match the folder name");
    }
    return module;
  }

  private Challenge loadChallenge(Path directory, ModuleSpec module, Domain domain) {
    Matcher name = CHALLENGE_DIRECTORY.matcher(directory.getFileName().toString());
    if (!name.matches()) {
      throw invalid(directory, "challenge folders are named <NN>-<slug>");
    }
    Path file = directory.resolve("challenge.yaml");
    ChallengeSpec spec = read(file, challengeSchema, ChallengeSpec.class);
    if (!spec.module().equals(module.id())) {
      throw invalid(
          file, "module " + spec.module() + " does not match the folder of " + module.id());
    }
    if (spec.order() != Integer.parseInt(name.group(1))) {
      throw invalid(file, "order " + spec.order() + " does not match the folder name");
    }
    if (!spec.id().equals("%s-%02d".formatted(module.id(), spec.order()))) {
      throw invalid(file, "id " + spec.id() + " must be <module>-<order>");
    }
    for (String rule : spec.rules()) {
      if (!domain.rules().containsKey(rule)) {
        throw invalid(file, "rule " + rule + " is not in content/domain/rules.json");
      }
    }
    if (spec.scene().realReference() != null) {
      for (String faculty : orEmpty(spec.scene().realReference().regionalFaculties())) {
        if (!domain.regionalFaculties().containsKey(faculty)) {
          throw invalid(file, "regional faculty " + faculty + " is not in content/domain");
        }
      }
      for (String body : orEmpty(spec.scene().realReference().governingBodies())) {
        if (!domain.governingBodies().containsKey(body)) {
          throw invalid(file, "governing body " + body + " is not in content/domain");
        }
      }
    }

    Set<String> pieces = new HashSet<>();
    for (ChallengeSpec.ExpectedPiece piece : spec.scene().pieces()) {
      if (!pieces.add(piece.id())) {
        throw invalid(file, "piece " + piece.id() + " is defined twice");
      }
    }
    List<Check> checks = new ArrayList<>();
    Set<String> checkIds = new HashSet<>();
    for (CheckSpec check : spec.checks()) {
      if (!checkIds.add(check.id())) {
        throw invalid(file, "check " + check.id() + " is defined twice");
      }
      if (check.piece() != null && !pieces.contains(check.piece())) {
        throw invalid(
            file, "check " + check.id() + " refers to a piece that scene.pieces does not define");
      }
      checks.add(parse(file, check));
    }
    for (ChallengeSpec.Criterion criterion : spec.criteria()) {
      for (String check : criterion.checks()) {
        if (!checkIds.contains(check)) {
          throw invalid(file, "a criterion refers to check " + check + ", which does not exist");
        }
      }
    }

    List<SourceFile> starter = sources(directory.resolve("starter"));
    List<SourceFile> solution = sources(directory.resolve("solution"));
    for (String editable : spec.editable()) {
      if (starter.stream().noneMatch(source -> source.path().equals(editable))) {
        throw invalid(file, "editable file " + editable + " is not in starter/");
      }
    }
    if (!names(starter).equals(names(solution))) {
      throw invalid(directory, "starter/ and solution/ must have the same files");
    }
    for (SourceFile source : starter) {
      SourceFile solved =
          solution.stream()
              .filter(other -> other.path().equals(source.path()))
              .findFirst()
              .orElseThrow();
      if (!spec.editable().contains(source.path()) && !source.content().equals(solved.content())) {
        throw invalid(
            directory, source.path() + " is read-only but differs between starter/ and solution/");
      }
    }
    Set<String> bound = new HashSet<>();
    spec.scene().bindings().forEach(binding -> bound.add(binding.type() + ".java"));
    for (SourceFile source : solution) {
      if (!source.path().equals(MAIN_FILE)
          && !bound.contains(source.path())
          && !INTERFACE.matcher(source.content()).find()) {
        throw invalid(file, "class " + source.path() + " of the solution has no scene binding");
      }
    }

    Path explanation = directory.resolve("solution.md");
    if (!Files.isRegularFile(explanation) || readText(explanation).isBlank()) {
      throw invalid(directory, "solution.md is missing or empty");
    }
    return new Challenge(
        spec,
        List.copyOf(checks),
        starter,
        solution,
        readText(explanation),
        variants(directory.resolve("tests"), spec, checkIds));
  }

  private Check parse(Path file, CheckSpec spec) {
    CheckParams params = params(file, spec.id(), spec.type(), spec.params());
    checkMessage(file, spec.id(), spec.feedback().pass(), params);
    checkMessage(file, spec.id(), spec.feedback().fail(), params);
    if (spec.feedback().fail().hint() == null) {
      throw invalid(file, "check " + spec.id() + " needs a hint for when it fails");
    }
    List<Check.Trap> traps = new ArrayList<>();
    if (spec.traps() != null) {
      for (ChallengeSpec.TrapSpec trap : spec.traps()) {
        checkMessage(file, spec.id(), new Message(trap.what(), trap.why(), trap.hint()), params);
        traps.add(
            new Check.Trap(
                trap, params(file, spec.id(), trap.when().type(), trap.when().params())));
      }
    }
    return new Check(spec, params, List.copyOf(traps));
  }

  private CheckParams params(Path file, String checkId, String typeName, JsonNode node) {
    CheckType type =
        CheckType.of(typeName)
            .orElseThrow(
                () -> invalid(file, "check " + checkId + " has the unknown type " + typeName));
    try {
      CheckParams params =
          strict.treeToValue(node == null ? strict.createObjectNode() : node, type.paramsType());
      params.validate();
      return params;
    } catch (JacksonException | IllegalArgumentException e) {
      throw new ContentException(
          file + ": check " + checkId + " has invalid params: " + e.getMessage(), e);
    }
  }

  /** The placeholders of a message must be ones its check fills, and the message must be short. */
  private static void checkMessage(Path file, String checkId, Message message, CheckParams params) {
    for (Localized text : new Localized[] {message.what(), message.why(), message.hint()}) {
      if (text == null) {
        continue;
      }
      for (String placeholder : Templates.placeholders(text.es())) {
        if (!params.placeholders().contains(placeholder)) {
          throw invalid(
              file,
              "check "
                  + checkId
                  + " uses the placeholder {"
                  + placeholder
                  + "}, which its type does not fill");
        }
      }
    }
    int length = message.what().es().length() + message.why().es().length();
    if (length > MAX_MESSAGE_LENGTH) {
      throw invalid(
          file,
          "check "
              + checkId
              + " has a message of "
              + length
              + " characters; the limit is "
              + MAX_MESSAGE_LENGTH);
    }
  }

  private Map<String, Variant> variants(Path directory, ChallengeSpec spec, Set<String> checkIds) {
    Map<String, Variant> variants = new LinkedHashMap<>();
    if (!Files.isDirectory(directory)) {
      return variants;
    }
    for (Path variant : directories(directory)) {
      List<SourceFile> files = sources(variant);
      for (SourceFile source : files) {
        if (!spec.editable().contains(source.path())) {
          throw invalid(variant, source.path() + " is not an editable file of the challenge");
        }
      }
      Expected expected = readYaml(variant.resolve("expected.yaml"), Expected.class);
      if (expected.outcome() == null || expected.log() == null) {
        throw invalid(variant, "expected.yaml needs outcome and log");
      }
      expected
          .log()
          .forEach(
              entry -> {
                if (!checkIds.contains(entry.check())) {
                  throw invalid(
                      variant,
                      "expected.yaml refers to check " + entry.check() + ", which does not exist");
                }
              });
      variants.put(variant.getFileName().toString(), new Variant(files, expected));
    }
    return variants;
  }

  // --- Files

  private List<SourceFile> sources(Path directory) {
    if (!Files.isDirectory(directory)) {
      throw invalid(directory, "the folder is missing");
    }
    List<SourceFile> files = new ArrayList<>();
    try (Stream<Path> entries = Files.list(directory)) {
      for (Path entry : entries.sorted().toList()) {
        String name = entry.getFileName().toString();
        if (name.endsWith(".java")) {
          if (!JAVA_FILE.matcher(name).matches()) {
            throw invalid(entry, "file names must match " + JAVA_FILE.pattern());
          }
          files.add(new SourceFile(name, readText(entry)));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (directory.getFileName().toString().matches("starter|solution")
        && files.stream().noneMatch(source -> source.path().equals(MAIN_FILE))) {
      throw invalid(directory, MAIN_FILE + " is missing");
    }
    return List.copyOf(files);
  }

  private static Set<String> names(List<SourceFile> files) {
    Set<String> names = new HashSet<>();
    files.forEach(file -> names.add(file.path()));
    return names;
  }

  private static List<Path> directories(Path parent) {
    if (!Files.isDirectory(parent)) {
      return List.of();
    }
    try (Stream<Path> entries = Files.list(parent)) {
      return entries.filter(Files::isDirectory).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <T> T read(Path file, Schema schema, Class<T> type) {
    try {
      JsonNode node = yaml.readTree(readText(file));
      List<Error> errors = schema.validate(node);
      if (!errors.isEmpty()) {
        throw invalid(file, "does not follow its schema: " + errors);
      }
      return yaml.treeToValue(node, type);
    } catch (JacksonException e) {
      throw new ContentException(file + ": " + e.getMessage(), e);
    }
  }

  private <T> T readYaml(Path file, Class<T> type) {
    try {
      return yaml.readValue(readText(file), type);
    } catch (JacksonException e) {
      throw new ContentException(file + ": " + e.getMessage(), e);
    }
  }

  private <T> T readJson(Path file, TypeReference<T> type) {
    try {
      return json.readValue(readText(file), type);
    } catch (JacksonException e) {
      throw new ContentException(file + ": " + e.getMessage(), e);
    }
  }

  private static String readText(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new ContentException("Cannot read " + file, e);
    }
  }

  private static void requireText(Path file, String rule, String... values) {
    for (String value : values) {
      if (value == null || value.isBlank()) {
        throw invalid(file, rule);
      }
    }
  }

  private static ContentException invalid(Path where, String problem) {
    return new ContentException(where + ": " + problem);
  }

  /** The JSON Schemas are packaged from packages/contracts (see pom.xml). */
  private static Schema schema(String name) {
    try (InputStream stream = ContentLoader.class.getResourceAsStream("/contracts/" + name)) {
      if (stream == null) {
        throw new ContentException("Schema " + name + " is not on the classpath");
      }
      return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
          .getSchema(new String(stream.readAllBytes(), StandardCharsets.UTF_8), InputFormat.JSON);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<String> orEmpty(List<String> values) {
    return values == null ? List.of() : values;
  }
}
