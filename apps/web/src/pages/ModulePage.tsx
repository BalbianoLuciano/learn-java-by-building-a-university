import { Suspense, lazy } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { ApiError, api } from '../api/client';
import { useApi } from '../api/useApi';
import { ErrorNotice, Loading } from '../components/Notice';
import { StateIcon } from '../components/StateIcon';
import { TopBar } from '../components/TopBar';
import { progressOf, useProgress } from '../state/progress';
import styles from './ModulePage.module.css';

const Miniature = lazy(() => import('../scene/MiniModel').then((m) => ({ default: m.MiniModel })));

export function ModulePage() {
  const { t } = useTranslation();
  const { moduleId = '' } = useParams();
  const [modules, retry] = useApi(() => api.modules(), 'modules');
  const challenges = useProgress((state) => state.challenges);
  const module =
    modules.status === 'ready' ? modules.data.modules.find((m) => m.id === moduleId) : undefined;

  return (
    <>
      <TopBar back={{ to: '/', label: t('common.back') }} />
      <main id="content" className={styles.page}>
        {modules.status === 'loading' && <Loading />}
        {modules.status === 'error' && <ErrorNotice error={modules.error} onRetry={retry} />}
        {modules.status === 'ready' && !module && (
          <ErrorNotice error={new ApiError(404, 'notFound')} />
        )}
        {module && (
          <>
            <p className={styles.kicker}>{t('module.kicker', { order: module.order })}</p>
            <h1 className={styles.title}>{module.title}</h1>
            <p className={styles.goal}>{module.goal}</p>
            <h2 className={styles.sectionTitle}>{t('module.challengesTitle')}</h2>
            <div className={styles.cardsArea}>
              <Suspense fallback={<Loading />}>
                <ol className={styles.cards}>
                  {module.challenges.map((challenge) => {
                    const progress = progressOf(challenges, challenge.id);
                    return (
                      <li key={challenge.id} className={styles.card}>
                        <Link className={styles.cardLink} to={`/desafios/${challenge.id}`}>
                          <Miniature pieces={challenge.pieces} className={styles.mini} />
                          <span className={styles.order}>
                            {t('module.challenge', { order: challenge.order })}
                          </span>
                          <span className={styles.name}>{challenge.title}</span>
                          <span className={styles.pieces}>
                            {t('module.pieces', { count: challenge.pieces.length })}
                          </span>
                          <span className={styles.status} data-status={progress.status}>
                            {progress.status !== 'pending' && <StateIcon state="passed" />}
                            {progress.status === 'pending'
                              ? t('status.pending')
                              : progress.status === 'completed'
                                ? t('status.completed')
                                : t('status.completedWithSolution')}
                          </span>
                        </Link>
                      </li>
                    );
                  })}
                </ol>
              </Suspense>
            </div>
          </>
        )}
      </main>
    </>
  );
}
