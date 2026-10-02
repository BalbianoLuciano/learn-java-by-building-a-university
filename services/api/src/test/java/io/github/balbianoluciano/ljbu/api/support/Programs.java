package io.github.balbianoluciano.ljbu.api.support;

import io.github.balbianoluciano.ljbu.api.checks.RunFacts;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.trace.Trace;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Learner programs for the tests and what the runner reports about them. */
public final class Programs {

  private Programs() {}

  public static SourceFile file(String path, String content) {
    return new SourceFile(path, content);
  }

  /** A Main.java whose main method has the given body. */
  public static SourceFile mainWithBody(String body) {
    return new SourceFile(
        "Main.java",
        """
        public class Main {
          public static void main(String[] args) {
        %s
          }
        }
        """
            .formatted(body.indent(4).stripTrailing()));
  }

  public static List<SourceFile> program(String mainBody, SourceFile... others) {
    List<SourceFile> files = new ArrayList<>(List.of(others));
    files.add(mainWithBody(mainBody));
    return files;
  }

  public static Trace trace(List<SourceFile> files) {
    return TestRunner.client().execute(files, null);
  }

  /** Runs the program, which must compile and be allowed, and indexes its trace. */
  public static RunFacts facts(List<SourceFile> files) {
    Trace trace = trace(files);
    if (!(trace instanceof Trace.Executed executed)) {
      throw new AssertionError("The program did not run: " + trace);
    }
    Map<String, String> sources = new LinkedHashMap<>();
    files.forEach(file -> sources.put(file.path(), file.content()));
    return new RunFacts(executed, sources);
  }
}
