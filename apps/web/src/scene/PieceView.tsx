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
import { Crane } from './archetypes/Crane';
import { Island } from './archetypes/Island';
import type { SceneColors } from './colors';
import { buildingBubble, heightOf, type BubbleTexts } from './java';
import type { Placement } from './layout';
import { BubbleAnchor, ChipAnchor, StateBadge } from './overlay';
import type { FieldState } from './replay';

interface Props {
  piece: Piece;
  placement: Placement;
  colors: SceneColors;
  /** Whether the piece exists at this moment of the replay; otherwise it is a silhouette. */
  visible: boolean;
  classes: Map<string, ClassInfo>;
  serials: Map<string, number>;
  /** The value of each attribute at this moment. */
  fields: Map<string, FieldState> | undefined;
  /** The method running on the object at this moment. */
  running: string | undefined;
  /** The attribute the chosen step just wrote on this object. */
  lastWritten: string | undefined;
  /** The variables that hang from this piece, in tag order. */
  tags: Piece[];
  selected: boolean;
  /** The bubble is open: the piece is selected or the chosen step touches it. */
  open: boolean;
  float: boolean;
  animate: boolean;
  /** For inheritance floors during the replay: how many floors are up. */
  builtFloors?: number;
  texts: BubbleTexts;
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

/** Vertical distance between stacked chips, on screen. */
const ROW_PX = 22;

/**
 * One piece on its island (DESIGN.md §B4–B5): the building, its serial plate, the tags of
 * the variables that point to it, and its bubble when it is open.
 */
export function PieceView({
  piece,
  placement,
  colors,
  visible,
  classes,
  serials,
  fields,
  running,
  lastWritten,
  tags,
  selected,
  open,
  float,
  animate,
  builtFloors,
  texts,
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
  const height = heightOf(piece);
  const serial = serials.get(piece.id);
  // What lives on an attached island is drawn smaller: it belongs to its owner.
  const scale = placement.owner ? 0.62 : 1;

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
        float={float && !ghost && !placement.owner}
        selected={selected}
        ghost={ghost}
        colors={colors}
      >
        <group scale={[scale, scale, scale]}>
          <group ref={grower} scale={[1, ghost ? 1 : 0.0001, 1]}>
            {piece.archetype === 'regional-faculty' && (
              <RegionalFaculty piece={piece} look={look} />
            )}
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
            {!ghost && piece.state === 'incomplete' && (
              <Scaffold
                size={[island.width / scale - 0.3, height + 0.5, island.depth / scale - 0.3]}
                position={[0, (height + 0.5) / 2, 0]}
                color={colors.warning}
              />
            )}
            {!ghost && <InterfaceBadges piece={piece} look={look} y={height + 1.2} />}
          </group>
          {!ghost && running === '<init>' && <Crane height={height} colors={colors} />}
        </group>
        {/* The serial plate hangs under the name of the building. */}
        {!ghost && serial !== undefined && piece.type && (
          <ChipAnchor
            id={`${piece.id}:plate`}
            position={[0, (height + 0.9) * scale, 0]}
            offset={[0, 24]}
            chip={{
              shape: 'plate',
              text: texts.serial(piece.type, serial),
              flags: [],
              lit: false,
              ghost: false,
            }}
          />
        )}
        {/* Variables hang as tags from the front-left corner, one under the other. */}
        {!ghost &&
          tags.map((tag, index) => (
            <ChipAnchor
              key={tag.id}
              id={`${tag.id}:tag`}
              position={[
                (-island.width / 2) * 0.55,
                height * 0.75 * scale,
                (island.depth / 2) * 0.6,
              ]}
              offset={[0, index * ROW_PX]}
              chip={{ shape: 'tag', text: tag.label, flags: [], lit: false, ghost: false }}
            />
          ))}
        {!ghost && (
          <StateBadge id={`${piece.id}:badge`} state={piece.state} y={(height + 2.0) * scale} />
        )}
        {open && (
          <BubbleAnchor
            id={`${piece.id}:bubble`}
            position={[0, (height + 2.3) * scale, 0]}
            bubble={buildingBubble(piece, classes, serials, fields, running, lastWritten, texts)}
          />
        )}
      </Island>
    </group>
  );
}
