import type { SourceRef } from '@ljbu/contracts';
import { useTranslation } from 'react-i18next';
import type { Sources } from '../../state/results';
import styles from './MiniCode.module.css';

/** Lines around the selected one, with that one highlighted (DESIGN.md §A2). */
export function MiniCode({ sources, sourceRef }: { sources: Sources; sourceRef?: SourceRef }) {
  const { t } = useTranslation();
  const CONTEXT = 3;
  if (!sourceRef || sources[sourceRef.file] === undefined) {
    return <p className={styles.hint}>{t('result.codeHint')}</p>;
  }
  const lines = sources[sourceRef.file]?.split('\n') ?? [];
  const first = Math.max(1, sourceRef.line - CONTEXT);
  const last = Math.min(lines.length, sourceRef.line + CONTEXT);
  const shown = [];
  for (let number = first; number <= last; number++) {
    shown.push({ number, text: lines[number - 1] ?? '' });
  }
  return (
    <figure className={styles.code}>
      <figcaption className={styles.file}>{sourceRef.file}</figcaption>
      <pre>
        {shown.map((line) => (
          <div
            key={line.number}
            className={styles.line}
            data-selected={line.number === sourceRef.line}
          >
            <span className={styles.number} aria-hidden>
              {line.number}
            </span>
            <code>{line.text || ' '}</code>
          </div>
        ))}
      </pre>
    </figure>
  );
}
