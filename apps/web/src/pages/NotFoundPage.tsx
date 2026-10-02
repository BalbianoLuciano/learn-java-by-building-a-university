import { ApiError } from '../api/client';
import { ErrorNotice } from '../components/Notice';
import { TopBar } from '../components/TopBar';
import styles from './HomePage.module.css';

export function NotFoundPage() {
  return (
    <>
      <TopBar />
      <main id="content" className={styles.page}>
        <ErrorNotice error={new ApiError(404, 'notFound')} />
      </main>
    </>
  );
}
