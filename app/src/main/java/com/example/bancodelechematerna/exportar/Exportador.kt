package com.example.bancodelechematerna.exportar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
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

/** Subcarpeta de Descargas donde se guardan los Word. */
private const val CARPETA = "Banco de Leche"

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
     * Donde queda lo guardado, para decirselo al usuario. La carpeta real se llama
     * "Download" (algunas apps de archivos la traducen a "Descargas" y otras no).
     */
    const val CARPETA_LEGIBLE = "Memoria interna › Download › $CARPETA"

    /** Android 7 a 9 necesita el permiso de almacenamiento para escribir en Descargas. */
    val necesitaPermiso: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /**
     * Guarda cada modelo en Descargas/Banco de Leche, sin selector de carpetas: el de
     * Android no deja elegir Descargas y en muchos telefonos no muestra las carpetas.
     * Si el archivo ya existe (se exporta de nuevo el mismo dia), se sobrescribe.
     */
    fun guardarEnDescargas(context: Context, modelos: List<Modelo>, proceso: Proceso) {
        modelos.forEach { modelo ->
            val nombre = nombreArchivo(modelo, proceso)
            if (necesitaPermiso) {
                @Suppress("DEPRECATION")
                val carpeta = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    CARPETA,
                ).apply { mkdirs() }
                val archivo = File(carpeta, nombre)
                archivo.outputStream().use { escribir(context, modelo, proceso, it) }
                MediaScannerConnection.scanFile(context, arrayOf(archivo.path), arrayOf(TIPO_DOCX), null)
            } else {
                guardarConMediaStore(context, nombre) { escribir(context, modelo, proceso, it) }
            }
        }
    }

    /**
     * Desde Android 10 se escribe en Descargas por MediaStore, sin pedir permisos. La app
     * solo ve (y puede sobrescribir) los archivos que creo ella misma.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun guardarConMediaStore(context: Context, nombre: String, contenido: (OutputStream) -> Unit) {
        val resolver = context.contentResolver
        val coleccion = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val rutaRelativa = "${Environment.DIRECTORY_DOWNLOADS}/$CARPETA/"

        val existente = resolver.query(
            coleccion,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(nombre, rutaRelativa),
            null,
        )?.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(coleccion, c.getLong(0)) else null }

        val destino = existente ?: resolver.insert(coleccion, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
            put(MediaStore.MediaColumns.MIME_TYPE, TIPO_DOCX)
            put(MediaStore.MediaColumns.RELATIVE_PATH, rutaRelativa)
        }) ?: error("No se pudo crear $nombre")

        resolver.openOutputStream(destino, "wt")?.use(contenido)
            ?: error("No se pudo abrir $nombre")
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
