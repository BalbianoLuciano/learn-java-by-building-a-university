import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { ApiError, api } from '../api/client';
import { useApi } from '../api/useApi';
import { ErrorNotice, Loading } from '../components/Notice';
import { StateIcon } from '../components/StateIcon';
import { TopBar } from '../components/TopBar';
import { progressOf, useProgress } from '../state/progress';
import styles from './ModulePage.module.css';

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
            <h1 className={styles.title}>{module.title}</h1>
            <p className={styles.goal}>{module.goal}</p>
            <h2 className={styles.sectionTitle}>{t('module.challengesTitle')}</h2>
            <ol className={styles.list}>
              {module.challenges.map((challenge) => {
                const progress = progressOf(challenges, challenge.id);
                return (
                  <li key={challenge.id}>
                    <Link className={styles.challenge} to={`/desafios/${challenge.id}`}>
                      <span className={styles.order}>
                        {t('module.challenge', { order: challenge.order })}
                      </span>
                      <span className={styles.name}>{challenge.title}</span>
                      <span className={styles.status}>
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
          </>
        )}
      </main>
    </>
  );
}
