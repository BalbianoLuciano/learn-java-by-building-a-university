import type { Piece } from '@ljbu/contracts';
import { ContactShadows, OrthographicCamera } from '@react-three/drei';
import { Canvas, useFrame } from '@react-three/fiber';
import { useRef } from 'react';
import type { Group } from 'three';
import { RegionalFaculty } from './archetypes/Buildings';
import { Island } from './archetypes/Island';
import { useSceneColors } from './colors';
import { matte, UNIT_BOX, UNIT_CYLINDER } from './materials';
import { useReducedMotion } from './motion';

const PIECE: Piece = {
  id: 'nowhere',
  archetype: 'regional-faculty',
  state: 'failed',
  built: true,
  label: '',
};

/** A loose brick that tumbled off the building and keeps rocking on the ground. */
function Brick({ color, float }: { color: string; float: boolean }) {
  const group = useRef<Group>(null);
  useFrame(({ clock }) => {
    if (group.current && float) {
      group.current.rotation.z = 0.35 + Math.sin(clock.elapsedTime * 1.3) * 0.08;
    }
  });
  return (
    <group ref={group} position={[1.6, 0.12, 1.1]} rotation={[0, 0.6, 0.35]}>
      <mesh geometry={UNIT_BOX} material={matte(color)} scale={[0.5, 0.25, 0.3]} castShadow />
    </group>
  );
}

/** A signpost with nothing hanging from it: the address of this page points to null. */
function EmptyPost({ color }: { color: string }) {
  return (
    <group position={[-1.9, 0, 1.3]} rotation={[0, 0, 0.12]}>
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(color)}
        position={[0, 0.6, 0]}
        scale={[0.08, 1.2, 0.08]}
        castShadow
      />
      <mesh
        geometry={UNIT_BOX}
        material={matte(color)}
        position={[0.25, 1.15, 0]}
        rotation={[0, 0, -0.1]}
        scale={[0.5, 0.06, 0.06]}
        castShadow
      />
    </group>
  );
}

function Scene() {
  const colors = useSceneColors();
  const reducedMotion = useReducedMotion();
  const look = { colors, failed: true, ghost: false };
  return (
    <>
      <OrthographicCamera
        makeDefault
        position={[20, 20, 20]}
        zoom={78}
        near={0.1}
        far={100}
        onUpdate={(camera) => {
          camera.lookAt(0, 0.4, 0);
        }}
      />
      <hemisphereLight args={['#fff4e0', '#b8c4d8', 0.9]} />
      <directionalLight position={[-6, 10, 4]} intensity={1.1} castShadow />
      <ContactShadows position={[0, -0.02, 0]} opacity={0.35} scale={14} blur={2.5} far={3} />
      <group rotation={[0, 0, 0.06]}>
        <Island
          width={2.6}
          depth={2.6}
          phase={0}
          float={!reducedMotion}
          selected={false}
          ghost={false}
          colors={colors}
        >
          <RegionalFaculty piece={PIECE} look={look} />
        </Island>
      </group>
      <Brick color={colors.faculty} float={!reducedMotion} />
      <EmptyPost color={colors.link} />
    </>
  );
}

/** The broken building of the page that does not exist (decorative). */
export function NotFoundScene({ className }: { className?: string }) {
  return (
    <div className={className} aria-hidden>
      <Canvas
        orthographic
        shadows
        dpr={[1, 2]}
        gl={{ alpha: true, antialias: true, powerPreference: 'low-power' }}
        style={{ pointerEvents: 'none' }}
      >
        <Scene />
      </Canvas>
    </div>
  );
}
