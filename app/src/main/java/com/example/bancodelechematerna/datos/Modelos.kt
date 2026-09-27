package com.example.bancodelechematerna.datos

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Cantidad de columnas de lectura (1, 2, 3) y de casillas por fila. */
const val COLUMNAS = 3
const val LECTURAS = COLUMNAS * 2

/** Columnas de acidez (1, 2, 3), un numero entero cada una. */
const val ACIDECES = 3

/** Temperaturas de pasteurizacion, en este orden: Baño M, Punto frio, Agua. */
const val TEMPERATURAS = 3

/** Acidez esperada. Fuera de este rango se permite, pero se marca en rojo. */
val ACIDEZ_NORMAL = 2..8

/**
 * Una fila de la tabla. [lecturas] guarda lo tecleado en orden
 * total1, crema1, total2, crema2, total3, crema3.
 * Todo se guarda como texto para poder dejar casillas vacias mientras se escribe.
 * Acidez, hora y temperaturas son solo registro: no entran en ningun calculo.
 */
data class Muestra(
    val id: Long = 0,
    val numero: String = "",
    val lecturas: List<String> = List(LECTURAS) { "" },
    val acidez: List<String> = List(ACIDECES) { "" },
    /** Hora de pasteurizacion como "HH:mm" en 24 h, o vacia. Ver [horaLegible]. */
    val hora: String = "",
    val temperaturas: List<String> = List(TEMPERATURAS) { "" },
) {
    private val valores: List<Int?> get() = lecturas.map { it.trim().toIntOrNull() }

    val totales: List<Int?> get() = List(COLUMNAS) { valores[it * 2] }
    val cremas: List<Int?> get() = List(COLUMNAS) { valores[it * 2 + 1] }

    val resultado: Resultado get() = calcular(totales, cremas)

    val vacia: Boolean
        get() = lecturas.all { it.isBlank() } && acidez.all { it.isBlank() } &&
            hora.isBlank() && temperaturas.all { it.isBlank() }

    fun conLectura(indice: Int, valor: String) = copy(lecturas = lecturas.cambiar(indice, valor))
    fun conAcidez(indice: Int, valor: String) = copy(acidez = acidez.cambiar(indice, valor))
    /**
     * Al escribir una temperatura en una fila sin hora, se le pone la hora de ese momento
     * ([ahora], "HH:mm"). Despues se puede cambiar desde el reloj.
     */
    fun conTemperatura(indice: Int, valor: String, ahora: String) = copy(
        temperaturas = temperaturas.cambiar(indice, valor),
        hora = if (hora.isBlank() && valor.isNotBlank()) ahora else hora,
    )
}

private fun List<String>.cambiar(indice: Int, valor: String): List<String> =
    toMutableList().also { it[indice] = valor }

/** Una acidez escrita que no es un entero dentro de [ACIDEZ_NORMAL]. */
fun acidezFueraDeRango(texto: String): Boolean {
    if (texto.isBlank()) return false
    val valor = texto.trim().toIntOrNull() ?: return true
    return valor !in ACIDEZ_NORMAL
}

/** "14:30" -> (14, 30); null si esta vacia o mal formada. */
fun horaYMinutos(hora: String): Pair<Int, Int>? {
    val partes = hora.split(':')
    if (partes.size != 2) return null
    val h = partes[0].toIntOrNull() ?: return null
    val m = partes[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) h to m else null
}

fun horaGuardada(hora: Int, minuto: Int): String =
    String.format(Locale.US, "%02d:%02d", hora, minuto)

/** La hora del reloj del telefono, como "HH:mm". */
fun horaActual(): String = Calendar.getInstance().let {
    horaGuardada(it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE))
}

/** "14:30" -> "02:30 pm". Vacia si no hay hora. */
fun horaLegible(hora: String): String {
    val (h, m) = horaYMinutos(hora) ?: return ""
    val h12 = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.US, "%02d:%02d %s", h12, m, if (h < 12) "am" else "pm")
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

    /** Cuantas filas con Kcal calculada caen en cada uno de los [RangosKcal]. */
    val cantidadesPorRangoKcal: List<Int>
        get() {
            val cantidades = MutableList(RangosKcal.NOMBRES.size) { 0 }
            muestras.mapNotNull { it.resultado.kcal }.forEach { cantidades[RangosKcal.indice(it)]++ }
            return cantidades
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

/** Formatea un resultado a un decimal, o "—" si todavia no se puede calcular. */
fun Double?.aTexto(): String =
    if (this == null) "—" else String.format(Locale.US, "%.1f", this)

/** La casilla Total: "P1 : P2", por ejemplo "86.7 : 5". */
fun Resultado.totalTexto(): String = "${promTotal.aTexto()} : ${promCrema ?: "—"}"
