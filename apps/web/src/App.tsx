import { useTranslation } from 'react-i18next';
import { ThemeToggle } from './components/ThemeToggle';
import { HomePage } from './pages/HomePage';
import styles from './App.module.css';

export function App() {
  const { t } = useTranslation();

  return (
    <>
      <header className={styles.bar}>
        <span className={styles.name}>{t('app.name')}</span>
        <ThemeToggle />
      </header>
      <HomePage />
    </>
  );
}
