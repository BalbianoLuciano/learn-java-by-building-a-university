// Generates the TypeScript types in src/generated/ from the JSON Schemas.
// With --check it writes nothing and fails if the committed files are stale.
import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { compileFromFile } from 'json-schema-to-typescript';

const root = fileURLToPath(new URL('..', import.meta.url));
const schemas = [
  'trace',
  'result',
  'challenge',
  'module',
  'execution-request',
  'run-request',
  'module-list',
  'challenge-view',
  'hint',
  'solution',
];
const check = process.argv.includes('--check');

const stale = [];
for (const name of schemas) {
  const source = `${name}.schema.json`;
  const target = `src/generated/${name}.ts`;
  const generated = await compileFromFile(`${root}${source}`, {
    cwd: root,
    bannerComment: `// Generated from ${source} by scripts/generate.mjs. Do not edit.`,
    style: { printWidth: 100, singleQuote: true },
  });
  if (check) {
    const committed = await readFile(`${root}${target}`, 'utf8').catch(() => '');
    if (committed !== generated) stale.push(target);
  } else {
    await writeFile(`${root}${target}`, generated);
  }
}

if (stale.length > 0) {
  console.error(`Stale generated types: ${stale.join(', ')}. Run "pnpm generate" and commit.`);
  process.exit(1);
}
