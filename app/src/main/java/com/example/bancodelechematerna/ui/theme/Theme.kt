package com.example.bancodelechematerna.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Sin color dinamico a proposito: la app debe verse igual en cualquier telefono.
private val EsquemaClaro = lightColorScheme(
    primary = VerdeAgua,
    onPrimary = Color.White,
    primaryContainer = VerdeAguaClaro,
    onPrimaryContainer = VerdeAguaOscuro,
    secondary = AzulGris,
    onSecondary = Color.White,
    secondaryContainer = AzulGrisClaro,
    onSecondaryContainer = AzulGrisOscuro,
    tertiary = Arena,
    onTertiary = Color.White,
    tertiaryContainer = ArenaClara,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = SuperficieClara,
    onSurface = TextoClaro,
    surfaceVariant = VarianteClara,
    onSurfaceVariant = AzulGris,
    outline = ContornoClaro,
    outlineVariant = ContornoClaro,
)

private val EsquemaOscuro = darkColorScheme(
    primary = TurquesaClaro,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = VerdeAguaClaro,
    secondary = Color(0xFFB0CCCD),
    onSecondary = Color(0xFF1B3436),
    secondaryContainer = Color(0xFF324B4C),
    onSecondaryContainer = AzulGrisClaro,
    tertiary = Color(0xFFB4C8E9),
    onTertiary = Color(0xFF1D3149),
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = SuperficieOscura,
    onSurface = TextoOscuro,
    surfaceVariant = VarianteOscura,
    onSurfaceVariant = Color(0xFFBEC8C9),
    outline = ContornoOscuro,
    outlineVariant = VarianteOscura,
)

@Composable
fun BancoDeLecheMaternaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content,
    )
}
