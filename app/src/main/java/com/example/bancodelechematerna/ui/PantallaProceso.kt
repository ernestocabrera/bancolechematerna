package com.example.bancodelechematerna.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bancodelechematerna.datos.COLUMNAS
import com.example.bancodelechematerna.datos.Muestra
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.aTexto
import com.example.bancodelechematerna.datos.fechaLegible

private val ANCHO_NRO = 40.dp
private val ANCHO_LECTURA = 44.dp
private val ANCHO_TOTAL = 76.dp
private val ANCHO_PORC = 54.dp
private val ANCHO_KCAL = 62.dp
private val ANCHO_BORRAR = 36.dp
private val ESPACIO = 6.dp
private val ANCHO_GRUPO = ANCHO_LECTURA * 2 + ESPACIO
private val ALTO_CELDA = 38.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProceso(
    proceso: Proceso,
    mostrarLecturas: Boolean,
    mostrarCalculos: Boolean,
    alAlternarLecturas: () -> Unit,
    alAlternarCalculos: () -> Unit,
    alVolver: () -> Unit,
    alCambiarFecha: (Long) -> Unit,
    alAgregarMuestra: () -> Unit,
    alCambiarNumero: (Long, String) -> Unit,
    alCambiarLectura: (Long, Int, String) -> Unit,
    alEliminarMuestra: (Long) -> Unit,
) {
    val scroll = rememberScrollState()
    var eligiendoFecha by remember { mutableStateOf(false) }
    var exportando by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Muestra?>(null) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Proceso") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    InterruptorColumnas(
                        icono = IconoColumnasDatos,
                        descripcion = "Mostrar u ocultar las columnas de datos",
                        visible = mostrarLecturas,
                        alAlternar = alAlternarLecturas,
                    )
                    InterruptorColumnas(
                        icono = IconoColumnasCalculadas,
                        descripcion = "Mostrar u ocultar las columnas calculadas",
                        visible = mostrarCalculos,
                        alAlternar = alAlternarCalculos,
                    )
                    IconButton(onClick = { exportando = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar a Excel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = { PieResumen(proceso) },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            BarraFecha(proceso.fecha) { eligiendoFecha = true }
            HorizontalDivider()
            Encabezado(scroll, mostrarLecturas, mostrarCalculos)
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(proceso.muestras, key = { _, m -> m.id }) { indice, muestra ->
                    FilaMuestra(
                        muestra = muestra,
                        scroll = scroll,
                        fondo = if (indice % 2 == 0) Color.Transparent
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        mostrarLecturas = mostrarLecturas,
                        mostrarCalculos = mostrarCalculos,
                        alCambiarNumero = { alCambiarNumero(muestra.id, it) },
                        alCambiarLectura = { i, valor -> alCambiarLectura(muestra.id, i, valor) },
                        alEliminar = {
                            if (muestra.vacia) alEliminarMuestra(muestra.id) else porEliminar = muestra
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }

                item {
                    FilledTonalButton(
                        onClick = alAgregarMuestra,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar fila")
                    }
                }
            }
        }
    }

    if (eligiendoFecha) {
        SelectorFecha(
            fechaInicial = proceso.fecha,
            alElegir = alCambiarFecha,
            alCerrar = { eligiendoFecha = false },
        )
    }

    if (exportando) {
        DialogoExportar(procesos = listOf(proceso), alCerrar = { exportando = false })
    }

    porEliminar?.let { muestra ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar fila") },
            text = { Text("Se borrara la fila ${muestra.numero.ifBlank { "sin numero" }}.") },
            confirmButton = {
                TextButton(onClick = {
                    alEliminarMuestra(muestra.id)
                    porEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun BarraFecha(fecha: Long, alTocar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = alTocar) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Fecha: ${fechaLegible(fecha)}", fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Interruptor de un grupo de columnas. Encendido = columnas a la vista, con el icono
 * resaltado sobre un fondo; apagado = icono atenuado y sin fondo.
 */
@Composable
private fun InterruptorColumnas(
    icono: ImageVector,
    descripcion: String,
    visible: Boolean,
    alAlternar: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    IconToggleButton(
        checked = visible,
        onCheckedChange = { alAlternar() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = color.copy(alpha = 0.4f),
            checkedContentColor = color,
            checkedContainerColor = color.copy(alpha = 0.16f),
        ),
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Encabezado(scroll: ScrollState, mostrarLecturas: Boolean, mostrarCalculos: Boolean) {
    FilaTabla(
        scroll,
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(vertical = 8.dp)
    ) {
        CeldaTitulo("Nro", ANCHO_NRO)
        if (mostrarLecturas) {
            repeat(COLUMNAS) { CeldaTitulo("${it + 1}", ANCHO_GRUPO) }
        }
        if (mostrarCalculos) {
            CeldaTitulo("Total", ANCHO_TOTAL)
            CeldaTitulo("% Crema", ANCHO_PORC)
            CeldaTitulo("% Grasa", ANCHO_PORC)
            CeldaTitulo("Kcal", ANCHO_KCAL)
        }
        Spacer(Modifier.width(ANCHO_BORRAR))
    }
}

@Composable
private fun FilaMuestra(
    muestra: Muestra,
    scroll: ScrollState,
    fondo: Color,
    mostrarLecturas: Boolean,
    mostrarCalculos: Boolean,
    alCambiarNumero: (String) -> Unit,
    alCambiarLectura: (Int, String) -> Unit,
    alEliminar: () -> Unit,
) {
    val r = muestra.resultado
    FilaTabla(scroll, Modifier.background(fondo).padding(vertical = 4.dp)) {
        CeldaEntrada(
            valor = muestra.numero,
            ancho = ANCHO_NRO,
            largoMax = 4,
            soloDigitos = false,
            alCambiar = alCambiarNumero,
        )
        if (mostrarLecturas) {
            repeat(COLUMNAS) { columna ->
                CeldaEntrada(
                    valor = muestra.lecturas[columna * 2],
                    ancho = ANCHO_LECTURA,
                    largoMax = 3,
                    alCambiar = { alCambiarLectura(columna * 2, it) },
                )
                CeldaEntrada(
                    valor = muestra.lecturas[columna * 2 + 1],
                    ancho = ANCHO_LECTURA,
                    largoMax = 3,
                    alCambiar = { alCambiarLectura(columna * 2 + 1, it) },
                )
            }
        }
        if (mostrarCalculos) {
            CeldaResultado("${r.promTotal.aTexto()} / ${r.promCrema}", ANCHO_TOTAL)
            CeldaResultado(r.porcCrema.aTexto(), ANCHO_PORC, destacado = true)
            CeldaResultado(r.porcGrasa.aTexto(), ANCHO_PORC, destacado = true)
            CeldaResultado(r.kcal.aTexto(), ANCHO_KCAL)
        }
        IconButton(onClick = alEliminar, modifier = Modifier.size(ANCHO_BORRAR)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar fila",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Todas las filas comparten el mismo scroll horizontal, asi quedan siempre alineadas. */
@Composable
private fun FilaTabla(
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ESPACIO),
        verticalAlignment = Alignment.CenterVertically,
    ) { contenido() }
}

@Composable
private fun CeldaTitulo(texto: String, ancho: Dp, tenue: Boolean = false) {
    Text(
        text = texto,
        modifier = Modifier.width(ancho),
        textAlign = TextAlign.Center,
        maxLines = 1,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (tenue) FontWeight.Normal else FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer
            .copy(alpha = if (tenue) 0.7f else 1f),
    )
}

@Composable
private fun CeldaEntrada(
    valor: String,
    ancho: Dp,
    largoMax: Int,
    soloDigitos: Boolean = true,
    alCambiar: (String) -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    BasicTextField(
        value = valor,
        onValueChange = { nuevo ->
            val limpio = (if (soloDigitos) nuevo.filter(Char::isDigit) else nuevo.trim()).take(largoMax)
            if (limpio != valor) alCambiar(limpio)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            color = colores.onSurface,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (soloDigitos) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        cursorBrush = SolidColor(colores.primary),
        modifier = Modifier.width(ancho).height(ALTO_CELDA),
        decorationBox = { interior ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colores.surface, RoundedCornerShape(6.dp))
                    .border(1.dp, colores.outlineVariant, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { interior() }
        },
    )
}

@Composable
private fun CeldaResultado(texto: String, ancho: Dp, destacado: Boolean = false) {
    Box(
        modifier = Modifier.width(ancho).height(ALTO_CELDA),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            color = if (destacado) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PieResumen(proceso: Proceso) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Promedio % Grasa del proceso",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${proceso.muestras.size} fila(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
            Text(
                proceso.promedioGrasa.aTexto(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
