import DOMPurify from 'dompurify';
import { marked } from 'marked';

/** Renders the Markdown of the content (briefs, solutions) to HTML, sanitized just in case. */
export function renderMarkdown(markdown: string): string {
  return DOMPurify.sanitize(marked.parse(markdown, { async: false, gfm: true }));
}

export type InlinePart = { kind: 'text' | 'code' | 'strong'; text: string };

/**
 * Splits a log message into text, `code` and **strong** parts. Messages can quote what the
 * learner wrote, so they are never interpreted as HTML.
 */
export function inlineParts(text: string): InlinePart[] {
  const parts: InlinePart[] = [];
  const pattern = /`([^`]+)`|\*\*([^*]+)\*\*/g;
  let last = 0;
  for (const match of text.matchAll(pattern)) {
    if (match.index > last) {
      parts.push({ kind: 'text', text: text.slice(last, match.index) });
    }
    if (match[1] !== undefined) {
      parts.push({ kind: 'code', text: match[1] });
    } else if (match[2] !== undefined) {
      parts.push({ kind: 'strong', text: match[2] });
    }
    last = match.index + match[0].length;
  }
  if (last < text.length) {
    parts.push({ kind: 'text', text: text.slice(last) });
  }
  return parts;
}
