import type { ChallengeView } from '@ljbu/contracts';
import { ChevronDown, Play } from 'lucide-react';
import { Suspense, lazy, useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { ApiError, api } from '../api/client';
import { useApi } from '../api/useApi';
import buttons from '../components/Buttons.module.css';
import { Criteria } from '../components/Criteria';
import { FileTabs } from '../components/FileTabs';
import { HintsPanel } from '../components/HintsPanel';
import { Markdown } from '../components/Markdown';
import { ErrorNotice, Loading } from '../components/Notice';
import { LogList } from '../components/result/LogList';
import { PiecesList } from '../components/result/PiecesList';
import type { Selection } from '../components/result/selection';
import { Timeline } from '../components/result/Timeline';
import { StateIcon } from '../components/StateIcon';
import { TopBar } from '../components/TopBar';
import { Legend } from '../scene/Legend';
import { useAttempts } from '../state/attempts';
import { progressOf, useProgress } from '../state/progress';
import { useResults } from '../state/results';
import { useTheme } from '../theme/useTheme';
import styles from './ChallengePage.module.css';

const CodeEditor = lazy(() => import('../components/CodeEditor'));
const SceneView = lazy(() => import('../scene/SceneView'));

export function ChallengePage() {
  const { challengeId = '' } = useParams();
  const [challenge, retry] = useApi(() => api.challenge(challengeId), challengeId);
  return (
    <>
      {challenge.status === 'loading' && (
        <>
          <TopBar back={{ to: '/', label: '' }} />
          <main id="content" className={styles.loading}>
            <Loading />
          </main>
        </>
      )}
      {challenge.status === 'error' && (
        <>
          <TopBar back={{ to: '/', label: '' }} />
          <main id="content" className={styles.loading}>
            <ErrorNotice error={challenge.error} onRetry={retry} />
          </main>
        </>
      )}
      {challenge.status === 'ready' && <Workbench challenge={challenge.data} />}
    </>
  );
}

/** One screen: the brief, the code, the model and the log, running and watching in place (DESIGN.md §A2). */
function Workbench({ challenge }: { challenge: ChallengeView }) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const [modules] = useApi(() => api.modules(), 'modules');
  const attempt = useAttempts((state) => state.files[challenge.id]);
  const setFile = useAttempts((state) => state.setFile);
  const resetAttempt = useAttempts((state) => state.reset);
  const progress = useProgress((state) => progressOf(state.challenges, challenge.id));
  const recordRun = useProgress((state) => state.recordRun);
  const lastRun = useResults((state) => state.runs[challenge.id]);
  const setRun = useResults((state) => state.setRun);
  const [active, setActive] = useState(
    challenge.files.find((file) => file.editable)?.path ?? challenge.files[0]?.path ?? '',
  );
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  const [selection, setSelection] = useState<Selection>({});
  // The brief opens on its own until the first run; after that it stays folded.
  const [briefOpen, setBriefOpen] = useState(lastRun === undefined);
  const [legendOpen, setLegendOpen] = useState(false);

  const files = challenge.files.map((file) => ({
    ...file,
    content: attempt?.[file.path] ?? file.content,
  }));
  const activeFile = files.find((file) => file.path === active) ?? files[0];

  const select = useCallback(
    (next: Selection) => {
      setSelection(next);
      if (next.sourceRef && files.some((file) => file.path === next.sourceRef?.file)) {
        setActive(next.sourceRef.file);
      }
    },
    // files is rebuilt on every render; only the paths matter here.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [challenge.files],
  );

  const run = useCallback(async () => {
    if (running) {
      return;
    }
    setRunning(true);
    setError(null);
    try {
      const result = await api.run({
        challengeId: challenge.id,
        files: files
          .filter((file) => file.editable)
          .map(({ path, content }) => ({ path, content })),
      });
      setRun(challenge.id, {
        result,
        sources: Object.fromEntries(files.map((file) => [file.path, file.content])),
      });
      recordRun(challenge.id, result.outcome === 'passed');
      setSelection({});
      setBriefOpen(false);
    } catch (failure) {
      setError(failure instanceof ApiError ? failure : new ApiError(0, 'unknown'));
    } finally {
      setRunning(false);
    }
    // files changes on every keystroke; the latest ones are read when the run starts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [challenge.id, running, attempt, recordRun, setRun]);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        void run();
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [run]);

  const module =
    modules.status === 'ready'
      ? modules.data.modules.find((m) => m.id === challenge.module)
      : undefined;
  const position = module
    ? `${module.title} · ${t('challenge.position', { order: challenge.order, total: module.challenges.length })}`
    : undefined;

  const result = lastRun?.result;
  const passed = new Set(
    (result?.log ?? [])
      .filter((entry) => entry.state === 'passed' && entry.checkId)
      .map((entry) => entry.checkId),
  );
  const criteriaDone = challenge.criteria.filter((criterion) =>
    criterion.checks.every((check) => passed.has(check)),
  ).length;
  const headline = result
    ? t(`result.outcome.${result.outcome}`, {
        passed: result.progress.passed,
        total: result.progress.total,
      })
    : undefined;
  const highlightLine =
    selection.sourceRef && selection.sourceRef.file === activeFile?.path
      ? selection.sourceRef.line
      : undefined;

  return (
    <>
      <TopBar
        back={{ to: `/modulos/${challenge.module}`, label: module?.title ?? t('common.back') }}
        center={position}
      />
      <main id="content" className={styles.workbench}>
        <section className={styles.work} aria-label={t('challenge.editor')}>
          <details
            className={styles.brief}
            open={briefOpen}
            onToggle={(event) => {
              setBriefOpen(event.currentTarget.open);
            }}
          >
            <summary className={styles.briefSummary}>
              <ChevronDown size={18} strokeWidth={1.75} aria-hidden className={styles.chevron} />
              <span className={styles.kicker}>{t('challenge.brief')}</span>
              <span className={styles.briefTitle}>{challenge.title}</span>
              <span className={styles.briefProgress}>
                {t('challenge.criteriaProgress', {
                  done: criteriaDone,
                  total: challenge.criteria.length,
                })}
              </span>
            </summary>
            <div className={styles.briefBody}>
              <h1 className={styles.title}>{challenge.title}</h1>
              <Markdown source={challenge.brief} className={styles.text} />
              <h2 className={styles.subtitle}>{t('challenge.goals')}</h2>
              <Criteria criteria={challenge.criteria} result={result} />
              {challenge.rules.length > 0 && (
                <>
                  <h2 className={styles.subtitle}>{t('challenge.rules')}</h2>
                  <ul className={styles.rules}>
                    {challenge.rules.map((rule) => (
                      <li key={rule.id}>
                        {rule.statement}{' '}
                        <a
                          className={styles.source}
                          href={rule.url}
                          target="_blank"
                          rel="noreferrer"
                        >
                          ({rule.source})
                        </a>
                      </li>
                    ))}
                  </ul>
                </>
              )}
              {(challenge.realReference.regionalFaculties.length > 0 ||
                challenge.realReference.governingBodies.length > 0) && (
                <>
                  <h2 className={styles.subtitle}>{t('challenge.realReference')}</h2>
                  <ul className={styles.rules}>
                    {challenge.realReference.regionalFaculties.map((faculty) => (
                      <li key={faculty.id}>
                        {t('challenge.realFaculty', {
                          name: faculty.name,
                          city: faculty.city,
                          province: faculty.province,
                        })}
                      </li>
                    ))}
                    {challenge.realReference.governingBodies.map((body) => (
                      <li key={body.id}>
                        {body.name}: {body.composition}
                        {body.mandateInYears !== null && body.mandateInYears !== undefined
                          ? ` ${t('challenge.mandate', { years: body.mandateInYears })}`
                          : ''}
                      </li>
                    ))}
                  </ul>
                </>
              )}
              <HintsPanel
                challengeId={challenge.id}
                hintCount={challenge.hintCount}
                progress={progress}
              />
            </div>
          </details>

          <FileTabs files={files} active={activeFile?.path ?? ''} onSelect={setActive} />
          <div className={styles.monaco}>
            {activeFile && (
              <Suspense fallback={<Loading />}>
                <CodeEditor
                  path={activeFile.path}
                  value={activeFile.content}
                  readOnly={!activeFile.editable}
                  theme={theme}
                  highlightLine={highlightLine}
                  onChange={(content) => {
                    setFile(challenge.id, activeFile.path, content);
                  }}
                  onRun={() => void run()}
                />
              </Suspense>
            )}
          </div>
          <div className={styles.actions}>
            <div className={styles.secondaryActions}>
              <button
                type="button"
                className={styles.textButton}
                onClick={() => {
                  resetAttempt(challenge.id);
                }}
              >
                {t('challenge.resetCode')}
              </button>
            </div>
            <span className={styles.shortcut}>{t('challenge.runShortcut')}</span>
            <button
              type="button"
              className={buttons.primary}
              disabled={running}
              onClick={() => void run()}
            >
              <Play size={18} strokeWidth={1.75} aria-hidden />
              {running ? t('challenge.running') : t('challenge.run')}
            </button>
          </div>
          {error && (
            <div className={styles.error}>
              <ErrorNotice error={error} />
            </div>
          )}
        </section>

        <section className={styles.model} aria-labelledby="model-title">
          <h2 id="model-title" className={styles.visuallyHidden}>
            {t('result.pieces')}
          </h2>
          <div className={styles.scene}>
            {result ? (
              <Suspense fallback={<Loading />}>
                <SceneView
                  result={result}
                  selection={selection}
                  onSelect={select}
                  label={t('result.sceneLabel', {
                    headline: headline ?? '',
                    count: result.pieces.length,
                  })}
                />
              </Suspense>
            ) : (
              <p className={styles.sceneEmpty}>{t('result.sceneEmpty')}</p>
            )}
            <button
              type="button"
              className={styles.legendButton}
              aria-expanded={legendOpen}
              onClick={() => {
                setLegendOpen((open) => !open);
              }}
            >
              {t('legend.open')}
            </button>
            {legendOpen && (
              <Legend
                onClose={() => {
                  setLegendOpen(false);
                }}
              />
            )}
          </div>
          {result && (
            <details className={styles.piecesDetails}>
              <summary className={styles.summary}>{t('result.piecesList')}</summary>
              <PiecesList pieces={result.pieces} selection={selection} onSelect={select} />
            </details>
          )}
        </section>

        <aside className={styles.log} aria-labelledby="log-title">
          <div className={styles.logHead}>
            <h2 id="log-title" className={styles.sectionTitle}>
              {t('result.log')}
            </h2>
            {result && (
              <span className={styles.headline} aria-live="polite">
                <StateIcon state={result.outcome} />
                <span>{headline}</span>
              </span>
            )}
          </div>
          {result ? (
            <>
              <LogList log={result.log} selection={selection} onSelect={select} />
              {result.stdout && (
                <details className={styles.piecesDetails}>
                  <summary className={styles.summary}>{t('result.stdout')}</summary>
                  <pre className={styles.stdout}>{result.stdout}</pre>
                </details>
              )}
              <div className={styles.timeline}>
                <Timeline
                  steps={result.timeline}
                  current={selection.step}
                  onSelect={(step) => {
                    const chosen = result.timeline[step];
                    select({ step, sourceRef: chosen?.sourceRef, pieceId: chosen?.pieceId });
                  }}
                />
              </div>
            </>
          ) : (
            <p className={styles.muted}>{t('result.logBeforeRun')}</p>
          )}
        </aside>
      </main>
    </>
  );
}
