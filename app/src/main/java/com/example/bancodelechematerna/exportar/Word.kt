package com.example.bancodelechematerna.exportar

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Rellena una plantilla .docx de las del banco de leche, sin Apache POI.
 *
 * Lo que se espera de la plantilla (asi son los tres modelos):
 * - Un parrafo con "FECHA: ____" donde va la fecha.
 * - Una sola tabla: la primera fila es el encabezado y las demas estan vacias.
 *
 * Las filas vacias ya traen su formato (letra, tamaño), asi que solo se les mete el
 * texto, centrado salvo en [sinCentrar] (la columna Nro). Si hay menos datos que filas,
 * sobran filas en blanco como en el papel; si hay mas, se agregan copias de la primera
 * fila vacia.
 */
internal fun rellenarDocx(
    plantilla: InputStream,
    salida: OutputStream,
    fecha: String,
    filas: List<List<Casilla>>,
    sinCentrar: Set<Int>,
) {
    val origen = ZipInputStream(plantilla)
    val destino = ZipOutputStream(salida)
    generateSequence { origen.nextEntry }.forEach { archivo ->
        // Se lee entero: el parser de XML cerraria el zip al terminar.
        var bytes = origen.readBytes()
        if (archivo.name == DOCUMENTO) bytes = rellenarDocumento(bytes, fecha, filas, sinCentrar)
        destino.putNextEntry(ZipEntry(archivo.name))
        destino.write(bytes)
        destino.closeEntry()
    }
    destino.finish()
}

private const val DOCUMENTO = "word/document.xml"
private const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
private const val XML = "http://www.w3.org/XML/1998/namespace"

/** Lo que va en una celda. [alerta] = valor fuera de rango, se escribe en rojo. */
data class Casilla(val texto: String, val alerta: Boolean = false)

private const val ROJO = "FF0000"

private fun rellenarDocumento(
    xml: ByteArray,
    fecha: String,
    filas: List<List<Casilla>>,
    sinCentrar: Set<Int>,
): ByteArray {
    val doc = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(xml))

    ponerFecha(doc, fecha)

    val tabla = doc.getElementsByTagNameNS(W, "tbl").item(0) as? Element
        ?: error("La plantilla no tiene tabla")
    val renglones = tabla.hijos("tr")
    check(renglones.size >= 2) { "La plantilla necesita al menos una fila vacia" }

    val molde = renglones[1]
    renglones.drop(1).forEach { tabla.removeChild(it) }
    repeat(maxOf(filas.size, renglones.size - 1)) { i ->
        val renglon = molde.cloneNode(true) as Element
        filas.getOrNull(i)?.let { llenar(doc, renglon, it, sinCentrar) }
        tabla.appendChild(renglon)
    }

    return ByteArrayOutputStream().also {
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc), StreamResult(it))
    }.toByteArray()
}

/** Cambia "FECHA: ______" por "FECHA: 26/09/2026" en el primer texto que lo tenga. */
private fun ponerFecha(doc: Document, fecha: String) {
    val textos = doc.getElementsByTagNameNS(W, "t")
    for (i in 0 until textos.length) {
        val t = textos.item(i)
        val original = t.textContent
        if ("FECHA:" in original) {
            t.textContent = original.replace(Regex("FECHA:\\s*_+"), "FECHA: $fecha")
            return
        }
    }
}

/** Mete cada texto en la celda que le toca, dentro de la corrida que ya tiene el formato. */
private fun llenar(doc: Document, renglon: Element, casillas: List<Casilla>, sinCentrar: Set<Int>) {
    renglon.hijos("tc").zip(casillas).forEachIndexed { columna, (celda, casilla) ->
        if (casilla.texto.isBlank()) return@forEachIndexed
        val parrafo = celda.hijos("p").firstOrNull() ?: return@forEachIndexed
        if (columna !in sinCentrar) {
            val pPr = hijoOCrear(doc, parrafo, "pPr", antesDe = null)
            val jc = hijoOCrear(doc, pPr, "jc", antesDe = DESPUES_DE_JC)
            jc.setAttributeNS(W, "w:val", "center")
        }
        val corrida = parrafo.hijos("r").firstOrNull()
            ?: doc.createElementNS(W, "w:r").also { parrafo.appendChild(it) }
        if (casilla.alerta) {
            val rPr = hijoOCrear(doc, corrida, "rPr", antesDe = null)
            val color = hijoOCrear(doc, rPr, "color", antesDe = DESPUES_DE_COLOR)
            color.setAttributeNS(W, "w:val", ROJO)
        }
        val t = doc.createElementNS(W, "w:t")
        t.setAttributeNS(XML, "xml:space", "preserve")
        t.textContent = casilla.texto
        corrida.appendChild(t)
    }
}

/*
 * Word rechaza el documento si los elementos de w:pPr y w:rPr no van en el orden del
 * esquema. Estas son las etiquetas que tienen que quedar detras del elemento que se agrega.
 */
private val DESPUES_DE_JC = setOf(
    "textDirection", "textAlignment", "textboxTightWrap", "outlineLvl", "divId",
    "cnfStyle", "rPr", "sectPr", "pPrChange",
)
private val DESPUES_DE_COLOR = setOf(
    "spacing", "w", "kern", "position", "sz", "szCs", "highlight", "u", "effect", "bdr",
    "shd", "fitText", "vertAlign", "rtl", "cs", "em", "lang", "eastAsianLayout",
    "specVanish", "oMath",
)

/**
 * El hijo [nombre] de [padre]; si no existe, lo crea delante del primero de [antesDe],
 * o como primer hijo si [antesDe] es null (asi van w:pPr y w:rPr).
 */
private fun hijoOCrear(doc: Document, padre: Element, nombre: String, antesDe: Set<String>?): Element {
    padre.hijos(nombre).firstOrNull()?.let { return it }
    val nuevo = doc.createElementNS(W, "w:$nombre")
    var nodo = padre.firstChild
    if (antesDe != null) {
        while (nodo != null && !(nodo is Element && nodo.localName in antesDe)) nodo = nodo.nextSibling
    }
    padre.insertBefore(nuevo, nodo)
    return nuevo
}

/** Hijos directos con ese nombre en el espacio de Word (no nietos: hay tablas en celdas). */
private fun Element.hijos(nombre: String): List<Element> {
    val lista = mutableListOf<Element>()
    var nodo = firstChild
    while (nodo != null) {
        if (nodo is Element && nodo.namespaceURI == W && nodo.localName == nombre) lista += nodo
        nodo = nodo.nextSibling
    }
    return lista
}
