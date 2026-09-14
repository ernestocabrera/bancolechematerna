package com.example.bancodelechematerna.datos

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Constantes del protocolo con el que se calcula en el banco de leche.
 * Si alguna vez cambian, se cambian aqui y afecta a toda la app.
 */
object Formula {
    /** % Grasa = % Crema - GRASA_A / GRASA_B  (tal cual se calcula a mano hoy). */
    const val GRASA_A = 0.59
    const val GRASA_B = 1.46

    /** Kcal/L = % Crema * KCAL_FACTOR + KCAL_BASE */
    const val KCAL_FACTOR = 66.8
    const val KCAL_BASE = 290.0
}

/** Redondeo "de escuela": 0.05 sube. El de Java por defecto no se comporta asi. */
fun redondear(valor: Double, decimales: Int = 1): Double {
    if (valor.isNaN() || valor.isInfinite()) return valor
    return BigDecimal.valueOf(valor).setScale(decimales, RoundingMode.HALF_UP).toDouble()
}

/** Resultado de una fila. Cualquier campo puede ser null si faltan lecturas. */
data class Resultado(
    val promTotal: Double? = null,   // P1: promedio de las columnas totales
    val promCrema: Int? = null,   // P2: promedio de las columnas de crema
    val porcCrema: Double? = null,
    val porcGrasa: Double? = null,
    val kcal: Double? = null,
)

/**
 * Encadena los calculos usando siempre el valor ya redondeado del paso anterior,
 * igual que cuando se hace a lapiz.
 */
fun calcular(totales: List<Int?>, cremas: List<Int?>): Resultado {
    val p1 = promedio(totales)
    val p2 = cremas.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0
    //val p2 = promedio(cremas)
    if (p1 == null || p2 == null || p1 == 0.0) return Resultado(p1, p2)

    val crema = redondear(p2 * 100.0 / p1)
    val grasa = redondear(crema - Formula.GRASA_A / Formula.GRASA_B)
    val kcal = redondear(crema * Formula.KCAL_FACTOR + Formula.KCAL_BASE)
    return Resultado(p1, p2, crema, grasa, kcal)
}

/** Promedia solo las lecturas que esten escritas, redondeado a un decimal. */
private fun promedio(valores: List<Int?>): Double? {
    val presentes = valores.filterNotNull()
    if (presentes.isEmpty()) return null
    return redondear(presentes.sum().toDouble() / presentes.size)
}
