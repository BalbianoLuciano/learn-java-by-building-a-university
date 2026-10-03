# Colegiados y unipersonales

## Sobrescribir un método

```java
public class OrganoColegiado extends OrganoDeGobierno {
  List<String> integrantes = new ArrayList<>();
  ...
  @Override
  String describirComposicion() {
    return "Órgano colegiado de " + integrantes.size() + " integrantes";
  }
}
```

La subclase vuelve a definir un método que ya existía en la superclase, con **la misma
firma** (nombre, parámetros y tipo de retorno). Desde ese momento, para los objetos de esa
clase corre la nueva versión. `OrganoUnipersonal` hace lo mismo con otro texto, usando su
atributo `titular`.

## El mismo mensaje, dos respuestas

```java
OrganoDeGobierno[] organos = {consejoDirectivo, decanato};
for (OrganoDeGobierno organo : organos) {
  System.out.println(organo.nombre + ": " + organo.describirComposicion());
}
```

En las dos vueltas la llamada es idéntica: `organo.describirComposicion()`, sobre una
variable de tipo `OrganoDeGobierno`. Pero en la primera vuelta corre la versión del
colegiado y en la segunda, la del unipersonal, porque Java mira la clase del **objeto** en
el momento de la llamada. Eso es **polimorfismo**: un mensaje, muchas formas. En la línea
de tiempo se ve qué implementación se ejecutó en cada vuelta.

## Para qué sirve `@Override`

`@Override` no cambia nada en tiempo de ejecución. Le pide al compilador que verifique que
el método de abajo realmente sobrescribe uno de la superclase. Sin él, un error de tipeo
(`describirComposición` con tilde, o un parámetro de más) crea silenciosamente un método
nuevo que nadie llama, y el objeto sigue respondiendo con el texto genérico. Con `@Override`
ese error no compila.

## Para llevarte

- Sobrescribir: misma firma en la subclase, otro comportamiento.
- Polimorfismo: la clase del objeto, no la de la variable, decide qué código corre.
- `@Override` siempre: convierte un error silencioso en un error de compilación.
