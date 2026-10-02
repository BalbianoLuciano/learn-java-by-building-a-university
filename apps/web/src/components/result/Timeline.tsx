import type { TimelineStep } from '@ljbu/contracts';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import type { KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import styles from './Timeline.module.css';

interface Props {
  steps: TimelineStep[];
  current?: number;
  onSelect: (step: number) => void;
}

/** Step by step through the execution; ← and → move when it has focus (DESIGN.md §A5). */
export function Timeline({ steps, current, onSelect }: Props) {
  const { t } = useTranslation();
  if (steps.length === 0) {
    return <p className={styles.empty}>{t('timeline.empty')}</p>;
  }
  const index = current ?? -1;
  const step = index >= 0 ? steps[index] : undefined;

  function move(delta: number) {
    const next = Math.min(steps.length - 1, Math.max(0, index + delta));
    onSelect(next);
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault();
      move(event.key === 'ArrowLeft' ? -1 : 1);
    }
  }

  return (
    <div
      className={styles.timeline}
      role="group"
      aria-label={t('timeline.title')}
      tabIndex={0}
      onKeyDown={onKeyDown}
    >
      <button
        type="button"
        className={styles.arrow}
        aria-label={t('timeline.previous')}
        disabled={index <= 0}
        onClick={() => {
          move(-1);
        }}
      >
        <ChevronLeft size={18} strokeWidth={1.75} aria-hidden />
      </button>
      <input
        type="range"
        className={styles.track}
        min={0}
        max={steps.length - 1}
        value={Math.max(0, index)}
        aria-label={t('timeline.title')}
        aria-valuetext={step ? describe(step) : ''}
        onChange={(event) => {
          onSelect(Number(event.target.value));
        }}
      />
      <button
        type="button"
        className={styles.arrow}
        aria-label={t('timeline.next')}
        disabled={index >= steps.length - 1}
        onClick={() => {
          move(1);
        }}
      >
        <ChevronRight size={18} strokeWidth={1.75} aria-hidden />
      </button>
      <p className={styles.status} aria-live="polite">
        {step
          ? `${t('timeline.position', { current: index + 1, total: steps.length, file: step.sourceRef.file, line: step.sourceRef.line })} · ${describe(step)}`
          : t('timeline.help')}
      </p>
    </div>
  );

  function describe(s: TimelineStep): string {
    const key = s.event === 'exception' && s.caught ? 'exceptionCaught' : s.event;
    return t(`timeline.event.${key}`, { name: s.name ?? '' });
  }
}
