package com.example.bancodelechematerna.exportar

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.bancodelechematerna.datos.COLUMNAS
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.fechaArchivo
import com.example.bancodelechematerna.datos.fechaLegible
import java.io.File

const val TIPO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * Los dos numeros de cada columna no tienen nombre en el modelo de papel, asi que el
 * encabezado solo lleva el numero del grupo (1, 2, 3) sobre la primera de sus dos celdas.
 * Igual con "Total", que en el papel es una sola casilla con los dos promedios.
 */
private val ENCABEZADOS = buildList {
    add("Nro")
    repeat(COLUMNAS) { add("${it + 1}"); add("") }
    add("Total")
    add("")
    add("% Crema")
    add("% Grasa")
    add("Kcal")
}

private val ANCHOS = buildList {
    add(6.0)
    repeat(COLUMNAS * 2) { add(9.0) }
    add(11.0); add(11.0); add(9.0); add(9.0); add(9.0)
}

object Exportador {

    /** Nombre sugerido del archivo, segun sea un proceso o un rango. */
    fun nombreArchivo(procesos: List<Proceso>): String = when {
        procesos.isEmpty() -> "Procesos.xlsx"
        procesos.size == 1 -> "Proceso ${fechaArchivo(procesos[0].fecha)}.xlsx"
        else -> {
            val fechas = procesos.map { it.fecha }
            "Procesos ${fechaArchivo(fechas.min())} a ${fechaArchivo(fechas.max())}.xlsx"
        }
    }

    /** Deja el archivo en la cache de la app, listo para compartir. */
    fun generarEnCache(context: Context, procesos: List<Proceso>): File {
        val carpeta = File(context.cacheDir, "exportes").apply { mkdirs() }
        carpeta.listFiles()?.forEach { it.delete() }
        val archivo = File(carpeta, nombreArchivo(procesos))
        archivo.outputStream().use { escribirXlsx(it, construirHojas(procesos)) }
        return archivo
    }

    /** Escribe directamente en el destino que eligio el usuario (Guardar como...). */
    fun generarEn(context: Context, destino: Uri, procesos: List<Proceso>) {
        context.contentResolver.openOutputStream(destino, "wt")?.use {
            escribirXlsx(it, construirHojas(procesos))
        } ?: error("No se pudo abrir el destino")
    }

    fun compartir(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", archivo
        )
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = TIPO_XLSX
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, archivo.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(envio, "Compartir ${archivo.name}")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    internal fun construirHojas(procesos: List<Proceso>): List<Hoja> {
        if (procesos.isEmpty()) return listOf(Hoja("Sin datos"))

        val usados = mutableMapOf<String, Int>()
        return procesos.sortedBy { it.fecha }.map { proceso ->
            val base = fechaLegible(proceso.fecha).replace('/', '-')
            val repetidos = usados.merge(base, 1, Int::plus)!!
            val nombre = if (repetidos == 1) base else "$base ($repetidos)"
            hojaDeProceso(nombre, proceso)
        }
    }

    private fun hojaDeProceso(nombre: String, proceso: Proceso): Hoja = Hoja(nombre).apply {
        anchos = ANCHOS

        fila(texto("Banco de Leche Materna", negrita = true))
        fila(texto("Fecha:", negrita = true), texto(fechaLegible(proceso.fecha)))
        filaVacia()

        fila(*ENCABEZADOS.map { texto(it, negrita = true) }.toTypedArray())

        proceso.muestras.forEach { muestra ->
            val r = muestra.resultado
            val celdas = buildList {
                add(texto(muestra.numero))
                repeat(COLUMNAS) {
                    add(numero(muestra.totales[it]?.toDouble()))
                    add(numero(muestra.cremas[it]?.toDouble()))
                }
                add(numero(r.promTotal))
                add(numero(r.promCrema))
                add(numero(r.porcCrema))
                add(numero(r.porcGrasa))
                add(numero(r.kcal))
            }
            fila(*celdas.toTypedArray())
        }

        filaVacia()
        fila(
            texto("Promedio % Grasa", negrita = true),
            numero(proceso.promedioGrasa, negrita = true),
        )
    }
}
