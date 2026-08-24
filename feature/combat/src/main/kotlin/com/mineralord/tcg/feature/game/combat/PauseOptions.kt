package com.mineralord.tcg.feature.game.combat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * **Menú de PAUSA / OPCIONES del combate** (réplica de TCG Live). Se superpone al tablero con un velo
 * OSCURO SEMITRANSPARENTE —el tablero sigue visible detrás, como en la referencia— y presenta las
 * pestañas ACCESIBILIDAD | GENERAL, el botón RENDIRSE y las tarjetas de ajustes (Sonido, Registro de
 * combate). Es ÚNICO para PvE y PvP (vive en la `CombatScreen` compartida), sin distinción de modo.
 *
 * El estado de los ajustes se HOISTEA en el llamador (para que persista mientras dura la partida y
 * pueda cablearse a audio/animación); esta capa solo lo pinta y notifica cambios.
 */
@Composable
fun PauseOptionsSheet(
    music: Float,
    onMusic: (Float) -> Unit,
    sfx: Float,
    onSfx: (Float) -> Unit,
    disableVfx: Boolean,
    onDisableVfx: (Boolean) -> Unit,
    cardVfxEnabled: Boolean,
    onCardVfxEnabled: (Boolean) -> Unit,
    onSurrender: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(PauseTab.GENERAL) }
    BackHandler(enabled = true) { onDismiss() }

    // Velo oscuro semitransparente: el tablero se ve DETRÁS (misma sensación que la referencia).
    // El scrim captura los toques (bloquea el tablero) y cerrar toca en el vacío o la ✕.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .noRippleClick(onDismiss),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---- Barra superior: título + pestañas ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xF20E1118))
                    .statusBarsPadding()
                    .noRippleClick { }
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "OPCIONES",
                    color = Color.White, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black,
                    fontSize = 18.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PauseTabItem("ACCESIBILIDAD", active = tab == PauseTab.ACCESIBILIDAD) { tab = PauseTab.ACCESIBILIDAD }
                    PauseTabItem("GENERAL", active = tab == PauseTab.GENERAL) { tab = PauseTab.GENERAL }
                }
            }

            // ---- Contenido de la pestaña ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                    PauseTab.GENERAL -> {
                        SurrenderButton(onClick = onSurrender)
                        SettingsCard(title = "SONIDO") {
                            SliderRow("MÚSICA", music, onMusic)
                            Spacer(Modifier.height(12.dp))
                            SliderRow("EFECTOS DE SONIDO", sfx, onSfx)
                        }
                        SettingsCard(title = "OPCIONES DEL REGISTRO DEL COMBATE") {
                            CheckRow("DESACTIVAR EFECTOS VISUALES", disableVfx, onDisableVfx)
                        }
                    }
                    PauseTab.ACCESIBILIDAD -> SettingsCard(title = "EFECTOS VISUALES EN PARTIDA DE LAS CARTAS") {
                        Text(
                            "Activa los efectos visuales de animación de las cartas para los " +
                                "ataques y la Evolución durante las partidas.",
                            color = PauseInk.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (cardVfxEnabled) "ACTIVADO" else "DESACTIVADO",
                                color = PauseInk, fontWeight = FontWeight.Black, fontSize = 13.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = cardVfxEnabled,
                                onCheckedChange = onCardVfxEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SliderRed,
                                    checkedBorderColor = SliderRed,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFB8BDC4),
                                    uncheckedBorderColor = Color(0xFFB8BDC4),
                                ),
                            )
                        }
                    }
                }
            }
        }

        // ---- Botón de cerrar (✕) inferior-centro ----
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xF20E1118))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .noRippleClick(onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * Cuerpo del panel de CONFIRMACIÓN de RENDIRSE (réplica de TCG Live): sube desde abajo sobre el
 * tablero (el llamador lo envuelve en un `MotionPanel` de borde inferior). "ME RINDO" abandona la
 * partida (cuenta como derrota); "MEJOR NO" cancela. Único para PvE y PvP.
 */
@Composable
fun SurrenderConfirmBody(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xFFEDEEF0))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("¿TE RINDES?", color = PauseInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "¿Seguro que quieres abandonar esta partida y que cuente como derrota?",
                color = PauseInk.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 17.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
        SurrenderChoice("ME RINDO", onConfirm)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1A000000)))
        SurrenderChoice("MEJOR NO", onCancel)
    }
}

@Composable
private fun SurrenderChoice(label: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().noRippleClick(onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = PauseInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

private enum class PauseTab { ACCESIBILIDAD, GENERAL }

private val PauseInk = Color(0xFF23262B)
private val SliderRed = Color(0xFFE23B3B)

@Composable
private fun PauseTabItem(label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.noRippleClick(onClick).padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = if (active) CombatTheme.Gold else Color(0xB3FFFFFF),
            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .height(3.dp)
                .width(if (active) 56.dp else 0.dp)
                .clip(RoundedCornerShape(50))
                .background(CombatTheme.Gold),
        )
    }
}

@Composable
private fun SurrenderButton(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF4D670), CombatTheme.Gold, Color(0xFFCB9A2E))))
            .border(1.dp, Color(0x66000000), RoundedCornerShape(10.dp))
            .noRippleClick(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("RENDIRSE", color = Color(0xFF3A2A08), fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F4F4))
            .noRippleClick { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            title,
            color = PauseInk, fontWeight = FontWeight.Black, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = PauseInk.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = SliderRed,
                activeTrackColor = SliderRed,
                inactiveTrackColor = Color(0xFFD9D9D9),
            ),
        )
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().noRippleClick { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PauseInk.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SliderRed,
                uncheckedColor = Color(0xFF9AA0A6),
            ),
        )
    }
}

/** Click sin ripple ni indicación (para scrim/tarjetas/pestañas). */
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
