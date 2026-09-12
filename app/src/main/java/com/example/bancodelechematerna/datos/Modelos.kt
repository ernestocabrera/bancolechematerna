package com.example.bancodelechematerna.datos

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Cantidad de columnas de lectura (1, 2, 3) y de casillas por fila. */
const val COLUMNAS = 3
const val LECTURAS = COLUMNAS * 2

/**
 * Una fila de la tabla. [lecturas] guarda lo tecleado en orden
 * total1, crema1, total2, crema2, total3, crema3.
 * Se guarda como texto para poder dejar casillas vacias mientras se escribe.
 */
data class Muestra(
    val id: Long = 0,
    val numero: String = "",
    val lecturas: List<String> = List(LECTURAS) { "" },
) {
    private val valores: List<Int?> get() = lecturas.map { it.trim().toIntOrNull() }

    val totales: List<Int?> get() = List(COLUMNAS) { valores[it * 2] }
    val cremas: List<Int?> get() = List(COLUMNAS) { valores[it * 2 + 1] }

    val resultado: Resultado get() = calcular(totales, cremas)

    val vacia: Boolean get() = lecturas.all { it.isBlank() }
}

/** Un proceso = una tabla completa, identificada por su fecha. */
data class Proceso(
    val id: Long = 0,
    val fecha: Long = System.currentTimeMillis(),
    val muestras: List<Muestra> = emptyList(),
) {
    /** Unico total del proceso: el promedio de los % de Grasa de cada fila. */
    val promedioGrasa: Double?
        get() {
            val grasas = muestras.mapNotNull { it.resultado.porcGrasa }
            if (grasas.isEmpty()) return null
            return redondear(grasas.sum() / grasas.size)
        }

    /** Numero que se le propone a la proxima fila: el ultimo numerico + 1. */
    val siguienteNumero: String
        get() {
            val ultimo = muestras.lastOrNull()?.numero?.trim()?.toIntOrNull()
            return if (ultimo != null) (ultimo + 1).toString() else (muestras.size + 1).toString()
        }
}

/**
 * Las fechas se guardan como la medianoche UTC del dia elegido, que es justo lo que
 * devuelve el calendario de Material. Por eso tambien se formatean en UTC: si se
 * formatearan en la zona local, en Cuba (UTC-4) se mostraria el dia anterior.
 */
private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = UTC }
private val formatoArchivo = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = UTC }

fun fechaLegible(millis: Long): String = formatoFecha.format(Date(millis))
fun fechaArchivo(millis: Long): String = formatoArchivo.format(Date(millis))

/** Medianoche UTC del dia de hoy segun el calendario local del telefono. */
fun hoy(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(UTC).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Suma dias a una fecha guardada (se usa para los rangos de exportacion). */
fun sumarDias(millis: Long, dias: Int): Long =
    Calendar.getInstance(UTC).apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_MONTH, dias)
    }.timeInMillis

/** Formatea un resultado a un decimal, o "—" si todavia no se puede calcular. */
fun Double?.aTexto(): String =
    if (this == null) "—" else String.format(Locale.US, "%.1f", this)
