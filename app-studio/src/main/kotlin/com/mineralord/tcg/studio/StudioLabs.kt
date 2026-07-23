package com.mineralord.tcg.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mineralord.tcg.core.designsystem.tokens.EmptyStateStyle
import com.mineralord.tcg.core.designsystem.tokens.StudioTheme
import com.mineralord.tcg.studio.shell.Lab

/**
 * **Registro de Labs del Studio (APK 2).**
 *
 * Aquí —fuera del Shell— se declara qué Labs existen y en qué orden aparecen en el Rail. El Shell
 * solo los hospeda; la composición del catálogo vive en la cáscara de la app. La **Galería de
 * Animaciones** es simplemente el primer Lab registrado: su contenido real (11.2) llegará en un
 * Sprint posterior, así que por ahora expone un marcador de posición hospedable.
 *
 * Añadir un Lab futuro = añadir un `Lab(...)` más a esta lista; el mecanismo de conmutación del Shell
 * ya lo cubre sin cambios.
 */
fun studioLabs(): List<Lab> = listOf(
    Lab(id = "animation-gallery", title = "Galería de Animaciones") { LabPlaceholder("Galería de Animaciones") },
)

/**
 * Marcador de posición genérico para un Lab aún sin contenido, materializado con el Design System
 * congelado (sin tokens/componentes nuevos). Se sustituirá por el contenido real del Lab.
 */
@Composable
private fun LabPlaceholder(title: String) {
    val scheme = StudioTheme.colors
    Column(
        modifier = Modifier.padding(EmptyStateStyle.padding),
        verticalArrangement = Arrangement.spacedBy(EmptyStateStyle.gap),
    ) {
        BasicText(
            text = title,
            style = EmptyStateStyle.titleStyle.copy(color = EmptyStateStyle.titleColor(scheme)),
        )
        BasicText(
            text = "Este laboratorio aún no tiene contenido.",
            style = EmptyStateStyle.bodyStyle.copy(color = EmptyStateStyle.bodyColor(scheme)),
        )
    }
}
