import type { ClassInfo, Piece, Value } from '@ljbu/contracts';
import type { Bubble, Chip } from './overlayStore';
import type { FieldState } from './replay';

/** The flags of a member, for its chip (DESIGN.md §B4). */
export function memberFlags(member: {
  visibility: string;
  static?: boolean;
  final?: boolean;
  abstract?: boolean;
}): string[] {
  const flags: string[] = [];
  if (member.visibility === 'private') {
    flags.push('private');
  }
  if (member.static) {
    flags.push('static');
  }
  if (member.final) {
    flags.push('final');
  }
  if (member.abstract) {
    flags.push('abstract');
  }
  return flags;
}

/** The height of the building of a piece. */
export function heightOf(piece: Piece): number {
  return piece.archetype === 'rectorate'
    ? 1.9
    : piece.archetype === 'inheritance-floors'
      ? 0.7 * (piece.floors?.length ?? 1)
      : 1.1;
}

export function signature(method: { name: string; parameterTypes: string[] }): string {
  return `${method.name}(${method.parameterTypes.join(', ')})`;
}

/** How a Java value reads in a bubble. */
export function formatValue(value: Value | null): string {
  if (value === null) {
    return '?';
  }
  if ('string' in value) {
    return `"${value.string}"`;
  }
  if ('int' in value) {
    return String(value.int);
  }
  if ('double' in value) {
    return String(value.double);
  }
  if ('boolean' in value) {
    return String(value.boolean);
  }
  if ('char' in value) {
    return `'${value.char}'`;
  }
  if ('null' in value) {
    return 'null';
  }
  return '→';
}

/** The class and its superclasses, top of the hierarchy first. */
export function hierarchyOf(
  type: string | undefined,
  classes: Map<string, ClassInfo>,
): ClassInfo[] {
  const chain: ClassInfo[] = [];
  for (let current = type; current;) {
    const info = classes.get(current);
    if (!info) {
      break;
    }
    chain.unshift(info);
    current = info.superclass ?? undefined;
  }
  return chain;
}

/** The serial of every object: its place among the instances of its class, in creation order. */
export function serialsOf(pieces: Piece[]): Map<string, number> {
  const counters = new Map<string, number>();
  const serials = new Map<string, number>();
  for (const piece of pieces) {
    if (piece.built && piece.type) {
      const next = (counters.get(piece.type) ?? 0) + 1;
      counters.set(piece.type, next);
      serials.set(piece.id, next);
    }
  }
  return serials;
}

export interface BubbleTexts {
  attributes: string;
  methods: string;
  constructors: string;
  serial: (type: string, number: number) => string;
  abstract: string;
  ghost: string;
}

/**
 * What the bubble of a building says (DESIGN.md §B4): the class and serial, then one plaque
 * per attribute with its value (grouped by floor when there is inheritance) and one window per
 * method, lit when it is the one running.
 */
export function buildingBubble(
  piece: Piece,
  classes: Map<string, ClassInfo>,
  serials: Map<string, number>,
  fields: Map<string, FieldState> | undefined,
  running: string | undefined,
  lastWritten: string | undefined,
  texts: BubbleTexts,
): Bubble {
  const type = piece.type ?? '';
  const chain = hierarchyOf(type, classes);
  const serial = serials.get(piece.id);
  const title = piece.label;
  const subtitle = serial !== undefined ? texts.serial(type, serial) : type;
  if (!piece.built) {
    return { title, subtitle, note: texts.ghost, sections: [] };
  }
  const sections: Bubble['sections'] = [];
  for (const info of chain) {
    const rows: Chip[] = info.fields
      .filter((field) => !field.static)
      .map((field) => {
        const state = fields?.get(field.name);
        const flags = memberFlags(field);
        let value: string;
        if (state?.pieceId) {
          const target = serials.get(state.pieceId);
          value = target === undefined ? '→' : `→ #${String(target)}`;
          flags.push('ref');
        } else if (state?.pieceIds) {
          value = `[${String(state.pieceIds.length)}]`;
          flags.push('ref');
        } else {
          value = formatValue(state?.value ?? null);
          if (state?.value && 'null' in state.value) {
            flags.push('null');
          }
        }
        return {
          shape: 'plaque' as const,
          text: `${field.name} = ${value}`,
          flags,
          lit: lastWritten === field.name,
          ghost: false,
        };
      });
    if (rows.length > 0) {
      sections.push({
        title: chain.length > 1 ? `${texts.attributes} · ${info.name}` : texts.attributes,
        rows,
      });
    }
  }
  const methods: Chip[] = [];
  const seen = new Set<string>();
  for (const info of [...chain].reverse()) {
    for (const method of info.methods) {
      const key = signature(method);
      if (!seen.has(key) && !method.static) {
        seen.add(key);
        methods.push({
          shape: 'window',
          text: key,
          flags: memberFlags(method),
          lit: running === method.name,
          ghost: false,
        });
      }
    }
  }
  if (methods.length > 0) {
    sections.push({ title: texts.methods, rows: methods });
  }
  return { title, subtitle, sections };
}

/** What the bubble of a blueprint says: every member of the class, static ones with a flag. */
export function blueprintBubble(info: ClassInfo, texts: BubbleTexts): Bubble {
  const sections: Bubble['sections'] = [];
  if (info.fields.length > 0) {
    sections.push({
      title: texts.attributes,
      rows: info.fields.map((field) => ({
        shape: 'plaque' as const,
        text: `${field.name}: ${field.type}`,
        flags: memberFlags(field),
        lit: false,
        ghost: false,
      })),
    });
  }
  if (info.constructors.length > 0 && info.kind !== 'interface') {
    sections.push({
      title: texts.constructors,
      rows: info.constructors.map((constructor) => ({
        shape: 'window' as const,
        text: `${info.name}(${constructor.parameterTypes.join(', ')})`,
        flags: memberFlags(constructor),
        lit: false,
        ghost: false,
      })),
    });
  }
  if (info.methods.length > 0) {
    sections.push({
      title: texts.methods,
      rows: info.methods.map((method) => ({
        shape: 'window' as const,
        text: signature(method),
        flags: info.kind === 'interface' ? ['abstract'] : memberFlags(method),
        lit: false,
        ghost: false,
      })),
    });
  }
  const parents = [info.superclass, ...info.interfaces].filter((name): name is string => !!name);
  return {
    title: info.name,
    subtitle: [info.abstract ? texts.abstract : null, ...parents.map((name) => `← ${name}`)]
      .filter((part): part is string => part !== null)
      .join(' · '),
    sections,
  };
}
