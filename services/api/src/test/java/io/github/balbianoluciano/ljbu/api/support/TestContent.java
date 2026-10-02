package io.github.balbianoluciano.ljbu.api.support;

import io.github.balbianoluciano.ljbu.api.content.Content;
import io.github.balbianoluciano.ljbu.api.content.ContentLoader;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.result.ResultBuilder;
import io.github.balbianoluciano.ljbu.api.result.RunResult;
import io.github.balbianoluciano.ljbu.api.web.RunService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import tools.jackson.databind.json.JsonMapper;

/** The content of the repository and copies of it that tests can change. */
public final class TestContent {

  /** The real content: content/ at the root of the repository. */
  public static final Path REAL = Path.of("../../content");

  private static final Path FIXTURES = Path.of("src/test/resources/fixtures/challenges");
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private TestContent() {}

  public static Content real() {
    return new ContentLoader().load(REAL);
  }

  /** The real domain data and catalogs with the challenges of the test fixtures. */
  public static Content fixtures(Path temporary) {
    copy(REAL.resolve("domain"), temporary.resolve("domain"));
    copy(REAL.resolve("feedback"), temporary.resolve("feedback"));
    copy(FIXTURES, temporary.resolve("challenges"));
    return new ContentLoader().load(temporary);
  }

  /** A copy of the real content, to break it on purpose. */
  public static Path copyOfReal(Path temporary) {
    copy(REAL, temporary);
    return temporary;
  }

  /** Runs through the real runner, as the api does. */
  public static RunService runs(Content content) {
    return new RunService(content, TestRunner.client(), new ResultBuilder(content.feedback()));
  }

  /** Runs the challenge with the given files and checks the result against its contract. */
  public static RunResult run(Content content, String challengeId, List<SourceFile> submitted) {
    RunResult result = runs(content).run(challengeId, submitted, "es");
    Contracts.assertMatches("result.schema.json", MAPPER.writeValueAsString(result));
    return result;
  }

  private static void copy(Path from, Path to) {
    try (Stream<Path> paths = Files.walk(from)) {
      for (Path source : paths.toList()) {
        Path target = to.resolve(from.relativize(source).toString());
        if (Files.isDirectory(source)) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(source, target);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
