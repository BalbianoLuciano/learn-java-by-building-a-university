import { inlineParts } from '../lib/markdown';

/** A log message with `code` and **strong**, rendered as text: it may quote learner code. */
export function Inline({ text }: { text: string }) {
  return (
    <>
      {inlineParts(text).map((part, index) => {
        switch (part.kind) {
          case 'code':
            return <code key={index}>{part.text}</code>;
          case 'strong':
            return <strong key={index}>{part.text}</strong>;
          default:
            return <span key={index}>{part.text}</span>;
        }
      })}
    </>
  );
}
