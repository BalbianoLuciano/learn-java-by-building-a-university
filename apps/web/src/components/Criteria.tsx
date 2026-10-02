import type { ChallengeView, RunResult } from '@ljbu/contracts';
import { Square, SquareCheck } from 'lucide-react';
import { Inline } from './Inline';
import styles from './Criteria.module.css';

interface Props {
  criteria: ChallengeView['criteria'];
  /** The last result, whose passed checks tick the criteria. */
  result?: RunResult;
}

/** "Lo que tenés que lograr": each item ticks itself when all its checks pass. */
export function Criteria({ criteria, result }: Props) {
  const passed = new Set(
    (result?.log ?? [])
      .filter((entry) => entry.state === 'passed' && entry.checkId)
      .map((entry) => entry.checkId),
  );
  return (
    <ul className={styles.list}>
      {criteria.map((criterion) => {
        const done = criterion.checks.every((check) => passed.has(check));
        const Icon = done ? SquareCheck : Square;
        return (
          <li key={criterion.text} className={styles.item} data-done={done}>
            <Icon size={18} strokeWidth={1.75} aria-hidden />
            <span>
              <Inline text={criterion.text} />
            </span>
          </li>
        );
      })}
    </ul>
  );
}
