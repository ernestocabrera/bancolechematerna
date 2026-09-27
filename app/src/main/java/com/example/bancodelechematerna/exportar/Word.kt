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
 * Las filas vacias ya traen su formato (letra, tamaño, alineacion), asi que solo se
 * les mete el texto. Si hay menos datos que filas, sobran filas en blanco como en el
 * papel; si hay mas, se agregan copias de la primera fila vacia.
 */
internal fun rellenarDocx(
    plantilla: InputStream,
    salida: OutputStream,
    fecha: String,
    filas: List<List<String>>,
) {
    val origen = ZipInputStream(plantilla)
    val destino = ZipOutputStream(salida)
    generateSequence { origen.nextEntry }.forEach { archivo ->
        // Se lee entero: el parser de XML cerraria el zip al terminar.
        var bytes = origen.readBytes()
        if (archivo.name == DOCUMENTO) bytes = rellenarDocumento(bytes, fecha, filas)
        destino.putNextEntry(ZipEntry(archivo.name))
        destino.write(bytes)
        destino.closeEntry()
    }
    destino.finish()
}

private const val DOCUMENTO = "word/document.xml"
private const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
private const val XML = "http://www.w3.org/XML/1998/namespace"

private fun rellenarDocumento(xml: ByteArray, fecha: String, filas: List<List<String>>): ByteArray {
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
        filas.getOrNull(i)?.let { llenar(doc, renglon, it) }
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
private fun llenar(doc: Document, renglon: Element, textos: List<String>) {
    renglon.hijos("tc").zip(textos).forEach { (celda, texto) ->
        if (texto.isBlank()) return@forEach
        val parrafo = celda.hijos("p").firstOrNull() ?: return@forEach
        val corrida = parrafo.hijos("r").firstOrNull()
            ?: doc.createElementNS(W, "w:r").also { parrafo.appendChild(it) }
        val t = doc.createElementNS(W, "w:t")
        t.setAttributeNS(XML, "xml:space", "preserve")
        t.textContent = texto
        corrida.appendChild(t)
    }
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
