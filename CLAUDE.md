# Banco de Leche Materna

App Android para el banco de leche materna de un hospital: se teclean las lecturas del
crematocrito de cada muestra, la app calcula % Crema, % Grasa y Kcal/L, guarda los
procesos por fecha en una base local y los exporta a Excel.

## Los calculos

Cada fila de la tabla tiene tres columnas de lectura (1, 2, 3) y cada columna lleva dos
numeros: la **columna total** (tipico 60-100) y la **columna de crema** (tipico 3-5).

```
P1 = promedio de las tres columnas totales   -> redondeado a 1 decimal
P2 = promedio de las tres columnas de crema  -> redondeado a ENTERO (0.5 sube)
% Crema = P2 * 100 / P1                      -> redondeado a 1 decimal
% Grasa = % Crema - 0.59 / 1.46              -> redondeado a 1 decimal
Kcal/L  = % Crema * 66.8 + 290               -> redondeado a 1 decimal
```

Tres cosas que **no** son descuidos y no hay que "arreglar" sin hablarlo antes:

1. **Cada paso usa el resultado ya redondeado del paso anterior**, no el valor exacto.
   Es como se hace a lapiz en el hospital y la app tiene que dar el mismo numero.
2. **P2 es un entero.** Se redondea a entero directamente desde el promedio exacto
   (no desde el promedio ya redondeado a 1 decimal, eso redondearia dos veces).
   En el modelo es `Resultado.promCrema: Int?`; en la tabla y el Excel sale sin decimales.
3. **La formula de % Grasa esta mal de origen**: deberia ser `(% Crema - 0.59) / 1.46`.
   Un libro local la publico sin los parentesis y asi la usan. Se replica a proposito.
   Corregirla cambia todos los resultados historicos.

El redondeo es "de escuela" (0.05 sube, y 0.5 sube a entero), siempre con `BigDecimal`
y `RoundingMode.HALF_UP`. El redondeo por defecto de Java no se comporta asi, y
`kotlin.math.round` redondea al par (4.5 -> 4): no usarlo.

Una fila solo da resultados si tiene al menos una lectura total **y** una de crema;
si falta alguna de las dos, % Crema, % Grasa y Kcal quedan vacios.
Se promedian solo las casillas escritas (una fila con dos columnas llenas promedia dos).

Totales del proceso (en el pie de la pantalla), contando solo filas con resultado:

- **Promedio de los % Grasa.**
- **Cuantas filas caen en cada rango de Kcal**: x <= 500, 500 < x <= 650, 650 < x <= 750,
  750 < x <= 800, x > 800. Cada limite cuenta en el rango de abajo (650.0 va en 500-650).
  Viven en `RangosKcal`.

Las constantes viven en `datos/Calculo.kt` (`object Formula`).

## La tabla del proceso

Cada fila tiene, de izquierda a derecha: **Nro** (fija, no se desplaza), **Acidez**
(3 enteros), **Crematocrito** (las lecturas y los calculos de arriba), **Pasteurizacion**
(hora + Baño M, Punto frio, Agua en °C) y el boton de eliminar al final.

- Acidez y Pasteurizacion son solo registro: **no entran en ningun calculo**.
- Acidez acepta cualquier entero, pero fuera de `ACIDEZ_NORMAL` (2..8) se pinta en rojo.
- La hora no se teclea: se elige en un reloj de 12 h. Se guarda como `"HH:mm"` en 24 h
  y se muestra como `02:30 pm` (`horaLegible`).
- Las temperaturas admiten un decimal; la coma se convierte en punto.
- Las secciones se muestran u ocultan desde un menu (`Secciones`); Nro siempre se ve.

## Estructura

```
datos/     Calculo.kt   formulas y redondeo (sin dependencias de Android)
           Modelos.kt   Proceso, Muestra, manejo de fechas
           BaseDatos.kt SQLiteOpenHelper + Repositorio
exportar/  Xlsx.kt      escritor .xlsx minimo, hecho a mano (sin Apache POI)
           Exportador.kt arma las hojas y comparte/guarda el archivo
ui/        ProcesosViewModel, PantallaProcesos (lista), PantallaProceso (tabla)
```

### Decisiones a tener en cuenta

- **Sin tests.** Se quitaron a proposito; no agregarlos salvo que se pidan.
- **SQLite a mano, sin Room.** Son dos tablas; evita KSP y problemas de version en el build.
  Al agregar columnas: subir `BD_VERSION`, sumarlas en `onCreate` y en `onUpgrade`
  (`ALTER TABLE ... ADD COLUMN`), para no perder los datos de telefonos ya instalados.
- **Sin Apache POI.** Pesa demasiado en Android; `Xlsx.kt` genera el zip + XML directo.
  Verificado con POI del lado del escritorio: se lee sin errores.
- **Las lecturas se guardan como TEXT**, no como enteros, para que una casilla a medio
  escribir se vea igual que como quedo.
- **Se escribe en la base en cada tecla**, sobre un dispatcher de un solo hilo
  (`hiloBd` en el ViewModel) para que el orden de las escrituras sea el de las teclas.
  No hay boton de "guardar" a proposito.
- **Las fechas se guardan como medianoche UTC** del dia elegido, que es lo que devuelve el
  calendario de Material. Los formatos tambien son UTC: en horario de Cuba (UTC-4),
  formatear en zona local mostraria el dia anterior.
- **Sin color dinamico** en el tema: la app debe verse igual en cualquier telefono.

## Comandos

```
./gradlew :app:assembleDebug        # compilar
./gradlew :app:installDebug         # instalar en emulador/telefono conectado
```
