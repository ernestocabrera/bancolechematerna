package com.example.bancodelechematerna.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bancodelechematerna.datos.LECTURAS
import com.example.bancodelechematerna.datos.Muestra
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.Repositorio
import com.example.bancodelechematerna.datos.hoy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ProcesosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(app)

    /**
     * Un solo hilo para la base de datos: asi las escrituras se aplican en el mismo
     * orden en que se teclean, sin necesidad de candados.
     */
    private val hiloBd = Dispatchers.IO.limitedParallelism(1)

    private val _procesos = MutableStateFlow<List<Proceso>>(emptyList())
    val procesos: StateFlow<List<Proceso>> = _procesos.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init {
        viewModelScope.launch {
            _procesos.value = withContext(hiloBd) { repo.cargarTodo() }
            _cargando.value = false
        }
    }

    fun proceso(id: Long?): Proceso? = _procesos.value.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- procesos

    fun crearProceso(alCrear: (Long) -> Unit) {
        viewModelScope.launch {
            val fecha = hoy()
            val id = withContext(hiloBd) { repo.crearProceso(fecha) }
            _procesos.update { ordenar(it + Proceso(id = id, fecha = fecha)) }
            alCrear(id)
        }
    }

    fun cambiarFecha(procesoId: Long, fecha: Long) {
        _procesos.update { lista ->
            ordenar(lista.map { if (it.id == procesoId) it.copy(fecha = fecha) else it })
        }
        enBd { repo.actualizarFecha(procesoId, fecha) }
    }

    fun eliminarProceso(procesoId: Long) {
        _procesos.update { lista -> lista.filterNot { it.id == procesoId } }
        enBd { repo.eliminarProceso(procesoId) }
    }

    // ---------------------------------------------------------------- muestras

    fun agregarMuestra(procesoId: Long) {
        val proceso = proceso(procesoId) ?: return
        val numero = proceso.siguienteNumero
        val orden = proceso.muestras.size
        viewModelScope.launch {
            val id = withContext(hiloBd) { repo.crearMuestra(procesoId, orden, numero) }
            editarProceso(procesoId) { it.copy(muestras = it.muestras + Muestra(id, numero)) }
        }
    }

    fun cambiarNumero(procesoId: Long, muestraId: Long, numero: String) {
        editarMuestra(procesoId, muestraId) { it.copy(numero = numero) }
    }

    fun cambiarLectura(procesoId: Long, muestraId: Long, indice: Int, valor: String) {
        require(indice in 0 until LECTURAS)
        editarMuestra(procesoId, muestraId) { muestra ->
            muestra.copy(lecturas = muestra.lecturas.toMutableList().also { it[indice] = valor })
        }
    }

    fun eliminarMuestra(procesoId: Long, muestraId: Long) {
        editarProceso(procesoId) { proceso ->
            proceso.copy(muestras = proceso.muestras.filterNot { it.id == muestraId })
        }
        enBd {
            repo.eliminarMuestra(muestraId)
            proceso(procesoId)?.let { repo.reordenar(it.muestras) }
        }
    }

    // ------------------------------------------------------------------ ayudas

    private fun editarMuestra(procesoId: Long, muestraId: Long, bloque: (Muestra) -> Muestra) {
        var editada: Muestra? = null
        editarProceso(procesoId) { proceso ->
            proceso.copy(
                muestras = proceso.muestras.map { muestra ->
                    if (muestra.id != muestraId) muestra else bloque(muestra).also { editada = it }
                }
            )
        }
        editada?.let { muestra -> enBd { repo.guardarMuestra(muestra) } }
    }

    private fun editarProceso(procesoId: Long, bloque: (Proceso) -> Proceso) {
        _procesos.update { lista ->
            lista.map { if (it.id == procesoId) bloque(it) else it }
        }
    }

    private fun ordenar(procesos: List<Proceso>): List<Proceso> =
        procesos.sortedWith(compareByDescending<Proceso> { it.fecha }.thenByDescending { it.id })

    private fun enBd(bloque: () -> Unit) {
        viewModelScope.launch(hiloBd) { bloque() }
    }
}
