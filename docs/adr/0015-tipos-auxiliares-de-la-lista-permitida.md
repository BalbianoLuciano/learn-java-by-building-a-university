# ADR 0015 · Tipos auxiliares y exclusiones de la lista permitida

- **Estado:** aceptada
- **Fecha:** 2026-10-02

## Contexto

Al implementar el verificador de bytecode, la lista inicial de `docs/SECURITY.md` §3 resultó corta y a la vez demasiado amplia. Corta: `javac` traduce construcciones que el currículo usa a tipos que la lista no nombraba (un `for` sobre una lista llama a `Iterable.iterator()` y a `Iterator`; recorrer un `Map` usa `Map.Entry` y `Collection`; todo `enum` extiende `Enum` y clona un arreglo en `values()`; todo `record` extiende `Record`). Amplia: "completo" incluía métodos que leen propiedades del sistema o trabajan con hilos.

## Decisión

Se agregan a la lista permitida, sin sumar ninguna capacidad nueva:

- `java.lang.Iterable`, `java.util.Iterator`, `java.util.Collection` y `java.util.Map.Entry`, completos.
- `java.lang.Enum`, completo, y el constructor de `java.lang.Record`.
- `clone()` sobre arreglos.
- Los métodos de `Object` (`toString`, `equals`, `hashCode`, `getClass`) sobre cualquier tipo permitido.

Se excluyen de las clases "completas":

- `Integer.getInteger`, `Long.getLong` y `Boolean.getBoolean` (leen propiedades del sistema).
- `Arrays.parallelSort`, `parallelPrefix`, `parallelSetAll` y `Collection.parallelStream` (usan hilos).

## Consecuencias

- Funcionan `for` sobre colecciones, enums, records, lambdas y referencias a métodos permitidos.
- `java.lang.Class` sigue prohibida: `getClass()` se puede llamar, pero sobre su resultado no se puede invocar nada (`getSimpleName()` incluido). Si un desafío lo necesita, se agrega con otro ADR.
- Siguen afuera `Comparable`, `Comparator`, `Collections`, los streams y `printStackTrace()`; se evalúan junto con un eventual módulo de colecciones.
- Cada tipo agregado y cada exclusión tiene su caso en la suite de seguridad del runner (`ForbiddenCodeTest`, `AllowedCodeTest`).
