package com.example.bancodelechematerna.datos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val BD_NOMBRE = "banco_leche.db"
private const val BD_VERSION = 1

private const val T_PROCESO = "proceso"
private const val T_MUESTRA = "muestra"

/** Columnas l1..l6 = total1, crema1, total2, crema2, total3, crema3. */
private val COLS_LECTURA = List(LECTURAS) { "l${it + 1}" }

internal class BaseDatos(context: Context) :
    SQLiteOpenHelper(context, BD_NOMBRE, null, BD_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T_PROCESO (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_MUESTRA (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                proceso_id INTEGER NOT NULL,
                orden INTEGER NOT NULL,
                numero TEXT NOT NULL DEFAULT '',
                ${COLS_LECTURA.joinToString(",\n") { "$it TEXT NOT NULL DEFAULT ''" }},
                FOREIGN KEY (proceso_id) REFERENCES $T_PROCESO (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_muestra_proceso ON $T_MUESTRA (proceso_id, orden)")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
        // Todavia no hay migraciones: la version 1 es la primera.
    }
}

/**
 * Acceso a la base local. Todo el trabajo es en memoria del lado de la app
 * (son pocos datos) y aqui solo se persiste.
 */
class Repositorio(context: Context) {
    private val helper = BaseDatos(context.applicationContext)

    fun cargarTodo(): List<Proceso> {
        val db = helper.readableDatabase
        val muestrasPorProceso = mutableMapOf<Long, MutableList<Muestra>>()

        db.rawQuery(
            "SELECT id, proceso_id, numero, ${COLS_LECTURA.joinToString()} " +
                "FROM $T_MUESTRA ORDER BY proceso_id, orden, id",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val procesoId = c.getLong(1)
                val muestra = Muestra(
                    id = c.getLong(0),
                    numero = c.getString(2),
                    lecturas = List(LECTURAS) { c.getString(3 + it) },
                )
                muestrasPorProceso.getOrPut(procesoId) { mutableListOf() } += muestra
            }
        }

        val procesos = mutableListOf<Proceso>()
        db.rawQuery(
            "SELECT id, fecha FROM $T_PROCESO ORDER BY fecha DESC, id DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                procesos += Proceso(
                    id = id,
                    fecha = c.getLong(1),
                    muestras = muestrasPorProceso[id].orEmpty(),
                )
            }
        }
        return procesos
    }

    fun crearProceso(fecha: Long): Long =
        helper.writableDatabase.insert(T_PROCESO, null, ContentValues().apply {
            put("fecha", fecha)
        })

    fun actualizarFecha(procesoId: Long, fecha: Long) {
        helper.writableDatabase.update(
            T_PROCESO, ContentValues().apply { put("fecha", fecha) },
            "id = ?", arrayOf(procesoId.toString())
        )
    }

    fun eliminarProceso(procesoId: Long) {
        helper.writableDatabase.delete(T_PROCESO, "id = ?", arrayOf(procesoId.toString()))
    }

    fun crearMuestra(procesoId: Long, orden: Int, numero: String): Long =
        helper.writableDatabase.insert(T_MUESTRA, null, ContentValues().apply {
            put("proceso_id", procesoId)
            put("orden", orden)
            put("numero", numero)
        })

    fun guardarMuestra(muestra: Muestra) {
        helper.writableDatabase.update(
            T_MUESTRA,
            ContentValues().apply {
                put("numero", muestra.numero)
                COLS_LECTURA.forEachIndexed { i, col -> put(col, muestra.lecturas[i]) }
            },
            "id = ?", arrayOf(muestra.id.toString())
        )
    }

    fun eliminarMuestra(muestraId: Long) {
        helper.writableDatabase.delete(T_MUESTRA, "id = ?", arrayOf(muestraId.toString()))
    }

    /** Reescribe el campo orden segun la posicion actual en la lista. */
    fun reordenar(muestras: List<Muestra>) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            muestras.forEachIndexed { i, m ->
                db.update(
                    T_MUESTRA, ContentValues().apply { put("orden", i) },
                    "id = ?", arrayOf(m.id.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
