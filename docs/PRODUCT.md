# Producto

> Spec de producto. Define **qué** construimos y **para quién**. El **cómo** está en
> [`ARCHITECTURE.md`](ARCHITECTURE.md) y el aspecto visual en [`../DESIGN.md`](../DESIGN.md).

## 1. Visión

Aprender Programación Orientada a Objetos en Java **modelando una institución real que el
alumno ya conoce: la Universidad Tecnológica Nacional (UTN)**. El alumno escribe Java de
verdad, lo ejecuta y ve en una maqueta 3D isométrica qué construyó su código, qué le falta
y por qué algo falló, siempre explicado con referencia a las líneas que escribió.

## 2. Problema

En Programación II los conceptos de POO (referencias, aliasing, `super()`, composición vs
herencia, polimorfismo) se explican en el pizarrón o con ejemplos abstractos
(`Figura → Círculo`). El alumno los memoriza pero no los **ve**: no ve que dos variables
apuntan al mismo objeto, ni que la parte de `Persona` de un `Docente` se construye primero.

## 3. Público

**Persona principal:** estudiante de Programación II de la UTN (por ejemplo, de la
Tecnicatura Universitaria en Programación).

- Ya sabe: variables, tipos, condicionales, bucles, funciones, arreglos.
- Viene a aprender: clases, objetos, constructores, encapsulamiento, herencia,
  composición, polimorfismo e interfaces.
- Usa: navegador en notebook o PC. El celular es secundario (ver §7).

## 4. Principios

1. **Código real.** El alumno escribe Java que compila con `javac` y corre en una JVM. No hay
   bloques, ni pseudocódigo, ni botones que reemplacen al código.
2. **Fiel a la semántica de Java.** Lo visual nunca puede sugerir algo falso. Una variable
   *apunta* a un objeto; no lo contiene. Si una metáfora no representa lo que hace la JVM,
   no se usa.
3. **Fiel a la UTN real.** El dominio es la estructura actual de la UTN, con fuentes
   ([`DOMAIN.md`](DOMAIN.md)). No se inventan órganos, cargos ni relaciones.
4. **El feedback enseña.** Cada error dice qué pasó, por qué, en qué línea y da una pista,
   sin regalar la solución ([`FEEDBACK.md`](FEEDBACK.md)).
5. **Interfaz limpia.** La maqueta y los textos son protagonistas; la interfaz se aparta.
6. **Respeto por las fuentes.** Complementa al curso de la cátedra y lo cita; no copia su
   contenido.

## 5. Experiencia principal

```
Inicio → Módulo → Desafío ("pedido de obra")
   └─ Editor Java (con código base) ──[Ejecutar]──► Vista de resultado
                                                    ├─ Maqueta 3D (✅ 🚧 ❌ por pieza)
                                                    ├─ Bitácora (mensajes ligados a líneas)
                                                    └─ Línea de tiempo (paso a paso)
```

1. El alumno lee el **pedido de obra**: una consigna ambientada en la UTN real
   (p. ej. "Modelá la Facultad Regional Resistencia con su decano").
2. Escribe Java en el editor, que trae código base.
3. Aprieta **Ejecutar** (o `Ctrl/Cmd + Enter`).
4. Pasa a la **vista de resultado**:
   - **Maqueta 3D**: cada pieza que su código construyó, en estado ✅ correcto,
     🚧 incompleto o ❌ falló, junto a la silueta de cómo es en la UTN real.
   - **Bitácora**: un mensaje por cada pieza o problema, con la línea de código asociada.
   - **Línea de tiempo**: reproduce la construcción paso a paso, línea por línea.
5. Hacer clic en una pieza resalta su línea y hacer clic en una línea resalta su pieza.
6. Si se traba: pistas escalonadas y, tras varios intentos, una solución explicada.
7. El desafío se completa cuando todas las verificaciones pasan; el progreso queda guardado
   en el navegador.

## 6. Alcance de la v1

| Incluido | Detalle |
|---|---|
| 4 módulos | Clases, objetos y aliasing · Constructores y encapsulamiento · Herencia y composición · Polimorfismo e interfaces ([`CURRICULUM.md`](CURRICULUM.md)) |
| Ejecución real | Compilación y ejecución en servidor, en sandbox ([`SECURITY.md`](SECURITY.md)) |
| Vista de resultado | Maqueta 3D + bitácora + línea de tiempo |
| Pistas | Escalonadas (3 niveles) + solución explicada |
| Progreso | Local, en el navegador, sin cuentas |
| Idioma | Interfaz y contenido en español, preparado para traducir |
| Temas | Claro y oscuro |
| Accesibilidad | WCAG 2.2 AA; la bitácora es el equivalente textual de la maqueta |

## 7. Fuera de alcance de la v1

- Cuentas de usuario, login o sincronización entre dispositivos.
- Panel para docentes o seguimiento de alumnos.
- Modo libre (sandbox sin consigna).
- Contenido en inglés (la estructura sí queda preparada).
- Edición cómoda en celular: en pantallas chicas se puede leer y ver resultados, pero se
  prioriza escritorio para escribir código.
- Pistas generadas por IA.
- Temas fuera de los 4 módulos (colecciones, excepciones propias, archivos, etc.).
- Analítica o seguimiento de usuarios.

## 8. Métricas de éxito

| Métrica | Objetivo v1 |
|---|---|
| Tiempo desde que abre la web hasta su primera ejecución | < 60 s |
| Latencia de una ejecución (p95, del clic al resultado) | < 4 s |
| Compañeros de cursada que completan el módulo 1 en la beta | ≥ 5 |
| Errores de feedback reportados como "confusos o incorrectos" | se registran y corrigen antes de la v1.0 |

## 9. Glosario

| Término | Significado |
|---|---|
| **Pedido de obra** | La consigna de un desafío, ambientada en la UTN real. |
| **Pieza** | Elemento visual de la maqueta que representa un objeto, clase o relación. |
| **Verificación** (*check*) | Regla que comprueba si el código cumple una parte del pedido. |
| **Bitácora** | Lista de mensajes de una ejecución, cada uno ligado a una línea y a una pieza. |
| **Traza** (*trace*) | Registro paso a paso de lo que hizo el programa al ejecutarse. |
| **Silueta real** | Representación translúcida de cómo es esa parte en la UTN real. |
