package io.github.balbianoluciano.ljbu.api.content;

/** A learner-facing text by language. Spanish is mandatory; it is also the fallback. */
public record Localized(String es, String en) {

  public String in(String language) {
    return "en".equals(language) && en != null ? en : es;
  }
}
