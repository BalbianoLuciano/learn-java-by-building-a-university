# Integrador: el gobierno de una FR

## Una lista del tipo general

```java
List<OrganoDeGobierno> organos = new ArrayList<>();

void agregarOrgano(OrganoDeGobierno organo) {
  organos.add(organo);
}
```

La lista se declara con la **superclase**. Por eso acepta la Asamblea y el Consejo
Directivo (colegiados) y el Decanato (unipersonal) sin distinguirlos, y `agregarOrgano`
es un solo método en vez de uno por clase.

## Despacho dinámico

```java
void describirGobierno() {
  for (OrganoDeGobierno organo : organos) {
    System.out.println(organo.nombre + ": " + organo.describirComposicion());
  }
}
```

En cada vuelta, `organo` es una referencia de tipo `OrganoDeGobierno`, pero el objeto es
de una subclase concreta. Java decide **en tiempo de ejecución** qué `describirComposicion()`
correr, según la clase del objeto. En la línea de tiempo lo ves: la misma línea del `for`
entra dos veces en `OrganoColegiado.describirComposicion` y una en
`OrganoUnipersonal.describirComposicion`.

## La trampa del `instanceof`

```java
if (organo instanceof OrganoColegiado colegiado) {
  ... colegiado.integrantes.size() ...
} else if (organo instanceof OrganoUnipersonal unipersonal) {
  ... unipersonal.titular ...
}
```

Funciona, pero repite afuera lo que cada clase ya sabe hacer, y cada órgano nuevo (un
Consejo Departamental, por ejemplo) obliga a agregar otra rama. Con polimorfismo, el `for`
no cambia nunca: la clase nueva trae su propia descripción. Cuando veas una cadena de
`instanceof`, casi siempre hay un método polimórfico esperando.

## Para llevarte

- Colecciones del tipo general: aceptan cualquier subclase.
- Despacho dinámico: la clase del objeto decide qué implementación corre, en cada llamada.
- Si preguntás `instanceof` para decidir qué hacer, dejá que lo decida el objeto.
