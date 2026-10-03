import type { ClassInfo, Value } from '@ljbu/contracts';

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

export function signature(method: { name: string; parameterTypes: string[] }): string {
  return `${method.name}(${method.parameterTypes.join(', ')})`;
}

/** How a Java value reads on a plaque. */
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

/** Where the plaques hang on a building of that height and depth: the top of its front. */
export function plaquePosition(height: number, depth: number): [number, number, number] {
  return [0, height - 0.12, depth / 2 + 0.03];
}

/** The attributes of the class and of its superclasses, top of the hierarchy first. */
export function declaredFields(type: string | undefined, classes: Map<string, ClassInfo>) {
  const chain: ClassInfo[] = [];
  for (let current = type; current;) {
    const info = classes.get(current);
    if (!info) {
      break;
    }
    chain.unshift(info);
    current = info.superclass ?? undefined;
  }
  return chain.flatMap((info) => info.fields.filter((field) => !field.static));
}

export function declaredMethods(type: string | undefined, classes: Map<string, ClassInfo>) {
  const seen = new Set<string>();
  const methods: ClassInfo['methods'] = [];
  for (let current = type; current;) {
    const info = classes.get(current);
    if (!info) {
      break;
    }
    for (const method of info.methods) {
      const key = signature(method);
      if (!seen.has(key) && !method.static) {
        seen.add(key);
        methods.push(method);
      }
    }
    current = info.superclass ?? undefined;
  }
  return methods;
}
