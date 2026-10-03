import type { ClassInfo, Piece } from '@ljbu/contracts';
import { useFrame, type ThreeEvent } from '@react-three/fiber';
import { useRef } from 'react';
import * as THREE from 'three';
import {
  Career,
  Department,
  GenericBlock,
  InheritanceFloors,
  InterfaceBadges,
  Person,
  Rectorate,
  RegionalFaculty,
  Scaffold,
} from './archetypes/Buildings';
import { Island } from './archetypes/Island';
import { StateBadge } from './overlay';
import { Facade } from './archetypes/Facade';
import type { SceneColors } from './colors';
import type { Placement } from './layout';
import type { FieldState } from './replay';

interface Props {
  piece: Piece;
  placement: Placement;
  colors: SceneColors;
  /** Whether the piece exists at this moment of the replay; otherwise it is a silhouette. */
  visible: boolean;
  /** The learner classes, for the plaques and windows of the facade. */
  classes: Map<string, ClassInfo>;
  /** The value of each attribute at this moment. */
  fields: Map<string, FieldState> | undefined;
  /** The method running on the object at this moment. */
  running: string | undefined;
  selected: boolean;
  float: boolean;
  animate: boolean;
  /** For inheritance floors during the replay: how many floors are up. */
  builtFloors?: number;
  onSelect: (piece: Piece) => void;
}

/** Archetypes with a shape of their own; the rest are generic blocks. */
const DRAWN = [
  'regional-faculty',
  'rectorate',
  'inheritance-floors',
  'department',
  'career',
  'person',
];

/** One piece on its island: the archetype, its state and its facade (DESIGN.md §B4–B5). */
export function PieceView({
  piece,
  placement,
  colors,
  visible,
  classes,
  fields,
  running,
  selected,
  float,
  animate,
  builtFloors,
  onSelect,
}: Props) {
  const grower = useRef<THREE.Group>(null);
  const ghost = !visible;
  const failed = piece.state === 'failed' && !ghost;
  const look = { colors, failed, ghost };

  // Construction: the piece grows from its island (DESIGN.md §B6).
  useFrame((_, delta) => {
    if (!grower.current) {
      return;
    }
    const target = visible ? 1 : 0.0001;
    if (!animate || ghost) {
      grower.current.scale.y = ghost ? 1 : target;
      return;
    }
    grower.current.scale.y = THREE.MathUtils.damp(grower.current.scale.y, target, 8, delta);
  });

  const select = (event: ThreeEvent<MouseEvent>) => {
    event.stopPropagation();
    onSelect(piece);
  };
  const island = placement.island ?? { width: 1.8, depth: 1.8 };
  const missing = new Set(
    piece.state === 'incomplete' && fields
      ? [...fields]
          .filter(([, field]) => field.value === null || 'null' in field.value)
          .map(([name]) => name)
      : [],
  );
  const height =
    piece.archetype === 'rectorate'
      ? 1.9
      : piece.archetype === 'inheritance-floors'
        ? 0.7 * (piece.floors?.length ?? 1)
        : 1.1;

  return (
    <group
      position={placement.position}
      onClick={select}
      onPointerOver={(event) => {
        event.stopPropagation();
        document.body.style.cursor = 'pointer';
      }}
      onPointerOut={() => {
        document.body.style.cursor = '';
      }}
    >
      <Island
        width={island.width}
        depth={island.depth}
        phase={placement.phase}
        float={float && !ghost}
        selected={selected}
        ghost={ghost}
        colors={colors}
      >
        <group ref={grower} scale={[1, ghost ? 1 : 0.0001, 1]}>
          {piece.archetype === 'regional-faculty' && <RegionalFaculty piece={piece} look={look} />}
          {piece.archetype === 'rectorate' && <Rectorate piece={piece} look={look} />}
          {piece.archetype === 'inheritance-floors' && (
            <InheritanceFloors
              piece={piece}
              look={look}
              builtFloors={builtFloors ?? piece.floors?.length ?? 1}
            />
          )}
          {piece.archetype === 'department' && <Department piece={piece} look={look} />}
          {piece.archetype === 'career' && <Career piece={piece} look={look} />}
          {piece.archetype === 'person' && <Person piece={piece} look={look} />}
          {!DRAWN.includes(piece.archetype) && <GenericBlock piece={piece} look={look} />}
          {!ghost && <InterfaceBadges piece={piece} look={look} y={height + 1.2} />}
          {!ghost && piece.state === 'incomplete' && (
            <Scaffold
              size={[island.width - 0.3, height + 0.5, island.depth - 0.3]}
              position={[0, (height + 0.5) / 2, 0]}
              color={colors.warning}
            />
          )}
        </group>
        {!ghost && <StateBadge id={`${piece.id}:badge`} state={piece.state} y={height + 2.0} />}
        {!ghost && (
          <Facade
            piece={piece}
            classes={classes}
            fields={fields}
            running={running}
            missing={missing}
            height={height}
            width={island.width - 0.5}
            depth={island.depth - 0.6}
            ghost={ghost}
            colors={colors}
          />
        )}
      </Island>
    </group>
  );
}
