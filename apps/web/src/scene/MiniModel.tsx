import type { Piece } from '@ljbu/contracts';
import { OrthographicCamera, View } from '@react-three/drei';
import { Canvas } from '@react-three/fiber';
import { type ReactNode, type RefObject } from 'react';
import {
  Career,
  Department,
  GenericBlock,
  InheritanceFloors,
  Person,
  Rectorate,
  RegionalFaculty,
} from './archetypes/Buildings';
import { useSceneColors } from './colors';
import { matte, UNIT_BOX } from './materials';

export interface MiniPiece {
  archetype: string;
  label: string;
}

const SPACING = 2.2;

function Building({ archetype, index }: { archetype: string; index: number }) {
  const colors = useSceneColors();
  const look = { colors, failed: false, ghost: false };
  const piece: Piece = {
    id: `mini-${String(index)}`,
    archetype: archetype as Piece['archetype'],
    state: 'passed',
    built: true,
    label: '',
    floors: archetype === 'inheritance-floors' ? ['A', 'B'] : undefined,
  };
  switch (archetype) {
    case 'regional-faculty':
      return <RegionalFaculty piece={piece} look={look} />;
    case 'rectorate':
      return <Rectorate piece={piece} look={look} />;
    case 'inheritance-floors':
      return <InheritanceFloors piece={piece} look={look} builtFloors={2} />;
    case 'department':
      return <Department piece={piece} look={look} />;
    case 'career':
      return <Career piece={piece} look={look} />;
    case 'person':
      return <Person piece={piece} look={look} />;
    default:
      return <GenericBlock piece={piece} look={look} />;
  }
}

/**
 * A miniature of what a challenge asks to build (DESIGN.md §A2): its expected pieces in a
 * row on one slab. One View per card; all of them share the canvas of MiniModels.
 */
export function MiniModel({ pieces, className }: { pieces: MiniPiece[]; className?: string }) {
  const colors = useSceneColors();
  const shown = pieces.slice(0, 5);
  const width = Math.max(1, shown.length) * SPACING;
  return (
    <View className={className}>
      <OrthographicCamera
        makeDefault
        position={[20, 20, 20]}
        zoom={Math.min(34, 150 / width)}
        near={0.1}
        far={100}
        onUpdate={(camera) => {
          camera.lookAt(0, 0, 0);
        }}
      />
      <hemisphereLight args={['#fff4e0', '#b8c4d8', 0.9]} />
      <directionalLight position={[-6, 10, 4]} intensity={1.0} />
      <group position={[0, -0.6, 0]}>
        <mesh
          geometry={UNIT_BOX}
          material={matte(colors.island)}
          position={[0, -0.15, 0]}
          scale={[width + 0.6, 0.3, 2.6]}
        />
        {shown.map((piece, index) => (
          <group key={index} position={[(index - (shown.length - 1) / 2) * SPACING, 0, 0]}>
            <Building archetype={piece.archetype} index={index} />
          </group>
        ))}
      </group>
    </View>
  );
}

/** The shared canvas behind a page of miniatures: put it inside the positioned container. */
export function MiniModels({
  container,
  children,
}: {
  container: RefObject<HTMLElement | null>;
  children: ReactNode;
}) {
  return (
    <>
      {children}
      <Canvas
        eventSource={container}
        style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}
        gl={{ alpha: true, antialias: true }}
        dpr={[1, 2]}
      >
        <View.Port />
      </Canvas>
    </>
  );
}
