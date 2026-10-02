package io.github.balbianoluciano.ljbu.api.web;

import io.github.balbianoluciano.ljbu.api.content.Challenge;
import io.github.balbianoluciano.ljbu.api.content.Content;
import io.github.balbianoluciano.ljbu.api.content.SourceFile;
import io.github.balbianoluciano.ljbu.api.result.ResultBuilder;
import io.github.balbianoluciano.ljbu.api.result.RunResult;
import io.github.balbianoluciano.ljbu.api.runner.RunnerClient;
import io.github.balbianoluciano.ljbu.api.trace.Trace;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** A run: validate the request, ask the runner for a trace and explain it. */
@Service
public class RunService {

  /** docs/SECURITY.md §3, layer 1. */
  static final int MAX_TOTAL_BYTES = 64 * 1024;

  private static final Logger log = LoggerFactory.getLogger(RunService.class);

  private final Content content;
  private final RunnerClient runner;
  private final ResultBuilder results;

  public RunService(Content content, RunnerClient runner, ResultBuilder results) {
    this.content = content;
    this.runner = runner;
    this.results = results;
  }

  public RunResult run(String challengeId, List<SourceFile> submitted, String language) {
    Challenge challenge =
        content
            .challenge(challengeId == null ? "" : challengeId)
            .orElseThrow(
                () ->
                    ApiException.notFound(
                        "challenge_not_found", "There is no challenge " + challengeId + "."));
    List<SourceFile> files = program(challenge, submitted == null ? List.of() : submitted);

    long start = System.nanoTime();
    Integer timeoutMs =
        challenge.spec().limits() == null ? null : challenge.spec().limits().timeoutMs();
    Trace trace = runner.execute(files, timeoutMs);
    long executed = System.nanoTime();
    RunResult result = results.build(challenge, trace, files, language);
    // Learner code and its output are never logged (AGENTS.md).
    log.info(
        "run runId={} challenge={} status={} outcome={} runnerMs={} checksMs={}",
        result.runId(),
        challenge.id(),
        trace.status(),
        result.outcome(),
        (executed - start) / 1_000_000,
        (System.nanoTime() - executed) / 1_000_000);
    return result;
  }

  /**
   * The program to run: the files of the challenge, with the editable ones replaced by what the
   * learner sent. Read-only files always come from the challenge.
   */
  private static List<SourceFile> program(Challenge challenge, List<SourceFile> submitted) {
    Set<String> seen = new HashSet<>();
    for (SourceFile file : submitted) {
      if (file == null || file.path() == null || file.content() == null) {
        throw ApiException.invalid("invalid_file", "Every file needs a path and a content.");
      }
      if (!challenge.isEditable(file.path())) {
        throw ApiException.invalid(
            "not_editable", file.path() + " is not an editable file of the challenge.");
      }
      if (!seen.add(file.path())) {
        throw ApiException.invalid("duplicate_file", "File names must be unique.");
      }
    }
    List<SourceFile> files = new ArrayList<>();
    long totalBytes = 0;
    for (SourceFile original : challenge.starter()) {
      SourceFile file =
          submitted.stream()
              .filter(sent -> sent.path().equals(original.path()))
              .findFirst()
              .orElse(original);
      totalBytes += file.content().getBytes(StandardCharsets.UTF_8).length;
      files.add(file);
    }
    if (totalBytes > MAX_TOTAL_BYTES) {
      throw ApiException.invalid("files_too_large", "The code adds up to more than 64 KB.");
    }
    return files;
  }
}
