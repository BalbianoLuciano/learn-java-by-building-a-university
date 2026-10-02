package io.github.balbianoluciano.ljbu.runner.support;

import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Learner programs for the tests. */
public final class Programs {

  private Programs() {}

  /** The solution of challenge 1.3, the aliasing challenge, as published in content/. */
  public static List<SourceFile> aliasingSolution() {
    return in(
        Path.of(
            "../../content/challenges/m1-clases-y-objetos/03-la-facultad-donde-estudias/solution"));
  }

  /** The files of src/test/resources/programs/{name}. */
  public static List<SourceFile> named(String name) {
    return in(Path.of("src/test/resources/programs", name));
  }

  private static List<SourceFile> in(Path directory) {
    try (Stream<Path> paths = Files.list(directory)) {
      List<SourceFile> files = new ArrayList<>();
      for (Path path : paths.sorted().toList()) {
        files.add(new SourceFile(path.getFileName().toString(), Files.readString(path)));
      }
      return files;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** A program made of a single Main.java. */
  public static List<SourceFile> main(String source) {
    return List.of(new SourceFile("Main.java", source));
  }

  /** A Main.java whose main method has the given body. */
  public static List<SourceFile> mainWithBody(String body) {
    return main(
        """
        public class Main {
          public static void main(String[] args) throws Exception {
        %s
          }
        }
        """
            .formatted(body.indent(4).stripTrailing()));
  }

  public static List<SourceFile> files(SourceFile... files) {
    return List.of(files);
  }

  public static SourceFile file(String path, String content) {
    return new SourceFile(path, content);
  }
}
