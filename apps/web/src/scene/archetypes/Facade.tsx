import type { ClassInfo, Piece } from '@ljbu/contracts';
import type { SceneColors } from '../colors';
import { matte, UNIT_BOX, UNIT_CYLINDER } from '../materials';
import { ChipAnchor } from '../overlay';
import type { FieldState } from '../replay';
import {
  declaredFields,
  declaredMethods,
  formatValue,
  memberFlags,
  plaquePosition,
  signature,
} from '../java';

/** Vertical distance between stacked chips, on screen. */
const ROW_PX = 20;

interface Props {
  piece: Piece;
  classes: Map<string, ClassInfo>;
  fields: Map<string, FieldState> | undefined;
  /** The method running on this object at the chosen step; "<init>" while it is being built. */
  running: string | undefined;
  /** Attributes the brief expects and the code left empty. */
  missing: Set<string>;
  height: number;
  width: number;
  depth: number;
  ghost: boolean;
  colors: SceneColors;
}

/**
 * What Java shows on a building (DESIGN.md §B4): a plaque per attribute with its value, a
 * window per method (public at the front, private at the side) and a crane while the
 * constructor runs.
 */
export function Facade({
  piece,
  classes,
  fields,
  running,
  missing,
  height,
  width,
  depth,
  ghost,
  colors,
}: Props) {
  const attributes = declaredFields(piece.type, classes);
  const methods = declaredMethods(piece.type, classes);
  const publicMethods = methods.filter((method) => method.visibility !== 'private');
  const privateMethods = methods.filter((method) => method.visibility === 'private');
  return (
    <group>
      {attributes.map((field, index) => {
        const state = fields?.get(field.name);
        const isRef =
          state?.pieceId !== undefined ||
          (state?.pieceIds !== undefined && state.pieceIds.length > 0);
        const flags = memberFlags(field);
        if (isRef) {
          flags.push('ref');
        }
        if (missing.has(field.name)) {
          flags.push('missing');
        }
        const elements = state?.pieceIds;
        const value = isRef
          ? elements
            ? `[${String(elements.length)}]`
            : ''
          : formatValue(ghost ? null : (state?.value ?? null));
        return (
          <ChipAnchor
            key={field.name}
            id={`${piece.id}:plaque:${field.name}`}
            position={plaquePosition(height, depth)}
            offset={[0, index * ROW_PX]}
            chip={{
              shape: 'plaque',
              text: value ? `${field.name} = ${value}` : `${field.name} =`,
              flags,
              lit: false,
              ghost,
            }}
          />
        );
      })}
      {publicMethods.map((method, index) => (
        <ChipAnchor
          key={signature(method)}
          id={`${piece.id}:window:${signature(method)}`}
          position={[0, 0.2, depth / 2 + 0.03]}
          offset={[0, -index * ROW_PX]}
          chip={{
            shape: 'window',
            text: signature(method),
            flags: memberFlags(method),
            lit: running === method.name,
            ghost,
          }}
        />
      ))}
      {privateMethods.map((method, index) => (
        <ChipAnchor
          key={signature(method)}
          id={`${piece.id}:window:${signature(method)}`}
          position={[width / 2 + 0.03, 0.2, 0]}
          offset={[0, -index * ROW_PX]}
          chip={{
            shape: 'window',
            text: signature(method),
            flags: memberFlags(method),
            lit: running === method.name,
            ghost,
          }}
        />
      ))}
      {running === '<init>' && <Crane height={height} colors={colors} />}
    </group>
  );
}

/** A crane over the island while a constructor runs (DESIGN.md §B6). */
function Crane({ height, colors }: { height: number; colors: SceneColors }) {
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
