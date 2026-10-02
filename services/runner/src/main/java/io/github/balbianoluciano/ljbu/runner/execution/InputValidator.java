package io.github.balbianoluciano.ljbu.runner.execution;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Input rules of docs/SECURITY.md §3, layer 1, enforced again by the runner. */
public final class InputValidator {

  public static final int MAX_FILES = 10;
  public static final int MAX_TOTAL_BYTES = 64 * 1024;
  public static final String MAIN_FILE = "Main.java";

  private static final Pattern FILE_NAME = Pattern.compile("^[A-Z][A-Za-z0-9_]*\\.java$");

  private InputValidator() {}

  public static void validate(List<SourceFile> files) {
    if (files == null || files.isEmpty()) {
      throw new InvalidInputException("no_files", "At least one file is required.");
    }
    if (files.size() > MAX_FILES) {
      throw new InvalidInputException("too_many_files", "At most " + MAX_FILES + " files.");
    }
    Set<String> names = new HashSet<>();
    long totalBytes = 0;
    for (SourceFile file : files) {
      if (file == null || file.path() == null || file.content() == null) {
        throw new InvalidInputException("invalid_file", "Every file needs a path and a content.");
      }
      if (!FILE_NAME.matcher(file.path()).matches()) {
        throw new InvalidInputException(
            "invalid_file_name", "File names must match " + FILE_NAME.pattern() + ".");
      }
      if (!names.add(file.path())) {
        throw new InvalidInputException("duplicate_file", "File names must be unique.");
      }
      totalBytes += file.content().getBytes(StandardCharsets.UTF_8).length;
    }
    if (totalBytes > MAX_TOTAL_BYTES) {
      throw new InvalidInputException(
          "files_too_large", "The files add up to more than " + MAX_TOTAL_BYTES + " bytes.");
    }
    if (!names.contains(MAIN_FILE)) {
      throw new InvalidInputException("missing_main_file", MAIN_FILE + " is required.");
    }
  }
}
