import styles from './Logo.module.css';

/**
 * The mark: an isometric building (the object) with a tag hanging from it (the variable),
 * the two shapes of the model. Drawn with the theme's colors.
 */
export function Logo({ withName = true }: { withName?: boolean }) {
  return (
    <span className={styles.logo}>
      <svg
        className={styles.mark}
        viewBox="0 0 32 32"
        width="28"
        height="28"
        aria-hidden
        focusable="false"
      >
        {/* Roof */}
        <path d="M16 3 L29 10 L16 17 L3 10 Z" className={styles.roof} />
        {/* Left wall */}
        <path d="M3 10 L16 17 L16 30 L3 23 Z" className={styles.wallLeft} />
        {/* Right wall */}
        <path d="M16 17 L29 10 L29 23 L16 30 Z" className={styles.wallRight} />
        {/* Tag */}
        <path d="M5 15.5 L11.5 19 L11.5 25.5 L8.5 27 L5 25 Z" className={styles.tag} />
        <circle cx="9.5" cy="21.5" r="1" className={styles.hole} />
      </svg>
      {withName && (
        <span className={styles.name}>
          <span className={styles.nameStrong}>Learn Java</span>
          <span className={styles.nameRest}> by Building a University</span>
        </span>
      )}
    </span>
  );
}
