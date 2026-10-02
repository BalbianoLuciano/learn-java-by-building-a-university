import type { State } from '@ljbu/contracts';
import { useTranslation } from 'react-i18next';
import { StateCard } from '../components/StateCard';
import styles from './HomePage.module.css';

const STATES: State[] = ['passed', 'incomplete', 'failed'];
const REPOSITORY_URL = 'https://github.com/BalbianoLuciano/learn-java-by-building-a-university';

export function HomePage() {
  const { t } = useTranslation();

  return (
    <main className={styles.page}>
      <section className={styles.intro}>
        <h1 className={styles.title}>{t('home.title')}</h1>
        <p className={styles.lead}>{t('home.lead')}</p>
        <p className={styles.notice}>{t('home.notice')}</p>
        <a className={styles.primaryAction} href={REPOSITORY_URL}>
          {t('home.repoLink')}
        </a>
      </section>

      <section aria-labelledby="states-title">
        <h2 id="states-title" className={styles.sectionTitle}>
          {t('home.statesTitle')}
        </h2>
        <ul className={styles.states}>
          {STATES.map((state) => (
            <StateCard key={state} state={state} />
          ))}
        </ul>
      </section>

      <p className={styles.disclaimer}>{t('home.disclaimer')}</p>
    </main>
  );
}
