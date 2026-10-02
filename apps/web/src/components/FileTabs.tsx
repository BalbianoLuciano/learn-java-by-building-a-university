import { useTranslation } from 'react-i18next';
import styles from './FileTabs.module.css';

interface Props {
  files: { path: string; editable: boolean }[];
  active: string;
  onSelect: (path: string) => void;
}

/** One tab per file of the challenge (DESIGN.md §A3). */
export function FileTabs({ files, active, onSelect }: Props) {
  const { t } = useTranslation();
  return (
    <div className={styles.tabs} role="tablist">
      {files.map((file) => (
        <button
          key={file.path}
          type="button"
          role="tab"
          aria-selected={file.path === active}
          className={styles.tab}
          onClick={() => {
            onSelect(file.path);
          }}
        >
          {file.path}
          {!file.editable && <span className={styles.readOnly}> · {t('challenge.readOnly')}</span>}
        </button>
      ))}
    </div>
  );
}
