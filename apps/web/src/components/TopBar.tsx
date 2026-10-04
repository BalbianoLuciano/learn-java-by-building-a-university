import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { ChevronLeft } from 'lucide-react';
import { Logo } from './Logo';
import { ThemeToggle } from './ThemeToggle';
import styles from './TopBar.module.css';

interface Props {
  /** Where the left arrow goes; without it the bar shows the name of the site. */
  back?: { to: string; label: string };
  center?: ReactNode;
}

/** The 56px bar of DESIGN.md §A2. */
export function TopBar({ back, center }: Props) {
  return (
    <header className={styles.bar}>
      <div className={styles.side}>
        {back ? (
          <Link className={styles.back} to={back.to}>
            <ChevronLeft size={18} strokeWidth={1.75} aria-hidden />
            <span>{back.label}</span>
          </Link>
        ) : (
          <Link className={styles.name} to="/" aria-label="Learn Java by Building a University">
            <Logo />
          </Link>
        )}
      </div>
      <div className={styles.center}>{center}</div>
      <div className={styles.side} data-align="end">
        {back && (
          <Link className={styles.home} to="/" aria-label="Learn Java by Building a University">
            <Logo withName={false} />
          </Link>
        )}
        <ThemeToggle />
      </div>
    </header>
  );
}
