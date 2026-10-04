import { ArrowRight, Code2, Eye, Play } from 'lucide-react';
import { Suspense, lazy, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { api } from '../api/client';
import { useApi } from '../api/useApi';
import buttons from '../components/Buttons.module.css';
import { Inline } from '../components/Inline';
import { ErrorNotice, Loading } from '../components/Notice';
import { TopBar } from '../components/TopBar';
import { progressOf, useProgress } from '../state/progress';
import styles from './HomePage.module.css';

const DemoScene = lazy(() => import('../scene/DemoScene').then((m) => ({ default: m.DemoScene })));
const Miniatures = lazy(() =>
  import('../scene/MiniModel').then((m) => ({ default: m.MiniModels })),
);
const Miniature = lazy(() => import('../scene/MiniModel').then((m) => ({ default: m.MiniModel })));

const STEPS = [
  { key: 'write', Icon: Code2 },
  { key: 'run', Icon: Play },
  { key: 'see', Icon: Eye },
] as const;

const SHAPES = [
  'blueprint',
  'object',
  'tag',
  'bubble',
  'attached',
  'floors',
  'seal',
  'ghost',
] as const;

/** The landing page: the model at work behind the title, then how it works and the modules. */
export function HomePage() {
  const { t } = useTranslation();
  const [modules, retry] = useApi(() => api.modules(), 'modules');
  const challenges = useProgress((state) => state.challenges);
  const cards = useRef<HTMLDivElement>(null);
  const first = modules.status === 'ready' ? modules.data.modules[0]?.challenges[0] : undefined;

  return (
    <>
      <TopBar />
      <main id="content" className={styles.page}>
        <section className={styles.hero} aria-labelledby="hero-title">
          <div className={styles.heroScene}>
            <Suspense fallback={null}>
              <DemoScene />
            </Suspense>
          </div>
          <div className={styles.heroText}>
            <p className={styles.kicker}>{t('home.kicker')}</p>
            <h1 id="hero-title" className={styles.title}>
              {t('home.title')}
            </h1>
            <p className={styles.lead}>{t('home.lead')}</p>
            <div className={styles.heroActions}>
              <Link
                className={buttons.primary}
                to={first ? `/desafios/${first.id}` : '/modulos/m1'}
              >
                {t('home.start')}
                <ArrowRight size={18} strokeWidth={1.75} aria-hidden />
              </Link>
              <a className={styles.secondaryLink} href="#modules-title">
                {t('home.seeModules')}
              </a>
            </div>
          </div>
        </section>

        <section className={styles.section} aria-labelledby="how-title">
          <h2 id="how-title" className={styles.sectionTitle}>
            {t('home.howTitle')}
          </h2>
          <ol className={styles.steps}>
            {STEPS.map(({ key, Icon }, index) => (
              <li key={key} className={styles.step}>
                <span className={styles.stepIcon}>
                  <Icon size={22} strokeWidth={1.75} aria-hidden />
                </span>
                <span className={styles.stepNumber}>{index + 1}</span>
                <span className={styles.stepTitle}>{t(`home.steps.${key}.title`)}</span>
                <span className={styles.stepText}>
                  <Inline text={t(`home.steps.${key}.text`)} />
                </span>
              </li>
            ))}
          </ol>
        </section>

        <section className={styles.section} aria-labelledby="modules-title">
          <h2 id="modules-title" className={styles.sectionTitle}>
            {t('home.modulesTitle')}
          </h2>
          {modules.status === 'loading' && <Loading />}
          {modules.status === 'error' && <ErrorNotice error={modules.error} onRetry={retry} />}
          {modules.status === 'ready' && (
            <div ref={cards} className={styles.cardsArea}>
              <Suspense fallback={<Loading />}>
                <Miniatures container={cards}>
                  <ol className={styles.modules}>
                    {modules.data.modules.map((module) => {
                      const done = module.challenges.filter(
                        (challenge) => progressOf(challenges, challenge.id).status !== 'pending',
                      ).length;
                      const last = module.challenges[module.challenges.length - 1];
                      return (
                        <li key={module.id} className={styles.module}>
                          <Link className={styles.moduleLink} to={`/modulos/${module.id}`}>
                            <Miniature pieces={last?.pieces ?? []} className={styles.moduleMini} />
                            <span className={styles.moduleOrder}>
                              {t('home.moduleOrder', { order: module.order })}
                            </span>
                            <span className={styles.moduleTitle}>{module.title}</span>
                            <span className={styles.moduleGoal}>{module.goal}</span>
                            <span className={styles.moduleProgress}>
                              {t('home.moduleProgress', { done, total: module.challenges.length })}
                            </span>
                          </Link>
                        </li>
                      );
                    })}
                  </ol>
                </Miniatures>
              </Suspense>
            </div>
          )}
        </section>

        <section className={styles.section} aria-labelledby="shapes-title">
          <h2 id="shapes-title" className={styles.sectionTitle}>
            {t('home.shapesTitle')}
          </h2>
          <p className={styles.sectionLead}>{t('home.shapesLead')}</p>
          <dl className={styles.shapes}>
            {SHAPES.map((shape) => (
              <div key={shape} className={styles.shape}>
                <dt className={styles.shapeName} data-shape={shape}>
                  <span className={styles.swatch} aria-hidden />
                  {t(`legend.items.${shape}.name`)}
                </dt>
                <dd className={styles.shapeText}>
                  <Inline text={t(`legend.items.${shape}.text`)} />
                </dd>
              </div>
            ))}
          </dl>
        </section>

        <section className={styles.section} aria-labelledby="utn-title">
          <h2 id="utn-title" className={styles.sectionTitle}>
            {t('home.utnTitle')}
          </h2>
          <p className={styles.sectionLead}>
            <Inline text={t('home.utnText')} />
          </p>
        </section>

        <p className={styles.disclaimer}>{t('home.disclaimer')}</p>
      </main>
    </>
  );
}
