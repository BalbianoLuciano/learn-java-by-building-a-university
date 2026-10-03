# Nadie toca el padrón por la ventana

## Cerrar los atributos

```java
private String nombre;
private String ciudad;
private String provincia;
```

Con `private`, solo el código escrito dentro de `FacultadRegional` puede leer o escribir
esos atributos. `resistencia.nombre = "otra cosa"` desde `main` deja de compilar: el
compilador te avisa con "nombre has private access in FacultadRegional".

## Abrir ventanillas

```java
public String getNombre() {
  return nombre;
}
```

Un *getter* es un método público que devuelve el valor de un atributo. Abre el atributo
solo para lectura: desde afuera se puede preguntar el nombre, pero no cambiarlo. Como esta
clase no tiene *setters*, el estado que fija el constructor es definitivo.

## Por qué vale la pena

Hoy parece más trabajo que escribir `resistencia.nombre`. La diferencia aparece cuando la
clase tiene reglas: en los próximos desafíos, un cargo va a rechazar a quien no cumpla los
requisitos del Estatuto. Eso solo es posible si nadie puede cambiar los atributos "por la
ventana", salteándose la verificación.

## Para llevarte

- `private` + getters públicos: el objeto decide qué se ve.
- Sin setters, un atributo solo se fija en el constructor.
- Encapsular es lo que permite que las reglas vivan dentro de la clase.
