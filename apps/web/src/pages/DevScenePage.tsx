import type { Piece, RunResult } from '@ljbu/contracts';
import { Suspense, lazy, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Selection } from '../components/result/selection';
import { TopBar } from '../components/TopBar';
import styles from './ChallengePage.module.css';

const SceneView = lazy(() => import('../scene/SceneView'));

/**
 * A synthetic result like the one of challenge 1.5 (a Rectorado, the Facultades Regionales
 * in a ring, their variables) scaled up to the budget of DESIGN.md §B7, to measure the frame
 * rate. Only in development.
 */
function syntheticResult(faculties: number, extras: number): RunResult {
  const pieces: Piece[] = [
    {
      id: 'rectorado',
      archetype: 'rectorate',
      state: 'passed',
      built: true,
      label: 'Rectorado',
      sourceRef: { file: 'Main.java', line: 3 },
    },
  ];
  for (let i = 0; i < faculties; i++) {
    pieces.push({
      id: `fr-${String(i)}`,
      archetype: 'regional-faculty',
      state: i % 7 === 3 ? 'incomplete' : i % 11 === 5 ? 'failed' : 'passed',
      built: i % 13 !== 8,
      label: `FR ${String(i + 1)}`,
      sourceRef: { file: 'Main.java', line: 4 + i },
    });
  }
  for (let i = 0; i < extras; i++) {
    pieces.push({
      id: `bloque-${String(i)}`,
      archetype: 'generic-block',
      state: 'passed',
      built: true,
      label: `Bloque ${String(i + 1)}`,
      sourceRef: { file: 'Main.java', line: 40 + i },
    });
  }
  for (let i = 0; i < Math.min(faculties, 12); i++) {
    pieces.push({
      id: `var-fr${String(i)}`,
      archetype: 'variable-sign',
      state: 'passed',
      built: true,
      label: `fr${String(i)}`,
      sourceRef: { file: 'Main.java', line: 4 + i },
      target: `fr-${String(i)}`,
    });
  }
  pieces.push({
    id: 'var-nada',
    archetype: 'variable-sign',
    state: 'passed',
    built: true,
    label: 'nada',
    sourceRef: { file: 'Main.java', line: 2 },
  });
  return {
    runId: 'dev',
    outcome: 'incomplete',
    progress: { passed: 1, total: 2 },
    pieces,
    log: [],
    timeline: [],
    stdout: '',
    classes: [],
  };
}

export function DevScenePage() {
  const { t } = useTranslation();
  const [count, setCount] = useState(() => {
    const wanted = Number(new URLSearchParams(window.location.search).get('piezas'));
    return wanted >= 5 && wanted <= 150 ? wanted : 150;
  });
  const [selection, setSelection] = useState<Selection>({});
  const [fps, setFps] = useState(0);
  const result = useMemo(
    () => syntheticResult(Math.min(30, count - 1), Math.max(0, count - 1 - 30 - 13)),
    [count],
  );

  return (
    <>
      <TopBar back={{ to: '/', label: 'dev' }} center={<FpsMeter onFps={setFps} />} />
      <main id="content" className={styles.workbench}>
        <section className={styles.model}>
          <p>
            {t('dev.fps', { fps, pieces: result.pieces.length })} ·{' '}
            <label>
              {t('dev.pieces')}{' '}
              <input
                type="range"
                min={5}
                max={150}
                value={count}
                onChange={(event) => {
                  setCount(Number(event.target.value));
                }}
              />
            </label>
          </p>
          <div className={styles.scene} style={{ height: 'calc(100vh - 160px)' }}>
            <Suspense fallback={null}>
              <SceneView
                result={result}
                selection={selection}
                onSelect={setSelection}
                label="dev"
              />
            </Suspense>
          </div>
        </section>
      </main>
    </>
  );
}

/** Frames per second over the last second, from requestAnimationFrame. */
function FpsMeter({ onFps }: { onFps: (fps: number) => void }) {
  useEffect(() => {
    let frames = 0;
    let last = performance.now();
    let handle = 0;
    const tick = (now: number) => {
      frames++;
      if (now - last >= 1000) {
        onFps(Math.round((frames * 1000) / (now - last)));
        frames = 0;
        last = now;
      }
      handle = requestAnimationFrame(tick);
    };
    handle = requestAnimationFrame(tick);
    return () => {
      cancelAnimationFrame(handle);
    };
  }, [onFps]);
  return null;
}
