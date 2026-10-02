import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { api } from '../api/client';
import { useApi } from '../api/useApi';
import { ErrorNotice, Loading } from '../components/Notice';
import { TopBar } from '../components/TopBar';
import { progressOf, useProgress } from '../state/progress';
import styles from './HomePage.module.css';

export function HomePage() {
  const { t } = useTranslation();
  const [modules, retry] = useApi(() => api.modules(), 'modules');
  const challenges = useProgress((state) => state.challenges);

  return (
    <>
      <TopBar />
      <main id="content" className={styles.page}>
        <section className={styles.intro}>
          <h1 className={styles.title}>{t('home.title')}</h1>
          <p className={styles.lead}>{t('home.lead')}</p>
        </section>

        <section aria-labelledby="modules-title">
          <h2 id="modules-title" className={styles.sectionTitle}>
            {t('home.modulesTitle')}
          </h2>
          {modules.status === 'loading' && <Loading />}
          {modules.status === 'error' && <ErrorNotice error={modules.error} onRetry={retry} />}
          {modules.status === 'ready' && (
            <ul className={styles.modules}>
              {modules.data.modules.map((module) => {
                const done = module.challenges.filter(
                  (challenge) => progressOf(challenges, challenge.id).status !== 'pending',
                ).length;
                return (
                  <li key={module.id} className={styles.module}>
                    <Link className={styles.moduleLink} to={`/modulos/${module.id}`}>
                      <span className={styles.moduleOrder}>{module.order}</span>
                      <span className={styles.moduleBody}>
                        <span className={styles.moduleTitle}>{module.title}</span>
                        <span className={styles.moduleGoal}>{module.goal}</span>
                        <span className={styles.moduleProgress}>
                          {t('home.moduleProgress', { done, total: module.challenges.length })}
                        </span>
                      </span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </section>

        <p className={styles.disclaimer}>{t('home.disclaimer')}</p>
      </main>
    </>
  );
}
