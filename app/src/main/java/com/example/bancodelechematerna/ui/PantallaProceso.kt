package com.example.bancodelechematerna.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bancodelechematerna.datos.ACIDECES
import com.example.bancodelechematerna.datos.COLUMNAS
import com.example.bancodelechematerna.datos.LECTURAS
import com.example.bancodelechematerna.datos.Muestra
import com.example.bancodelechematerna.datos.Proceso
import com.example.bancodelechematerna.datos.RangosKcal
import com.example.bancodelechematerna.datos.TEMPERATURAS
import com.example.bancodelechematerna.datos.aTexto
import com.example.bancodelechematerna.datos.acidezFueraDeRango
import com.example.bancodelechematerna.datos.fechaLegible
import com.example.bancodelechematerna.datos.horaActual
import com.example.bancodelechematerna.datos.horaLegible
import com.example.bancodelechematerna.datos.totalTexto

private val ESPACIO = 6.dp
/** Nro de donante, a veces con consecutivo: "234" o "234.1". */
private val ANCHO_NRO = 60.dp
private val ANCHO_ACIDEZ = 44.dp
private val ANCHO_LECTURA = 44.dp
private val ANCHO_GRUPO = ANCHO_LECTURA * 2 + ESPACIO
private val ANCHO_TOTAL = 76.dp
private val ANCHO_PORC = 54.dp
private val ANCHO_KCAL = 62.dp
private val ANCHO_HORA = 76.dp
private val ANCHO_TEMP = 66.dp
private val ANCHO_BORRAR = 36.dp
private val ALTO_CELDA = 38.dp
private val FORMA_CASILLA = RoundedCornerShape(6.dp)

/** Ancho que ocupan varias celdas seguidas, contando el espacio entre ellas. */
private fun ancho(celdas: List<Dp>): Dp =
    celdas.fold(0.dp) { suma, celda -> suma + celda } + ESPACIO * (celdas.size - 1)

private val ANCHO_ACIDECES = ancho(List(ACIDECES) { ANCHO_ACIDEZ })
private val ANCHO_LECTURAS = ancho(List(COLUMNAS) { ANCHO_GRUPO })
private val ANCHO_CALCULOS = ancho(listOf(ANCHO_TOTAL, ANCHO_PORC, ANCHO_PORC, ANCHO_KCAL))
private val ANCHO_PASTEURIZACION = ancho(listOf(ANCHO_HORA) + List(TEMPERATURAS) { ANCHO_TEMP })

/** En el orden de [Muestra.temperaturas]. */
private val TITULOS_TEMPERATURA = listOf("Baño M", "Punto frio", "Agua")

/** Grupos de columnas que se pueden ocultar. La columna Nro siempre se ve. */
data class Secciones(
    val acidez: Boolean = true,
    val lecturas: Boolean = true,
    val calculos: Boolean = true,
    val pasteurizacion: Boolean = true,
) {
    companion object {
        val Guardado: Saver<Secciones, Any> = listSaver(
            save = { listOf(it.acidez, it.lecturas, it.calculos, it.pasteurizacion) },
            restore = { Secciones(it[0], it[1], it[2], it[3]) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProceso(
    proceso: Proceso,
    secciones: Secciones,
    alCambiarSecciones: (Secciones) -> Unit,
    alVolver: () -> Unit,
    alCambiarFecha: (Long) -> Unit,
    alAgregarMuestra: () -> Unit,
    alEditarMuestra: (Long, (Muestra) -> Muestra) -> Unit,
    alEliminarMuestra: (Long) -> Unit,
) {
    val scroll = rememberScrollState()
    var eligiendoFecha by remember { mutableStateOf(false) }
    var eligiendoHoraDe by remember { mutableStateOf<Long?>(null) }
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
                    MenuSecciones(secciones, alCambiarSecciones)
                    IconButton(onClick = { exportando = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar a Word")
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
            Encabezado(scroll, secciones)
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
                        secciones = secciones,
                        alEditar = { cambio -> alEditarMuestra(muestra.id, cambio) },
                        alElegirHora = { eligiendoHoraDe = muestra.id },
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

    eligiendoHoraDe?.let { id ->
        val hora = proceso.muestras.firstOrNull { it.id == id }?.hora.orEmpty()
        SelectorHora(
            horaInicial = hora,
            alElegir = { nueva -> alEditarMuestra(id) { it.copy(hora = nueva) } },
            alBorrar = if (hora.isBlank()) null else ({ alEditarMuestra(id) { it.copy(hora = "") } }),
            alCerrar = { eligiendoHoraDe = null },
        )
    }

    if (exportando) {
        DialogoExportar(proceso = proceso, alCerrar = { exportando = false })
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
 * Un solo boton con la lista de secciones para marcar o desmarcar. El menu queda
 * abierto al tocar una opcion, para poder cambiar varias de una vez.
 */
@Composable
private fun MenuSecciones(secciones: Secciones, alCambiar: (Secciones) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                IconoColumnasDatos,
                contentDescription = "Mostrar u ocultar columnas",
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            OpcionSeccion("Acidez", secciones.acidez) {
                alCambiar(secciones.copy(acidez = it))
            }
            OpcionSeccion("Crematocrito: lecturas", secciones.lecturas) {
                alCambiar(secciones.copy(lecturas = it))
            }
            OpcionSeccion("Crematocrito: calculos", secciones.calculos) {
                alCambiar(secciones.copy(calculos = it))
            }
            OpcionSeccion("Pasteurizacion", secciones.pasteurizacion) {
                alCambiar(secciones.copy(pasteurizacion = it))
            }
        }
    }
}

@Composable
private fun OpcionSeccion(texto: String, marcada: Boolean, alCambiar: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(texto) },
        leadingIcon = { Checkbox(checked = marcada, onCheckedChange = null) },
        onClick = { alCambiar(!marcada) },
    )
}

/** Dos filas de titulos: arriba el nombre de cada seccion, abajo el de cada columna. */
@Composable
private fun Encabezado(scroll: ScrollState, secciones: Secciones) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(vertical = 6.dp)
    ) {
        FilaTabla(scroll, fija = { Spacer(Modifier.width(ANCHO_NRO)) }) {
            if (secciones.acidez) CeldaSeccion("Acidez", ANCHO_ACIDECES)
            val crema = listOfNotNull(
                ANCHO_LECTURAS.takeIf { secciones.lecturas },
                ANCHO_CALCULOS.takeIf { secciones.calculos },
            )
            if (crema.isNotEmpty()) CeldaSeccion("Crematocrito", ancho(crema))
            if (secciones.pasteurizacion) CeldaSeccion("Pasteurizacion", ANCHO_PASTEURIZACION)
            Spacer(Modifier.width(ANCHO_BORRAR))
        }
        Spacer(Modifier.height(4.dp))
        FilaTabla(scroll, fija = { CeldaTitulo("Nro", ANCHO_NRO) }) {
            if (secciones.acidez) {
                repeat(ACIDECES) { CeldaTitulo("${it + 1}", ANCHO_ACIDEZ) }
            }
            if (secciones.lecturas) {
                repeat(COLUMNAS) { CeldaTitulo("${it + 1}", ANCHO_GRUPO) }
            }
            if (secciones.calculos) {
                CeldaTitulo("Total", ANCHO_TOTAL)
                CeldaTitulo("% Crema", ANCHO_PORC)
                CeldaTitulo("% Grasa", ANCHO_PORC)
                CeldaTitulo("Kcal", ANCHO_KCAL)
            }
            if (secciones.pasteurizacion) {
                CeldaTitulo("Hora", ANCHO_HORA)
                TITULOS_TEMPERATURA.forEach { CeldaTitulo(it, ANCHO_TEMP) }
            }
            Spacer(Modifier.width(ANCHO_BORRAR))
        }
    }
}

@Composable
private fun FilaMuestra(
    muestra: Muestra,
    scroll: ScrollState,
    fondo: Color,
    secciones: Secciones,
    alEditar: ((Muestra) -> Muestra) -> Unit,
    alElegirHora: () -> Unit,
    alEliminar: () -> Unit,
) {
    val r = muestra.resultado
    FilaTabla(
        scroll,
        Modifier.background(fondo).padding(vertical = 4.dp),
        fija = {
            CeldaEntrada(
                valor = muestra.numero,
                ancho = ANCHO_NRO,
                largoMax = 8,
                tipo = Tipo.TEXTO,
                alCambiar = { v -> alEditar { it.copy(numero = v) } },
            )
        },
    ) {
        if (secciones.acidez) {
            repeat(ACIDECES) { i ->
                CeldaEntrada(
                    valor = muestra.acidez[i],
                    ancho = ANCHO_ACIDEZ,
                    largoMax = 3,
                    alerta = acidezFueraDeRango(muestra.acidez[i]),
                    alCambiar = { v -> alEditar { it.conAcidez(i, v) } },
                )
            }
        }
        if (secciones.lecturas) {
            repeat(LECTURAS) { i ->
                CeldaEntrada(
                    valor = muestra.lecturas[i],
                    ancho = ANCHO_LECTURA,
                    largoMax = 3,
                    alCambiar = { v -> alEditar { it.conLectura(i, v) } },
                )
            }
        }
        if (secciones.calculos) {
            CeldaResultado(r.totalTexto(), ANCHO_TOTAL)
            CeldaResultado(r.porcCrema.aTexto(), ANCHO_PORC)
            CeldaResultado(r.porcGrasa.aTexto(), ANCHO_PORC)
            CeldaResultado(r.kcal.aTexto(), ANCHO_KCAL)
        }
        if (secciones.pasteurizacion) {
            CeldaHora(muestra.hora, ANCHO_HORA, alElegirHora)
            repeat(TEMPERATURAS) { i ->
                CeldaEntrada(
                    valor = muestra.temperaturas[i],
                    ancho = ANCHO_TEMP,
                    largoMax = 5,
                    tipo = Tipo.DECIMAL,
                    sufijo = "°C",
                    alCambiar = { v -> alEditar { it.conTemperatura(i, v, horaActual()) } },
                )
            }
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

/**
 * [fija] es la columna Nro: queda quieta a la izquierda. El resto comparte el mismo
 * scroll horizontal en todas las filas, asi quedan siempre alineadas.
 */
@Composable
private fun FilaTabla(
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    fija: @Composable () -> Unit,
    contenido: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        fija()
        Spacer(Modifier.width(ESPACIO))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scroll)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ESPACIO),
            verticalAlignment = Alignment.CenterVertically,
        ) { contenido() }
    }
}

@Composable
private fun CeldaSeccion(texto: String, ancho: Dp) {
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    Column(Modifier.width(ancho), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = texto,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(color = color.copy(alpha = 0.3f))
    }
}

@Composable
private fun CeldaTitulo(texto: String, ancho: Dp) {
    Text(
        text = texto,
        modifier = Modifier.width(ancho),
        textAlign = TextAlign.Center,
        maxLines = 1,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** Que se deja teclear en cada casilla y con que teclado. */
private enum class Tipo(val teclado: KeyboardType, val limpiar: (String) -> String) {
    TEXTO(KeyboardType.Text, { it.trim() }),
    ENTERO(KeyboardType.Number, { it.filter(Char::isDigit) }),
    /** Digitos y un solo punto decimal; la coma se toma como punto. */
    DECIMAL(KeyboardType.Decimal, { texto ->
        var hayPunto = false
        texto.replace(',', '.').filter { c ->
            c.isDigit() || (c == '.' && !hayPunto).also { if (it) hayPunto = true }
        }
    }),
}

/** Fondo y borde comunes a todas las casillas; en rojo si [alerta]. */
@Composable
private fun Modifier.casilla(alerta: Boolean = false): Modifier {
    val colores = MaterialTheme.colorScheme
    return this
        .clip(FORMA_CASILLA)
        .background(colores.surface)
        .border(
            width = if (alerta) 1.5.dp else 1.dp,
            color = if (alerta) colores.error else colores.outlineVariant,
            shape = FORMA_CASILLA,
        )
}

@Composable
private fun CeldaEntrada(
    valor: String,
    ancho: Dp,
    largoMax: Int,
    tipo: Tipo = Tipo.ENTERO,
    alerta: Boolean = false,
    sufijo: String? = null,
    alCambiar: (String) -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    BasicTextField(
        value = valor,
        onValueChange = { nuevo ->
            val limpio = tipo.limpiar(nuevo).take(largoMax)
            if (limpio != valor) alCambiar(limpio)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            color = if (alerta) colores.error else colores.onSurface,
            fontWeight = if (alerta) FontWeight.Bold else FontWeight.Normal,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = tipo.teclado,
            imeAction = ImeAction.Next,
        ),
        cursorBrush = SolidColor(colores.primary),
        modifier = Modifier.width(ancho).height(ALTO_CELDA),
        decorationBox = { interior ->
            Row(
                modifier = Modifier.fillMaxSize().casilla(alerta).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { interior() }
                if (sufijo != null) {
                    Text(
                        sufijo,
                        style = MaterialTheme.typography.labelSmall,
                        color = colores.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/** La hora no se teclea: se toca la casilla y se elige en un reloj. */
@Composable
private fun CeldaHora(hora: String, ancho: Dp, alTocar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val texto = horaLegible(hora)
    Box(
        modifier = Modifier
            .width(ancho)
            .height(ALTO_CELDA)
            .casilla()
            .clickable(onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto.ifEmpty { "--:--" },
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = if (texto.isEmpty()) colores.onSurfaceVariant.copy(alpha = 0.6f)
            else colores.onSurface,
        )
    }
}

@Composable
private fun CeldaResultado(texto: String, ancho: Dp) {
    Box(
        modifier = Modifier.width(ancho).height(ALTO_CELDA),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PieResumen(proceso: Proceso) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        // El fondo llega hasta el borde, pero el contenido se aparta de la barra de
        // navegacion. Con el teclado abierto, imePadding ya consumio ese espacio.
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
        ) {
            val color = MaterialTheme.colorScheme.onPrimaryContainer
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Promedio % Grasa del proceso",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                    )
                    Text(
                        "${proceso.muestras.size} fila(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.7f),
                    )
                }
                Text(
                    proceso.promedioGrasa.aTexto(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
            Spacer(Modifier.height(6.dp))
            RangosKcalResumen(proceso.cantidadesPorRangoKcal)
        }
    }
}

/** Una fila de cinco casillas: el rango de Kcal arriba y cuantas muestras caen debajo. */
@Composable
private fun RangosKcalResumen(cantidades: List<Int>) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RangosKcal.NOMBRES.forEachIndexed { i, nombre ->
            val cantidad = cantidades[i]
            Column(
                Modifier
                    .weight(1f)
                    .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    nombre,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.75f),
                )
                Text(
                    "$cantidad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (cantidad == 0) color.copy(alpha = 0.4f) else color,
                )
            }
        }
    }
}
