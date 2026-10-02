import { Suspense, lazy, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useParams } from 'react-router';
import { LogList } from '../components/result/LogList';
import { MiniCode } from '../components/result/MiniCode';
import { PiecesList } from '../components/result/PiecesList';
import type { Selection } from '../components/result/selection';
import { Timeline } from '../components/result/Timeline';
import { StateIcon } from '../components/StateIcon';
import { TopBar } from '../components/TopBar';
import { useResults } from '../state/results';
import { Loading } from '../components/Notice';
import styles from './ResultPage.module.css';

const SceneView = lazy(() => import('../scene/SceneView'));

export function ResultPage() {
  const { t } = useTranslation();
  const { challengeId = '' } = useParams();
  const run = useResults((state) => state.runs[challengeId]);
  const [selection, setSelection] = useState<Selection>({});

  if (!run) {
    // A reload loses the result: back to the editor.
    return <Navigate to={`/desafios/${challengeId}`} replace />;
  }
  const { result, sources } = run;
  const headline = t(`result.outcome.${result.outcome}`, {
    passed: result.progress.passed,
    total: result.progress.total,
  });

  return (
    <>
      <TopBar
        back={{ to: `/desafios/${challengeId}`, label: t('result.backToCode') }}
        center={
          <span className={styles.headline} aria-live="polite">
            <StateIcon state={result.outcome} />
            <span>
              {t('result.title')}: {headline}
            </span>
          </span>
        }
      />
      <main id="content" className={styles.layout}>
        <section className={styles.model} aria-labelledby="pieces-title">
          <h1 id="pieces-title" className={styles.visuallyHidden}>
            {t('result.pieces')}
          </h1>
          <div className={styles.scene}>
            <Suspense fallback={<Loading />}>
              <SceneView
                result={result}
                selection={selection}
                onSelect={setSelection}
                label={t('result.sceneLabel', { headline, count: result.pieces.length })}
              />
            </Suspense>
          </div>
          <details className={styles.piecesDetails}>
            <summary className={styles.summary}>{t('result.piecesList')}</summary>
            <PiecesList pieces={result.pieces} selection={selection} onSelect={setSelection} />
          </details>
          <h2 className={styles.sectionTitle}>{t('result.stdout')}</h2>
          {result.stdout ? (
            <pre className={styles.stdout}>{result.stdout}</pre>
          ) : (
            <p className={styles.muted}>{t('result.stdoutEmpty')}</p>
          )}
        </section>

        <aside className={styles.log} aria-labelledby="log-title">
          <h2 id="log-title" className={styles.sectionTitle}>
            {t('result.log')}
          </h2>
          <LogList log={result.log} selection={selection} onSelect={setSelection} />
          <h2 className={styles.sectionTitle}>{t('result.code')}</h2>
          <MiniCode sources={sources} sourceRef={selection.sourceRef} />
        </aside>

        <footer className={styles.footer}>
          <Timeline
            steps={result.timeline}
            current={selection.step}
            onSelect={(step) => {
              const chosen = result.timeline[step];
              setSelection({ step, sourceRef: chosen?.sourceRef, pieceId: chosen?.pieceId });
            }}
          />
        </footer>
      </main>
    </>
  );
}
