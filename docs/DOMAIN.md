# Dominio: la UTN real

> Spec del dominio. Define **qué parte de la UTN real** modela el curso, con qué reglas y con
> qué fuentes. Los hechos y enlaces están en
> [`research/2026-10-utn-estructura.md`](research/2026-10-utn-estructura.md).
> Los datos que usa la plataforma están en `content/domain/` y deben coincidir con este
> documento.

## 1. Política de fidelidad

1. **Solo hechos con fuente.** Cada órgano, cargo, cantidad o regla del curso cita un
   artículo del Estatuto o una fuente oficial. Si no hay fuente, no entra.
2. **Instituciones reales, personas ficticias.** Se usan las 30 Facultades Regionales, sus
   ciudades y los órganos reales. **No se usan nombres de personas reales** (autoridades
   actuales, docentes): los cargos cambian y no queremos atribuirles nada. Las personas de
   los desafíos son ficticias.
3. **Simplificar sin falsear.** Un desafío puede modelar una parte (p. ej. solo el Decano de
   una FR), pero nunca contradecir la estructura real. Si se simplifica, la consigna lo
   dice ("en este desafío ignoramos al Vicedecano").
4. **Marcar lo incierto.** Los datos marcados ⚠ en la investigación no se usan como regla
   que el alumno deba verificar en código.
5. **Revisión periódica.** Al inicio de cada año lectivo se revisan las fuentes; la fecha de
   la última revisión figura en `content/domain/meta.json`.

**Última revisión:** 2026-10-01 · **Estatuto:** Res. AU 1/2011 (BO 23/12/2011).

## 2. Mapa del dominio

```
UTN
├── Rectorado (Sarmiento 440, CABA)
│   ├── Rector · Vicerrector                      (cargos unipersonales, 4 años)
│   └── Secretarías del Rectorado                  (designadas por el Rector; no estatutarias)
├── Órganos colegiados de la universidad
│   ├── Asamblea Universitaria                     (órgano supremo)
│   └── Consejo Superior                           (Rector + Decanos + 15 doc + 5 grad + 5 est + 5 nodoc)
└── Facultades Regionales (30)
    ├── Decano · Vicedecano                        (4 años; profesores con ≥ 3 años)
    ├── Asamblea de FR · Consejo Directivo
    ├── Secretarías de FR                          (varían entre FR)
    ├── Departamentos de Enseñanza
    │   ├── Director de Departamento · Consejo Departamental
    │   └── Áreas → Cátedras (Profesor Titular)
    └── Regionales Académicas                      (dependen de una FR; sin gobierno propio)

Comunidad: 4 claustros → docentes · graduados · estudiantes · no docentes
```

## 3. Entidades del curso

Nombres en español, tal como los escribe el alumno.

| Clase | Qué representa | Atributos base | Fuente |
|---|---|---|---|
| `Universidad` | La UTN | `nombre`, `rectorado`, `facultades` | art. 5 |
| `Rectorado` | Sede central | `direccion`, `rector`, `vicerrector` | Sedes, arts. 66–71 |
| `UnidadAcademica` (abstracta) | Lo que compone la universidad | `nombre`, `ciudad`, `provincia` | art. 5 |
| `FacultadRegional` | Una de las 30 FR | + `decano`, `vicedecano`, `departamentos` | arts. 5, 78, 86, 92 |
| `RegionalAcademica` | Depende de una FR | + `facultadDeQueDepende` | arts. 5, 112 |
| `Departamento` | Departamento de Enseñanza | `nombre`, `director`, `areas` | arts. 92, 95 |
| `Area` | Área de un departamento | `nombre`, `director` (profesor titular) | art. 17 |
| `Persona` | Cualquier integrante | `nombre`, `apellido`, `dni`, `roles` | — |
| `Rol` y subtipos | Lo que una persona *es* en la UTN | según el rol | arts. 18, 36, 114 |
| `Cargo` y subtipos | Lo que una persona *ocupa* por un período | `titular` (Persona), `desde`, `mandatoEnAnios` | arts. 66, 67, 86, 95 |
| `OrganoDeGobierno` (abstracta) | Órgano del art. 45 | `nombre`, `mandatoEnAnios` | art. 45 |
| `OrganoColegiado` / `OrganoUnipersonal` | Clasificación de órganos | integrantes / titular | arts. 104–110 |
| `Carrera` | Carrera de grado o pregrado | `nombre`, `duracionEnAnios`, `materias` | art. 53 |
| `Materia` | Asignatura de un plan | `nombre`, `nivel`, `bloque`, `correlativasCursadas`, `correlativasAprobadas` | Ord. 1753, planes 2023 |

## 4. Reglas reales que el curso usa

Cada regla tiene un identificador que los desafíos citan en `challenge.yaml`.

| ID | Regla | Fuente | Concepto de Java que enseña |
|---|---|---|---|
| `R-UNI-01` | La universidad tiene un único Rectorado | art. 5 | objeto único, referencias compartidas |
| `R-UNI-02` | Hay 30 Facultades Regionales, cada una con nombre, ciudad y provincia | Sedes (la provincia, de la página oficial de cada provincia) | clases, objetos, arreglos de objetos |
| `R-RECT-01` | Rector y Vicerrector: argentinos, ≥ 30 años, profesores o ex profesores | art. 66 | validación en constructores |
| `R-RECT-02` | Mandato de Rector y Vicerrector: 4 años | art. 67 | constantes, `final`, `static` |
| `R-FR-01` | Cada FR tiene Decano y Vicedecano | art. 78 | composición |
| `R-FR-02` | Decano y Vicedecano: profesores con ≥ 3 años de antigüedad docente; 4 años de mandato | art. 86 | validación, encapsulamiento |
| `R-FR-03` | Una FR se organiza en Departamentos de Enseñanza, incluido Materias Básicas | art. 92 | composición con colecciones |
| `R-FR-04` | Una Regional Académica depende de una FR y no tiene gobierno propio | arts. 5, 112 | herencia (comparte con FR lo de "unidad académica") vs diferencias |
| `R-DEP-01` | El Director de Departamento es profesor del departamento, dura 4 años | art. 95 | composición, validación cruzada |
| `R-DEP-02` | Consejo Departamental: Director + 5 docentes + 3 estudiantes + 2 graduados | art. 110 | colecciones, invariantes |
| `R-CS-01` | Consejo Superior: Rector, Decanos de todas las FR, 15 docentes, 5 graduados, 5 estudiantes, 5 no docentes | art. 105 | órgano colegiado, polimorfismo |
| `R-GOB-01` | Órganos de gobierno: Asamblea Universitaria, Consejo Superior, Rector, Asambleas de FR, Consejos Directivos, Decanos, Consejos Departamentales, Directores | art. 45 | jerarquía abstracta, interfaces |
| `R-CLA-01` | Cuatro claustros: docentes, graduados, estudiantes, no docentes | art. 44 | enums / tipos |
| `R-CLA-02` | Profesores: Titular, Asociado, Adjunto; auxiliares: JTP, Ayudante 1ª, Ayudante 2ª (para alumnos) | arts. 18, 36 | herencia legítima entre categorías docentes |
| `R-ROL-01` | Una persona puede tener varios roles (alumno y Ayudante de 2ª; profesor y Decano) | arts. 30, 36, 66, 86 | **composición sobre herencia** |
| `R-ROL-02` | Una persona vota en un solo claustro | art. 114 d | invariantes en encapsulamiento |
| `R-CAR-01` | No todas las FR dictan todas las carreras | art. 53 j | relaciones muchos a muchos |
| `R-MAT-01` | Cada materia tiene correlativas "cursadas" y "aprobadas" | planes 2023 | colecciones, validación |

## 5. Decisiones de modelado

### 5.1 Cargos: composición, no herencia

`class Decano extends Docente` es **incorrecto** y el curso lo enseña como trampa:

- El decanato es un **cargo por un período** (4 años, art. 86); la persona sigue siendo
  profesor (art. 30). Si `Decano` hereda de `Docente`, al terminar el mandato habría que
  "convertir" el objeto, cosa que Java no puede hacer.
- Modelo correcto: `FacultadRegional` **tiene** un `CargoDeDecano` cuyo `titular` es una
  `Persona` que **tiene** el rol `Profesor`.

### 5.2 Dónde sí hay herencia

Herencia se usa solo donde la relación "es un" es permanente y real:

| Superclase | Subclases | Por qué es herencia |
|---|---|---|
| `UnidadAcademica` | `FacultadRegional`, `RegionalAcademica` | Ambas son unidades que componen la UTN (art. 5); una FR no deja de ser unidad académica |
| `OrganoDeGobierno` | `OrganoColegiado`, `OrganoUnipersonal` | Todos son órganos del art. 45; su naturaleza no cambia |
| `OrganoColegiado` | `AsambleaUniversitaria`, `ConsejoSuperior`, `ConsejoDirectivo`, `ConsejoDepartamental` | Cada uno es un órgano colegiado con integrantes definidos |
| `Rol` | `Estudiante`, `Graduado`, `NoDocente`, `Profesor`, `Auxiliar` | Un rol es un tipo de pertenencia; la persona puede tener varios (composición arriba, herencia entre roles) |
| `Profesor` | `ProfesorTitular`, `ProfesorAsociado`, `ProfesorAdjunto` | Categorías del art. 18 |

Un objeto `Rol` representa una **designación**. Si la categoría de una persona cambia
(p. ej. de Adjunto a Titular por concurso), se reemplaza su objeto `Rol`; la `Persona` es
la misma. Así la herencia entre roles nunca obliga a "transformar" un objeto.

### 5.3 Simplificaciones permitidas

- Fechas como `int anio` (sin `LocalDate`) en los primeros módulos.
- Las Secretarías no se modelan en la v1 (no son estatutarias y varían).
- Extensiones Áulicas, INSPT, CTDR y Cenits quedan fuera de la v1.
- Las carreras por FR se usan solo con ejemplos verificables (p. ej. Pesquera en Chubut,
  Mar del Plata y Tierra del Fuego).

## 6. Datos de la plataforma

`content/domain/` contiene, en JSON validado por schema:

| Archivo | Contenido |
|---|---|
| `meta.json` | Fecha de revisión, versión del Estatuto |
| `regional-faculties.json` | Las 30 FR: `id`, `name`, `city`, `province`, fuentes |
| `governing-bodies.json` | Órganos del art. 45 con composición, mandato y fuente |
| `rules.json` | Las reglas de §4 con `id`, texto, artículo y URL |

Las claves de los archivos van en inglés; los valores que ve el alumno, en español.

`regional-faculties.json` tiene las 30 FR con ciudad y provincia, cada una con la página
de Sedes de la UTN y una segunda fuente (el sitio de la FR o una página oficial) que nombra
la provincia; las fuentes se verificaron el 2026-10-02. La api rechaza una FR sin fuente.
`governing-bodies.json` tiene los ocho órganos del art. 45 con su composición y mandato.

**Regionales Académicas.** El Estatuto las contempla (arts. 5 y 112), pero hoy no existe
ninguna: las que hubo pasaron a ser Facultades Regionales (la última, Mar del Plata, por
Res. AU 1/2017), y las páginas institucionales de la UTN y su informe de autoevaluación de
2020 no nombran ninguna. Los desafíos que necesitan una Regional Académica usan una **de
ejemplo**, y la consigna lo dice.
