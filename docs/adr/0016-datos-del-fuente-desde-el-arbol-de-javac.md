# ADR 0016 · Los datos que solo están en el fuente salen del árbol de javac

- **Estado:** aceptada
- **Fecha:** 2026-10-02

## Contexto

La estructura de las clases del alumno se lee del bytecode con `java.lang.classfile`. Dos datos que las verificaciones necesitan no están ahí: `@Override` tiene retención `SOURCE` y nunca llega al `.class`, y los campos y los métodos abstractos no tienen número de línea. El desafío 4.2 enseña justamente `@Override`.

## Decisión

El runner ya compila con `javac` en el mismo proceso. Después del análisis recorre el árbol de sintaxis con la API pública `com.sun.source` y toma de ahí solo lo que el bytecode no tiene: la línea de cada declaración (clase, campo, constructor, método) y si un método lleva `@Override`. Todo lo demás (nombres, tipos, modificadores, herencia) sigue saliendo del bytecode.

Con el mismo árbol el runner informa dos reglas propias como errores de compilación, con códigos `ljbu.err.*`: una declaración `package` y la falta de `public static void main(String[] args)` en `Main`.

## Consecuencias

- La estructura informa `override: true/false` por método y la verificación `method` puede exigir la anotación.
- La línea de una declaración es la de su firma, no la de la anotación que tenga arriba.
- No se ejecuta código del alumno: leer el árbol no es procesar anotaciones (`-proc:none` se mantiene).
- El runner depende de `jdk.compiler`, que ya necesitaba para compilar.
