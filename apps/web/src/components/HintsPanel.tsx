import type { Solution } from '@ljbu/contracts';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError, api } from '../api/client';
import {
  hintAvailable,
  solutionAvailable,
  useProgress,
  type ChallengeProgress,
} from '../state/progress';
import buttons from './Buttons.module.css';
import { Markdown } from './Markdown';
import { ErrorNotice } from './Notice';
import styles from './HintsPanel.module.css';

interface Props {
  challengeId: string;
  hintCount: number;
  progress: ChallengeProgress;
}

/** Staged hints and, with confirmation, the solution (docs/FEEDBACK.md §7). */
export function HintsPanel({ challengeId, hintCount, progress }: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [hints, setHints] = useState<Record<number, string>>({});
  const [confirming, setConfirming] = useState(false);
  const [solution, setSolution] = useState<Solution | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const recordHint = useProgress((state) => state.recordHint);
  const recordSolution = useProgress((state) => state.recordSolution);

  const levels = Array.from({ length: hintCount }, (_, index) => index + 1);

  async function reveal(level: number) {
    try {
      const hint = await api.hint(challengeId, level);
      setHints((known) => ({ ...known, [level]: hint.text }));
      recordHint(challengeId, level);
      setError(null);
    } catch (failure) {
      setError(failure instanceof ApiError ? failure : new ApiError(0, 'unknown'));
    }
  }

  async function showSolution() {
    setConfirming(false);
    try {
      const loaded = await api.solution(challengeId);
      setSolution(loaded);
      recordSolution(challengeId);
      setError(null);
    } catch (failure) {
      setError(failure instanceof ApiError ? failure : new ApiError(0, 'unknown'));
    }
  }

  return (
    <section className={styles.panel} aria-labelledby="hints-title">
      <button
        type="button"
        className={buttons.secondary}
        aria-expanded={open}
        aria-controls="hints-content"
        id="hints-title"
        onClick={() => {
          setOpen((value) => !value);
        }}
      >
        {t('hints.ask')}
      </button>
      {open && (
        <div id="hints-content" className={styles.content}>
          {levels.map((level) => {
            const available = hintAvailable(progress, level);
            const text = hints[level];
            if (text !== undefined) {
              return (
                <p key={level} className={styles.hint}>
                  <strong>{t('hints.level', { level })}:</strong>{' '}
                  <Markdown source={text} className={styles.inline} />
                </p>
              );
            }
            return (
              <button
                key={level}
                type="button"
                className={styles.level}
                disabled={!available}
                onClick={() => void reveal(level)}
              >
                {available
                  ? t('hints.level', { level })
                  : level === 2
                    ? t('hints.lockedOne', { level })
                    : t('hints.locked', { level, runs: level - 1 })}
              </button>
            );
          })}
          {solution === null && !confirming && (
            <button
              type="button"
              className={styles.level}
              disabled={!solutionAvailable(progress)}
              onClick={() => {
                setConfirming(true);
              }}
            >
              {solutionAvailable(progress) ? t('hints.solution') : t('hints.solutionLocked')}
            </button>
          )}
          {confirming && (
            <div className={styles.confirm} role="group" aria-label={t('hints.solution')}>
              <p>{t('hints.solutionConfirm')}</p>
              <button type="button" className={buttons.primary} onClick={() => void showSolution()}>
                {t('hints.solutionYes')}
              </button>
              <button
                type="button"
                className={buttons.secondary}
                onClick={() => {
                  setConfirming(false);
                }}
              >
                {t('hints.solutionNo')}
              </button>
            </div>
          )}
          {solution && (
            <section className={styles.solution} aria-label={t('hints.solutionTitle')}>
              {solution.files.map((file) => (
                <pre key={file.path} className={styles.code}>
                  <code>
                    {`// ${file.path}\n`}
                    {file.content}
                  </code>
                </pre>
              ))}
              <Markdown source={solution.explanation} />
            </section>
          )}
          {error && <ErrorNotice error={error} />}
        </div>
      )}
    </section>
  );
}
