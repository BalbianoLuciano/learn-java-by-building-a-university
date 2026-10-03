import { useMemo } from 'react';
import { useTheme } from '../theme/useTheme';

/** The colors of DESIGN.md §B3 and the state colors, as the CSS of the current theme resolves them. */
export interface SceneColors {
  island: string;
  islandTop: string;
  wall: string;
  rectorate: string;
  faculty: string;
  department: string;
  career: string;
  person: string;
  link: string;
  ghost: string;
  blueprint: string;
  plaque: string;
  accent: string;
  success: string;
  warning: string;
  danger: string;
  surface: string;
  text: string;
}

const TOKENS: Record<keyof SceneColors, string> = {
  island: '--scene-island',
  islandTop: '--scene-island-top',
  wall: '--scene-wall',
  rectorate: '--scene-rectorate',
  faculty: '--scene-faculty',
  department: '--scene-department',
  career: '--scene-career',
  person: '--scene-person',
  link: '--scene-link',
  ghost: '--scene-ghost',
  blueprint: '--scene-blueprint',
  plaque: '--scene-plaque',
  accent: '--color-accent',
  success: '--color-success',
  warning: '--color-warning',
  danger: '--color-danger',
  surface: '--color-surface',
  text: '--color-text',
};

/**
 * Resolves each token through the color property of a throwaway element: a custom property
 * holding light-dark() only resolves when a real property uses it.
 */
export function readSceneColors(): SceneColors {
  const probe = document.createElement('span');
  probe.style.display = 'none';
  document.body.appendChild(probe);
  const colors = {} as SceneColors;
  for (const [key, token] of Object.entries(TOKENS) as [keyof SceneColors, string][]) {
    probe.style.color = `var(${token})`;
    colors[key] = getComputedStyle(probe).color;
  }
  probe.remove();
  return colors;
}

/** "rgb(255 255 255 / 0.22)" or "rgba(255, 255, 255, 0.22)" → the alpha, 1 when absent. */
export function alphaOf(cssColor: string): number {
  const match = /\/\s*([\d.]+)\s*\)|rgba\([^,]+,[^,]+,[^,]+,\s*([\d.]+)\)/.exec(cssColor);
  const value = match?.[1] ?? match?.[2];
  return value === undefined ? 1 : Number(value);
}

/** The scene colors of the current theme, read again when the theme changes. */
export function useSceneColors(): SceneColors {
  const { theme } = useTheme();
  // The theme attribute is set before the choice is stored, so the CSS is already resolved.
  // eslint-disable-next-line react-hooks/exhaustive-deps -- the theme is the trigger, not an input
  return useMemo(() => readSceneColors(), [theme]);
}
