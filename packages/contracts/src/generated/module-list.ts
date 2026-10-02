// Generated from module-list.schema.json by scripts/generate.mjs. Do not edit.

/**
 * Answer of GET /api/v1/modules: modules and their challenges, without solutions.
 */
export interface ModuleList {
  modules: ModuleSummary[];
}
export interface ModuleSummary {
  id: string;
  order: number;
  title: string;
  goal: string;
  challenges: ChallengeSummary[];
}
export interface ChallengeSummary {
  id: string;
  order: number;
  title: string;
  concept: string;
}
