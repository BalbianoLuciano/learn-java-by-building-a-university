import { RoundedBox } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { useRef, type ReactNode } from 'react';
import type { Group } from 'three';
import type { SceneColors } from '../colors';
import { matte, UNIT_BOX, UNIT_TORUS } from '../materials';

interface Props {
  width: number;
  depth: number;
  phase: number;
  float: boolean;
  selected: boolean;
  ghost: boolean;
  colors: SceneColors;
  children?: ReactNode;
}

/** The floating slab every piece stands on (DESIGN.md §B2). Its top is at y = 0.3. */
export function Island({ width, depth, phase, float, selected, ghost, colors, children }: Props) {
  const group = useRef<Group>(null);
  useFrame(({ clock }) => {
    if (!group.current) {
      return;
    }
    const bob = float ? Math.sin((clock.elapsedTime / 6) * Math.PI * 2 + phase) * 0.05 : 0;
    group.current.position.y = bob + (selected ? 0.15 : 0);
  });
  const slab = matte(colors.island, { ghost });
  const grass = matte(colors.islandTop, { ghost });
  return (
    <group ref={group}>
      <RoundedBox
        args={[width, 0.3, depth]}
        radius={0.06}
        smoothness={2}
        position={[0, 0.15, 0]}
        material={slab}
        castShadow
        receiveShadow
      />
      <mesh
        geometry={UNIT_BOX}
        material={grass}
        position={[0, 0.3, 0]}
        scale={[width - 0.2, 0.04, depth - 0.2]}
        receiveShadow
      />
      {selected && (
        <mesh
          geometry={UNIT_TORUS}
          material={matte(colors.accent)}
          rotation={[-Math.PI / 2, 0, 0]}
          position={[0, 0.33, 0]}
          scale={Math.max(width, depth) * 0.55}
        />
      )}
      <group position={[0, 0.32, 0]}>{children}</group>
    </group>
  );
}
