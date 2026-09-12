package com.example.bancodelechematerna.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.aTexto
import com.example.bancodelechematerna.datos.fechaLegible
import com.example.bancodelechematerna.datos.hoy
import com.example.bancodelechematerna.datos.sumarDias

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProcesos(
    procesos: List<Proceso>,
    cargando: Boolean,
    alAbrir: (Long) -> Unit,
    alCrear: () -> Unit,
    alEliminar: (Long) -> Unit,
) {
    var porEliminar by remember { mutableStateOf<Proceso?>(null) }
    var rangoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Procesos") },
                actions = {
                    IconButton(onClick = { rangoAbierto = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Exportar por fechas")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = alCrear,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo proceso") },
            )
        },
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when {
                cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                procesos.isEmpty() -> Text(
                    "Todavia no hay procesos guardados.\nToca «Nuevo proceso» para empezar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(procesos, key = { it.id }) { proceso ->
                        TarjetaProceso(
                            proceso = proceso,
                            alAbrir = { alAbrir(proceso.id) },
                            alPedirEliminar = { porEliminar = proceso },
                        )
                    }
                }
            }
        }
    }

    porEliminar?.let { proceso ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar proceso") },
            text = {
                Text(
                    "Se borrara el proceso del ${fechaLegible(proceso.fecha)} " +
                        "con sus ${proceso.muestras.size} fila(s). Esto no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    alEliminar(proceso.id)
                    porEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text("Cancelar") }
            },
        )
    }

    if (rangoAbierto) {
        DialogoRango(procesos = procesos, alCerrar = { rangoAbierto = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaProceso(
    proceso: Proceso,
    alAbrir: () -> Unit,
    alPedirEliminar: () -> Unit,
) {
    Card(
        onClick = alAbrir,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    fechaLegible(proceso.fecha),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${proceso.muestras.size} fila(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "% Grasa prom.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    proceso.promedioGrasa.aTexto(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = alPedirEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar proceso",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Elegir desde/hasta y exportar todos los procesos de ese rango en un solo archivo. */
@Composable
private fun DialogoRango(
    procesos: List<Proceso>,
    alCerrar: () -> Unit,
) {
    var desde by remember { mutableStateOf(sumarDias(hoy(), -30)) }
    var hasta by remember { mutableStateOf(hoy()) }
    var eligiendo by remember { mutableStateOf<String?>(null) }
    var exportando by remember { mutableStateOf(false) }

    val seleccion = procesos.filter { it.fecha in desde..hasta }.sortedBy { it.fecha }

    if (exportando) {
        DialogoExportar(procesos = seleccion, alCerrar = { exportando = false; alCerrar() })
        return
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Exportar por fechas") },
        text = {
            Column {
                OutlinedButton(
                    onClick = { eligiendo = "desde" },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Desde:  ${fechaLegible(desde)}") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { eligiendo = "hasta" },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Hasta:  ${fechaLegible(hasta)}") }
                Spacer(Modifier.height(12.dp))
                Text(
                    "${seleccion.size} proceso(s) en el rango",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = seleccion.isNotEmpty(),
                onClick = { exportando = true },
            ) { Text("Continuar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )

    when (eligiendo) {
        "desde" -> SelectorFecha(
            fechaInicial = desde,
            alElegir = { elegida ->
                desde = elegida
                if (hasta < elegida) hasta = elegida
            },
            alCerrar = { eligiendo = null },
        )

        "hasta" -> SelectorFecha(
            fechaInicial = hasta,
            alElegir = { elegida ->
                hasta = elegida
                if (desde > elegida) desde = elegida
            },
            alCerrar = { eligiendo = null },
        )
    }
}
