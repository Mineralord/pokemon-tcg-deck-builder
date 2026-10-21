package com.mineralord.tcg.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.RainbowBar

/** Una entrada del historial de cambios (una versión). */
data class ChangeEntry(val version: String, val date: String, val changes: List<String>)

/**
 * Historial de cambios de la app (más reciente primero). Se muestra en el popup de "Mensajes".
 * Al publicar una versión nueva, añade su entrada ARRIBA.
 */
val CHANGELOG: List<ChangeEntry> = listOf(
    ChangeEntry(
        "0.2.13", "21 oct 2026",
        listOf(
            "Más cartas de Brecha Paradójica con efecto real (Turbo Energize, Drawareness, Mountain Scrounging).",
            "Arrastra energías a tus Pokémon y menús de combate con el diseño oficial de la app.",
            "Los sobres se agrandan y son interactivos (se inclinan) con brillo plateado realista.",
        ),
    ),
    ChangeEntry(
        "0.2.12", "20 oct 2026",
        listOf(
            "Comunes e infrecuentes: brillo reverse-holo plateado realista (sin arcoíris).",
            "Quitadas dos líneas verdes que se colaban en la Colección.",
            "El botón de novedades (✉) apaga su punto rojo al abrirlo.",
        ),
    ),
    ChangeEntry(
        "0.2.11", "20 oct 2026",
        listOf(
            "Corregido un cierre inesperado al abrir la Colección.",
        ),
    ),
    ChangeEntry(
        "0.2.10", "20 oct 2026",
        listOf(
            "Novedades: este historial de cambios (botón ✉).",
            "Brillo holo fiel a las cartas reales: cada rareza brilla solo en sus zonas correctas (reverse en el marco, holo en la ilustración).",
            "Comunes e infrecuentes de todas las expansiones ya muestran su brillo reverse-holo.",
            "Bandeja de regalos rediseñada (se abre desde abajo, con fondo difuminado).",
            "Ajustes visuales en la pantalla de inicio (sobres y contador).",
        ),
    ),
    ChangeEntry(
        "0.2.9", "20 oct 2026",
        listOf(
            "Nueva bandeja de Regalos: recibe y reclama recompensas (cristales, monedas, fichas).",
            "Compra de sobres con cristales SIN límite diario.",
            "Barra arcoíris animada en Colección, Barajas y Amigos.",
            "Diagnóstico de nube al iniciar sesión para verificar tu sincronización.",
        ),
    ),
    ChangeEntry(
        "0.2.6", "20 oct 2026",
        listOf(
            "Arreglado el inicio de sesión con Google (proyecto Firebase correcto).",
            "Mensajes de error de sesión más claros.",
        ),
    ),
    ChangeEntry(
        "0.2.4", "20 oct 2026",
        listOf(
            "Pantalla de bienvenida al inicio (Google o invitado).",
            "Sincronización de colección y cosméticos con tu cuenta en la nube.",
        ),
    ),
    ChangeEntry(
        "0.2.2", "20 oct 2026",
        listOf(
            "Aviso dentro de la app cuando hay una versión nueva.",
            "Rediseño claro de la pantalla de carga.",
        ),
    ),
)

/**
 * Popup de "Mensajes": historial de cambios de cada actualización. Hoja inferior (bottom sheet) con
 * el mismo lenguaje visual que la bandeja de regalos (design system claro [BarajasPalette], entra de
 * abajo hacia arriba; el blur del fondo lo aplica [AppShell]).
 */
@Composable
fun ChangelogSheet(visible: Boolean, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BarajasPalette.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(300)) { it },
            exit = slideOutVertically(tween(240)) { it },
        ) {
            ChangelogBody(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun ChangelogBody(onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Brush.verticalGradient(listOf(BarajasPalette.BgTop, BarajasPalette.BgBottom)))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(BarajasPalette.HairlineBorder),
        )
        Spacer(Modifier.height(16.dp))
        Text("Novedades", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text("Historial de cambios de cada actualización.", color = BarajasPalette.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        RainbowBar(colors = BarajasPalette.DividerGradient, modifier = Modifier.fillMaxWidth(), height = 3.dp)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CHANGELOG.forEach { entry -> ChangeCard(entry) }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BarajasPalette.Hollow)
                .border(1.dp, BarajasPalette.HollowBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Cerrar", color = BarajasPalette.NavIcon, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}

@Composable
private fun ChangeCard(entry: ChangeEntry) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("v${entry.version}", color = BarajasPalette.Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(entry.date, color = BarajasPalette.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        entry.changes.forEach { line ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text("•", color = BarajasPalette.NavIcon, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(line, color = BarajasPalette.DeckName, fontSize = 13.sp)
            }
        }
    }
}
