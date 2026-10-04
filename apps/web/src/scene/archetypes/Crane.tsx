import type { SceneColors } from '../colors';
import { matte, UNIT_BOX, UNIT_CYLINDER } from '../materials';

/** A crane over the island while a constructor runs (DESIGN.md §B6). */
export function Crane({ height, colors }: { height: number; colors: SceneColors }) {
  const mast = height + 1.4;
  const steel = matte(colors.accent);
  return (
    <group position={[1.1, 0, -0.9]}>
      <mesh
        geometry={UNIT_BOX}
        material={steel}
        position={[0, mast / 2, 0]}
        scale={[0.12, mast, 0.12]}
        castShadow
      />
      <mesh
        geometry={UNIT_BOX}
        material={steel}
        position={[-0.9, mast, 0.45]}
        rotation={[0, Math.PI / 5, 0]}
        scale={[2.2, 0.1, 0.1]}
        castShadow
      />
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(colors.link)}
        position={[-1.1, mast - 0.45, 0.8]}
        scale={[0.02, 0.9, 0.02]}
      />
      <mesh
        geometry={UNIT_BOX}
        material={steel}
        position={[-1.1, mast - 0.95, 0.8]}
        scale={[0.18, 0.12, 0.18]}
        castShadow
      />
    </group>
  );
}
