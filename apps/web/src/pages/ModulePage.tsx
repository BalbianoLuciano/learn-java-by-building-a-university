import { ArrowRight } from 'lucide-react';
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

/** The name of a concept, or its slug when the catalog does not know it. */
function conceptName(t: ReturnType<typeof useTranslation>['t'], concept: string): string {
  const key = `concepts.${concept}`;
  const name = t(key as 'concepts.classes-and-objects');
  return name === key ? concept.replaceAll('-', ' ') : name;
}

const Miniature = lazy(() => import('../scene/MiniModel').then((m) => ({ default: m.MiniModel })));

/** A module: its goal on the left, and its challenges as a route on the right (DESIGN.md §A2). */
export function ModulePage() {
  const { t } = useTranslation();
  const { moduleId = '' } = useParams();
  const [modules, retry] = useApi(() => api.modules(), 'modules');
  const challenges = useProgress((state) => state.challenges);
  const module =
    modules.status === 'ready' ? modules.data.modules.find((m) => m.id === moduleId) : undefined;
  const done = module
    ? module.challenges.filter((c) => progressOf(challenges, c.id).status !== 'pending').length
    : 0;

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
            <aside className={styles.intro}>
              <p className={styles.kicker}>{t('module.kicker', { order: module.order })}</p>
              <h1 className={styles.title}>{module.title}</h1>
              <p className={styles.goal}>{module.goal}</p>
              <div className={styles.progress}>
                <div
                  className={styles.progressBar}
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={module.challenges.length}
                  aria-valuenow={done}
                >
                  <span
                    className={styles.progressFill}
                    style={{ width: `${String((done / module.challenges.length) * 100)}%` }}
                  />
                </div>
                <span className={styles.progressText}>
                  {t('module.progress', { done, total: module.challenges.length })}
                </span>
              </div>
              <h2 className={styles.learnTitle}>{t('module.learn')}</h2>
              <ul className={styles.learn}>
                {module.challenges.map((challenge) => (
                  <li key={challenge.id}>{conceptName(t, challenge.concept)}</li>
                ))}
              </ul>
            </aside>

            <section className={styles.route} aria-labelledby="route-title">
              <h2 id="route-title" className={styles.routeTitle}>
                {t('module.pathTitle')}
              </h2>
              <ol className={styles.steps}>
                {module.challenges.map((challenge, index) => {
                  const progress = progressOf(challenges, challenge.id);
                  const last = index === module.challenges.length - 1;
                  const pending = progress.status === 'pending';
                  return (
                    <li key={challenge.id} className={styles.step} data-done={!pending}>
                      <span className={styles.marker} aria-hidden>
                        {pending ? index + 1 : <StateIcon state="passed" />}
                      </span>
                      <Link className={styles.stepLink} to={`/desafios/${challenge.id}`}>
                        <span className={styles.stepBody}>
                          <span className={styles.order}>
                            {t('module.challenge', { order: challenge.order })}
                            {last && (
                              <span className={styles.integrator}> · {t('module.integrator')}</span>
                            )}
                          </span>
                          <span className={styles.name}>{challenge.title}</span>
                          <span className={styles.pieces}>
                            {t('module.pieces', { count: challenge.pieces.length })}
                          </span>
                          <span className={styles.status} data-status={progress.status}>
                            {pending
                              ? t('status.pending')
                              : progress.status === 'completed'
                                ? t('status.completed')
                                : t('status.completedWithSolution')}
                          </span>
                          <span className={styles.cta}>
                            {pending ? t('module.start') : t('module.again')}
                            <ArrowRight size={16} strokeWidth={1.75} aria-hidden />
                          </span>
                        </span>
                        <Suspense fallback={<span className={styles.mini} />}>
                          <Miniature pieces={challenge.pieces} className={styles.mini} />
                        </Suspense>
                      </Link>
                    </li>
                  );
                })}
              </ol>
            </section>
          </>
        )}
      </main>
    </>
  );
}
