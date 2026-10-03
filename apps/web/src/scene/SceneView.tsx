import type { Piece, RunResult } from '@ljbu/contracts';
import { ContactShadows, Line, MapControls, OrthographicCamera } from '@react-three/drei';
import { Canvas, useThree } from '@react-three/fiber';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as THREE from 'three';
import type { Selection } from '../components/result/selection';
import { Blueprint, Seal } from './archetypes/Blueprint';
import { useSceneColors } from './colors';
import { heightOf, serialsOf, type BubbleTexts } from './java';
import { layoutBlueprints, layoutScene, type BlueprintPlacement, type Placement } from './layout';
import { matte, UNIT_BOX } from './materials';
import { useReducedMotion } from './motion';
import { ChipAnchor, OverlayDriver, OverlayLayer } from './overlay';
import { createOverlayStore, OverlayContext } from './overlayStore';
import { PieceView } from './PieceView';
import { sceneStateAt } from './replay';

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
function FitCamera({ placements }: { placements: (Placement | BlueprintPlacement)[] }) {
  const { camera, size, controls } = useThree();
  // Three.js objects are meant to be mutated; the camera and the controls are not React state.
  /* eslint-disable react-hooks/immutability */
  useEffect(() => {
    if (!(camera instanceof THREE.OrthographicCamera)) {
      return;
    }
    // Fit the projection of every footprint and its height, not a sphere: the isometric view
    // squashes depth, so a sphere wastes a lot of the canvas.
    // Camera along (1,1,1): screen right is (1,0,-1)/√2 and screen up is (-1,2,-1)/√6.
    const toScreen = (x: number, y: number, z: number): [number, number] => [
      (x - z) / Math.SQRT2,
      (2 * y - x - z) / Math.sqrt(6),
    ];
    let minX = Infinity;
    let maxX = -Infinity;
    let minY = Infinity;
    let maxY = -Infinity;
    for (const placement of placements) {
      const half = (placement.island?.width ?? 1) / 2;
      // Blueprints stand on posts; buildings carry a badge about two units above the roof.
      const top = 'height' in placement ? placement.height + 0.9 : 2.6;
      const corners: [number, number][] = [
        [-half, -half],
        [half, -half],
        [-half, half],
        [half, half],
      ];
      for (const [dx, dz] of corners) {
        for (const y of [0, top]) {
          const [sx, sy] = toScreen(
            placement.position[0] + dx,
            placement.position[1] + y,
            placement.position[2] + dz,
          );
          minX = Math.min(minX, sx);
          maxX = Math.max(maxX, sx);
          minY = Math.min(minY, sy);
          maxY = Math.max(maxY, sy);
        }
      }
    }
    if (minX === Infinity) {
      return;
    }
    const width = Math.max(4, maxX - minX);
    const height = Math.max(3, maxY - minY);
    // The point on the ground whose projection is the middle of the fitted box.
    const sxc = (minX + maxX) / 2;
    const syc = (minY + maxY) / 2;
    const a = sxc * Math.SQRT2; // x - z
    const b = -syc * Math.sqrt(6); // x + z, on the ground (y = 0)
    const target = new THREE.Vector3((a + b) / 2, 0, (b - a) / 2);
    camera.position.copy(target).add(CAMERA_DIRECTION.clone().multiplyScalar(60));
    camera.lookAt(target);
    camera.zoom = Math.min(size.width / (width * 1.06), size.height / (height * 1.1));
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
  const { t } = useTranslation();
  const colors = useSceneColors();
  const reducedMotion = useReducedMotion();
  const layout = useMemo(() => layoutScene(result.pieces), [result.pieces]);
  const { placements, bridges } = layout;
  const classes = useMemo(
    () => new Map(result.classes.map((info) => [info.name, info])),
    [result.classes],
  );
  // Main only holds main(): it is the program, not part of the model.
  const modelClasses = useMemo(
    () => result.classes.filter((info) => info.name !== 'Main'),
    [result.classes],
  );
  const blueprints = useMemo(() => {
    // Just behind the farthest island: depth is what the isometric view can least afford.
    let back = 0;
    for (const placement of placements.values()) {
      back = Math.min(back, placement.position[2] - (placement.island?.depth ?? 1) / 2);
    }
    return layoutBlueprints(modelClasses, back - 2.4);
  }, [placements, modelClasses]);
  const placed = useMemo(
    () => [...placements.values(), ...blueprints.values()],
    [placements, blueprints],
  );
  const state = useMemo(
    () => sceneStateAt(result.pieces, result.timeline, selection.step),
    [result, selection.step],
  );
  const serials = useMemo(() => serialsOf(result.pieces), [result.pieces]);
  const archetypeOf = useMemo(() => {
    const map = new Map<string, string>();
    for (const piece of result.pieces) {
      if (piece.type && !map.has(piece.type)) {
        map.set(piece.type, piece.archetype);
      }
    }
    return map;
  }, [result.pieces]);
  const texts: BubbleTexts = useMemo(
    () => ({
      attributes: t('scene.attributes'),
      methods: t('scene.methods'),
      constructors: t('scene.constructors'),
      serial: (type, number) => t('scene.serial', { type, number }),
      abstract: t('scene.abstract'),
      ghost: t('scene.ghostBubble'),
    }),
    [t],
  );

  const select = (piece: Piece) => {
    onSelect({ pieceId: piece.id, sourceRef: piece.sourceRef });
  };
  const selectClass = (name: string) => {
    onSelect({ className: name });
  };

  /** A bubble is open on the selected piece or, failing that, on the one the step touches. */
  const openOn = selection.pieceId ?? state.focus?.pieceId;
  const signs = result.pieces.filter((piece) => piece.archetype === 'variable-sign');

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
      <MapControls makeDefault enableRotate={false} enableDamping={!reducedMotion} />
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
        if (!placement || piece.archetype === 'variable-sign') {
          return null;
        }
        const tags = signs.filter(
          (sign) => state.visible.has(sign.id) && (state.targets.get(sign.id) ?? null) === piece.id,
        );
        return (
          <PieceView
            key={piece.id}
            piece={piece}
            placement={placement}
            colors={colors}
            visible={state.visible.has(piece.id)}
            classes={classes}
            serials={serials}
            fields={state.fields.get(piece.id)}
            running={state.running.get(piece.id)}
            lastWritten={state.focus?.pieceId === piece.id ? state.focus.field : undefined}
            tags={tags}
            selected={selection.pieceId === piece.id}
            open={openOn === piece.id}
            float={!reducedMotion}
            animate={!reducedMotion}
            builtFloors={state.floors.get(piece.id)}
            texts={texts}
            onSelect={select}
          />
        );
      })}
      {/* Variables pointing to nothing: tags lying on the ground at the front. */}
      {signs
        .filter(
          (sign) =>
            state.visible.has(sign.id) &&
            !placements.has(state.targets.get(sign.id) ?? '') &&
            placements.get(sign.id)?.hangsFrom === undefined,
        )
        .map((sign) => {
          const placement = placements.get(sign.id);
          if (!placement) {
            return null;
          }
          return (
            <group
              key={sign.id}
              position={placement.position}
              onClick={(event) => {
                event.stopPropagation();
                select(sign);
              }}
            >
              <ChipAnchor
                id={`${sign.id}:tag`}
                position={[0, 0.1, 0]}
                chip={{
                  shape: 'tag',
                  text: t('scene.nullTag', { name: sign.label }),
                  flags: ['null'],
                  lit: false,
                  ghost: false,
                }}
              />
            </group>
          );
        })}
      {/* Walkways between an owner and what it holds. */}
      {bridges.map((bridge, index) => {
        const [x1, , z1] = bridge.from;
        const [x2, , z2] = bridge.to;
        return (
          <mesh
            key={index}
            geometry={UNIT_BOX}
            material={matte(colors.island)}
            position={[(x1 + x2) / 2, 0.2, (z1 + z2) / 2]}
            scale={[Math.max(0.5, Math.abs(x2 - x1)), 0.1, Math.max(0.5, Math.abs(z2 - z1))]}
            receiveShadow
          />
        );
      })}
      {/* The selected object hangs from its blueprint: a dotted line, class to instance. */}
      {result.pieces.map((piece) => {
        const placement = placements.get(piece.id);
        const blueprint = piece.type ? blueprints.get(piece.type) : undefined;
        if (
          !placement ||
          !blueprint ||
          selection.pieceId !== piece.id ||
          !state.visible.has(piece.id)
        ) {
          return null;
        }
        const island = placement.island ?? { width: 1.8, depth: 1.8 };
        return (
          <Line
            key={`${piece.id}<${blueprint.name}`}
            points={[
              [
                placement.position[0],
                0.32 + heightOf(piece) + 0.1,
                placement.position[2] - island.depth / 2 + 0.3,
              ],
              [blueprint.position[0], blueprint.position[1] + 0.5, blueprint.position[2] + 0.1],
            ]}
            color={colors.accent}
            lineWidth={1.5}
            dashed
            dashSize={0.25}
            gapSize={0.18}
          />
        );
      })}
      {modelClasses.map((info) => {
        const placement = blueprints.get(info.name);
        if (!placement) {
          return null;
        }
        return info.kind === 'interface' ? (
          <Seal
            key={info.name}
            info={info}
            placement={placement}
            colors={colors}
            selected={selection.className === info.name}
            texts={texts}
            onSelect={selectClass}
          />
        ) : (
          <Blueprint
            key={info.name}
            info={info}
            placement={placement}
            archetype={archetypeOf.get(info.name)}
            colors={colors}
            selected={selection.className === info.name}
            texts={texts}
            onSelect={selectClass}
          />
        );
      })}
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
