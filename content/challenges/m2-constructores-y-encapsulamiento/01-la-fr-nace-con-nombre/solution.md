# La FR nace con nombre

## El constructor

```java
FacultadRegional(String nombre, String ciudad, String provincia) {
  this.nombre = nombre;
  this.ciudad = ciudad;
  this.provincia = provincia;
}
```

Un constructor se llama igual que la clase y no tiene tipo de retorno. Java lo ejecuta en
cada `new`, con el objeto recién creado: ese objeto es `this`. Así una facultad nunca
existe "a medio cargar": nace con nombre, ciudad y provincia.

## `this.nombre` y `nombre`

Los parámetros se llaman igual que los atributos, a propósito: es lo habitual en Java. Para
distinguirlos, `this.nombre` es el atributo del objeto y `nombre` a secas, el parámetro.
Escribir `nombre = nombre;` asigna el parámetro a sí mismo y el atributo queda en `null`:
esa es la trampa más común de este desafío.

## Una línea en `main`

```java
FacultadRegional resistencia = new FacultadRegional("Resistencia", "Resistencia", "Chaco");
```

Los argumentos van en el orden de los parámetros. Si la clase define este constructor, el
viejo `new FacultadRegional()` deja de existir: Java solo genera el constructor vacío
cuando la clase no declara ninguno.

## Para llevarte

- El constructor es el lugar para que un objeto nazca completo.
- `this` es el objeto que se está construyendo (o sobre el que corre un método).
- Al declarar un constructor con parámetros, el constructor vacío desaparece.
