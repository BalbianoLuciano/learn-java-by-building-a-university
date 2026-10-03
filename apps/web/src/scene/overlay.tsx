import type { State } from '@ljbu/contracts';
import { useFrame, useThree } from '@react-three/fiber';
import { CircleCheck, CircleX, Construction, Flag, Lock, Pin, Plug, Tag } from 'lucide-react';
import { useContext, useLayoutEffect, useRef, type RefObject } from 'react';
import * as THREE from 'three';
import { useStore } from 'zustand';
import styles from './overlay.module.css';
import {
  OverlayContext,
  type AnchorContent,
  type Bubble,
  type Chip,
  type OverlayStore,
} from './overlayStore';

function useOverlayStore(): OverlayStore {
  const store = useContext(OverlayContext);
  if (!store) {
    throw new Error('Anchors need an OverlayContext');
  }
  return store;
}

function useAnchor(
  id: string,
  content: AnchorContent,
  offset: [number, number] = [0, 0],
): RefObject<THREE.Group | null> {
  const store = useOverlayStore();
  const invalidate = useThree((state) => state.invalidate);
  const ref = useRef<THREE.Group>(null);
  // Primitive dependencies, so a re-render with equal content does not re-register.
  const key = JSON.stringify({ content, offset });
  useLayoutEffect(() => {
    const object = ref.current;
    if (!object) {
      return;
    }
    const parsed = JSON.parse(key) as { content: AnchorContent; offset: [number, number] };
    store.getState().set(id, { object, content: parsed.content, offset: parsed.offset });
    invalidate();
    return () => {
      store.getState().remove(id);
    };
  }, [store, invalidate, id, key]);
  return ref;
}

/** A plaque, a window or a seal anchored at the given offset of the current group. */
export function ChipAnchor({
  id,
  chip,
  position,
  offset,
}: {
  id: string;
  chip: Chip;
  position: [number, number, number];
  /** Screen offset in pixels from the anchor: stacked chips keep their spacing at any zoom. */
  offset?: [number, number];
}) {
  const ref = useAnchor(id, { kind: 'chip', chip }, offset);
  return <group ref={ref} position={position} />;
}

/** The detail bubble of a building or a blueprint, with its tail on the anchor. */
export function BubbleAnchor({
  id,
  bubble,
  position,
}: {
  id: string;
  bubble: Bubble;
  position: [number, number, number];
}) {
  const ref = useAnchor(id, { kind: 'bubble', bubble });
  return <group ref={ref} position={position} />;
}

/** A text sign anchored this high above the current group. */
export function Label({
  id,
  text,
  y,
  ghost = false,
}: {
  id: string;
  text: string;
  y: number;
  ghost?: boolean;
}) {
  const ref = useAnchor(id, { kind: 'label', text, ghost });
  return <group ref={ref} position={[0, y, 0]} />;
}

/** The floating state icon of a piece (DESIGN.md §B5). Decorative: the log carries the text. */
export function StateBadge({ id, state, y }: { id: string; state: State; y: number }) {
  const ref = useAnchor(id, { kind: 'badge', state });
  return <group ref={ref} position={[0, y, 0]} />;
}

const projected = new THREE.Vector3();

/** Pixels per world unit at which chips show at their natural size. */
export const BASE_ZOOM = 40;

/** Inside the canvas: projects every anchor and moves its element. */
export function OverlayDriver() {
  const store = useOverlayStore();
  useFrame(({ camera, size }) => {
    const { anchors, elements } = store.getState();
    for (const [id, anchor] of anchors) {
      const element = elements.get(id);
      if (!element) {
        continue;
      }
      anchor.object.getWorldPosition(projected).project(camera);
      // Chips scale with the zoom (within limits), so their size in the world stays the same
      // and the layout can space pieces for them; zooming in makes them legible.
      const zoom = camera instanceof THREE.OrthographicCamera ? camera.zoom : BASE_ZOOM;
      const scale = Math.min(1.3, Math.max(0.55, zoom / BASE_ZOOM));
      let x = ((projected.x + 1) / 2) * size.width + anchor.offset[0] * scale;
      let y = ((1 - projected.y) / 2) * size.height + anchor.offset[1] * scale;
      if (anchor.content.kind === 'bubble') {
        // A bubble stays inside the canvas: it slides rather than getting cut.
        const halfWidth = (element.offsetWidth * scale) / 2;
        x = Math.min(size.width - halfWidth - 4, Math.max(halfWidth + 4, x));
        y = Math.max(element.offsetHeight * scale + 4, y);
      }
      // A bubble sits on its anchor; everything else is centered on it.
      const origin =
        anchor.content.kind === 'bubble' ? 'translate(-50%, -100%)' : 'translate(-50%, -50%)';
      const transform = `translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0) ${origin} scale(${scale.toFixed(3)})`;
      if (element.style.transform !== transform) {
        element.style.transform = transform;
      }
      const zIndex = String(Math.round((1 - projected.z) * 500));
      if (element.style.zIndex !== zIndex) {
        element.style.zIndex = zIndex;
      }
      if (element.style.visibility !== 'visible') {
        element.style.visibility = 'visible';
      }
    }
  });
  return null;
}

/** Outside the canvas: the elements of the anchors, as the store lists them. */
export function OverlayLayer({ store }: { store: OverlayStore }) {
  const anchors = useStore(store, (state) => state.anchors);
  const elements = store.getState().elements;
  return (
    <div className={styles.layer} aria-hidden>
      {[...anchors].map(([id, anchor]) => (
        <div
          key={id}
          className={styles.anchor}
          ref={(element) => {
            if (element) {
              elements.set(id, element);
            } else {
              elements.delete(id);
            }
          }}
        >
          {anchor.content.kind === 'label' && (
            <span className={styles.label} data-ghost={anchor.content.ghost}>
              {anchor.content.text}
            </span>
          )}
          {anchor.content.kind === 'badge' && <Badge state={anchor.content.state} />}
          {anchor.content.kind === 'chip' && <ChipView chip={anchor.content.chip} />}
          {anchor.content.kind === 'bubble' && <BubbleView bubble={anchor.content.bubble} />}
        </div>
      ))}
    </div>
  );
}

function Badge({ state }: { state: State }) {
  const Icon = state === 'passed' ? CircleCheck : state === 'incomplete' ? Construction : CircleX;
  return (
    <span className={styles.badge} data-state={state}>
      <Icon size={18} strokeWidth={1.75} />
    </span>
  );
}

function ChipView({ chip }: { chip: Chip }) {
  const flags = new Set(chip.flags);
  return (
    <span
      className={styles.chip}
      data-shape={chip.shape}
      data-lit={chip.lit}
      data-ghost={chip.ghost}
      data-private={flags.has('private')}
      data-abstract={flags.has('abstract')}
      data-missing={flags.has('missing')}
      data-null={flags.has('null')}
    >
      {chip.shape === 'tag' && <Tag size={12} strokeWidth={2} aria-hidden />}
      {flags.has('private') && <Lock size={12} strokeWidth={2} aria-hidden />}
      {flags.has('static') && <Flag size={12} strokeWidth={2} aria-hidden />}
      {flags.has('final') && <Pin size={12} strokeWidth={2} aria-hidden />}
      <span className={styles.chipText}>{chip.text}</span>
      {flags.has('ref') && <Plug size={12} strokeWidth={2} aria-hidden />}
    </span>
  );
}

function BubbleView({ bubble }: { bubble: Bubble }) {
  return (
    <div className={styles.bubble}>
      <div className={styles.bubbleHead}>
        <span className={styles.bubbleTitle}>{bubble.title}</span>
        {bubble.subtitle && <span className={styles.bubbleSubtitle}>{bubble.subtitle}</span>}
      </div>
      {bubble.note && <p className={styles.bubbleNote}>{bubble.note}</p>}
      {bubble.sections.map((section) => (
        <div key={section.title} className={styles.bubbleSection}>
          <span className={styles.bubbleSectionTitle}>{section.title}</span>
          <div className={styles.bubbleRows}>
            {section.rows.map((row) => (
              <ChipView key={row.text} chip={row} />
            ))}
          </div>
        </div>
      ))}
      <span className={styles.bubbleTail} aria-hidden />
    </div>
  );
}
