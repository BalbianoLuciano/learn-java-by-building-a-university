import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Inline } from '../components/Inline';
import styles from './Legend.module.css';

const ITEMS = [
  'blueprint',
  'object',
  'plaque',
  'window',
  'sign',
  'floors',
  'seal',
  'ghost',
] as const;

/** What each shape of the model means in Java (DESIGN.md §B4). */
export function Legend({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation();
  return (
    <div className={styles.legend} role="dialog" aria-labelledby="legend-title">
      <div className={styles.head}>
        <h3 id="legend-title" className={styles.title}>
          {t('legend.title')}
        </h3>
        <button
          type="button"
          className={styles.close}
          aria-label={t('legend.close')}
          onClick={onClose}
        >
          <X size={18} strokeWidth={1.75} aria-hidden />
        </button>
      </div>
      <dl className={styles.list}>
        {ITEMS.map((item) => (
          <div key={item} className={styles.item}>
            <dt className={styles.name} data-shape={item}>
              <span className={styles.swatch} aria-hidden />
              {t(`legend.items.${item}.name`)}
            </dt>
            <dd className={styles.text}>
              <Inline text={t(`legend.items.${item}.text`)} />
            </dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
