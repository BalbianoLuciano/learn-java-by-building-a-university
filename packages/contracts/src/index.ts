export type * from './generated/trace';
export type * from './generated/challenge';
export type * from './generated/execution-request';
export type * from './generated/run-request';
export type * from './generated/module-list';
export type * from './generated/challenge-view';
export type * from './generated/hint';
export type * from './generated/solution';
// The result repeats some definitions of the trace and of the challenge; these are the ones
// the web reads.
export type {
  Archetype,
  LogEntry,
  Piece,
  RunResult,
  Slot,
  SourceRef,
  State,
  TimelineStep,
} from './generated/result';
// A module.yaml and a challenge.yaml share LocalizedText.
export type { Module } from './generated/module';
