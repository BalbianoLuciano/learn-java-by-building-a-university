# Primero la planta baja

## El constructor de la superclase

```java
UnidadAcademica(String nombre, String ciudad, String provincia) {
  this.nombre = nombre;
  this.ciudad = ciudad;
  this.provincia = provincia;
}
```

Cada clase se encarga de inicializar **sus** atributos. Los tres datos comunes son de
`UnidadAcademica`, así que es su constructor el que los guarda.

## `super(...)` en las subclases

```java
FacultadRegional(String nombre, String ciudad, String provincia) {
  super(nombre, ciudad, provincia);
}

RegionalAcademica(String nombre, String ciudad, String provincia, FacultadRegional facultad) {
  super(nombre, ciudad, provincia);
  this.facultadDeQueDepende = facultad;
}
```

`super(...)` llama al constructor de la superclase y **tiene que ser la primera
instrucción**. Es la regla de Java que hace visible el orden de construcción: primero la
planta baja (`UnidadAcademica`), después el piso de la subclase. En la línea de tiempo lo
ves: entra el constructor de `RegionalAcademica`, llama al de `UnidadAcademica`, ese
termina, y recién entonces se asigna `facultadDeQueDepende`.

## Qué pasa si te olvidás

Cuando la superclase declara un constructor con parámetros, el constructor vacío deja de
existir. Si la subclase no llama a `super(...)` explícitamente, Java intenta llamar a
`super()` sin argumentos, no lo encuentra y el programa no compila: "constructor
UnidadAcademica in class UnidadAcademica cannot be applied to given types". El mensaje
suena raro, pero dice exactamente eso.

## Para llevarte

- La parte heredada se construye antes que la propia.
- `super(...)` es la primera línea del constructor de una subclase.
- Cada clase inicializa sus propios atributos; las subclases delegan lo común.
