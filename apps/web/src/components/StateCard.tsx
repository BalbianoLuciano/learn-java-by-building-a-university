import type { State } from '@ljbu/contracts';
import { CircleCheck, CircleX, Construction, type LucideIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import styles from './StateCard.module.css';

const ICONS: Record<State, LucideIcon> = {
  passed: CircleCheck,
  incomplete: Construction,
  failed: CircleX,
};

export function StateCard({ state }: { state: State }) {
  const { t } = useTranslation();
  const Icon = ICONS[state];

  return (
    <li className={styles.card}>
      <span className={styles.icon} data-state={state}>
        <Icon size={18} strokeWidth={1.75} aria-hidden />
      </span>
      <div>
        <h3 className={styles.title}>{t(`state.${state}.title`)}</h3>
        <p className={styles.description}>{t(`state.${state}.description`)}</p>
      </div>
    </li>
  );
}
