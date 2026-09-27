package com.example.bancodelechematerna.exportar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.example.bancodelechematerna.datos.COLUMNAS
import com.example.bancodelechematerna.datos.Muestra
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.aTexto
import com.example.bancodelechematerna.datos.acidezFueraDeRango
import com.example.bancodelechematerna.datos.fechaArchivo
import com.example.bancodelechematerna.datos.fechaLegible
import com.example.bancodelechematerna.datos.horaLegible
import com.example.bancodelechematerna.datos.totalTexto
import java.io.File
import java.io.OutputStream

const val TIPO_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

/**
 * Los tres modelos en papel del banco de leche. Cada uno es una plantilla .docx en
 * assets/modelos y se llena con las filas del proceso que tengan datos de su seccion.
 * Todo va centrado menos la columna Nro ([sinCentrar]).
 */
enum class Modelo(val nombre: String, val plantilla: String, val sinCentrar: Set<Int>) {
    ACIDEZ("Acidez", "modelos/acidez.docx", setOf(0)),
    CREMATOCRITO("Crematocrito", "modelos/crematocrito.docx", setOf(0)),
    PASTEURIZACION("Pasteurizacion", "modelos/pasteurizacion.docx", emptySet());

    /** Una lista de casillas por fila, en el orden de las columnas de la plantilla. */
    fun filas(proceso: Proceso): List<List<Casilla>> = when (this) {
        // Nro | 1 | 2 | 3   (la acidez fuera de rango va en rojo, como en la app)
        ACIDEZ -> proceso.muestras
            .filter { m -> m.acidez.any { it.isNotBlank() } }
            .map { m -> listOf(Casilla(m.numero)) + m.acidez.map { Casilla(it, acidezFueraDeRango(it)) } }

        // NO. | T. de crema 1 | 2 | 3 | Total | %Crema | %Grasa | Kcal
        CREMATOCRITO -> proceso.muestras
            .filter { m -> m.lecturas.any { it.isNotBlank() } }
            .map { filaCrematocrito(it).map(::Casilla) }

        // Hora | Baño M | Punto frio | Agua  (esta plantilla no lleva Nro)
        PASTEURIZACION -> proceso.muestras
            .filter { m -> m.hora.isNotBlank() || m.temperaturas.any { it.isNotBlank() } }
            .map { m ->
                (listOf(horaLegible(m.hora)) + m.temperaturas.map { if (it.isBlank()) "" else "$it °C" })
                    .map(::Casilla)
            }
    }
}

/** Cada columna de lectura va en una celda como "total : crema", igual que el Total. */
private fun filaCrematocrito(m: Muestra): List<String> {
    val r = m.resultado
    return buildList {
        add(m.numero)
        repeat(COLUMNAS) { add(par(m.lecturas[it * 2], m.lecturas[it * 2 + 1])) }
        add(if (r.promTotal == null) "" else r.totalTexto())
        add(r.porcCrema.enCelda())
        add(r.porcGrasa.enCelda())
        add(r.kcal.enCelda())
    }
}

private fun par(total: String, crema: String): String =
    if (total.isBlank() && crema.isBlank()) "" else "${total.ifBlank { "—" }} : ${crema.ifBlank { "—" }}"

/** En el papel, lo que no se pudo calcular queda en blanco (no con raya). */
private fun Double?.enCelda(): String = if (this == null) "" else aTexto()

object Exportador {

    fun nombreArchivo(modelo: Modelo, proceso: Proceso): String =
        "${modelo.nombre} ${fechaArchivo(proceso.fecha)}.docx"

    /** Deja los archivos en la cache de la app, listos para compartir. */
    fun generarEnCache(context: Context, modelos: List<Modelo>, proceso: Proceso): List<File> {
        val carpeta = File(context.cacheDir, "exportes").apply { mkdirs() }
        carpeta.listFiles()?.forEach { it.delete() }
        return modelos.map { modelo ->
            File(carpeta, nombreArchivo(modelo, proceso)).also { archivo ->
                archivo.outputStream().use { escribir(context, modelo, proceso, it) }
            }
        }
    }

    /**
     * Escribe cada modelo en la carpeta que eligio el usuario. Si ya hay un archivo con
     * ese nombre, el sistema le agrega un numero en vez de pisarlo.
     */
    fun generarEnCarpeta(context: Context, carpeta: Uri, modelos: List<Modelo>, proceso: Proceso) {
        val resolver = context.contentResolver
        val padre = DocumentsContract.buildDocumentUriUsingTree(
            carpeta, DocumentsContract.getTreeDocumentId(carpeta)
        )
        modelos.forEach { modelo ->
            val destino = DocumentsContract.createDocument(
                resolver, padre, TIPO_DOCX, nombreArchivo(modelo, proceso)
            ) ?: error("No se pudo crear ${nombreArchivo(modelo, proceso)}")
            resolver.openOutputStream(destino, "wt")?.use {
                escribir(context, modelo, proceso, it)
            } ?: error("No se pudo abrir ${nombreArchivo(modelo, proceso)}")
        }
    }

    fun compartir(context: Context, archivos: List<File>) {
        val uris = archivos.map {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
        }
        val envio = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }.apply {
            type = TIPO_DOCX
            putExtra(Intent.EXTRA_SUBJECT, archivos.joinToString { it.nameWithoutExtension })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val titulo = if (archivos.size == 1) "Compartir ${archivos[0].name}"
        else "Compartir ${archivos.size} archivos"
        context.startActivity(
            Intent.createChooser(envio, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun escribir(context: Context, modelo: Modelo, proceso: Proceso, salida: OutputStream) {
        context.assets.open(modelo.plantilla).use { plantilla ->
            rellenarDocx(
                plantilla, salida, fechaLegible(proceso.fecha), modelo.filas(proceso), modelo.sinCentrar
            )
        }
    }
}
