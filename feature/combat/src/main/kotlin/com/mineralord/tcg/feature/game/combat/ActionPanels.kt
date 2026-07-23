package com.mineralord.tcg.feature.game.combat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.EnergySphere
import com.mineralord.tcg.core.designsystem.tilt.Finish
import com.mineralord.tcg.core.designsystem.typeColor
import com.mineralord.tcg.core.designsystem.tilt.holoAmbient
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.feature.combat.R

/**
 * SISTEMA DE PANELES DE ACCIÓN (ataques y habilidades) — identidad propia, sin Material.
 *
 * Filosofía (ver auditoría de la referencia):
 *  - Los PANELES son informativos: son [Row]/[Column] puros SIN área táctil.
 *  - El ÚNICO elemento interactivo es [PrecisionButton] (USAR ATAQUE / USAR HABILIDAD).
 *  - El marco plateado y el bisel se DIBUJAN con gradientes (cero assets → escala a cualquier
 *    tipo, ancho y longitud de texto, y es mantenible por décadas).
 *  - El holo del panel reutiliza EXACTAMENTE el shader de la carta ([holoAmbient] con el mismo
 *    [Finish] resuelto por rareza): sincronía real, no un efecto parecido. No-op en no-holo.
 */

// ------------------------------------------------------------------ Materiales

/**
 * CROMO del marco (bisel de acero): pulido brillante arriba, valle oscuro al medio y filo claro
 * abajo → lee como metal biselado con volumen, no como un borde plano.
 */
private fun chrome(): Brush = Brush.verticalGradient(
    0.00f to Color(0xFFFAFBFD),
    0.14f to Color(0xFFD3D8DF),
    0.30f to Color(0xFF9AA1AB),
    0.52f to Color(0xFF6B7079),
    0.72f to Color(0xFF9BA2AC),
    0.88f to Color(0xFFDDE1E7),
    1.00f to Color(0xFF7C828B),
)

/** Cromo radial para piezas circulares (la moneda de energía). */
private fun chromeRadial(): Brush = Brush.radialGradient(
    listOf(Color(0xFFFAFBFD), Color(0xFFAEB4BD), Color(0xFF6B7079)),
)

/** Color de acento del tipo (dorado por defecto para Pokémon sin tipo asignado). */
private fun accentFor(type: EnergyType?): Color = type?.let { typeColor(it) } ?: Color(0xFFF0C24B)

/**
 * Textura OFICIAL de fondo por tipo (tiras texturizadas de Pokémon). FAIRY y DRAGON no tienen
 * lámina propia empaquetada → se resuelven al fondo más cercano; sin tipo → incoloro.
 */
private fun fondoRes(type: EnergyType?): Int = when (type) {
    EnergyType.GRASS -> R.drawable.fondo_grass
    EnergyType.FIRE -> R.drawable.fondo_fire
    EnergyType.WATER -> R.drawable.fondo_water
    EnergyType.LIGHTNING -> R.drawable.fondo_lightning
    EnergyType.PSYCHIC, EnergyType.FAIRY -> R.drawable.fondo_psychic
    EnergyType.FIGHTING -> R.drawable.fondo_fighting
    EnergyType.DARKNESS -> R.drawable.fondo_darkness
    EnergyType.METAL -> R.drawable.fondo_metal
    EnergyType.COLORLESS, EnergyType.DRAGON, null -> R.drawable.fondo_colorless
}

// Tinta de los PANELES (interior brillante → texto oscuro, como la referencia).
private val TitleInk = Color(0xFF1C1206)   // títulos de ataque y número de daño (casi negro)
private val BodyInk = Color(0xFF33271A)    // descripciones
private val AbilityRed = Color(0xFFC01A1A) // nombre de Habilidad (rojo TCG)

/**
 * Tipografía. La oficial del TCG es Gill Sans (propietaria; no se puede empaquetar), y la carta
 * ya la trae "horneada" en su bitmap. Para el texto NATIVO usamos la familia humanista sans más
 * cercana disponible en el sistema; centralizada aquí para cambiarla en un solo sitio el día que
 * se licencie una fuente propia.
 */
private val TcgFamily = FontFamily.SansSerif

/** Realce GRABADO (emboss) para dar peso a títulos y daño sobre el interior brillante. */
private val EmbossLight = Shadow(Color(0xB3FFFFFF), Offset(0f, 1f), 0.5f)
/** Sombra de PROFUNDIDAD para el texto claro sobre la gema del botón. */
private val InkOnGem = Shadow(Color(0x73000000), Offset(0f, 1f), 1.5f)

private val FrameShape = RoundedCornerShape(18.dp)
private val FrameInner = RoundedCornerShape(12.dp)

/** Altura mínima común de los paneles → ritmo vertical uniforme entre habilidad y ataque. */
private val PanelMinHeight = 58.dp
/** Ancho FIJO del botón → sus bordes se alinean perfectamente entre todos los paneles. */
private val ButtonWidth = 136.dp
/** Ranura FIJA del daño → el botón no se desplaza haya o no daño, y admite 3 dígitos (p. ej. 300). */
private val DamageSlot = 48.dp

// ------------------------------------------------------------------ Contenedor

/**
 * Marco de panel: bisel de CROMO GRUESO (filo exterior claro + surco interior oscuro) que
 * enmarca un interior con gradiente del tipo + la MISMA capa holo de la carta + reflejo
 * especular. El contenido ([content]) es un [Row] puramente informativo: NO recibe toques.
 */
@Composable
private fun ActionFrame(
    finish: Finish,
    bgType: EnergyType?,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(FrameShape)
            .background(chrome())
            .border(1.5.dp, Color(0xB3FFFFFF), FrameShape)          // filo exterior pulido
            .padding(6.dp),                                          // GROSOR del marco cromado
    ) {
        // Surco oscuro interior (separa el metal del interior → sensación de bisel excavado).
        Box(
            Modifier
                .matchParentSize()
                .clip(FrameInner)
                .border(1.5.dp, Color(0x99202832), FrameInner),
        )
        // Interior: TEXTURA OFICIAL del tipo (tira texturizada de Pokémon) recortada al bisel,
        // + HOLO sincronizado con la carta (mismo shader) encima. No-op en no-holo.
        Image(
            painter = painterResource(fondoRes(bgType)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .padding(1.5.dp)
                .clip(FrameInner),
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(1.5.dp)
                .clip(FrameInner)
                .holoAmbient(finish),
        )
        // Reflejo especular sutil en el filo superior (vidrio/laca).
        Box(
            Modifier
                .matchParentSize()
                .padding(1.5.dp)
                .clip(FrameInner)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x4DFFFFFF),
                        0.24f to Color(0x0FFFFFFF),
                        1.0f to Color.Transparent,
                    ),
                ),
        )
        // Contenido informativo (sin pointerInput). Altura mínima común → ritmo uniforme.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = PanelMinHeight)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// ------------------------------------------------------------------ El botón

/**
 * Moneda metálica engastada en un anillo de cromo (pieza izquierda del botón). Muestra el emblema
 * del tipo por defecto, o un [glyph] (p. ej. el icono de intercambio de la RETIRADA) si se indica.
 */
@Composable
private fun EnergyCoin(type: EnergyType?, active: Boolean, glyph: String? = null) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (active) chromeRadial() else Brush.radialGradient(listOf(Color(0xFF9AA0A9), Color(0xFF5C616A))))
            .border(1.dp, Color(0xCCFFFFFF), CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Text(
                glyph,
                color = if (active) Color(0xFF2A2E36) else Color(0xFF6E7278),
                fontFamily = TcgFamily, fontWeight = FontWeight.Black, fontSize = 20.sp,
                style = TextStyle(shadow = EmbossLight),
            )
        } else {
            EnergySphere(type ?: EnergyType.COLORLESS, size = 28.dp)
        }
    }
}

/**
 * BOTÓN DE PRECISIÓN — ensamblaje CROMADO de 3 piezas (como la referencia), no un rectángulo:
 *   [moneda de energía] · [GEMA esmaltada con el texto] · [pestaña de chevrón].
 * Todo dentro de un marco de acero continuo.
 *
 * Física de 3 fases: ASIENTO al tocar (se hunde en el bisel), TENSIÓN mientras se mantiene
 * (especular baja, halo del tipo sube) y LIBERACIÓN al soltar (rebote elástico + destello).
 * Estados: normal · pressed · disabled · used · selected.
 */
@Composable
private fun PrecisionButton(
    label: String,
    accent: Color,
    energyType: EnergyType?,
    ivory: Boolean,
    enabled: Boolean,
    used: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    coinGlyph: String? = null,
) {
    val active = enabled && !used
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val seat by animateFloatAsState(
        targetValue = if (pressed && active) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "seat",
    )
    val release = remember { Animatable(0f) }
    var wasPressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (wasPressed && !pressed && active) {
            release.snapTo(1f)
            release.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 380f))
        }
        wasPressed = pressed
    }

    // Color de la GEMA: marfil para habilidad (como la referencia), color del tipo para ataque.
    val gemDark = if (active) accent else lerp(accent, Color(0xFF6A6E76), 0.72f)
    val gemTop: Color; val gemBottom: Color; val ink: Color
    if (ivory) {
        gemTop = if (active) Color(0xFFFFFBEE) else Color(0xFFC9CBC4)
        gemBottom = if (active) Color(0xFFCDB985) else Color(0xFF9A9C96)
        ink = if (active) lerp(accent, Color.Black, 0.35f) else Color(0xFF6E7278)
    } else {
        gemTop = lerp(gemDark, Color.White, 0.34f - seat * 0.20f)
        gemBottom = lerp(gemDark, Color.Black, 0.40f + seat * 0.14f)
        ink = if (active) Color.White else Color(0xFFCED2D9)
    }
    val frameBorder = if (selected) CombatTheme.Gold else Color(0xCCFFFFFF)

    val scale = 1f - seat * 0.04f + release.value * 0.028f
    val outer = RoundedCornerShape(13.dp)
    val gemShape = RoundedCornerShape(8.dp)

    Box(
        modifier
            .width(ButtonWidth)
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationY = seat * 3.dp.toPx()
            }
            .clip(outer)
            .background(chrome())                              // MARCO de acero continuo
            .border(1.5.dp, frameBorder, outer)
            .padding(3.dp)
            .then(
                if (active) Modifier.clickable(
                    interactionSource = interaction, indication = null, onClick = onClick,
                ) else Modifier,
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            EnergyCoin(energyType, active, glyph = coinGlyph)

            // GEMA central esmaltada con el texto (ocupa el espacio flexible).
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(gemShape)
                    .background(Brush.verticalGradient(listOf(gemTop, gemBottom)))
                    .border(1.dp, Color(0x66000000), gemShape),
                contentAlignment = Alignment.Center,
            ) {
                // Halo de TENSIÓN (energía acumulándose al mantener).
                if (active && !ivory) {
                    Box(
                        Modifier.matchParentSize().clip(gemShape).background(
                            Brush.radialGradient(
                                listOf(lerp(gemDark, Color.White, 0.5f).copy(alpha = seat * 0.4f), Color.Transparent),
                            ),
                        ),
                    )
                }
                // Especular superior: brilla en reposo, baja con la tensión, DESTELLA al soltar.
                val specA = (0.35f - seat * 0.26f + release.value * 0.7f).coerceIn(0f, 1f)
                Box(
                    Modifier.matchParentSize().clip(gemShape).background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = specA),
                            0.45f to Color.White.copy(alpha = specA * 0.12f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                )
                Text(
                    if (used) "USADO" else label,
                    color = ink,
                    fontFamily = TcgFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = TextStyle(shadow = if (ivory) null else InkOnGem),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            // PESTAÑA de chevrón (pieza derecha), engastada en el mismo cromo.
            Box(
                Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .clip(gemShape)
                    .background(Brush.verticalGradient(listOf(lerp(gemDark, Color.White, 0.18f), lerp(gemDark, Color.Black, 0.42f))))
                    .border(1.dp, Color(0x66000000), gemShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "›",
                    color = if (active) Color.White else Color(0xFFC2C6CD),
                    fontFamily = TcgFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    style = TextStyle(shadow = InkOnGem),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Átomos

/** Etiqueta roja "Habilidad" (píldora), como en la referencia. */
@Composable
private fun HabilidadPill() {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFFE24A48), Color(0xFFB01F1D))))
            .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 2.dp),
    ) {
        Text(
            "Habilidad",
            color = Color.White, fontFamily = TcgFamily, fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic, fontSize = 12.sp, letterSpacing = 0.3.sp,
        )
    }
}

/** Coste de energía con las ESFERAS oficiales (incoloro si el coste es libre). */
@Composable
private fun EnergyCostColumn(cost: List<EnergyType>) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (cost.isEmpty()) EnergySphere(EnergyType.COLORLESS, size = 20.dp)
        else cost.forEach { EnergySphere(it, size = 20.dp) }
    }
}

// ------------------------------------------------------------------ Paneles

/**
 * PANEL DE ATAQUE — energías · nombre · descripción · daño · botón USAR ATAQUE.
 * Escala a ataques largos/cortos, con/sin daño, muchas/pocas energías.
 */
@Composable
fun AttackPanel(
    attack: Attack,
    type: EnergyType?,
    finish: Finish,
    enabled: Boolean,
    used: Boolean,
    description: String,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = accentFor(type)
    val dmg = (attack.baseDamage as? Damage.Fixed)?.value
    ActionFrame(finish, bgType = type, modifier = modifier) {
        EnergyCostColumn(attack.cost)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                attack.name.es,
                color = TitleInk, fontFamily = TcgFamily, fontWeight = FontWeight.Black, fontSize = 17.sp,
                lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = EmbossLight),
            )
            if (description.isNotBlank()) {
                Text(
                    description, color = BodyInk, fontFamily = TcgFamily, fontSize = 10.sp, lineHeight = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Ranura de DAÑO de ancho FIJO → el botón queda siempre en la misma posición.
        Box(Modifier.width(DamageSlot).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            if (dmg != null) {
                Text(
                    "$dmg",
                    color = TitleInk, fontFamily = TcgFamily, fontWeight = FontWeight.Black, fontSize = 26.sp,
                    maxLines = 1, softWrap = false, style = TextStyle(shadow = EmbossLight),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        PrecisionButton(
            "USAR ATAQUE", accent, energyType = type, ivory = false,
            enabled = enabled, used = used, onClick = onUse,
        )
    }
}

/**
 * PANEL DE HABILIDAD MANUAL — etiqueta · nombre · descripción · botón USAR HABILIDAD.
 */
@Composable
fun ManualAbilityPanel(
    ability: Ability,
    type: EnergyType?,
    finish: Finish,
    enabled: Boolean,
    used: Boolean,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = accentFor(type)
    ActionFrame(finish, bgType = type, modifier = modifier) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HabilidadPill()
            Text(
                ability.name.es,
                color = AbilityRed, fontFamily = TcgFamily, fontWeight = FontWeight.Black, fontSize = 17.sp,
                lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = EmbossLight),
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(Modifier.width(12.dp))
        PrecisionButton(
            "USAR HABILIDAD", accent, energyType = type, ivory = true,
            enabled = enabled, used = used, onClick = onUse,
        )
    }
}

/**
 * PANEL DE HABILIDAD PASIVA — layout propio SIN botón: emblema de "pasiva" donde iría la acción,
 * equilibrando la composición en lugar de dejar un hueco.
 */
@Composable
fun PassiveAbilityPanel(
    ability: Ability,
    type: EnergyType?,
    finish: Finish,
    modifier: Modifier = Modifier,
) {
    ActionFrame(finish, bgType = type, modifier = modifier) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HabilidadPill()
            Text(
                ability.name.es,
                color = AbilityRed, fontFamily = TcgFamily, fontWeight = FontWeight.Black, fontSize = 17.sp,
                lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = EmbossLight),
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(Modifier.width(12.dp))
        // Emblema "PASIVA" (medalla grabada) — MISMA huella que el botón (alineación de rejilla),
        // pero sin marco cromado ni toque: comunica "no hay acción que ejecutar".
        Box(
            Modifier
                .width(ButtonWidth)
                .height(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0x1F000000))
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "PASIVA",
                color = TitleInk.copy(alpha = 0.68f), fontFamily = TcgFamily, fontWeight = FontWeight.Black,
                fontSize = 12.sp, letterSpacing = 2.sp,
            )
        }
    }
}

/**
 * PANEL DE RETIRADA — coste (esferas incoloras) · nombre "Retirada" · descripción · botón RETIRAR.
 * Comparte marco cromado, fondo del tipo y botón cápsula con los demás paneles para una tercera
 * fila coherente con la referencia. El botón usa el glifo de intercambio en lugar de moneda de tipo.
 */
@Composable
fun RetreatPanel(
    type: EnergyType?,
    finish: Finish,
    cost: List<EnergyType>,
    enabled: Boolean,
    onRetreat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = accentFor(type)
    // Fondo SIEMPRE incoloro para la retirada (coste incoloro), independientemente del tipo.
    ActionFrame(finish, bgType = EnergyType.COLORLESS, modifier = modifier) {
        EnergyCostColumn(cost)
        Spacer(Modifier.width(8.dp))
        Text(
            "Retirada",
            color = TitleInk, fontFamily = TcgFamily, fontWeight = FontWeight.Black, fontSize = 17.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = EmbossLight),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        PrecisionButton(
            "RETIRAR", accent, energyType = type, ivory = true,
            enabled = enabled, used = false, onClick = onRetreat,
            coinGlyph = "⇄", // ⇄ icono de intercambio
        )
    }
}
