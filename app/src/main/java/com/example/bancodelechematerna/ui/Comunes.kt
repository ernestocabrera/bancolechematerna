package com.example.bancodelechematerna.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.horaGuardada
import com.example.bancodelechematerna.datos.horaActual
import com.example.bancodelechematerna.datos.horaYMinutos
import com.example.bancodelechematerna.exportar.Exportador
import com.example.bancodelechematerna.exportar.Modelo
import com.example.bancodelechematerna.exportar.TIPO_DOCX
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

/**
 * Reloj para elegir la hora de pasteurizacion, en 12 h con am/pm.
 * [horaInicial] viene como "HH:mm"; si esta vacia arranca en la hora actual.
 * [alBorrar] es null cuando no hay nada que borrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorHora(
    horaInicial: String,
    alElegir: (String) -> Unit,
    alBorrar: (() -> Unit)?,
    alCerrar: () -> Unit,
) {
    val (h, m) = horaYMinutos(horaInicial.ifBlank { horaActual() }) ?: (0 to 0)
    val estado = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = false)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Hora de pasteurizacion") },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = {
                alElegir(horaGuardada(estado.hour, estado.minute))
                alCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = {
            Row {
                if (alBorrar != null) {
                    TextButton(onClick = {
                        alBorrar()
                        alCerrar()
                    }) { Text("Borrar") }
                }
                TextButton(onClick = alCerrar) { Text("Cancelar") }
            }
        },
    )
}

/**
 * Marcar uno, varios o los tres modelos de Word y compartirlos o guardarlos juntos.
 * Un modelo sin filas con datos en su seccion no se puede marcar.
 */
@Composable
fun DialogoExportar(
    proceso: Proceso,
    alCerrar: () -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }
    val filasPorModelo = remember(proceso) { Modelo.entries.associateWith { it.filas(proceso).size } }
    // Arrancan marcados todos los que tienen datos.
    var elegidos by remember { mutableStateOf(Modelo.entries.filter { filasPorModelo.getValue(it) > 0 }.toSet()) }
    // En el orden de la lista, no en el que se fueron marcando.
    val seleccion = Modelo.entries.filter { it in elegidos }

    val guardarEnCarpeta = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { carpeta ->
        if (carpeta == null) {
            alCerrar()
        } else {
            alcance.launch {
                val fallo = withContext(Dispatchers.IO) {
                    runCatching {
                        Exportador.generarEnCarpeta(contexto, carpeta, seleccion, proceso)
                    }.exceptionOrNull()
                }
                avisar(
                    contexto,
                    if (fallo == null) "${seleccion.size} archivo(s) guardado(s)"
                    else "No se pudo guardar: ${fallo.message}"
                )
                alCerrar()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!trabajando) alCerrar() },
        title = { Text("Exportar a Word") },
        text = {
            Column {
                Modelo.entries.forEach { opcion ->
                    val filas = filasPorModelo.getValue(opcion)
                    val marcado = opcion in elegidos
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = marcado,
                                enabled = filas > 0,
                                role = Role.Checkbox,
                                onValueChange = { elegidos = if (it) elegidos + opcion else elegidos - opcion },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = marcado, onCheckedChange = null, enabled = filas > 0)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                opcion.nombre,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = if (filas > 0) 1f else 0.4f),
                            )
                            Text(
                                if (filas == 0) "Sin datos en este proceso" else "$filas fila(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = seleccion.isNotEmpty() && !trabajando,
                onClick = {
                    trabajando = true
                    alcance.launch {
                        val resultado = withContext(Dispatchers.IO) {
                            runCatching { Exportador.generarEnCache(contexto, seleccion, proceso) }
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
                enabled = seleccion.isNotEmpty() && !trabajando,
                onClick = { guardarEnCarpeta.launch(null) },
            ) { Text("Guardar") }
        },
    )
}

internal fun avisar(contexto: Context, mensaje: String) {
    Toast.makeText(contexto, mensaje, Toast.LENGTH_LONG).show()
}
