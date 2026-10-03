import type { ClassInfo } from '@ljbu/contracts';
import { Edges } from '@react-three/drei';
import type { SceneColors } from '../colors';
import { BLUEPRINT_WIDTH, type BlueprintPlacement } from '../layout';
import { matte, UNIT_BOX, UNIT_CYLINDER } from '../materials';
import { ChipAnchor } from '../overlay';
import type { Chip } from '../overlayStore';
import { memberFlags, signature } from '../java';

/** Vertical distance between stacked chips, on screen. */
const ROW_PX = 20;
const POST = 0.5;
const THICKNESS = 0.08;

/**
 * A class as a blueprint: a panel on two posts at the back of the scene, with one plaque per
 * attribute and one window per constructor and method. Abstract classes are translucent.
 */
export function Blueprint({
  info,
  placement,
  colors,
}: {
  info: ClassInfo;
  placement: BlueprintPlacement;
  colors: SceneColors;
}) {
  const { height } = placement;
  const top = POST + height;
  const rows: { id: string; chip: Chip }[] = [
    ...info.fields.map((field) => ({
      id: `field:${field.name}`,
      chip: {
        shape: 'plaque' as const,
        text: `${field.name}: ${field.type}`,
        flags: memberFlags(field),
        lit: false,
        ghost: false,
      },
    })),
    ...info.constructors.map((constructor, index) => ({
      id: `ctor:${String(index)}`,
      chip: {
        shape: 'window' as const,
        text: `${info.name}(${constructor.parameterTypes.join(', ')})`,
        flags: memberFlags(constructor),
        lit: false,
        ghost: false,
      },
    })),
    ...info.methods.map((method) => ({
      id: `method:${signature(method)}`,
      chip: {
        shape: 'window' as const,
        text: signature(method),
        flags: memberFlags(method),
        lit: false,
        ghost: false,
      },
    })),
  ];
  const paper = matte(colors.blueprint, { ghost: info.abstract });
  return (
    <group position={placement.position}>
      {[-1, 1].map((side) => (
        <mesh
          key={side}
          geometry={UNIT_CYLINDER}
          material={matte(colors.link)}
          position={[side * (BLUEPRINT_WIDTH / 2 - 0.2), POST / 2 + 0.2, 0]}
          scale={[0.06, POST + 0.4, 0.06]}
          castShadow
        />
      ))}
      <mesh
        geometry={UNIT_BOX}
        material={paper}
        position={[0, POST + height / 2, 0]}
        scale={[BLUEPRINT_WIDTH, height, THICKNESS]}
        castShadow
        receiveShadow
      >
        <Edges color={colors.link} threshold={15} />
      </mesh>
      <ChipAnchor
        id={`class:${info.name}:header`}
        position={[0, top - 0.25, THICKNESS]}
        chip={{
          shape: 'header',
          text: info.abstract ? `${info.name} · abstracta` : info.name,
          flags: info.abstract ? ['abstract'] : [],
          lit: false,
          ghost: false,
        }}
      />
      {rows.map((row, index) => (
        <ChipAnchor
          key={row.id}
          id={`class:${info.name}:${row.id}`}
          position={[0, top - 0.25, THICKNESS]}
          offset={[0, 24 + index * ROW_PX]}
          chip={row.chip}
        />
      ))}
      {info.interfaces.map((name, index) => (
        <ChipAnchor
          key={name}
          id={`class:${info.name}:seal:${name}`}
          position={[BLUEPRINT_WIDTH / 2 - 0.3, top + 0.15 + index * 0.3, THICKNESS]}
          chip={{ shape: 'seal', text: name, flags: [], lit: false, ghost: false }}
        />
      ))}
    </group>
  );
}

/** An interface as a seal: a disk on a post, with one window per method of the contract. */
export function Seal({
  info,
  placement,
  colors,
}: {
  info: ClassInfo;
  placement: BlueprintPlacement;
  colors: SceneColors;
}) {
  const radius = 0.9;
  const center = POST + radius;
  return (
    <group position={placement.position}>
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(colors.link)}
        position={[0, POST / 2, 0]}
        scale={[0.06, POST, 0.06]}
        castShadow
      />
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(colors.accent)}
        position={[0, center, 0]}
        rotation={[Math.PI / 2, 0, 0]}
        scale={[radius * 2, THICKNESS, radius * 2]}
        castShadow
      />
      <ChipAnchor
        id={`class:${info.name}:header`}
        position={[0, center + 0.35, THICKNESS]}
        chip={{ shape: 'seal', text: info.name, flags: [], lit: false, ghost: false }}
      />
      {info.methods.map((method, index) => (
        <ChipAnchor
          key={signature(method)}
          id={`class:${info.name}:method:${signature(method)}`}
          position={[0, center, THICKNESS]}
          offset={[0, index * ROW_PX]}
          chip={{
            shape: 'window',
            text: signature(method),
            flags: ['abstract'],
            lit: false,
            ghost: false,
          }}
        />
      ))}
    </group>
  );
}
