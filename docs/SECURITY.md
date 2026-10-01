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
- Concurrencia global del runner acotada (cola con tamaño máximo; si se llena, `503`).

### Capa 2 · Compilación controlada (runner)

- `javax.tools.JavaCompiler` en memoria, sin acceso al sistema de archivos del alumno.
- `-proc:none`: sin procesadores de anotaciones (ejecutarían código en el compilador).
- Timeout de compilación: 10 s.

### Capa 3 · Lista permitida de bytecode (runner)

Antes de ejecutar, se recorren **todas** las clases compiladas con la API
`java.lang.classfile` y se revisa cada referencia a clases, métodos y campos externos.
**Todo lo que no está permitido está prohibido.**

Permitido (lista inicial, se amplía solo por ADR):

| Paquete / clase | Alcance |
|---|---|
| `java.lang.Object`, `String`, `StringBuilder`, `Math` | completo |
| Wrappers (`Integer`, `Double`, `Boolean`, `Character`, `Long`) | completo |
| `java.lang.System` | solo el campo `out` |
| `java.io.PrintStream` | solo `print`, `println`, `printf` |
| Excepciones de `java.lang` | construcción, `getMessage`, `throw`/`catch` |
| `java.util` | `List`, `ArrayList`, `Map`, `HashMap`, `Set`, `HashSet`, `Objects`, `Arrays` (sin `Scanner`) |
| `invokedynamic` | solo `StringConcatFactory` (concatenación) y `LambdaMetafactory` |
| Enums y records | mecanismos que genera `javac` (`ObjectMethods`, `Enum.valueOf`) |

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

- Directorio temporal propio por ejecución, borrado al terminar.
- Entorno del proceso vacío (sin variables heredadas).
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
rechazados o cortados**, ejecutada en CI:

- Bucle infinito, recursión infinita, asignación masiva de memoria, salida infinita.
- `System.exit`, `getenv`, `Runtime.exec`, `ProcessBuilder`, lectura de `/etc/passwd`.
- Reflexión, `MethodHandles`, `Class.forName`, `Thread`, `Socket`.
- Procesador de anotaciones, archivos de más de 64 KB, nombres de archivo con rutas.

Cualquier cambio a la lista permitida debe sumar casos a esta suite.
