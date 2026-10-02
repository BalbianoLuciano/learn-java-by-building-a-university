package io.github.balbianoluciano.ljbu.api.result;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Texts with {placeholders}. Braces around anything but a single word are left alone. */
public final class Templates {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}");

  private Templates() {}

  public static Set<String> placeholders(String template) {
    Set<String> names = new LinkedHashSet<>();
    Matcher matcher = PLACEHOLDER.matcher(template);
    while (matcher.find()) {
      names.add(matcher.group(1));
    }
    return names;
  }

  /** Fills the placeholders that have a value; the rest stay as written. */
  public static String render(String template, Map<String, String> values) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder rendered = new StringBuilder();
    while (matcher.find()) {
      String value = values.get(matcher.group(1));
      matcher.appendReplacement(
          rendered, Matcher.quoteReplacement(value == null ? matcher.group() : value));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }
}
