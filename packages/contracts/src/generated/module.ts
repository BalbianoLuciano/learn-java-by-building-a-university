// Generated from module.schema.json by scripts/generate.mjs. Do not edit.

/**
 * A module.yaml file (docs/specs/challenge-format.md).
 */
export interface Module {
  id: string;
  order: number;
  title: LocalizedText;
  goal: LocalizedText;
}
export interface LocalizedText {
  es: string;
  en?: string;
}
