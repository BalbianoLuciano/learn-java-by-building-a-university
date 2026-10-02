import { useTranslation } from 'react-i18next';
import { Route, Routes } from 'react-router';
import { ChallengePage } from './pages/ChallengePage';
import { HomePage } from './pages/HomePage';
import { ModulePage } from './pages/ModulePage';
import { NotFoundPage } from './pages/NotFoundPage';
import { ResultPage } from './pages/ResultPage';
import styles from './App.module.css';

/** The routes of docs/ARCHITECTURE.md §8. */
export function App() {
  const { t } = useTranslation();
  return (
    <>
      <a className={styles.skip} href="#content">
        {t('app.skipToContent')}
      </a>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/modulos/:moduleId" element={<ModulePage />} />
        <Route path="/desafios/:challengeId" element={<ChallengePage />} />
        <Route path="/desafios/:challengeId/resultado" element={<ResultPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </>
  );
}
