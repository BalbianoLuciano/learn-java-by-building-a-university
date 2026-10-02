import { useMemo } from 'react';
import * as THREE from 'three';
import type { Piece } from '@ljbu/contracts';
import type { SceneColors } from '../colors';
import { matte, UNIT_BOX, UNIT_CYLINDER } from '../materials';
import { Label } from '../overlay';

/** A variable: a sign on a post, outside the islands (DESIGN.md §B4). */
export function VariableSign({
  piece,
  colors,
  ghost,
}: {
  piece: Piece;
  colors: SceneColors;
  ghost: boolean;
}) {
  const post = ghost ? matte(colors.ghost, { ghost: true }) : matte(colors.link);
  return (
    <group>
      <mesh
        geometry={UNIT_CYLINDER}
        material={post}
        position={[0, 0.55, 0]}
        scale={[0.08, 1.1, 0.08]}
        castShadow
      />
      <mesh
        geometry={UNIT_BOX}
        material={ghost ? post : matte(colors.surface)}
        position={[0, 1.2, 0]}
        scale={[0.9, 0.4, 0.06]}
        castShadow
      />
      <Label id={`${piece.id}:label`} text={piece.label} y={1.2} ghost={ghost} />
    </group>
  );
}

interface CableProps {
  from: THREE.Vector3;
  /** Where the cable ends; without it the cable hangs loose in the air (a null reference). */
  to?: THREE.Vector3;
  colors: SceneColors;
}

/** A reference: a curved cable from a sign (or a slot) to a piece (DESIGN.md §B4, §B5). */
export function Cable({ from, to, colors }: CableProps) {
  const geometry = useMemo(() => {
    const end = to ?? from.clone().add(new THREE.Vector3(0.9, -0.5, 0.9));
    const lift = to ? Math.max(1, from.distanceTo(to) * 0.3) : 0.3;
    const control = from
      .clone()
      .lerp(end, 0.5)
      .add(new THREE.Vector3(0, lift, 0));
    const curve = new THREE.QuadraticBezierCurve3(from, control, end);
    return new THREE.TubeGeometry(curve, 24, 0.035, 6, false);
  }, [from, to]);
  return <mesh geometry={geometry} material={matte(colors.link)} />;
}

/** A composition slot next to its owner: a pedestal, empty or with its occupant on top. */
export function Pedestal({
  id,
  colors,
  empty,
  label,
}: {
  id: string;
  colors: SceneColors;
  empty: boolean;
  label: string;
}) {
  return (
    <group>
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(colors.island)}
        position={[0, 0.1, 0]}
        scale={[0.9, 0.2, 0.9]}
        receiveShadow
      />
      {empty && <Label id={id} text={label} y={0.6} ghost />}
    </group>
  );
}
