import type { Piece, RunResult } from '@ljbu/contracts';
import { ContactShadows, MapControls, OrthographicCamera } from '@react-three/drei';
import { Canvas, useThree } from '@react-three/fiber';
import { useEffect, useMemo, useState } from 'react';
import * as THREE from 'three';
import type { Selection } from '../components/result/selection';
import { Cable, VariableSign } from './archetypes/Links';
import { useSceneColors } from './colors';
import { boundsOf, layoutScene, SIGN_TOP, type Placement } from './layout';
import { useReducedMotion } from './motion';
import { OverlayDriver, OverlayLayer } from './overlay';
import { createOverlayStore, OverlayContext } from './overlayStore';
import { PieceView } from './PieceView';
import { sceneStateAt, slotKey } from './replay';

interface Props {
  result: RunResult;
  selection: Selection;
  onSelect: (selection: Selection) => void;
  /** Description of the scene for assistive technology; the log is its full text. */
  label: string;
}

/** Isometric: azimuth 45°, elevation atan(1/√2) ≈ 35.264° (DESIGN.md §B1). */
const CAMERA_DIRECTION = new THREE.Vector3(1, 1, 1).normalize();

/** Frames every piece with a 15% margin and bounds the zoom to 0.6×–2× of that. */
function FitCamera({ placements }: { placements: Placement[] }) {
  const { camera, size, controls } = useThree();
  // Three.js objects are meant to be mutated; the camera and the controls are not React state.
  /* eslint-disable react-hooks/immutability */
  useEffect(() => {
    if (!(camera instanceof THREE.OrthographicCamera)) {
      return;
    }
    const { center, radius } = boundsOf(placements);
    const target = new THREE.Vector3(...center);
    camera.position.copy(target).add(CAMERA_DIRECTION.clone().multiplyScalar(40));
    camera.lookAt(target);
    // Seen from the isometric direction, a circle on the ground is 2r wide and 2r·sin(35°) tall,
    // plus the height of the tallest building and its badge.
    const width = 2 * radius;
    const height = 2 * radius * Math.SQRT1_2 * Math.sin(Math.atan(Math.SQRT1_2)) + 4.5;
    camera.zoom = Math.min(size.width / (width * 1.1), size.height / (height * 1.1));
    camera.updateProjectionMatrix();
    const map = controls as
      | (THREE.EventDispatcher & {
          target: THREE.Vector3;
          minZoom: number;
          maxZoom: number;
          update: () => void;
        })
      | null;
    if (map) {
      map.target.copy(target);
      map.minZoom = camera.zoom * 0.6;
      map.maxZoom = camera.zoom * 2;
      map.update();
    }
  }, [camera, size, controls, placements]);
  /* eslint-enable react-hooks/immutability */
  return null;
}

function Model({ result, selection, onSelect }: Omit<Props, 'label'>) {
  const colors = useSceneColors();
  const reducedMotion = useReducedMotion();
  const placements = useMemo(() => layoutScene(result.pieces), [result.pieces]);
  const placed = useMemo(() => [...placements.values()], [placements]);
  const state = useMemo(
    () => sceneStateAt(result.pieces, result.timeline, selection.step),
    [result, selection.step],
  );
  const byId = useMemo(
    () => new Map(result.pieces.map((piece) => [piece.id, piece])),
    [result.pieces],
  );

  const select = (piece: Piece) => {
    onSelect({ pieceId: piece.id, sourceRef: piece.sourceRef });
  };

  /** Where a cable to a piece ends: the center of its island, just above the slab. */
  const anchorOf = (pieceId: string | null | undefined): THREE.Vector3 | undefined => {
    const placement = pieceId ? placements.get(pieceId) : undefined;
    return placement
      ? new THREE.Vector3(placement.position[0], 0.4, placement.position[2])
      : undefined;
  };

  return (
    <>
      <hemisphereLight args={['#fff4e0', '#b8c4d8', 0.9]} />
      <directionalLight
        position={[-6, 10, 4]}
        intensity={1.1}
        castShadow
        shadow-mapSize={[1024, 1024]}
        shadow-camera-left={-20}
        shadow-camera-right={20}
        shadow-camera-top={20}
        shadow-camera-bottom={-20}
      />
      {/* Rendered for a second after every change of state (enough for the growth) and then kept. */}
      <ContactShadows
        key={`${result.runId}/${String(selection.step ?? 'end')}`}
        position={[0, -0.02, 0]}
        opacity={0.35}
        scale={60}
        blur={2.5}
        far={4}
        resolution={512}
        frames={reducedMotion ? 1 : 60}
      />
      <FitCamera placements={placed} />
      <MapControls enableRotate={false} enableDamping={!reducedMotion} />
      <group
        onClick={() => {
          onSelect({});
        }}
      >
        <mesh position={[0, -0.05, 0]} rotation={[-Math.PI / 2, 0, 0]} visible={false}>
          <planeGeometry args={[200, 200]} />
        </mesh>
      </group>
      {result.pieces.map((piece) => {
        const placement = placements.get(piece.id);
        if (!placement) {
          return null;
        }
        if (piece.archetype === 'variable-sign') {
          if (!state.visible.has(piece.id)) {
            return null;
          }
          const target = state.targets.get(piece.id) ?? null;
          const from = new THREE.Vector3(...placement.position).add(
            new THREE.Vector3(0, SIGN_TOP, 0),
          );
          return (
            <group
              key={piece.id}
              position={placement.position}
              onClick={(event) => {
                event.stopPropagation();
                select(piece);
              }}
            >
              <VariableSign piece={piece} colors={colors} ghost={false} />
              <group
                position={[-placement.position[0], -placement.position[1], -placement.position[2]]}
              >
                <Cable from={from} to={anchorOf(target)} colors={colors} />
              </group>
            </group>
          );
        }
        const filled = new Set<string>();
        for (const slot of Object.values(piece.slots ?? {})) {
          for (const occupant of slot.pieceIds) {
            if (state.filled.has(slotKey(piece.id, occupant))) {
              filled.add(occupant);
            }
          }
        }
        return (
          <PieceView
            key={piece.id}
            piece={piece}
            placement={placement}
            colors={colors}
            visible={state.visible.has(piece.id)}
            filledSlots={filled}
            builtFloors={state.floors.get(piece.id)}
            selected={selection.pieceId === piece.id}
            float={!reducedMotion}
            animate={!reducedMotion}
            onSelect={select}
          />
        );
      })}
      {/* Slot cables: from the pedestal of the owner to its occupant. Drawn here to reach both. */}
      {result.pieces.flatMap((piece) =>
        Object.values(piece.slots ?? {}).flatMap((slot) =>
          slot.pieceIds
            .filter(
              (occupant) => state.filled.has(slotKey(piece.id, occupant)) && byId.has(occupant),
            )
            .map((occupant) => {
              const from = anchorOf(piece.id);
              const to = anchorOf(occupant);
              return from && to ? (
                <Cable
                  key={`${piece.id}>${occupant}`}
                  from={from.clone().setY(0.6)}
                  to={to}
                  colors={colors}
                />
              ) : null;
            }),
        ),
      )}
    </>
  );
}

/** The 3D model of the result (DESIGN.md Part B). Loaded on demand; the log is its textual equivalent. */
export default function SceneView({ result, selection, onSelect, label }: Props) {
  const reducedMotion = useReducedMotion();
  const [overlay] = useState(createOverlayStore);
  return (
    <div role="img" aria-label={label} style={{ position: 'absolute', inset: 0 }}>
      <Canvas
        orthographic
        shadows
        dpr={[1, 2]}
        frameloop={reducedMotion ? 'demand' : 'always'}
        gl={{ alpha: true, antialias: true }}
        style={{ background: 'transparent' }}
      >
        <OrthographicCamera makeDefault position={[40, 40, 40]} near={0.1} far={200} zoom={40} />
        <OverlayContext value={overlay}>
          <Model result={result} selection={selection} onSelect={onSelect} />
          <OverlayDriver />
        </OverlayContext>
      </Canvas>
      <OverlayLayer store={overlay} />
    </div>
  );
}
