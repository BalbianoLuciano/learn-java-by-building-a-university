import { alphaOf, readSceneColors } from './colors';

describe('alphaOf', () => {
  it('reads the alpha of modern and legacy rgb notations', () => {
    expect(alphaOf('rgb(255 255 255 / 0.22)')).toBe(0.22);
    expect(alphaOf('rgba(12, 34, 56, 0.5)')).toBe(0.5);
  });

  it('is opaque when the color has no alpha', () => {
    expect(alphaOf('rgb(12, 34, 56)')).toBe(1);
  });
});

describe('readSceneColors', () => {
  it('resolves every token and leaves nothing behind in the document', () => {
    const colors = readSceneColors();

    expect(Object.keys(colors)).toContain('faculty');
    expect(document.body.children).toHaveLength(0);
  });
});
