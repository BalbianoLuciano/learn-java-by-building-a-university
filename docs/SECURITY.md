# Seguridad del sandbox

> Spec de cómo se ejecuta código Java ajeno sin riesgo. Para **reportar una
> vulnerabilidad**, ver [`../SECURITY.md`](../SECURITY.md).

## 1. Contexto

La plataforma compila y ejecuta código escrito por cualquier visitante. Hay dos
restricciones que condicionan el diseño:

1. **No existe `SecurityManager`.** Fue deprecado en Java 17 y quedó deshabilitado de forma
   permanente en Java 24. No hay forma de restringir código dentro de la misma JVM.
2. **Railway no permite crear un contenedor por ejecución.** El aislamiento tiene que
   funcionar dentro de un contenedor ya existente y sin privilegios.

Por eso el aislamiento es **en capas**: ninguna capa alcanza sola, todas juntas reducen el
riesgo a un nivel aceptable para una plataforma educativa sin datos sensibles
([ADR 0004](adr/0004-sandbox-en-capas.md)).

## 2. Qué protegemos

| Activo | Riesgo |
|---|---|
| Disponibilidad del servicio | Bucles infinitos, memoria, salida masiva, muchas ejecuciones |
| El contenedor del runner | Lectura de archivos, variables de entorno, ejecución de procesos |
| Terceros | Usar el servidor para atacar otros sistemas por red |
| La api | Que el runner comprometido llegue a la api |
| Otros alumnos | Ver o alterar ejecuciones ajenas |

No hay datos personales: no hay cuentas y no se guarda el código de los alumnos.

## 3. Capas

### Capa 1 · Validación de entrada (api)

- Tamaño total del código ≤ 64 KB; ≤ 10 archivos; nombres `^[A-Z][A-Za-z0-9_]*\.java$`.
- Sin declaración `package` y sin `module-info.java`.
- Rate limit por IP: 10 ejecuciones por minuto (configurable) y 1 ejecución simultánea por IP.
- Concurrencia global del runner acotada: 2 ejecuciones a la vez y una cola de 8
  (configurables); si la cola se llena, `503`.
- El runner vuelve a aplicar las reglas de tamaño, cantidad y nombres: no confía en que la
  api las haya aplicado. Una declaración `package` se informa como error de compilación.
- El runner solo acepta pedidos con `RUNNER_TOKEN`.

### Capa 2 · Compilación controlada (runner)

- `javax.tools.JavaCompiler` en memoria, sin acceso al sistema de archivos del alumno.
- `-proc:none`: sin procesadores de anotaciones (ejecutarían código en el compilador).
- Classpath vacío: el código solo ve el JDK y sus propios archivos.
- Timeout de compilación: 10 s.

### Capa 3 · Lista permitida de bytecode (runner)

Antes de ejecutar, se recorren **todas** las clases compiladas con la API
`java.lang.classfile` y se revisa cada referencia a clases, métodos y campos externos.
**Todo lo que no está permitido está prohibido.**

Qué se revisa:

- Cada instrucción que nombra una clase o un miembro: llamadas, accesos a campos, `new`,
  conversiones, `instanceof`, arreglos, constantes de clase y tipos de un `catch`. Así cada
  uso prohibido se informa con su línea.
- Los `invokedynamic`: el método de arranque y sus argumentos. Una referencia a método
  (`Runtime::exec`) viaja ahí y no como una llamada.
- La superclase y las interfaces de cada clase, y que ningún método sea `native`.
- La tabla de constantes completa: ningún campo ni método prohibido puede estar ahí,
  aunque ninguna instrucción lo use.

Permitido (se amplía solo por ADR; ver [ADR 0015](adr/0015-tipos-auxiliares-de-la-lista-permitida.md)):

| Paquete / clase | Alcance |
|---|---|
| `java.lang.Object`, `String`, `StringBuilder`, `Math` | completo |
| Wrappers (`Integer`, `Double`, `Boolean`, `Character`, `Long`) | completo, salvo `Integer.getInteger`, `Long.getLong` y `Boolean.getBoolean` |
| `java.lang.System` | solo el campo `out` |
| `java.io.PrintStream` | solo `print`, `println`, `printf` |
| Excepciones y errores de `java.lang` | construcción, `getMessage`, `throw`/`catch` |
| `java.util` | `List`, `ArrayList`, `Map`, `Map.Entry`, `HashMap`, `Set`, `HashSet`, `Collection`, `Iterator`, `Objects`, `Arrays` (sin `Scanner`); sin `Arrays.parallel*` ni `Collection.parallelStream` |
| `java.lang.Iterable` | completo (lo usa el `for` sobre colecciones) |
| `invokedynamic` | solo `StringConcatFactory` (concatenación), `LambdaMetafactory` y `ObjectMethods` |
| Enums y records | `java.lang.Enum` completo, el constructor de `java.lang.Record` y `clone()` de arreglos |
| Métodos de `Object` | `toString`, `equals`, `hashCode` y `getClass` sobre cualquier tipo permitido |

Prohibido explícitamente (ejemplos): `System.exit`, `System.getenv`, `System.getProperty`,
`System.in`, `Runtime`, `ProcessBuilder`, `Thread` y concurrencia, `java.io` de archivos,
`java.nio`, `java.net`, `java.lang.reflect`, `java.lang.invoke` (salvo lo de arriba),
`ClassLoader`, `native`, `Unsafe`, serialización.

Si se encuentra un uso prohibido, la ejecución se rechaza con `status: "rejected"`, la línea
y un mensaje didáctico ("En este curso no hace falta leer archivos: …").

### Capa 4 · Proceso aislado con límites (runner)

Cada ejecución corre en una **JVM hija** nueva, nunca en la JVM del runner:

| Límite | Valor |
|---|---|
| Memoria heap | `-Xmx64m` |
| Pila por hilo | `-Xss1m` (cubre recursión infinita) |
| Tiempo de reloj | 5 s (luego se mata el árbol de procesos) |
| Pasos de traza | 5 000 |
| Objetos registrados | 500 |
| Salida estándar | 16 KB (se trunca y se avisa) |
| Profundidad de llamadas | 1 000 |

- Al llegar a un límite, la JVM hija se mata y la traza conserva lo hecho hasta ahí. El
  tiempo devuelve `timeout`; pasos, objetos, salida y profundidad, `limit_exceeded` con el
  límite en `limits.exceeded`; la memoria, `runtime_error` con `OutOfMemoryError`.
- La profundidad de llamadas corta la recursión infinita mucho antes de que se llene la
  pila: bajo JDI cada nivel de pila hace más lento el siguiente.
- Directorio temporal propio por ejecución, borrado al terminar. Es también el directorio
  de trabajo y el `java.io.tmpdir` de la JVM hija.
- Entorno del proceso vacío (sin variables heredadas).
- `--limit-modules`: la JVM hija solo tiene `java.base`. No existen ahí `java.net.http`,
  `java.sql` ni `jdk.unsupported`, aunque el verificador fallara.
- El puerto JDWP se abre solo en `127.0.0.1`, con puerto aleatorio, y se cierra al terminar.
- Una ejecución por JVM; ninguna JVM se reutiliza.

### Capa 5 · Runner sin privilegios ni secretos (despliegue)

- El runner **no tiene dominio público**: solo es accesible por la red privada de Railway.
- Su único secreto es `RUNNER_TOKEN`, que solo autoriza llamadas entrantes; no da acceso a
  nada más.
- No tiene base de datos ni almacenamiento persistente.
- Corre como usuario no root en la imagen de contenedor.
- La api trata las respuestas del runner como **no confiables**: las valida contra el schema
  y aplica límites de tamaño antes de procesarlas.

## 4. Riesgos residuales

| Riesgo | Por qué queda | Mitigación futura |
|---|---|---|
| Fallas de la JVM o del verificador | Sin aislamiento a nivel de kernel por ejecución | Migrar el runner a VPS con gVisor o nsjail ([ADR 0008](adr/0008-hosting-railway-cloudflare.md)) |
| Red saliente desde el contenedor | Railway no permite bloquearla | La capa 3 impide usar la red desde Java |
| Agotamiento por muchas IPs | El rate limit es por IP | Cloudflare delante de la api si hiciera falta |

## 5. Tests de seguridad obligatorios

En `services/runner` debe existir una suite de casos maliciosos que **deben ser
rechazados o cortados**, ejecutada en CI (`src/test/java/…/execution/security/`):

- Bucle infinito, recursión infinita, asignación masiva de memoria, salida infinita.
- `System.exit`, `getenv`, `Runtime.exec`, `ProcessBuilder`, lectura de `/etc/passwd`.
- Reflexión, `MethodHandles`, `Class.forName`, `Thread`, `Socket`.
- Procesador de anotaciones, archivos de más de 64 KB, nombres de archivo con rutas.

- Aislamiento de la JVM hija: entorno vacío, solo `java.base`, directorio propio y sin
  restos al terminar.

Cualquier cambio a la lista permitida debe sumar casos a esta suite.
