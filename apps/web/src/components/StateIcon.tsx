import type { State } from '@ljbu/contracts';
import { CircleCheck, CircleX, Construction, type LucideIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import styles from './StateIcon.module.css';

const ICONS: Record<State, LucideIcon> = {
  passed: CircleCheck,
  incomplete: Construction,
  failed: CircleX,
};

/** The icon of a state (DESIGN.md §A3), with its name for screen readers. */
export function StateIcon({ state, size = 18 }: { state: State; size?: number }) {
  const { t } = useTranslation();
  const Icon = ICONS[state];
  return (
    <span className={styles.icon} data-state={state} role="img" aria-label={t(`state.${state}`)}>
      <Icon size={size} strokeWidth={1.75} aria-hidden />
    </span>
  );
}
