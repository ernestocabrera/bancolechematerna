package com.example.bancodelechematerna

import com.example.bancodelechematerna.datos.Muestra
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.calcular
import com.example.bancodelechematerna.datos.redondear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Los casos vienen de tablas hechas a mano en el banco de leche.
 * Si algun dia se corrige la formula de % Grasa, estos numeros cambian a proposito.
 */
class CalculoTest {

    @Test
    fun `fila de ejemplo 80-4 70-4 80-4`() {
        val r = calcular(listOf(80, 70, 80), listOf(4, 4, 4))
        assertEquals(76.7, r.promTotal!!, 0.0)
        assertEquals(4.0, r.promCrema!!, 0.0)
        assertEquals(5.2, r.porcCrema!!, 0.0)
        assertEquals(4.8, r.porcGrasa!!, 0.0)
        assertEquals(637.4, r.kcal!!, 0.0)
    }

    @Test
    fun `fila con lecturas desiguales`() {
        val r = calcular(listOf(90, 85, 88), listOf(5, 4, 5))
        assertEquals(87.7, r.promTotal!!, 0.0)
        assertEquals(4.7, r.promCrema!!, 0.0)
        assertEquals(5.4, r.porcCrema!!, 0.0)
        assertEquals(5.0, r.porcGrasa!!, 0.0)
        assertEquals(650.7, r.kcal!!, 0.0)
    }

    @Test
    fun `se promedia solo lo que esta escrito`() {
        val r = calcular(listOf(80, null, null), listOf(4, null, null))
        assertEquals(80.0, r.promTotal!!, 0.0)
        assertEquals(4.0, r.promCrema!!, 0.0)
        assertEquals(5.0, r.porcCrema!!, 0.0)
    }

    @Test
    fun `fila vacia no calcula nada`() {
        val r = calcular(listOf(null, null, null), listOf(null, null, null))
        assertNull(r.promTotal)
        assertNull(r.porcCrema)
        assertNull(r.kcal)
    }

    @Test
    fun `el redondeo sube en el 5`() {
        assertEquals(76.7, redondear(76.66666), 0.0)
        assertEquals(4.3, redondear(4.25), 0.0)
        assertEquals(5.2, redondear(5.15), 0.0)
    }

    @Test
    fun `el promedio del proceso solo cuenta filas calculadas`() {
        val proceso = Proceso(
            muestras = listOf(
                Muestra(id = 1, numero = "1", lecturas = listOf("80", "4", "70", "4", "80", "4")),
                Muestra(id = 2, numero = "2", lecturas = listOf("90", "5", "85", "4", "88", "5")),
                Muestra(id = 3, numero = "3"),
            )
        )
        // (4.8 + 5.0) / 2
        assertEquals(4.9, proceso.promedioGrasa!!, 0.0)
    }

    @Test
    fun `el siguiente numero continua al ultimo`() {
        val proceso = Proceso(
            muestras = listOf(Muestra(id = 1, numero = "7"), Muestra(id = 2, numero = "8"))
        )
        assertEquals("9", proceso.siguienteNumero)
    }
}
