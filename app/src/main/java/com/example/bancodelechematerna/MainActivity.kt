package com.example.bancodelechematerna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bancodelechematerna.ui.PantallaProceso
import com.example.bancodelechematerna.ui.PantallaProcesos
import com.example.bancodelechematerna.ui.ProcesosViewModel
import com.example.bancodelechematerna.ui.theme.BancoDeLecheMaternaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BancoDeLecheMaternaTheme {
                App()
            }
        }
    }
}

@Composable
private fun App(vm: ProcesosViewModel = viewModel()) {
    val procesos by vm.procesos.collectAsState()
    val cargando by vm.cargando.collectAsState()

    var abiertoId by rememberSaveable { mutableStateOf(-1L) }
    // Se guardan aqui y no dentro de la pantalla para que las preferencias no se pierdan
    // al volver a la lista y abrir otro proceso.
    var mostrarLecturas by rememberSaveable { mutableStateOf(true) }
    var mostrarCalculos by rememberSaveable { mutableStateOf(true) }
    val abierto = procesos.firstOrNull { it.id == abiertoId }

    if (abierto == null) {
        PantallaProcesos(
            procesos = procesos,
            cargando = cargando,
            alAbrir = { abiertoId = it },
            alCrear = { vm.crearProceso { nuevoId -> abiertoId = nuevoId } },
            alEliminar = vm::eliminarProceso,
        )
    } else {
        BackHandler { abiertoId = -1L }
        PantallaProceso(
            proceso = abierto,
            mostrarLecturas = mostrarLecturas,
            mostrarCalculos = mostrarCalculos,
            alAlternarLecturas = { mostrarLecturas = !mostrarLecturas },
            alAlternarCalculos = { mostrarCalculos = !mostrarCalculos },
            alVolver = { abiertoId = -1L },
            alCambiarFecha = { vm.cambiarFecha(abierto.id, it) },
            alAgregarMuestra = { vm.agregarMuestra(abierto.id) },
            alCambiarNumero = { muestraId, texto -> vm.cambiarNumero(abierto.id, muestraId, texto) },
            alCambiarLectura = { muestraId, indice, texto ->
                vm.cambiarLectura(abierto.id, muestraId, indice, texto)
            },
            alEliminarMuestra = { vm.eliminarMuestra(abierto.id, it) },
        )
    }
}
