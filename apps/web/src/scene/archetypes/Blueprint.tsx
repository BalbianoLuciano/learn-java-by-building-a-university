import type { ClassInfo, Piece } from '@ljbu/contracts';
import type { ThreeEvent } from '@react-three/fiber';
import type { SceneColors } from '../colors';
import { blueprintBubble, type BubbleTexts } from '../java';
import { BLUEPRINT_HEIGHT, BLUEPRINT_WIDTH, type BlueprintPlacement } from '../layout';
import { matte, UNIT_BOX, UNIT_CYLINDER } from '../materials';
import { BubbleAnchor, ChipAnchor } from '../overlay';
import {
  Career,
  Department,
  GenericBlock,
  InheritanceFloors,
  Person,
  Rectorate,
  RegionalFaculty,
} from './Buildings';

const POST = 0.5;
const THICKNESS = 0.08;
/** The panel faces the camera, which looks along (1,1,1). */
const FACING = Math.PI / 4;

interface Props {
  info: ClassInfo;
  placement: BlueprintPlacement;
  /** The archetype the challenge binds the class to, for the miniature on the paper. */
  archetype: string | undefined;
  colors: SceneColors;
  selected: boolean;
  texts: BubbleTexts;
  onSelect: (name: string) => void;
}

/** A silhouette of what the class builds, drawn on its blueprint (DESIGN.md §B4). */
function Miniature({
  archetype,
  name,
  colors,
}: {
  archetype: string | undefined;
  name: string;
  colors: SceneColors;
}) {
  const piece: Piece = {
    id: `class:${name}`,
    archetype: (archetype ?? 'generic-block') as Piece['archetype'],
    state: 'passed',
    built: true,
    label: '',
    floors: archetype === 'inheritance-floors' ? [name] : undefined,
  };
  const look = { colors, failed: false, ghost: false };
  switch (archetype) {
    case 'regional-faculty':
      return <RegionalFaculty piece={piece} look={look} />;
    case 'rectorate':
      return <Rectorate piece={piece} look={look} />;
    case 'inheritance-floors':
      return <InheritanceFloors piece={piece} look={look} builtFloors={1} />;
    case 'department':
      return <Department piece={piece} look={look} />;
    case 'career':
      return <Career piece={piece} look={look} />;
    case 'person':
      return <Person piece={piece} look={look} />;
    default:
      return <GenericBlock piece={piece} look={look} />;
  }
}

/**
 * A class as a blueprint: a sheet on two posts at the back of the scene with the miniature of
 * the building it produces and its name. Its members show in a bubble when it is selected.
 */
export function Blueprint({
  info,
  placement,
  archetype,
  colors,
  selected,
  texts,
  onSelect,
}: Props) {
  const top = POST + BLUEPRINT_HEIGHT;
  const paper = matte(colors.blueprint, { ghost: info.abstract });
  const select = (event: ThreeEvent<MouseEvent>) => {
    event.stopPropagation();
    onSelect(info.name);
  };
  return (
    <group position={placement.position} rotation={[0, FACING, 0]} onClick={select}>
      {[-1, 1].map((side) => (
        <mesh
          key={side}
          geometry={UNIT_CYLINDER}
          material={matte(colors.link)}
          position={[side * (BLUEPRINT_WIDTH / 2 - 0.2), (POST + 0.4) / 2, 0]}
          scale={[0.06, POST + 0.4, 0.06]}
          castShadow
        />
      ))}
      <mesh
        geometry={UNIT_BOX}
        material={paper}
        position={[0, POST + BLUEPRINT_HEIGHT / 2, 0]}
        scale={[BLUEPRINT_WIDTH, BLUEPRINT_HEIGHT, THICKNESS]}
        castShadow
        receiveShadow
      />
      {selected && (
        <mesh
          geometry={UNIT_BOX}
          material={matte(colors.accent)}
          position={[0, POST + BLUEPRINT_HEIGHT / 2, -0.02]}
          scale={[BLUEPRINT_WIDTH + 0.12, BLUEPRINT_HEIGHT + 0.12, THICKNESS / 2]}
        />
      )}
      {/* The miniature stands on the paper, in front of it. */}
      <group position={[0, POST + 0.25, THICKNESS]} scale={[0.45, 0.45, 0.45]}>
        <Miniature archetype={archetype} name={info.name} colors={colors} />
      </group>
      <ChipAnchor
        id={`class:${info.name}:header`}
        position={[0, top - 0.2, THICKNESS]}
        chip={{
          shape: 'header',
          text: info.abstract ? `${info.name} · ${texts.abstract}` : info.name,
          flags: info.abstract ? ['abstract'] : [],
          lit: false,
          ghost: false,
        }}
      />
      {info.interfaces.map((name, index) => (
        <ChipAnchor
          key={name}
          id={`class:${info.name}:seal:${name}`}
          position={[BLUEPRINT_WIDTH / 2 - 0.2, top + 0.1, THICKNESS]}
          offset={[0, -index * 22]}
          chip={{ shape: 'seal', text: name, flags: [], lit: false, ghost: false }}
        />
      ))}
      {selected && (
        <BubbleAnchor
          id={`class:${info.name}:bubble`}
          position={[0, top + 0.5, THICKNESS]}
          bubble={blueprintBubble(info, texts)}
        />
      )}
    </group>
  );
}

/** An interface as a seal: a disk on a post; its contract shows in a bubble when selected. */
export function Seal({
  info,
  placement,
  colors,
  selected,
  texts,
  onSelect,
}: Omit<Props, 'archetype'>) {
  const radius = 0.8;
  const center = POST + radius;
  const select = (event: ThreeEvent<MouseEvent>) => {
    event.stopPropagation();
    onSelect(info.name);
  };
  return (
    <group position={placement.position} rotation={[0, FACING, 0]} onClick={select}>
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(colors.link)}
        position={[0, POST / 2, 0]}
        scale={[0.06, POST, 0.06]}
        castShadow
      />
      <mesh
        geometry={UNIT_CYLINDER}
        material={matte(selected ? colors.accent : colors.blueprint)}
        position={[0, center, 0]}
        rotation={[Math.PI / 2, 0, 0]}
        scale={[radius * 2, THICKNESS, radius * 2]}
        castShadow
      />
      <ChipAnchor
        id={`class:${info.name}:header`}
        position={[0, center, THICKNESS]}
        chip={{ shape: 'seal', text: info.name, flags: [], lit: false, ghost: false }}
      />
      {selected && (
        <BubbleAnchor
          id={`class:${info.name}:bubble`}
          position={[0, center + radius + 0.3, THICKNESS]}
          bubble={blueprintBubble(info, texts)}
        />
      )}
    </group>
  );
}
