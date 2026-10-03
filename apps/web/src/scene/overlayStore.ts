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
  | { kind: 'chip'; chip: Chip }
  | { kind: 'bubble'; bubble: Bubble };

/**
 * A small sign of DESIGN.md §B4: the header of a blueprint, a tag (a variable), a plate (the
 * serial of an object), a seal (an interface) or, inside a bubble, a plaque (an attribute) or a
 * window (a method). flags: private, static, final, ref, abstract, missing, null.
 */
export interface Chip {
  shape: 'plaque' | 'window' | 'seal' | 'header' | 'tag' | 'plate';
  text: string;
  flags: string[];
  /** The method is running, or the attribute was just written, at the chosen step. */
  lit: boolean;
  ghost: boolean;
}

/** The detail of a building or a blueprint, open over it (DESIGN.md §A3, §B4). */
export interface Bubble {
  title: string;
  subtitle?: string;
  note?: string;
  sections: { title: string; rows: Chip[] }[];
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
