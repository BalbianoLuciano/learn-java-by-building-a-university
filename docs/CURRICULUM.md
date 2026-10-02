# Currículo

> Spec del contenido de la v1: módulos, desafíos y qué aprende el alumno en cada uno.
> Cada desafío modela una parte de la UTN real ([`DOMAIN.md`](DOMAIN.md)) y cita sus
> reglas (`R-…`). El formato de los archivos está en
> [`specs/challenge-format.md`](specs/challenge-format.md).

## Principios

- **Un concepto nuevo por desafío.** Los demás ya se vieron antes.
- **El código base hace el trabajo aburrido.** El alumno escribe solo lo que enseña el
  desafío.
- **Cada módulo termina en un desafío integrador** que junta lo del módulo.
- **El orden sigue la cátedra:** las unidades de Programación II (clases y objetos →
  constructores y encapsulamiento → herencia → polimorfismo e interfaces).
- **Cada desafío arranca con su propio código base**, así un error anterior no bloquea el
  siguiente. La maqueta, en cambio, va mostrando una UTN cada vez más completa: las piezas
  de desafíos ya completados aparecen como contexto.

## Módulo 1 · Clases, objetos y referencias

Objetivo: entender que una clase es un molde, que cada `new` crea un objeto con estado
propio y que una variable **apunta** a un objeto.

| # | Desafío | Pedido de obra | Concepto | Reglas | Se ve en la maqueta |
|---|---|---|---|---|---|
| 1.1 | **Abrí tu primera Facultad Regional** | Declarar `FacultadRegional` con `nombre`, `ciudad`, `provincia` y crear la FR Resistencia | clase, atributos, `new` | R-UNI-02 | Aparece la isla y el edificio con su cartel |
| 1.2 | **Tres regionales, tres estados** | Crear Resistencia, Córdoba y Mendoza y registrar una consulta solo en una (`consultas` es un contador del programa, no un dato de la UTN) | estado propio de cada objeto | R-UNI-02 | Tres edificios; el cambio afecta a uno solo |
| 1.3 | **La facultad donde estudiás** | Usar una segunda variable `miFacultad` que apunte a una FR existente y cargarle la provincia por ahí | referencias y **aliasing** | R-UNI-02 | Dos carteles, dos cables, **un** edificio |
| 1.4 | **Un solo Rectorado** | Crear el `Rectorado` y que todas las FR referencien el mismo objeto | referencias compartidas, `==` vs igualdad | R-UNI-01 | Cables de cada FR al mismo Rectorado central |
| 1.5 | **Integrador: el mapa de la UTN** | Cargar las FR en un arreglo `FacultadRegional[]` y recorrerlo con métodos de instancia (`describir()`) | arreglos de objetos, métodos de instancia | R-UNI-02 | El anillo de FR alrededor del Rectorado |

Trampas que el feedback tiene que detectar: usar la clase sin `new`; creer que
`miFacultad = resistencia` copia el objeto; comparar FR con `==` esperando igualdad de datos.

## Módulo 2 · Constructores y encapsulamiento

Objetivo: los objetos **nacen válidos** y protegen su estado; las reglas reales de la UTN
viven dentro de la clase.

| # | Desafío | Pedido de obra | Concepto | Reglas | Se ve en la maqueta |
|---|---|---|---|---|---|
| 2.1 | **La FR nace con nombre** | Agregar un constructor a `FacultadRegional` que reciba nombre, ciudad y provincia | constructores, `this` | R-UNI-02 | El edificio se construye completo de una |
| 2.2 | **Nadie toca el padrón por la ventana** | Hacer `private` los atributos y exponer getters | encapsulamiento | R-UNI-02 | Paredes alrededor de los atributos; solo ventanillas (métodos) |
| 2.3 | **Requisitos para ser Rector** | La clase `CargoDeRector` rechaza en el constructor a quien no cumple: argentino, ≥ 30 años, profesor o ex profesor | validación en el constructor | R-RECT-01 | El pedestal del Rector rechaza al candidato inválido (rebota) |
| 2.4 | **Un mandato de 4 años** | `MANDATO_EN_ANIOS` como constante `static final`; `anioDeFin()` calculado | `static`, `final`, constantes | R-RECT-02, R-FR-02 | Un reloj de mandato compartido por todos los cargos |
| 2.5 | **Integrador: el Decano de Resistencia** | `CargoDeDecano` con constructor canónico + de conveniencia (`this(...)`), setter que valida antigüedad ≥ 3 años y devuelve `boolean` | sobrecarga de constructores, setters que validan | R-FR-01, R-FR-02 | La FR con su pedestal de Decano ocupado o en andamio |

Trampas: setter sin validación; atributo público "por las dudas"; constructor que deja
atributos en `null`; repetir validaciones en lugar de delegar con `this(...)`.

## Módulo 3 · Herencia y composición

Objetivo: distinguir **"es un"** de **"tiene un"** con la estructura real, y ver que la
parte de la superclase se construye primero.

| # | Desafío | Pedido de obra | Concepto | Reglas | Se ve en la maqueta |
|---|---|---|---|---|---|
| 3.1 | **Las unidades que componen la UTN** | `FacultadRegional` y `RegionalAcademica` extienden `UnidadAcademica` | `extends`, atributos heredados | R-FR-04 | Edificios de **dos pisos**: planta baja `UnidadAcademica`, arriba la subclase |
| 3.2 | **Primero la planta baja** | Constructores con `super(nombre, ciudad, provincia)` | `super()`, orden de construcción | R-FR-04 | Animación: se construye la planta baja y después el piso |
| 3.3 | **La FR tiene departamentos** | `FacultadRegional` tiene una lista de `Departamento`, incluido Materias Básicas | composición con colecciones | R-FR-03 | Pabellones sobre la isla de la FR |
| 3.4 | **La trampa del Decano** | Código base con `class Decano extends Profesor`; el pedido de obra (terminar un mandato y asignar otro decano) no se puede cumplir así: refactorizar a `CargoDeDecano` con `titular` | **composición sobre herencia** | R-ROL-01, R-FR-02 | Con herencia: el edificio de la persona queda "pegado" al cargo; con composición: el pedestal cambia de titular |
| 3.5 | **Integrador: una persona, varios roles** | Una `Persona` con roles `Estudiante` y `Auxiliar` (Ayudante de 2ª) a la vez, y un solo claustro electoral | herencia entre `Rol` + composición en `Persona` | R-ROL-01, R-ROL-02, R-CLA-02 | Una figura con dos insignias de rol |

Trampas: heredar para reutilizar un atributo; olvidar `super(...)` cuando la superclase no
tiene constructor vacío; duplicar atributos en la subclase.

## Módulo 4 · Polimorfismo e interfaces

Objetivo: el mismo mensaje produce respuestas distintas según el objeto, y las interfaces
describen capacidades compartidas por clases no relacionadas.

| # | Desafío | Pedido de obra | Concepto | Reglas | Se ve en la maqueta |
|---|---|---|---|---|---|
| 4.1 | **Los órganos de gobierno** | `OrganoDeGobierno` abstracta con `describirComposicion()` abstracto | clases y métodos abstractos | R-GOB-01 | Plano gris del órgano abstracto: no se puede construir |
| 4.2 | **Colegiados y unipersonales** | `OrganoColegiado` y `OrganoUnipersonal` implementan la descripción de forma distinta | `@Override`, polimorfismo | R-GOB-01 | Mismo mensaje, dos animaciones distintas |
| 4.3 | **El Consejo Superior** | `ConsejoSuperior` arma sus integrantes según el art. 105 y valida las cantidades | polimorfismo + invariantes | R-CS-01 | Una sala con bancas por claustro |
| 4.4 | **¿Quién elige a quién?** | Interfaz `CargoElectivo` con `organoQueElige()` y `mandatoEnAnios()` implementada por Rector, Decano y Director de Departamento | interfaces | R-RECT-02, R-FR-02, R-DEP-01 | Insignia "electivo" y cable al órgano que elige |
| 4.5 | **Integrador: el gobierno de una FR** | Recorrer una `List<OrganoDeGobierno>` de una FR y pedirle a cada uno su descripción | polimorfismo con colecciones, despacho dinámico | R-GOB-01, R-FR-01 | La línea de tiempo resalta qué implementación se ejecutó en cada vuelta |

Trampas: `instanceof` en cadena en lugar de polimorfismo; instanciar una clase abstracta;
olvidar `@Override` y escribir mal el nombre del método.

## Criterios de "listo" de un desafío

Un desafío está listo cuando:

- [ ] Cita las reglas `R-…` que usa y no contradice [`DOMAIN.md`](DOMAIN.md).
- [ ] Tiene código base que compila y **no** pasa todas las verificaciones.
- [ ] Tiene solución de referencia que pasa todas las verificaciones (test de contenido).
- [ ] Cada verificación tiene mensajes de acierto y de error según
      [`FEEDBACK.md`](FEEDBACK.md).
- [ ] Tiene 3 pistas y `solution.md`.
- [ ] Define bindings de escena para todas las clases del alumno.
- [ ] Contempla las trampas del módulo con un mensaje específico.
- [ ] Lo revisó al menos una persona además de quien lo escribió.
