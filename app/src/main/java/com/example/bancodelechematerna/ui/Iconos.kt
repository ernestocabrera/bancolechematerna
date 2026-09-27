package com.example.bancodelechematerna.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icono propio del menu de columnas.
 *
 * Se dibuja a mano en vez de usar material-icons-extended: esa biblioteca esta
 * descontinuada, pesa muchisimo y de ella solo haria falta este simbolo.
 * El color lo pone el Icon() que lo muestra, por eso aqui se dibuja en negro.
 */

/** Tabla con tres columnas: representa las columnas de la tabla. */
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


private fun PathBuilder.rectangulo(x: Float, y: Float, ancho: Float, alto: Float) {
    moveTo(x, y)
    horizontalLineTo(x + ancho)
    verticalLineTo(y + alto)
    horizontalLineTo(x)
    close()
}
