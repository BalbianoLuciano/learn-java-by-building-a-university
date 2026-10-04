import { Edges } from '@react-three/drei';
import type { Piece } from '@ljbu/contracts';
import * as THREE from 'three';
import type { SceneColors } from '../colors';
import { GABLE, matte, UNIT_BOX, UNIT_CYLINDER, UNIT_SPHERE } from '../materials';
import { ChipAnchor } from '../overlay';

/** How a building is painted: as built, broken, or as the silhouette of what should be there. */
export interface Look {
  colors: SceneColors;
  failed: boolean;
  ghost: boolean;
}

function paint(color: string, look: Look): THREE.Material {
  if (look.ghost) {
    return matte(look.colors.ghost, { ghost: true });
  }
  return matte(color, { desaturate: look.failed });
}

/** A crack on the front face of a broken piece. */
function Crack({ width, height }: { width: number; height: number }) {
  const points = [0.1, 0.92, -0.05, 0.7, 0.12, 0.5, -0.02, 0.3, 0.08, 0.1].map((v, i, all) =>
    i % 2 === 0 ? new THREE.Vector3(v * width, (all[i + 1] ?? 0) * height, 0.005) : null,
  );
  const geometry = new THREE.BufferGeometry().setFromPoints(
    points.filter((p): p is THREE.Vector3 => p !== null),
  );
  return (
    <line>
      <primitive object={geometry} attach="geometry" />
      <lineBasicMaterial color="#000000" transparent opacity={0.55} />
    </line>
  );
}

/** Facultad Regional: a two-by-two building with a gable roof (DESIGN.md §B4). */
export function RegionalFaculty({ look }: { piece: Piece; look: Look }) {
  const height = look.failed ? 0.6 : 1.1;
  return (
    <group>
      <mesh
        geometry={UNIT_BOX}
        material={paint(look.colors.wall, look)}
        position={[0, height / 2, 0]}
        scale={[1.7, height, 1.4]}
        castShadow
        receiveShadow
      />
      {!look.failed && (
        <mesh
          geometry={GABLE}
          material={paint(look.colors.faculty, look)}
          position={[0, height, 0]}
          scale={[1.9, 0.6, 1.6]}
          castShadow
        />
      )}
      {look.failed && (
        <group position={[0, 0, 0.71]}>
          <Crack width={1} height={height} />
        </group>
      )}
    </group>
  );
}

/** Rectorado: a wide three-story building with a pediment. */
export function Rectorate({ look }: { piece: Piece; look: Look }) {
  const height = look.failed ? 1.0 : 1.9;
  return (
    <group>
      <mesh
        geometry={UNIT_BOX}
        material={paint(look.colors.wall, look)}
        position={[0, height / 2, 0]}
        scale={[2.9, height, 1.7]}
        castShadow
        receiveShadow
      />
      {[-0.9, -0.3, 0.3, 0.9].map((x) => (
        <mesh
          key={x}
          geometry={UNIT_CYLINDER}
          material={paint(look.colors.wall, look)}
          position={[x, height / 2, 0.95]}
          scale={[0.16, height, 0.16]}
          castShadow
        />
      ))}
      {!look.failed && (
        <>
          <mesh
            geometry={UNIT_BOX}
            material={paint(look.colors.rectorate, look)}
            position={[0, height + 0.08, 0]}
            scale={[3.1, 0.16, 1.9]}
            castShadow
          />
          <mesh
            geometry={GABLE}
            material={paint(look.colors.rectorate, look)}
            position={[0, height + 0.16, 0.6]}
            scale={[1.6, 0.5, 0.9]}
            castShadow
          />
        </>
      )}
      {look.failed && (
        <group position={[0, 0, 0.86]}>
          <Crack width={1.4} height={height} />
        </group>
      )}
    </group>
  );
}

/** Departamento de Enseñanza: a low pavilion with a flat roof (DESIGN.md §B4). */
export function Department({ look }: { piece: Piece; look: Look }) {
  const height = look.failed ? 0.4 : 0.7;
  return (
    <group>
      <mesh
        geometry={UNIT_BOX}
        material={paint(look.colors.wall, look)}
        position={[0, height / 2, 0]}
        scale={[1.5, height, 1.0]}
        castShadow
        receiveShadow
      />
      <mesh
        geometry={UNIT_BOX}
        material={paint(look.colors.department, look)}
        position={[0, height + 0.06, 0]}
        scale={[1.6, 0.12, 1.1]}
        castShadow
      />
      {look.failed && (
        <group position={[0, 0, 0.51]}>
          <Crack width={0.9} height={height} />
        </group>
      )}
    </group>
  );
}

/** Carrera: a block with a pennant on a pole. */
export function Career({ look }: { piece: Piece; look: Look }) {
  const height = look.failed ? 0.5 : 0.9;
  return (
    <group>
      <mesh
        geometry={UNIT_BOX}
        material={paint(look.colors.career, look)}
        position={[0, height / 2, 0]}
        scale={[1.0, height, 1.0]}
        castShadow
        receiveShadow
      />
      <mesh
        geometry={UNIT_CYLINDER}
        material={paint(look.colors.link, look)}
        position={[0.3, height + 0.45, 0.3]}
        scale={[0.05, 0.9, 0.05]}
        castShadow
      />
      <mesh
        geometry={GABLE}
        material={paint(look.colors.career, look)}
        position={[0.49, height + 0.75, 0.3]}
        rotation={[0, 0, -Math.PI / 2]}
        scale={[0.3, 0.36, 0.04]}
        castShadow
      />
      {look.failed && (
        <group position={[0, 0, 0.51]}>
          <Crack width={0.6} height={height} />
        </group>
      )}
    </group>
  );
}

/** Persona: a low-poly figure, a capsule-like body with a head. */
export function Person({ look }: { piece: Piece; look: Look }) {
  const bodyHeight = 0.7;
  return (
    <group scale={look.failed ? [1, 0.6, 1] : [1, 1, 1]}>
      <mesh
        geometry={UNIT_CYLINDER}
        material={paint(look.colors.person, look)}
        position={[0, bodyHeight / 2, 0]}
        scale={[0.5, bodyHeight, 0.5]}
        castShadow
        receiveShadow
      />
      <mesh
        geometry={UNIT_SPHERE}
        material={paint(look.colors.wall, look)}
        position={[0, bodyHeight + 0.26, 0]}
        scale={[0.42, 0.42, 0.42]}
        castShadow
      />
    </group>
  );
}

/** The interfaces a piece implements: one seal under its nameplate per interface (DESIGN.md §B4). */
export function InterfaceBadges({ piece, look, y }: { piece: Piece; look: Look; y: number }) {
  const interfaces = piece.interfaces ?? [];
  return (
    <group>
      {interfaces.map((name, index) => (
        <ChipAnchor
          key={name}
          id={`${piece.id}:interface:${name}`}
          position={[0, y, 0]}
          offset={[0, -30 - index * 22]}
          chip={{ shape: 'seal', text: name, flags: [], lit: false, ghost: look.ghost }}
        />
      ))}
    </group>
  );
}

/** A type without a binding: a cube with a sign. */
export function GenericBlock({ look }: { piece: Piece; look: Look }) {
  const height = look.failed ? 0.6 : 1.1;
  return (
    <group>
      <mesh
        geometry={UNIT_BOX}
        material={paint(look.colors.wall, look)}
        position={[0, height / 2, 0]}
        scale={[1.1, height, 1.1]}
        castShadow
        receiveShadow
      />
      {look.failed && (
        <group position={[0, 0, 0.56]}>
          <Crack width={0.7} height={height} />
        </group>
      )}
    </group>
  );
}

/**
 * An object with inheritance: one floor per class, the superclass at the bottom. Floors above
 * {@code builtFloors} are not up yet (replay of the constructors, DESIGN.md §B6).
 */
export function InheritanceFloors({
  piece,
  look,
  builtFloors,
}: {
  piece: Piece;
  look: Look;
  builtFloors: number;
}) {
  const floors = piece.floors ?? [piece.label];
  const floorHeight = 0.7;
  const tints = [look.colors.wall, look.colors.faculty, look.colors.department, look.colors.career];
  return (
    <group>
      {floors.slice(0, builtFloors).map((name, index) => (
        <group key={name} position={[0, index * floorHeight, 0]}>
          <mesh
            geometry={UNIT_BOX}
            material={paint(tints[index % tints.length] ?? look.colors.wall, look)}
            position={[0, floorHeight / 2, 0]}
            scale={[1.7 - index * 0.1, floorHeight, 1.4 - index * 0.1]}
            castShadow
            receiveShadow
          />
        </group>
      ))}
    </group>
  );
}

/** The scaffold of what is missing (DESIGN.md §B5): warning edges and faint faces. */
export function Scaffold({
  size,
  position,
  color,
}: {
  size: [number, number, number];
  position: [number, number, number];
  color: string;
}) {
  return (
    <mesh geometry={UNIT_BOX} position={position} scale={size}>
      <meshStandardMaterial color={color} transparent opacity={0.15} depthWrite={false} />
      <Edges color={color} threshold={15} />
    </mesh>
  );
}
