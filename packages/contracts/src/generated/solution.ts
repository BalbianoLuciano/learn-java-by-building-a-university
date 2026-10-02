// Generated from solution.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Answer of GET /api/v1/challenges/{id}/solution.
 */
export interface Solution {
  files: {
    path: string;
    content: string;
  }[];
  /**
   * Markdown.
   */
  explanation: string;
}
