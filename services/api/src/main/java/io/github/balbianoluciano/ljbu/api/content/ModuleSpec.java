package io.github.balbianoluciano.ljbu.api.content;

/** A module.yaml file. */
public record ModuleSpec(String id, int order, Localized title, Localized goal) {}
