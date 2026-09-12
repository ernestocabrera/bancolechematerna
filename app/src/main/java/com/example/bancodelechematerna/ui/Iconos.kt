package com.example.bancodelechematerna.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos propios para los dos interruptores de columnas.
 *
 * Se dibujan a mano en vez de usar material-icons-extended: esa biblioteca esta
 * descontinuada, pesa muchisimo y de ella solo harian falta estos dos simbolos.
 * El color lo pone el Icon() que los muestra, por eso aqui se dibujan en negro.
 */

/** Tabla con tres columnas: representa las columnas de datos (1, 2, 3). */
val IconoColumnasDatos: ImageVector = ImageVector.Builder(
    name = "ColumnasDatos",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        rectangulo(3f, 4f, 18f, 3f)          // encabezado
        rectangulo(3f, 8.5f, 4.6f, 11f)      // columna 1
        rectangulo(9.7f, 8.5f, 4.6f, 11f)    // columna 2
        rectangulo(16.4f, 8.5f, 4.6f, 11f)   // columna 3
    }
}.build()

/** Calculadora: representa las columnas calculadas (Total, % Crema, % Grasa, Kcal). */
val IconoColumnasCalculadas: ImageVector = ImageVector.Builder(
    name = "ColumnasCalculadas",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
        rectangulo(5f, 3f, 14f, 18f)         // cuerpo
    }
    path(fill = SolidColor(Color.Black)) {
        rectangulo(7.5f, 5.5f, 9f, 3.5f)     // visor
        for (y in listOf(12f, 15.6f)) {      // teclas
            for (x in listOf(7.6f, 10.9f, 14.2f)) rectangulo(x, y, 2.2f, 2.2f)
        }
    }
}.build()

private fun PathBuilder.rectangulo(x: Float, y: Float, ancho: Float, alto: Float) {
    moveTo(x, y)
    horizontalLineTo(x + ancho)
    verticalLineTo(y + alto)
    horizontalLineTo(x)
    close()
}
