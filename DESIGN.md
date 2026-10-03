# DESIGN.md — Contrato visual

Este documento es el contrato visual del proyecto. Toda pantalla, componente y pieza 3D se
deriva de acá. No es una sugerencia de estilo: si algo no está definido, se define acá
primero y después se implementa.

- **Parte A:** la interfaz (pantallas, tipografía, color, componentes).
- **Parte B:** la escena 3D (cámara, piezas isométricas flotantes, estados).

## Dirección

**La interfaz se aparta; la maqueta y las explicaciones son protagonistas.**

- **Limpia:** fondos lisos, mucho espacio, bordes finos, sin sombras pesadas ni degradados.
- **La maqueta flota:** piezas isométricas low-poly en pastel suave, flotando sobre el
  fondo, sin suelo infinito ni cielo.
- **Todo texto apunta al código:** cada explicación referencia una línea; cada pieza
  también.
- **El estado se lee de un vistazo:** ✅ 🚧 ❌ siempre con la misma forma, color e ícono,
  en la maqueta y en la bitácora.

---

# Parte A · Interfaz

## A1. Tokens

Todo color, tipografía, espaciado, radio y duración sale de una variable CSS definida en
`apps/web/src/styles/tokens.css`. **Nunca** se escribe un color hex, una fuente o un valor
que ya tenga token.

### Color — tema claro

```
--color-bg            #F6F5F1   fondo de página
--color-surface       #FFFFFF   paneles, editor, tarjetas
--color-surface-2     #EFEDE7   barras, pestañas inactivas, chips
--color-border        #E2DFD7   bordes de 1px
--color-text          #1D1E22   texto principal
--color-text-muted    #62666E   texto secundario (cumple 4.5:1 sobre bg y surface)
--color-accent        #4458D6   acción principal, foco, enlaces
--color-on-accent     #FFFFFF   texto sobre --color-accent
--color-accent-soft   #E6E9FB   fondo de elementos activos
--color-success       #207A53   ✅
--color-warning       #975F00   🚧 (texto); relleno: --color-warning-soft
--color-danger        #BC3939   ❌
--color-success-soft  #E3F3EA
--color-warning-soft  #FBF0D9
--color-danger-soft   #F9E3E3
--color-code-line     #FFF6D6   línea de código resaltada
```

### Color — tema oscuro

```
--color-bg            #121318
--color-surface       #1A1B21
--color-surface-2     #23252D
--color-border        #2E3039
--color-text          #ECEDF0
--color-text-muted    #A0A4AD
--color-accent        #8C9BFF
--color-on-accent     #121318
--color-accent-soft   #262B4A
--color-success       #5CC796
--color-warning       #E8B24A
--color-danger        #F07C7C
--color-success-soft  #18322A
--color-warning-soft  #3A2E14
--color-danger-soft   #3D1E20
--color-code-line     #3A3420
```

El tema sigue `prefers-color-scheme` y se puede forzar con `data-theme="light|dark"` en
`<html>`; la elección manual se recuerda en `localStorage`.

**Contraste:** todo texto cumple WCAG 2.2 AA (4.5:1; 3:1 para texto ≥ 24px). Los colores
de estado del tema claro dan al menos 4.5:1 sobre `--color-bg`, `--color-surface`,
`--color-surface-2` y su propio fondo suave (medido: el más bajo es 4.52). Los colores de
estado nunca van solos: siempre con ícono y texto.

### Tipografía

```
--font-ui      "Inter Variable", system-ui, sans-serif
--font-display "Space Grotesk Variable", "Inter Variable", sans-serif
--font-code    "JetBrains Mono Variable", ui-monospace, monospace
```

Fuentes open source, autoalojadas con `@fontsource-variable/*` (sin pedidos a terceros).

| Token | Tamaño / interlineado | Uso |
|---|---|---|
| `--text-xs` | 12 / 16 | chips de línea, metadatos |
| `--text-sm` | 14 / 20 | bitácora, botones |
| `--text-md` | 16 / 24 | cuerpo, pedido de obra |
| `--text-lg` | 20 / 28 | títulos de panel |
| `--text-xl` | 28 / 34 | título de desafío (display) |
| `--text-2xl` | 40 / 46 | portada (display) |

El editor usa `--font-code` a 14px.

### Espaciado, radios, bordes, movimiento

```
--space-1 4px   --space-2 8px   --space-3 12px   --space-4 16px
--space-5 24px  --space-6 32px  --space-7 48px   --space-8 64px

--radius-sm 6px    chips, inputs
--radius-md 10px   botones, tarjetas
--radius-lg 16px   paneles

--border    1px solid var(--color-border)
--shadow-float 0 8px 24px rgb(0 0 0 / 0.06)   solo para elementos flotantes (popovers)

--duration-fast 120ms   --duration-base 200ms   --duration-slow 400ms
--ease-out cubic-bezier(0.2, 0.8, 0.2, 1)
```

Con `prefers-reduced-motion: reduce`, todas las duraciones pasan a 0 y la escena no anima
(ver B6).

## A2. Layout

### Vista de desafío (una sola pantalla: código y maqueta a la vez)

```
┌───────────────────────────────────────────────────────────────────────┐
│ ◂ Módulo 1 · Clases y objetos   1.3 · La facultad donde estudiás   ◐  │  barra (56px)
├────────────────────────────────┬──────────────────────────────────────┤
│ ▸ PEDIDO DE OBRA  ☑ 1/2        │                                      │
│ (panel desplegable: consigna,  │          maqueta 3D                  │
│  criterios, pistas, UTN real)  │   planos al fondo · edificios con    │
│────────────────────────────────│   placas y ventanillas · carteles    │
│ Main.java │ FacultadRegional   │                                      │
│────────────────────────────────│                      [ ¿Qué es? ]    │
│                                ├──────────────────────────────────────┤
│  editor Monaco                 │ BITÁCORA            ✅ Obra terminada│
│                                │ ✅ FR creada            Main.java:5  │
│                                │ 🚧 Sin provincia        Main.java:5  │
│                                │ ─────────────────────────────────────│
│                [ Ejecutar ▶ ]  │ ◀ ━━━━━●━━━━━━ ▶  Paso 4/9 · Main:7  │
└────────────────────────────────┴──────────────────────────────────────┘
   ~ 45%                              ~ 55%
```

- **Ejecutar y ver**: al ejecutar, la maqueta y la bitácora se actualizan en el lugar; no
  hay página de resultado. `/desafios/:id/resultado` redirige al desafío.
- **Pedido de obra**: panel desplegable sobre el editor. Se abre solo la primera vez que se
  entra al desafío y queda plegado después, mostrando el título y el progreso de los
  criterios (`☑ 1/2`). Adentro: consigna, "Lo que tenés que lograr" (se tilda en vivo),
  pistas y "En la UTN real".
- **Maqueta**: ocupa la mitad superior derecha. Botón "¿Qué es?" abre la **leyenda** de
  formas (B4). Clic en una pieza selecciona su línea en el editor y su entrada en la
  bitácora; clic en una entrada o un paso selecciona la pieza.
- **Bitácora + línea de tiempo**: mitad inferior derecha, con el encabezado de resultado.
  El chip de línea dice **archivo y línea** (`Main.java:7`), nunca solo `L7`.
- **Ancho mínimo soportado:** 360px. Por debajo de 960px los paneles se apilan (pedido →
  editor → maqueta → bitácora) y el editor es de solo lectura cómoda.
- Márgenes laterales: `--space-5` en escritorio, `--space-4` en móvil.
- Sin scroll horizontal de página.

## A3. Componentes

| Componente | Reglas |
|---|---|
| **Botón primario** (`Ejecutar`) | Fondo `--color-accent`, texto `--color-on-accent`, `--radius-md`, alto 40px. Uno solo por vista. |
| **Botón secundario** | Fondo `--color-surface-2`, texto `--color-text`. |
| **Pestañas de archivo** | Texto `--font-code` 13px; activa con borde inferior de 2px `--color-accent`. |
| **Entrada de bitácora** | Ícono de estado + título (1 línea, `--text-sm` semibold) + chip de línea + explicación (máx. 3 líneas) + pista opcional. Fondo `--color-*-soft` solo en la entrada seleccionada. |
| **Chip de línea** | `Main.java:12` en `--font-code` `--text-xs`, fondo `--color-surface-2`; al hacer clic resalta la línea y la pieza. |
| **Criterio del pedido** | Casilla que se marca sola cuando la verificación correspondiente pasa. |
| **Línea de tiempo** | Pista de 4px, cursor de 14px, botones paso anterior/siguiente, texto `Paso n / N · Archivo:línea`. |
| **Pista** | Panel desplegable dentro del pedido; nivel 1, 2, 3 y "Ver solución" (este último con confirmación). |
| **Leyenda** | Panel sobre la maqueta con una fila por forma de B4 (miniatura + nombre + una oración). |

Íconos: **Lucide** (licencia ISC), trazo 1.75px, 18px. Estados:
`circle-check` ✅ · `construction` 🚧 · `circle-x` ❌.

## A4. Escritura en la interfaz

- Español rioplatense con **voseo** ("Ejecutá", "Te falta…"), tono cercano y directo.
- Títulos en oración, no en mayúsculas.
- Cero jerga sin explicar; los términos de Java van en `código`.
- Las reglas de los mensajes de error están en [`docs/FEEDBACK.md`](docs/FEEDBACK.md).

## A5. Accesibilidad

- WCAG 2.2 AA.
- La **bitácora es el equivalente textual de la maqueta**: todo lo que muestra la escena
  está también en texto. El `<canvas>` tiene `aria-label` con el resumen del resultado.
- Todo es operable con teclado; foco visible (`outline: 2px solid var(--color-accent)`,
  separado 2px).
- Atajos: `Ctrl/Cmd + Enter` ejecuta; `←` `→` mueven la línea de tiempo cuando tiene foco.

---

# Parte B · Escena 3D

## B1. Cámara

- **Ortográfica isométrica fija:** rotación Y 45°, inclinación X ≈ 35.264°
  (`atan(1/√2)`). Sin perspectiva.
- Se permite **zoom** (0.6×–2×) y **desplazamiento** dentro de los límites de la escena.
- **No se permite rotación libre.** Opcional: girar en pasos de 90° con botones.
- Al cargar un resultado, la cámara encuadra todas las piezas con margen del 15%.

## B2. Unidad y grilla

- 1 unidad del mundo = 1 módulo de grilla.
- Cada pieza se apoya sobre su propia **isla flotante**: una losa de 0.3 de alto con
  bordes biselados.
- Separación mínima entre islas: 1 unidad.
- Distribución automática: los **planos en una fila al fondo** (los apilados por herencia
  ocupan una columna); el Rectorado al centro; Facultades Regionales en anillo alrededor
  (anillos concéntricos cuando no entran en uno); lo que pertenece a una FR
  (departamentos, carreras, personas) junto a la isla de su FR; las variables en una fila
  al frente.

## B3. Estilo low-poly pastel

- Geometría con caras planas (`flatShading`), polígonos mínimos, bordes ligeramente
  biselados.
- Materiales `MeshStandardMaterial` mates (`roughness 0.9`, `metalness 0`). Sin texturas.
- Sin contorno negro; la forma se lee por la luz.

### Paleta de la escena

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `scene-island` | `#EAE5DA` | `#3A3C46` | losa de las islas |
| `scene-island-top` | `#C9DDC0` | `#4F6656` | césped de la isla |
| `scene-wall` | `#F5EFE4` | `#D9D3C8` | paredes de edificios |
| `scene-rectorate` | `#A9C4EB` | `#7F9CC8` | techo / acento del Rectorado |
| `scene-faculty` | `#F2B8A9` | `#CF8E80` | Facultades Regionales |
| `scene-department` | `#C9B8E4` | `#A08DC2` | departamentos |
| `scene-career` | `#F4D68F` | `#CDAE66` | carreras |
| `scene-person` | `#F3C7A3` | `#C99D7C` | personas (figuras) |
| `scene-link` | `#7C8190` | `#B4B8C4` | cables de referencias |
| `scene-ghost` | blanco 22% | blanco 12% | silueta de la UTN real |
| `scene-blueprint` | `#DCE7F7` | `#2F3A52` | papel de los planos (clases) |
| `scene-plaque` | `#FFFDF7` | `#262A33` | placas de atributos en las fachadas |

La escena no tiene fondo propio: es transparente sobre `--color-bg`.

### Iluminación

- `HemisphereLight` (cielo cálido / suelo frío) intensidad 0.9.
- Una `DirectionalLight` desde arriba a la izquierda, sombras suaves (mapa 1024).
- **Sombras de contacto** difusas bajo cada isla, sobre un plano invisible, para reforzar
  que flota.

## B4. Lenguaje visual: cómo se ve Java

La maqueta muestra **dos cosas a la vez**: la UTN (qué construyó el alumno) y **Java** (con
qué lo construyó). Cada concepto del lenguaje tiene **una** forma, siempre la misma en los
20 desafíos, y la leyenda de la interfaz las lista. Regla: si dos cosas son distintas en
Java, se ven distintas; si se ven distintas, son distintas en Java.

| Concepto de Java | Forma | Detalle |
|---|---|---|
| **Clase** | **Plano** (`blueprint`): panel de obra parado al fondo de la escena, uno por clase, en una fila | Cabecera con el nombre; debajo, una placa por atributo (`nombre: String`) y una ventanilla por método. Datos de `result.classes` |
| **Objeto / instancia** | **Edificio sobre su isla**, con un **cable fino punteado** hasta su plano | El `new` lo construye a partir del plano (B6). `piece.type` dice cuál |
| **Atributo** (de instancia) | **Placa en la fachada**: `nombre = "Resistencia"` | Una por atributo, en el orden de la clase (heredados primero). El valor sale de `piece.fields` al final y de los `field_set` de la línea de tiempo durante la reproducción; sin valor todavía: `nombre = ?` |
| `private` | Placa **detrás de una reja** (ícono `lock`) | Se lee, no se toca desde afuera |
| `public` | Placa a la vista (sin ícono) | Package-private se dibuja como `public` con ícono `lock-open` atenuado |
| **Método** | **Ventanilla** en la fachada con su nombre: `getNombre()` | `public` al frente; `private` al costado. Se **ilumina** (`--color-accent`) mientras corre en el paso actual de la línea de tiempo (`call` con `pieceId`) |
| `static` | Placa o ventanilla con **bandera** (ícono `flag`), **en el plano**, no en los edificios | Pertenece a la clase: un solo valor compartido |
| `final` | Placa **remachada** (ícono `pin`) | No cambia después de asignarse |
| **Variable / referencia** | **Cartel en un poste** fuera de las islas + **cable** al edificio (`variable-sign`, `reference-link`) | `null`: cable que termina suelto |
| **Atributo que es una referencia** (composición) | Placa con **enchufe** del que sale un cable al edificio referenciado | Es un atributo más; el cable va de la placa, no del techo |
| **Colección** | Placa con enchufe múltiple: un cable por elemento dibujado | `List<Departamento>` con tres cables |
| **Herencia** | Edificio de **pisos** (`inheritance-floors`: planta baja = superclase) **y planos apilados** (el plano de la subclase sobre el de la superclase) | Las placas heredadas van en la planta baja |
| `abstract` | Plano con **borde punteado** y sello "ABSTRACTA"; método abstracto: ventanilla tapiada | No genera edificios |
| **Interfaz** | **Sello** (`interface-seal`): un panel redondo aparte, con sus ventanillas vacías; cada plano que la implementa lleva una **insignia** con su nombre, y sus edificios también (`interface-badge`) | No tiene atributos ni objetos: es un contrato |
| **Constructor** | **Grúa** sobre la isla mientras corre (`call` de `Clase.<init>`) | Con `super()`, construye primero la planta baja |

### Arquetipos de la UTN (la forma del edificio)

Qué edificio se dibuja lo decide el **binding** del desafío; lo de arriba (placas,
ventanillas, cable al plano) se dibuja **sobre cualquier arquetipo**.

| Arquetipo | Representa | Forma |
|---|---|---|
| `rectorate` | Rectorado | edificio ancho de 3 pisos con frontón |
| `regional-faculty` | Facultad Regional | edificio de 2×2 con techo a dos aguas |
| `department` | Departamento | pabellón bajo |
| `career` | Carrera | bloque con banderín |
| `person` | Persona y subtipos | figura low-poly (cilindro + cabeza) |
| `inheritance-floors` | Objeto con herencia | pisos apilados, uno por clase de la jerarquía |
| `slot` | Atributo de composición | pedestal junto a la pieza dueña (ocupado / vacío) |
| `variable-sign`, `reference-link` | Variable, referencia | cartel en poste, cable curvo |
| `interface-badge` | Interfaz implementada | insignia sobre el techo, una por interfaz; la agrega la pieza, no es binding |
| `generic-block` | Tipo sin binding | cubo |

Todas las piezas se **generan por código** desde `apps/web/src/scene/archetypes/`. Ningún
archivo de modelo externo en la v1.

Los carteles, placas, ventanillas y sellos son **texto HTML superpuesto** en `--font-ui`,
no texto 3D: se leen nítidos, se traducen y siguen el tema. Van en una sola capa sobre el
canvas (`scene/overlay.tsx`): cada uno se ancla a un objeto de la escena y un único bucle
los reposiciona por cuadro. No se usa `<Html>` de drei: crea un root de React por cartel y,
con React 19, desmontarlos durante un render deja carteles vacíos.

## B5. Estados de una pieza

| Estado | Aspecto | Ícono flotante |
|---|---|---|
| ✅ **correcto** | color pleno, opaca | `circle-check` en `--color-success` |
| 🚧 **incompleto** | color pleno; las partes faltantes se ven como **andamio** (aristas en `--color-warning`, caras al 15%) | `construction` |
| ❌ **falló** | desaturada 70%, con una grieta y detenida a media construcción | `circle-x` en `--color-danger` |
| **silueta real** | volumen translúcido `scene-ghost` donde debería haber algo según la UTN real | — |
| **seleccionada** | aro en el suelo de la isla en `--color-accent`, elevación +0.15 | — |

Una referencia `null` se dibuja como cable que termina suelto en el aire.

## B6. Animación

| Animación | Detalle |
|---|---|
| Flotación | Cada isla sube y baja 0.05 unidades, período 6 s, con desfase aleatorio estable |
| Construcción | Al reproducir un paso, la pieza crece desde la isla (escala Y 0→1) en `--duration-slow` con `--ease-out`; mientras corre el constructor hay una grúa sobre la isla |
| Placas | Cada `field_set` hace aparecer o cambiar la placa del atributo en la fachada |
| Ventanillas | La ventanilla del método que corre en el paso actual se ilumina |
| Herencia | Los pisos se construyen **de abajo hacia arriba**, en el orden real de los constructores (`super()` primero) |
| Referencia | El cable se "dibuja" del cartel a la pieza en `--duration-base` |
| Selección | Elevación suave en `--duration-fast` |

Con `prefers-reduced-motion: reduce`: sin flotación, sin crecimiento; los cambios de paso
son instantáneos.

## B7. Rendimiento

- Objetivo: 60 fps con 150 piezas en una notebook integrada; mínimo aceptable 30 fps.
- Geometrías y materiales compartidos por arquetipo; instancias cuando hay repetición.
- `dpr` máximo 2; sombras solo de la luz direccional.
- La escena se carga de forma diferida (`lazy`) para no frenar la carga del editor.
