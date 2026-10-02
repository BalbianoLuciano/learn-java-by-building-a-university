import { Moon, Sun } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useTheme } from '../theme/useTheme';
import styles from './ThemeToggle.module.css';

export function ThemeToggle() {
  const { t } = useTranslation();
  const { theme, toggleTheme } = useTheme();
  const Icon = theme === 'dark' ? Sun : Moon;

  return (
    <button
      type="button"
      className={styles.toggle}
      aria-label={theme === 'dark' ? t('theme.switchToLight') : t('theme.switchToDark')}
      onClick={toggleTheme}
    >
      <Icon size={18} strokeWidth={1.75} aria-hidden />
    </button>
  );
}
