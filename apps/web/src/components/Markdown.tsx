import { renderMarkdown } from '../lib/markdown';
import styles from './Markdown.module.css';

/** Markdown of the content (briefs, explanations). Not for anything the learner wrote. */
export function Markdown({ source, className }: { source: string; className?: string }) {
  return (
    <div
      className={[styles.markdown, className].filter(Boolean).join(' ')}
      dangerouslySetInnerHTML={{ __html: renderMarkdown(source) }}
    />
  );
}
