package com.example.bancodelechematerna.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.exportar.Exportador
import com.example.bancodelechematerna.exportar.TIPO_XLSX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    fechaInicial: Long,
    alElegir: (Long) -> Unit,
    alCerrar: () -> Unit,
) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = fechaInicial)
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(onClick = {
                alElegir(estado.selectedDateMillis ?: fechaInicial)
                alCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    ) {
        DatePicker(state = estado)
    }
}

/** Genera el .xlsx y deja elegir entre compartirlo o guardarlo en el telefono. */
@Composable
fun DialogoExportar(
    procesos: List<Proceso>,
    alCerrar: () -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }

    val guardarComo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TIPO_XLSX)
    ) { destino ->
        if (destino == null) {
            alCerrar()
        } else {
            alcance.launch {
                val fallo = withContext(Dispatchers.IO) {
                    runCatching { Exportador.generarEn(contexto, destino, procesos) }.exceptionOrNull()
                }
                avisar(
                    contexto,
                    if (fallo == null) "Archivo guardado" else "No se pudo guardar: ${fallo.message}"
                )
                alCerrar()
            }
        }
    }

    val vacio = procesos.isEmpty() || procesos.all { it.muestras.isEmpty() }

    AlertDialog(
        onDismissRequest = { if (!trabajando) alCerrar() },
        title = { Text("Exportar a Excel") },
        text = {
            Text(
                if (vacio) "No hay datos que exportar en la seleccion."
                else "Se exportara ${procesos.size} proceso(s) al archivo " +
                    Exportador.nombreArchivo(procesos)
            )
        },
        confirmButton = {
            TextButton(
                enabled = !vacio && !trabajando,
                onClick = {
                    trabajando = true
                    alcance.launch {
                        val resultado = withContext(Dispatchers.IO) {
                            runCatching { Exportador.generarEnCache(contexto, procesos) }
                        }
                        trabajando = false
                        resultado.fold(
                            onSuccess = { Exportador.compartir(contexto, it) },
                            onFailure = { avisar(contexto, "No se pudo exportar: ${it.message}") },
                        )
                        alCerrar()
                    }
                },
            ) { Text("Compartir") }
        },
        dismissButton = {
            TextButton(
                enabled = !vacio && !trabajando,
                onClick = { guardarComo.launch(Exportador.nombreArchivo(procesos)) },
            ) { Text("Guardar") }
        },
    )
}

internal fun avisar(contexto: Context, mensaje: String) {
    Toast.makeText(contexto, mensaje, Toast.LENGTH_LONG).show()
}
