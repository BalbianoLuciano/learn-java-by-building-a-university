// Generated from challenge-view.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Answer of GET /api/v1/challenges/{id}: what the learner needs to work on a challenge. Checks, hints and the solution are not here.
 */
export interface ChallengeView {
  id: string;
  module: string;
  order: number;
  title: string;
  concept: string;
  /**
   * Short Markdown.
   */
  brief: string;
  criteria: {
    /**
     * Checks that tick this item when all of them pass.
     */
    checks: string[];
    text: string;
  }[];
  /**
   * The starter code.
   */
  files: {
    path: string;
    content: string;
    editable: boolean;
  }[];
  hintCount: number;
  /**
   * Rules of the real UTN the challenge is based on.
   */
  rules: {
    id: string;
    statement: string;
    source: string;
    url: string;
  }[];
  /**
   * The part of the real UTN the model is compared with.
   */
  realReference: {
    regionalFaculties: {
      id: string;
      name: string;
      city: string;
      province: string;
    }[];
    governingBodies: {
      id: string;
      name: string;
      kind: 'collegiate' | 'unipersonal';
      composition: string;
      mandateInYears?: number | null;
    }[];
  };
  /**
   * What happened, said without code: one text per outcome; null when the challenge has none.
   */
  analogy: {
    passed: string;
    incomplete: string;
    failed: string;
  } | null;
}
