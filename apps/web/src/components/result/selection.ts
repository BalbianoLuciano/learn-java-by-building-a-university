import type { SourceRef } from '@ljbu/contracts';

/** What the learner is looking at: a line, a piece, a log entry and/or a step. */
export interface Selection {
  sourceRef?: SourceRef;
  pieceId?: string;
  entry?: number;
  step?: number;
}

export function sameLine(a: SourceRef | undefined, b: SourceRef | undefined): boolean {
  return a !== undefined && b !== undefined && a.file === b.file && a.line === b.line;
}
