package com.example.bancodelechematerna.datos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val BD_NOMBRE = "banco_leche.db"
/** 1: lecturas del crematocrito. 2: acidez, hora y temperaturas de pasteurizacion. */
private const val BD_VERSION = 2

private const val T_PROCESO = "proceso"
private const val T_MUESTRA = "muestra"

/** Columnas l1..l6 = total1, crema1, total2, crema2, total3, crema3. */
private val COLS_LECTURA = List(LECTURAS) { "l${it + 1}" }

private val COLS_ACIDEZ = List(ACIDECES) { "acidez${it + 1}" }
private const val COL_HORA = "hora"
/** En el orden de [Muestra.temperaturas]: Baño M, Punto frio, Agua. */
private val COLS_TEMPERATURA = listOf("temp_bano", "temp_frio", "temp_agua")

/** Todas las casillas de texto de una muestra, en el orden en que se leen. */
private val COLS_TEXTO = COLS_LECTURA + COLS_ACIDEZ + COL_HORA + COLS_TEMPERATURA
private val COLS_VERSION_2 = COLS_ACIDEZ + COL_HORA + COLS_TEMPERATURA

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
                ${COLS_TEXTO.joinToString(",\n") { "$it TEXT NOT NULL DEFAULT ''" }},
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
        if (versionAnterior < 2) {
            COLS_VERSION_2.forEach {
                db.execSQL("ALTER TABLE $T_MUESTRA ADD COLUMN $it TEXT NOT NULL DEFAULT ''")
            }
        }
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
            "SELECT id, proceso_id, numero, ${COLS_TEXTO.joinToString()} " +
                "FROM $T_MUESTRA ORDER BY proceso_id, orden, id",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val procesoId = c.getLong(1)
                // Las casillas empiezan en la columna 3, en el orden de COLS_TEXTO.
                val casillas = List(COLS_TEXTO.size) { c.getString(3 + it) }.iterator()
                fun siguientes(n: Int) = List(n) { casillas.next() }
                val muestra = Muestra(
                    id = c.getLong(0),
                    numero = c.getString(2),
                    lecturas = siguientes(LECTURAS),
                    acidez = siguientes(ACIDECES),
                    hora = casillas.next(),
                    temperaturas = siguientes(TEMPERATURAS),
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
                val casillas = muestra.lecturas + muestra.acidez + muestra.hora + muestra.temperaturas
                COLS_TEXTO.forEachIndexed { i, col -> put(col, casillas[i]) }
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
