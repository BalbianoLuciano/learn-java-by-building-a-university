import type { Piece } from '@ljbu/contracts';
import { useFrame, type ThreeEvent } from '@react-three/fiber';
import { useRef } from 'react';
import * as THREE from 'three';
import {
  GenericBlock,
  InheritanceFloors,
  Rectorate,
  RegionalFaculty,
  Scaffold,
} from './archetypes/Buildings';
import { Island } from './archetypes/Island';
import { StateBadge } from './overlay';
import { Pedestal } from './archetypes/Links';
import type { SceneColors } from './colors';
import type { Placement } from './layout';

interface Props {
  piece: Piece;
  placement: Placement;
  colors: SceneColors;
  /** Whether the piece exists at this moment of the replay; otherwise it is a silhouette. */
  visible: boolean;
  /** Slots of the piece that are filled at this moment. */
  filledSlots: Set<string>;
  selected: boolean;
  float: boolean;
  animate: boolean;
  /** For inheritance floors during the replay: how many floors are up. */
  builtFloors?: number;
  onSelect: (piece: Piece) => void;
}

/** One piece on its island: the archetype, its state and its slots (DESIGN.md §B4–B5). */
export function PieceView({
  piece,
  placement,
  colors,
  visible,
  filledSlots,
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
  const height = piece.archetype === 'rectorate' ? 1.9 : 1.1;
  const slots = Object.entries(piece.slots ?? {});

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
          {!['regional-faculty', 'rectorate', 'inheritance-floors'].includes(piece.archetype) && (
            <GenericBlock piece={piece} look={look} />
          )}
          {!ghost && piece.state === 'incomplete' && slots.length === 0 && (
            <Scaffold
              size={[island.width - 0.3, height + 0.5, island.depth - 0.3]}
              position={[0, (height + 0.5) / 2, 0]}
              color={colors.warning}
            />
          )}
        </group>
        {!ghost && <StateBadge id={`${piece.id}:badge`} state={piece.state} y={height + 2.0} />}
        {!ghost &&
          slots.map(([name, slot], index) => {
            const angle = Math.PI / 4 + (index / Math.max(slots.length, 3)) * Math.PI * 2;
            const distance = island.width / 2 + 0.9;
            const occupied = slot.pieceIds.some((occupant) => filledSlots.has(occupant));
            return (
              <group
                key={name}
                position={[Math.cos(angle) * distance, 0, Math.sin(angle) * distance]}
              >
                <Pedestal
                  id={`${piece.id}:slot:${name}`}
                  colors={colors}
                  empty={!occupied}
                  label={name}
                />
                {!occupied && (
                  <Scaffold size={[0.7, 0.9, 0.7]} position={[0, 0.65, 0]} color={colors.warning} />
                )}
              </group>
            );
          })}
      </Island>
    </group>
  );
}
