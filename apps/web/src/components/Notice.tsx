import { useTranslation } from 'react-i18next';
import type { ApiError } from '../api/client';
import styles from './Notice.module.css';

/** Explains an api error and offers to try again. */
export function ErrorNotice({ error, onRetry }: { error: ApiError; onRetry?: () => void }) {
  const { t } = useTranslation();
  const messages: Record<string, string> = {
    network: t('errors.network'),
    notFound: t('errors.notFound'),
    challenge_not_found: t('errors.challenge_not_found'),
    rate_limited: t('errors.rate_limited'),
    run_in_progress: t('errors.run_in_progress'),
    runner_busy: t('errors.runner_busy'),
    runner_unavailable: t('errors.runner_unavailable'),
  };
  const message =
    messages[error.code] ??
    (error.status === 404 ? messages['notFound'] : undefined) ??
    t('errors.unknown');
  return (
    <div className={styles.notice} role="alert">
      <p className={styles.title}>{t('errors.title')}</p>
      <p>{message}</p>
      {onRetry && (
        <button type="button" className={styles.retry} onClick={onRetry}>
          {t('common.retry')}
        </button>
      )}
    </div>
  );
}

export function Loading() {
  const { t } = useTranslation();
  return (
    <p className={styles.loading} role="status">
      {t('common.loading')}
    </p>
  );
}
