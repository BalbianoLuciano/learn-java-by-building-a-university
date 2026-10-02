import type { LogEntry } from '@ljbu/contracts';
import { useTranslation } from 'react-i18next';
import { Inline } from '../Inline';
import { LineChip } from '../LineChip';
import { StateIcon } from '../StateIcon';
import { sameLine, type Selection } from './selection';
import styles from './LogList.module.css';

interface Props {
  log: LogEntry[];
  selection: Selection;
  onSelect: (selection: Selection) => void;
}

/** The log: the textual equivalent of the model (DESIGN.md §A5). */
export function LogList({ log, selection, onSelect }: Props) {
  const { t } = useTranslation();
  if (log.length === 0) {
    return <p className={styles.empty}>{t('result.logEmpty')}</p>;
  }
  return (
    <ol className={styles.list}>
      {log.map((entry, index) => {
        const selected = selection.entry === index;
        const related = selection.pieceId !== undefined && selection.pieceId === entry.pieceId;
        return (
          <li
            key={index}
            className={styles.entry}
            data-state={entry.state}
            data-selected={selected}
            data-related={related}
            aria-current={selected ? 'true' : undefined}
          >
            <div className={styles.head}>
              <StateIcon state={entry.state} />
              <button
                type="button"
                className={styles.title}
                onClick={() => {
                  onSelect({ sourceRef: entry.sourceRef, pieceId: entry.pieceId, entry: index });
                }}
              >
                <Inline text={entry.title} />
              </button>
              {entry.sourceRef && (
                <LineChip
                  sourceRef={entry.sourceRef}
                  selected={selected && sameLine(selection.sourceRef, entry.sourceRef)}
                  onSelect={(sourceRef) => {
                    onSelect({ sourceRef, pieceId: entry.pieceId, entry: index });
                  }}
                />
              )}
            </div>
            <p className={styles.why}>
              <Inline text={entry.why} />
            </p>
            {entry.hint && (
              <p className={styles.hint}>
                <strong>{t('result.hint')}:</strong> <Inline text={entry.hint} />
              </p>
            )}
            {entry.detail && (
              <details className={styles.detail}>
                <summary>{t('result.detail')}</summary>
                <pre>{entry.detail}</pre>
              </details>
            )}
          </li>
        );
      })}
    </ol>
  );
}
