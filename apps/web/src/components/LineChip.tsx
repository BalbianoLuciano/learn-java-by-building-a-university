import type { SourceRef } from '@ljbu/contracts';
import { useTranslation } from 'react-i18next';
import styles from './LineChip.module.css';

interface Props {
  sourceRef: SourceRef;
  selected?: boolean;
  onSelect: (sourceRef: SourceRef) => void;
}

/** "Main.java:12": selecting it shows the line (DESIGN.md §A3). */
export function LineChip({ sourceRef, selected = false, onSelect }: Props) {
  const { t } = useTranslation();
  return (
    <button
      type="button"
      className={styles.chip}
      aria-pressed={selected}
      aria-label={t('common.line', { line: sourceRef.line, file: sourceRef.file })}
      onClick={() => {
        onSelect(sourceRef);
      }}
    >
      {t('common.lineChip', { line: sourceRef.line, file: sourceRef.file })}
    </button>
  );
}
