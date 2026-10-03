import type { ChallengeView } from '@ljbu/contracts';
import { Play } from 'lucide-react';
import { Suspense, lazy, useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { ApiError, api } from '../api/client';
import { useApi } from '../api/useApi';
import buttons from '../components/Buttons.module.css';
import { Criteria } from '../components/Criteria';
import { FileTabs } from '../components/FileTabs';
import { HintsPanel } from '../components/HintsPanel';
import { Markdown } from '../components/Markdown';
import { ErrorNotice, Loading } from '../components/Notice';
import { TopBar } from '../components/TopBar';
import { useAttempts } from '../state/attempts';
import { progressOf, useProgress } from '../state/progress';
import { useResults } from '../state/results';
import { useTheme } from '../theme/useTheme';
import styles from './ChallengePage.module.css';

const CodeEditor = lazy(() => import('../components/CodeEditor'));

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

function Workbench({ challenge }: { challenge: ChallengeView }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
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

  const files = challenge.files.map((file) => ({
    ...file,
    content: attempt?.[file.path] ?? file.content,
  }));
  const activeFile = files.find((file) => file.path === active) ?? files[0];

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
      await navigate(`/desafios/${challenge.id}/resultado`);
    } catch (failure) {
      setError(failure instanceof ApiError ? failure : new ApiError(0, 'unknown'));
    } finally {
      setRunning(false);
    }
    // files changes on every keystroke; the latest ones are read when the run starts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [challenge.id, running, attempt, navigate, recordRun, setRun]);

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

  return (
    <>
      <TopBar
        back={{ to: `/modulos/${challenge.module}`, label: module?.title ?? t('common.back') }}
        center={position}
      />
      <main id="content" className={styles.workbench}>
        <section className={styles.brief} aria-labelledby="brief-title">
          <p className={styles.kicker}>{t('challenge.brief')}</p>
          <h1 id="brief-title" className={styles.title}>
            {challenge.title}
          </h1>
          <Markdown source={challenge.brief} className={styles.text} />
          <h2 className={styles.subtitle}>{t('challenge.goals')}</h2>
          <Criteria criteria={challenge.criteria} result={lastRun?.result} />
          {challenge.rules.length > 0 && (
            <>
              <h2 className={styles.subtitle}>{t('challenge.rules')}</h2>
              <ul className={styles.rules}>
                {challenge.rules.map((rule) => (
                  <li key={rule.id}>
                    {rule.statement}{' '}
                    <a className={styles.source} href={rule.url} target="_blank" rel="noreferrer">
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
        </section>

        <section className={styles.editor} aria-label={t('challenge.editor')}>
          <FileTabs files={files} active={activeFile?.path ?? ''} onSelect={setActive} />
          <div className={styles.monaco}>
            {activeFile && (
              <Suspense fallback={<Loading />}>
                <CodeEditor
                  path={activeFile.path}
                  value={activeFile.content}
                  readOnly={!activeFile.editable}
                  theme={theme}
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
              {lastRun && (
                <Link className={styles.textButton} to={`/desafios/${challenge.id}/resultado`}>
                  {t('challenge.lastResult')}
                </Link>
              )}
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
      </main>
    </>
  );
}
