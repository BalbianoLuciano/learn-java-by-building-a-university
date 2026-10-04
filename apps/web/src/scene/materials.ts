import * as THREE from 'three';
import { alphaOf } from './colors';

/** Mats and geometries are shared: 150 pieces must not mean 150 of each (DESIGN.md §B7). */
const materials = new Map<string, THREE.MeshStandardMaterial>();

/** A matte, flat-shaded material of the color (DESIGN.md §B3). */
export function matte(
  cssColor: string,
  options: { desaturate?: boolean; ghost?: boolean; lit?: boolean } = {},
): THREE.MeshStandardMaterial {
  const key = `${cssColor}|${options.desaturate ? 'd' : ''}${options.ghost ? 'g' : ''}${options.lit ? 'l' : ''}`;
  let material = materials.get(key);
  if (!material) {
    const color = new THREE.Color(cssColor);
    if (options.desaturate) {
      // A failed piece keeps 30% of its saturation.
      const hsl = { h: 0, s: 0, l: 0 };
      color.getHSL(hsl);
      color.setHSL(hsl.h, hsl.s * 0.3, hsl.l);
    }
    material = new THREE.MeshStandardMaterial({
      color,
      // Paper (a blueprint) glows a little, so it reads the same from any angle.
      emissive: options.lit ? color : new THREE.Color(0x000000),
      emissiveIntensity: options.lit ? 0.45 : 0,
      roughness: 0.9,
      metalness: 0,
      flatShading: true,
      transparent: options.ghost,
      opacity: options.ghost ? alphaOf(cssColor) : 1,
      depthWrite: !options.ghost,
    });
    materials.set(key, material);
  }
  return material;
}

export const UNIT_BOX = new THREE.BoxGeometry(1, 1, 1);
export const UNIT_CYLINDER = new THREE.CylinderGeometry(0.5, 0.5, 1, 12);
export const UNIT_SPHERE = new THREE.SphereGeometry(0.5, 8, 6);
export const UNIT_TORUS = new THREE.TorusGeometry(1, 0.04, 6, 32);

/** A triangular prism: a gable roof when laid along the x axis. Base on y = 0. */
export function prismGeometry(width: number, height: number, depth: number): THREE.BufferGeometry {
  const w = width / 2;
  const d = depth / 2;
  const vertices = new Float32Array([
    // left gable (x = -w)
    -w,
    0,
    -d,
    -w,
    0,
    d,
    -w,
    height,
    0,
    // right gable (x = w)
    w,
    0,
    d,
    w,
    0,
    -d,
    w,
    height,
    0,
    // front slope (z = +d edge up to the ridge)
    -w,
    0,
    d,
    w,
    0,
    d,
    w,
    height,
    0,
    -w,
    0,
    d,
    w,
    height,
    0,
    -w,
    height,
    0,
    // back slope
    w,
    0,
    -d,
    -w,
    0,
    -d,
    -w,
    height,
    0,
    w,
    0,
    -d,
    -w,
    height,
    0,
    w,
    height,
    0,
    // bottom
    -w,
    0,
    -d,
    w,
    0,
    -d,
    w,
    0,
    d,
    -w,
    0,
    -d,
    w,
    0,
    d,
    -w,
    0,
    d,
  ]);
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
  geometry.computeVertexNormals();
  return geometry;
}

export const GABLE = prismGeometry(1, 1, 1);
