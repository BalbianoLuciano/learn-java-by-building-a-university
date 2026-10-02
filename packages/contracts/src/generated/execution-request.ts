// Generated from execution-request.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Body of POST /internal/v1/executions, sent by the api to the runner (docs/ARCHITECTURE.md §5.2).
 */
export interface ExecutionRequest {
  /**
   * At most 64 KB in total. One of them is Main.java.
   *
   * @minItems 1
   * @maxItems 10
   */
  files:
    | [SourceFile]
    | [SourceFile, SourceFile]
    | [SourceFile, SourceFile, SourceFile]
    | [SourceFile, SourceFile, SourceFile, SourceFile]
    | [SourceFile, SourceFile, SourceFile, SourceFile, SourceFile]
    | [SourceFile, SourceFile, SourceFile, SourceFile, SourceFile, SourceFile]
    | [SourceFile, SourceFile, SourceFile, SourceFile, SourceFile, SourceFile, SourceFile]
    | [
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile
      ]
    | [
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile
      ]
    | [
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile,
        SourceFile
      ];
  /**
   * Overrides of the docs/SECURITY.md limits; they can only be lowered.
   */
  limits?: {
    timeoutMs?: number;
  };
}
export interface SourceFile {
  path: string;
  content: string;
}
