package com.example.bancodelechematerna.exportar

import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escritor minimo de archivos .xlsx (un .zip con XML dentro).
 * Se hace a mano para no arrastrar Apache POI, que en Android pesa muchisimo.
 */

sealed interface Celda {
    data class Texto(val valor: String, val negrita: Boolean = false) : Celda
    data class Numero(val valor: Double, val negrita: Boolean = false) : Celda
    data class NumeroInt(val valor: Int, val negrita: Boolean = false) : Celda
    data object Vacia : Celda
}

fun texto(valor: String, negrita: Boolean = false): Celda = Celda.Texto(valor, negrita)
fun numero(valor: Double?, negrita: Boolean = false): Celda =
    if (valor == null) Celda.Vacia else Celda.Numero(valor, negrita)

fun numeroInt(valor: Int?, negrita: Boolean = false): Celda =
    if (valor == null) Celda.Vacia else Celda.NumeroInt(valor, negrita)

class Hoja(nombre: String) {
    val nombre: String = limpiarNombre(nombre)
    val filas = mutableListOf<List<Celda>>()
    var anchos: List<Double> = emptyList()

    fun fila(vararg celdas: Celda) {
        filas.add(celdas.toList())
    }

    fun filaVacia() {
        filas.add(emptyList())
    }
}

/** Excel no admite estos caracteres en el nombre de la hoja, ni mas de 31 letras. */
private fun limpiarNombre(nombre: String): String =
    nombre.replace(Regex("""[\\/*\[\]:?]"""), "-").take(31).ifBlank { "Hoja" }

fun escribirXlsx(salida: OutputStream, hojas: List<Hoja>) {
    require(hojas.isNotEmpty()) { "El libro necesita al menos una hoja" }

    ZipOutputStream(salida).use { zip ->
        zip.entrada("[Content_Types].xml", contentTypes(hojas.size))
        zip.entrada("_rels/.rels", relsRaiz())
        zip.entrada("xl/workbook.xml", workbook(hojas))
        zip.entrada("xl/_rels/workbook.xml.rels", relsWorkbook(hojas.size))
        zip.entrada("xl/styles.xml", styles())
        hojas.forEachIndexed { i, hoja ->
            zip.entrada("xl/worksheets/sheet${i + 1}.xml", worksheet(hoja))
        }
    }
}

private fun ZipOutputStream.entrada(ruta: String, contenido: String) {
    putNextEntry(ZipEntry(ruta))
    write(contenido.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private const val CABECERA = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
private const val NS_HOJA = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

private fun contentTypes(cantidadHojas: Int) = buildString {
    append(CABECERA)
    append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
    append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
    append("""<Default Extension="xml" ContentType="application/xml"/>""")
    append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
    append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
    repeat(cantidadHojas) { i ->
        append("""<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
    }
    append("</Types>")
}

private fun relsRaiz() = CABECERA +
    """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
    """<Relationship Id="rId1" Type="$NS_REL/officeDocument" Target="xl/workbook.xml"/>""" +
    "</Relationships>"

private fun workbook(hojas: List<Hoja>) = buildString {
    append(CABECERA)
    append("""<workbook xmlns="$NS_HOJA" xmlns:r="$NS_REL"><sheets>""")
    hojas.forEachIndexed { i, hoja ->
        append("""<sheet name="${escapar(hoja.nombre)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
    }
    append("</sheets></workbook>")
}

private fun relsWorkbook(cantidadHojas: Int) = buildString {
    append(CABECERA)
    append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
    repeat(cantidadHojas) { i ->
        append("""<Relationship Id="rId${i + 1}" Type="$NS_REL/worksheet" Target="worksheets/sheet${i + 1}.xml"/>""")
    }
    append("""<Relationship Id="rIdEstilos" Type="$NS_REL/styles" Target="styles.xml"/>""")
    append("</Relationships>")
}

/** Dos estilos: 0 = normal, 1 = negrita. Excel exige los dos rellenos de siempre. */
private fun styles() = CABECERA +
    """<styleSheet xmlns="$NS_HOJA">""" +
    """<fonts count="2">""" +
    """<font><sz val="11"/><name val="Calibri"/></font>""" +
    """<font><b/><sz val="11"/><name val="Calibri"/></font>""" +
    """</fonts>""" +
    """<fills count="2">""" +
    """<fill><patternFill patternType="none"/></fill>""" +
    """<fill><patternFill patternType="gray125"/></fill>""" +
    """</fills>""" +
    """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
    """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
    """<cellXfs count="2">""" +
    """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
    """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
    """</cellXfs></styleSheet>"""

private fun worksheet(hoja: Hoja) = buildString {
    append(CABECERA)
    append("""<worksheet xmlns="$NS_HOJA">""")
    if (hoja.anchos.isNotEmpty()) {
        append("<cols>")
        hoja.anchos.forEachIndexed { i, ancho ->
            append("""<col min="${i + 1}" max="${i + 1}" width="$ancho" customWidth="1"/>""")
        }
        append("</cols>")
    }
    append("<sheetData>")
    hoja.filas.forEachIndexed { fi, fila ->
        append("""<row r="${fi + 1}">""")
        fila.forEachIndexed { ci, celda ->
            append(celdaXml(celda, "${letraColumna(ci)}${fi + 1}"))
        }
        append("</row>")
    }
    append("</sheetData></worksheet>")
}

private fun celdaXml(celda: Celda, referencia: String): String = when (celda) {
    is Celda.Vacia -> ""
    is Celda.Texto -> {
        val estilo = if (celda.negrita) """ s="1"""" else ""
        """<c r="$referencia"$estilo t="inlineStr"><is><t xml:space="preserve">${escapar(celda.valor)}</t></is></c>"""
    }
    is Celda.Numero -> {
        val estilo = if (celda.negrita) """ s="1"""" else ""
        """<c r="$referencia"$estilo><v>${recortar(celda.valor)}</v></c>"""
    }
    is Celda.NumeroInt -> {
        val estilo = if (celda.negrita) """ s="1"""" else ""
        """<c r="$referencia"$estilo><v>${celda.valor}</v></c>"""
    }

}

/** Evita notaciones tipo 1.0E2 y ceros de mas en el XML. */
private fun recortar(valor: Double): String {
    val redondeado = Math.round(valor * 1_000_000.0) / 1_000_000.0
    return if (redondeado == Math.floor(redondeado)) redondeado.toLong().toString()
    else String.format(Locale.US, "%.6f", redondeado).trimEnd('0').trimEnd('.')
}

internal fun letraColumna(indice: Int): String {
    var n = indice
    val sb = StringBuilder()
    while (true) {
        sb.insert(0, 'A' + (n % 26))
        n = n / 26 - 1
        if (n < 0) break
    }
    return sb.toString()
}

private fun escapar(texto: String): String = texto
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
