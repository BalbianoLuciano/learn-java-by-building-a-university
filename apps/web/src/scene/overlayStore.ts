import type { State } from '@ljbu/contracts';
import { createContext } from 'react';
import type * as THREE from 'three';
import { createStore, type StoreApi } from 'zustand';

/**
 * Signs and badges are HTML over the canvas (DESIGN.md §B4): crisp, translatable, themed by
 * CSS. Each one is anchored to an object of the scene; one driver projects every anchor per
 * frame and moves its element, so the whole layer costs one DOM write per sign.
 */
export type AnchorContent =
  | { kind: 'label'; text: string; ghost: boolean }
  | { kind: 'badge'; state: State }
  | { kind: 'chip'; chip: Chip };

/**
 * A plaque (an attribute), a window (a method) or a seal (an interface) of DESIGN.md §B4.
 * flags: private, static, final, ref (the plaque holds a reference), abstract, missing.
 */
export interface Chip {
  shape: 'plaque' | 'window' | 'seal' | 'header';
  text: string;
  flags: string[];
  /** The method is running at the chosen step. */
  lit: boolean;
  ghost: boolean;
}

interface Anchor {
  object: THREE.Object3D;
  content: AnchorContent;
  /** Screen offset in pixels, so stacked chips keep their spacing at any zoom. */
  offset: [number, number];
}

interface OverlayState {
  anchors: ReadonlyMap<string, Anchor>;
  /** The element of each anchor, written by the layer and read by the driver: not reactive. */
  elements: Map<string, HTMLElement>;
  set: (id: string, anchor: Anchor) => void;
  remove: (id: string) => void;
}

export type OverlayStore = StoreApi<OverlayState>;

export function createOverlayStore(): OverlayStore {
  return createStore<OverlayState>((set) => ({
    anchors: new Map(),
    elements: new Map(),
    set: (id, anchor) => {
      set((state) => ({ anchors: new Map(state.anchors).set(id, anchor) }));
    },
    remove: (id) => {
      set((state) => {
        const anchors = new Map(state.anchors);
        anchors.delete(id);
        return { anchors };
      });
    },
  }));
}

export const OverlayContext = createContext<OverlayStore | null>(null);
