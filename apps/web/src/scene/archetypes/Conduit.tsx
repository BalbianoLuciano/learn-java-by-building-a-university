import { useFrame } from '@react-three/fiber';
import { useMemo, useRef } from 'react';
import * as THREE from 'three';
import type { SceneColors } from '../colors';
import { UNIT_BOX } from '../materials';

const WIDTH = 0.14;
const HEIGHT = 0.06;
const Y = HEIGHT / 2;

interface Props {
  /** Where the conduit leaves the drafting board: the foot of the blueprint. */
  from: [number, number, number];
  /** Where it plugs in: the back edge of the island of the instance. */
  to: [number, number, number];
  /** Lit while the instance is being built, or when it is the chosen one. */
  lit: boolean;
  colors: SceneColors;
}

/**
 * A rigid conduit on the ground from a blueprint to the base of each of its instances
 * (DESIGN.md §B4): it runs forward from the board, turns at a right angle and plugs into the
 * island. It lights up while the instance is being built.
 */
export function Conduit({ from, to, lit, colors }: Props) {
  const material = useMemo(
    () =>
      new THREE.MeshStandardMaterial({
        color: colors.link,
        roughness: 0.6,
        flatShading: true,
      }),
    [colors.link],
  );
  const glow = useRef(0);
  // The material is a Three.js object, mutated per frame; it is not React state.
  /* eslint-disable react-hooks/immutability */
  useFrame((state, delta) => {
    glow.current = THREE.MathUtils.damp(glow.current, lit ? 1 : 0, 6, delta);
    const pulse = lit ? 0.75 + 0.25 * Math.sin(state.clock.elapsedTime * 6) : 1;
    material.color.set(colors.link).lerp(new THREE.Color(colors.accent), glow.current * pulse);
    material.emissive.set(colors.accent);
    material.emissiveIntensity = glow.current * 0.35 * pulse;
  });
  /* eslint-enable react-hooks/immutability */
  // Forward along z from the board to the row of the island, then sideways along x.
  const [x1, , z1] = from;
  const [x2, , z2] = to;
  const legZ = Math.abs(z2 - z1);
  const legX = Math.abs(x2 - x1);
  return (
    <group>
      <mesh
        geometry={UNIT_BOX}
        material={material}
        position={[x1, Y, (z1 + z2) / 2]}
        scale={[WIDTH, HEIGHT, Math.max(WIDTH, legZ + WIDTH)]}
        receiveShadow
      />
      {legX > WIDTH && (
        <mesh
          geometry={UNIT_BOX}
          material={material}
          position={[(x1 + x2) / 2, Y, z2]}
          scale={[legX + WIDTH, HEIGHT, WIDTH]}
          receiveShadow
        />
      )}
      {/* The plug, at the edge of the island. */}
      <mesh
        geometry={UNIT_BOX}
        material={material}
        position={[x2, 0.09, z2]}
        scale={[0.34, 0.18, 0.26]}
        castShadow
      />
      {/* The socket, at the foot of the blueprint. */}
      <mesh
        geometry={UNIT_BOX}
        material={material}
        position={[x1, 0.3 + 0.06, z1]}
        scale={[0.3, 0.12, 0.2]}
      />
    </group>
  );
}
