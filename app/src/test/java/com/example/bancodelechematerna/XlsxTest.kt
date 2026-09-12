package com.example.bancodelechematerna

import com.example.bancodelechematerna.datos.Muestra
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.exportar.Exportador
import com.example.bancodelechematerna.exportar.escribirXlsx
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxTest {

    private val procesos = listOf(
        Proceso(
            id = 1,
            fecha = 1_757_635_200_000L, // 12/09/2026
            muestras = listOf(
                Muestra(1, "1", listOf("80", "4", "70", "4", "80", "4")),
                Muestra(2, "2", listOf("90", "5", "85", "4", "88", "5")),
                Muestra(3, "3", listOf("75", "3", "78", "4", "", "")),
            ),
        ),
        Proceso(
            id = 2,
            fecha = 1_757_376_000_000L, // 09/09/2026
            muestras = listOf(
                // Con caracteres que hay que escapar en el XML.
                Muestra(4, "A&1", listOf("66", "3", "71", "4", "69", "3")),
            ),
        ),
    )

    private fun generar(): ByteArray = ByteArrayOutputStream().also { salida ->
        escribirXlsx(salida, Exportador.construirHojas(procesos))
    }.toByteArray()

    @Test
    fun `el libro es un zip con todas sus partes y XML valido`() {
        val bytes = generar()
        val partes = mutableSetOf<String>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entrada ->
                partes += entrada.name
                val contenido = zip.readBytes()
                // Si algo quedo mal escapado o sin cerrar, esto revienta.
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(ByteArrayInputStream(contenido))
            }
        }

        val esperadas = setOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml",
        )
        assertTrue("Faltan partes: ${esperadas - partes}", partes.containsAll(esperadas))
    }
}
