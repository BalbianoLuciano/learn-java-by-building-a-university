import { useTranslation } from 'react-i18next';
import { Navigate, Route, Routes, useParams } from 'react-router';
import { ChallengePage } from './pages/ChallengePage';
import { HomePage } from './pages/HomePage';
import { ModulePage } from './pages/ModulePage';
import { NotFoundPage } from './pages/NotFoundPage';
import { DevScenePage } from './pages/DevScenePage';
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
        <Route path="/desafios/:challengeId/resultado" element={<BackToChallenge />} />
        {/* Only while developing: a synthetic scene to measure the frame rate (DESIGN.md §B7). */}
        {import.meta.env.DEV && <Route path="/dev/escena" element={<DevScenePage />} />}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </>
  );
}

/** The result is part of the challenge screen now (DESIGN.md §A2); old links still work. */
function BackToChallenge() {
  const { challengeId = '' } = useParams();
  return <Navigate to={`/desafios/${challengeId}`} replace />;
}
