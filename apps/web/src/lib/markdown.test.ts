import { inlineParts, renderMarkdown } from './markdown';

describe('markdown', () => {
  it('splits a log message into text, code and strong parts', () => {
    expect(inlineParts('Una facultad **tiene** un `Decano`.')).toEqual([
      { kind: 'text', text: 'Una facultad ' },
      { kind: 'strong', text: 'tiene' },
      { kind: 'text', text: ' un ' },
      { kind: 'code', text: 'Decano' },
      { kind: 'text', text: '.' },
    ]);
  });

  it('keeps anything else as text', () => {
    expect(inlineParts('<script>alert(1)</script>')).toEqual([
      { kind: 'text', text: '<script>alert(1)</script>' },
    ]);
  });

  it('renders the markdown of the content without scripts', () => {
    const html = renderMarkdown('Hola **UTN** <script>alert(1)</script>');

    expect(html).toContain('<strong>UTN</strong>');
    expect(html).not.toContain('<script>');
  });
});
