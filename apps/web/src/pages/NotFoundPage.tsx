import { ArrowLeft } from 'lucide-react';
import { Suspense, lazy } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import buttons from '../components/Buttons.module.css';
import { Inline } from '../components/Inline';
import { TopBar } from '../components/TopBar';
import styles from './NotFoundPage.module.css';

const NotFoundScene = lazy(() =>
  import('../scene/NotFoundScene').then((m) => ({ default: m.NotFoundScene })),
);

/** 404: the address points to null, and the building that should be here fell down. */
export function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <>
      <TopBar back={{ to: '/', label: t('common.back') }} />
      <main id="content" className={styles.page}>
        <Suspense fallback={<div className={styles.scene} />}>
          <NotFoundScene className={styles.scene} />
        </Suspense>
        <div className={styles.text}>
          <p className={styles.code}>
            <code>{t('notFound.code')}</code>
          </p>
          <h1 className={styles.title}>{t('notFound.title')}</h1>
          <p className={styles.lead}>
            <Inline text={t('notFound.lead')} />
          </p>
          <p className={styles.hint}>
            <Inline text={t('notFound.hint')} />
          </p>
          <div className={styles.actions}>
            <Link className={buttons.primary} to="/">
              <ArrowLeft size={18} strokeWidth={1.75} aria-hidden />
              {t('notFound.home')}
            </Link>
            <Link className={styles.secondary} to="/modulos/m1">
              {t('notFound.modules')}
            </Link>
          </div>
        </div>
      </main>
    </>
  );
}
