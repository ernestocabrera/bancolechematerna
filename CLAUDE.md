# Banco de Leche Materna

App Android para el banco de leche materna de un hospital: se teclean las lecturas del
crematocrito de cada muestra, la app calcula % Crema, % Grasa y Kcal/L, guarda los
procesos por fecha en una base local y los exporta a Excel.

## Los calculos

Cada fila de la tabla tiene tres columnas de lectura (1, 2, 3) y cada columna lleva dos
numeros: la **columna total** (tipico 60-100) y la **columna de crema** (tipico 3-5).

```
P1 = promedio de las tres columnas totales   -> redondeado a 1 decimal
P2 = promedio de las tres columnas de crema  -> redondeado a 1 decimal
% Crema = P2 * 100 / P1                      -> redondeado a 1 decimal
% Grasa = % Crema - 0.59 / 1.46              -> redondeado a 1 decimal
Kcal/L  = % Crema * 66.8 + 290               -> redondeado a 1 decimal
```

Dos cosas que **no** son descuidos y no hay que "arreglar" sin hablarlo antes:

1. **Cada paso usa el resultado ya redondeado del paso anterior**, no el valor exacto.
   Es como se hace a lapiz en el hospital y la app tiene que dar el mismo numero.
2. **La formula de % Grasa esta mal de origen**: deberia ser `(% Crema - 0.59) / 1.46`.
   Un libro local la publico sin los parentesis y asi la usan. Se replica a proposito.
   Corregirla cambia todos los resultados historicos.

El redondeo es "de escuela" (0.05 sube), via `BigDecimal.valueOf(...).setScale(1, HALF_UP)`.
El redondeo por defecto de Java no se comporta asi.

El unico total del proceso es el **promedio de los % Grasa** de las filas que tengan datos.

Las constantes viven en `datos/Calculo.kt` (`object Formula`).

## Estructura

```
datos/     Calculo.kt   formulas y redondeo (sin dependencias de Android, testeable)
           Modelos.kt   Proceso, Muestra, manejo de fechas
           BaseDatos.kt SQLiteOpenHelper + Repositorio
exportar/  Xlsx.kt      escritor .xlsx minimo, hecho a mano (sin Apache POI)
           Exportador.kt arma las hojas y comparte/guarda el archivo
ui/        ProcesosViewModel, PantallaProcesos (lista), PantallaProceso (tabla)
```

### Decisiones a tener en cuenta

- **SQLite a mano, sin Room.** Son dos tablas; evita KSP y problemas de version en el build.
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
./gradlew :app:testDebugUnitTest    # tests de formulas y del .xlsx
./gradlew :app:installDebug         # instalar en emulador/telefono conectado
```
