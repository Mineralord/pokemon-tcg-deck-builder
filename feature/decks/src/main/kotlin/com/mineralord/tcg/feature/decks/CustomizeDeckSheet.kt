package com.mineralord.tcg.feature.decks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.BarajasPalette
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.engine.model.EnergyType

/**
 * Popup "Personalizar baraja" (réplica de TCG Pocket): entra deslizando **desde arriba**
 * sobre un velo tenue, con estética **neumórfica clara** — cada sección es una píldora de
 * título (blanca, texto gris centrado) sobre un panel blanco redondeado con sombra suave.
 * Secciones: Caja para cartas · Energía · Cartas destacadas · Accesorios. Pie Cancelar/Aceptar.
 *
 * Chasis visual: el contenido interno (cambiar caja/energía/destacadas/accesorios) abre por
 * ahora [onSoon] "Próximamente"; Aceptar/Cancelar y el velo cierran.
 */
@Composable
internal fun CustomizeDeckSheet(
    type: EnergyType,
    featured: List<String?>,
    onSoon: (String) -> Unit,
    onEditFeatured: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { state.targetState = true }
    // Cuando la animación de salida termina, notifica el cierre real.
    LaunchedEffect(state.currentState, state.targetState) {
        if (!state.targetState && !state.currentState) onDismiss()
    }
    fun close() { state.targetState = false }

    Box(Modifier.fillMaxSize()) {
        // Velo tenue detrás (toca para cerrar).
        Box(
            Modifier.fillMaxSize().background(Color(0x22000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        )

        AnimatedVisibility(
            visibleState = state,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(BarajasPalette.BgBottom)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
                    // No cerrar al tocar dentro del panel.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Spacer(Modifier.height(14.dp))

                // Caja para cartas.
                SectionHeader("Caja para cartas")
                NeuPanel(Modifier.fillMaxWidth().clickable { onSoon("Caja para cartas") }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        DeckBoxPreview(
                            type = type,
                            cover = featured.getOrNull(0),
                            sides = featured.drop(1).filterNotNull(),
                            modifier = Modifier.size(150.dp, 120.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Energía · Cartas destacadas.
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        SectionHeader("Energía")
                        NeuPanel(Modifier.fillMaxWidth().clickable { onSoon("Energía") }) {
                            Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                                EnergyBadge(type)
                            }
                        }
                    }
                    Column(Modifier.weight(1.25f)) {
                        SectionHeader("Cartas destacadas")
                        NeuPanel(Modifier.fillMaxWidth().clickable { onEditFeatured() }) {
                            Row(
                                Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(3) { i ->
                                    FeaturedThumb(featured.getOrNull(i), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Accesorios.
                SectionHeader("Accesorios")
                NeuPanel(Modifier.fillMaxWidth().clickable { onSoon("Accesorios") }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        repeat(3) { AccessoryThumb(Modifier.weight(1f)) }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Pie: Cancelar / Aceptar.
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 18.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DialogButton(
                        text = "Cancelar",
                        textColor = BarajasPalette.Muted,
                        borderColor = BarajasPalette.HairlineBorder,
                        modifier = Modifier.weight(1f),
                        onClick = { close() },
                    )
                    DialogButton(
                        text = "Aceptar",
                        textColor = Color.White,
                        borderColor = Color.Transparent,
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { close() },
                    )
                }
            }
        }
    }
}

/** Píldora de título de sección: blanca, redondeada completa, texto gris centrado. */
@Composable
private fun SectionHeader(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(BarajasPalette.Surface)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = BarajasPalette.DeckName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Panel de contenido neumórfico: blanco, esquinas suaves, sombra tenue. */
@Composable
private fun NeuPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .padding(top = 8.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BarajasPalette.Surface)
            .border(1.dp, BarajasPalette.HairlineBorder.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        content = content,
    )
}

/** Insignia circular de energía del tipo dominante (círculo tintado + icono real). */
@Composable
private fun EnergyBadge(type: EnergyType) {
    val res = energyIconRes(type)
    Box(
        Modifier.size(56.dp).shadow(4.dp, CircleShape).clip(CircleShape)
            .background(typeColor(type)),
        contentAlignment = Alignment.Center,
    ) {
        if (res != null) {
            Image(painterResource(res), null, Modifier.size(34.dp))
        } else {
            Text("⚡", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FeaturedThumb(imageEs: String?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(6.dp))
            .background(BarajasPalette.Hollow),
    ) {
        if (imageEs != null) {
            AsyncImage(
                model = imageEs, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
            )
        }
    }
}

@Composable
private fun AccessoryThumb(modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(BarajasPalette.Hollow),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(com.mineralord.tcg.feature.decks.R.drawable.card_back_default),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
        )
    }
}

/** Icono de energía real (assets de core:designsystem). Hada no tiene asset. */
private fun energyIconRes(t: EnergyType): Int? = when (t) {
    EnergyType.GRASS -> com.mineralord.tcg.core.designsystem.R.drawable.energy_grass
    EnergyType.FIRE -> com.mineralord.tcg.core.designsystem.R.drawable.energy_fire
    EnergyType.WATER -> com.mineralord.tcg.core.designsystem.R.drawable.energy_water
    EnergyType.LIGHTNING -> com.mineralord.tcg.core.designsystem.R.drawable.energy_lightning
    EnergyType.PSYCHIC -> com.mineralord.tcg.core.designsystem.R.drawable.energy_psychic
    EnergyType.FIGHTING -> com.mineralord.tcg.core.designsystem.R.drawable.energy_fighting
    EnergyType.DARKNESS -> com.mineralord.tcg.core.designsystem.R.drawable.energy_darkness
    EnergyType.METAL -> com.mineralord.tcg.core.designsystem.R.drawable.energy_metal
    EnergyType.DRAGON -> com.mineralord.tcg.core.designsystem.R.drawable.energy_dragon
    EnergyType.COLORLESS -> com.mineralord.tcg.core.designsystem.R.drawable.energy_colorless
    EnergyType.FAIRY -> null
}
