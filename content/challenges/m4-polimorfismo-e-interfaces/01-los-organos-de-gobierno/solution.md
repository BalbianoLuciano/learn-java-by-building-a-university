# Los órganos de gobierno

## Una clase que es un plano

```java
public abstract class OrganoDeGobierno {
  String nombre;
  int mandatoEnAnios;

  OrganoDeGobierno(String nombre, int mandatoEnAnios) { … }

  abstract String describirComposicion();
}
```

`abstract` en la clase significa que no se puede hacer `new OrganoDeGobierno(...)`. La
clase existe para juntar lo que todos los órganos del art. 45 comparten (nombre, mandato)
y para declarar qué se les puede pedir. Sigue teniendo constructor: lo usan las subclases
con `super(...)`, como ya viste.

`abstract` en el método significa que **no tiene cuerpo**: declara que todo órgano puede
describir su composición, pero no dice cómo, porque no hay un "cómo" válido para todos.
Cada subclase concreta está obligada a implementarlo; si no lo hace, no compila.

## Lo concreto se instancia

```java
OrganoDeGobierno organo = new ConsejoSuperior();
System.out.println(organo.nombre + ": " + organo.describirComposicion());
```

La variable es de tipo `OrganoDeGobierno` (el plano) y el objeto es un `ConsejoSuperior`
(el edificio). Al llamar `describirComposicion()`, Java ejecuta la versión de la clase del
**objeto**, no la del tipo de la variable. Ese mecanismo tiene nombre, polimorfismo, y es
el tema del módulo.

## Qué pasa si intentás `new` de la abstracta

El compilador lo frena: "OrganoDeGobierno is abstract; cannot be instantiated". En la
maqueta, el órgano abstracto es un plano gris: se ve, pero no se construye.

## Para llevarte

- `abstract class`: lo común de una familia, sin instancias propias.
- `abstract` método: qué se puede pedir, sin decir cómo; las subclases lo completan.
- Variable del tipo general, objeto del tipo concreto: el objeto decide qué código corre.
