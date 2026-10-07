package com.mineralord.tcg.feature.game.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import com.mineralord.tcg.core.designsystem.motion.AnimationSprings
import com.mineralord.tcg.core.designsystem.motion.animatePlacement
import com.mineralord.tcg.core.designsystem.motion.motionAppear
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.animationcompose.AbilityGlowVisuals
import com.mineralord.tcg.core.animationcompose.PersistentGlow
import com.mineralord.tcg.core.designsystem.HoloCardImage
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.SpecialEnergy
import com.mineralord.tcg.engine.model.TrainerCard

/**
 * Categoría (supertipo) de una carta de la mano, en el ORDEN que usa TCG Live para
 * agrupar la mano: primero Pokémon, luego Entrenadores, al final Energías.
 */
enum class HandCat { POKEMON, TRAINER, ENERGY }

/**
 * Indicador de jugabilidad de una carta de la mano (glow pulsante estilo TCG Live):
 * - PLAYABLE (azul): se puede usar AHORA (Objeto/Herramienta, Apoyo si no se gastó uno, Energía
 *   si no se adjuntó la del turno, Básico a la Banca).
 * - EVOLVE (amarillo): esta carta de evolución puede hacer evolucionar un Pokémon en juego.
 * La autoridad es el motor (legalIntents); la UI solo pinta el color.
 */
enum class HandGlow { PLAYABLE, EVOLVE }

fun handCatOf(card: Card): HandCat = when (card) {
    is PokemonCard -> HandCat.POKEMON
    is TrainerCard -> HandCat.TRAINER
    else -> HandCat.ENERGY // BasicEnergy / SpecialEnergy
}

/**
 * Ordena la mano como TCG Live: agrupada por supertipo (Pokémon → Entrenador → Energía).
 * `sortedBy` es ESTABLE, así que dentro de cada grupo se conserva el orden de llegada.
 */
fun sortedHandCards(cards: List<Card>): List<Card> = cards.sortedBy { handCatOf(it).ordinal }

/** Azul de resaltado de TCG Live para la categoría activa; tinta oscura del texto. */
private val HandBarBlue = Color(0xFF2C79E0)
private val HandBarInk = Color(0xFF20232B)

/**
 * Barra inferior de la mano — RÉPLICA de TCG Live: barra BLANCA redondeada con el
 * contador TOTAL de cartas a la izquierda y un icono por supertipo
 * (Pokémon / Entrenador / Energía). El icono de la categoría activa se pinta en AZUL,
 * los demás en gris. Tocar un icono alterna el resaltado de ese grupo en la mano
 * (tócalo otra vez para quitarlo). Iconos sin cartas de ese tipo quedan deshabilitados.
 */
@Composable
fun HandFilterBar(
    cards: List<Card>,
    active: HandCat?,
    onToggle: (HandCat) -> Unit,
) {
    val counts = cards.groupingBy { handCatOf(it) }.eachCount()
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3F1EA))
            .border(1.dp, Color(0x33000000), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Contador TOTAL: número oscuro grande, como TCG Live.
        Text("${cards.size}", color = HandBarInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
        HandCat.values().forEach { cat ->
            val n = counts[cat] ?: 0
            val on = active == cat
            val tint = when {
                on -> HandBarBlue
                n > 0 -> Color(0xFF7C7F86)
                else -> Color(0xFFC7C9CE)
            }
            HandTypeIcon(
                cat = cat,
                tint = tint,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(enabled = n > 0) { onToggle(cat) },
            )
        }
    }
}

/**
 * Iconos de categoría dibujados a mano (arte PROPIO, nunca assets de TPC):
 * Pokémon = silueta, Entrenador = Poké Ball, Energía = estrella.
 */
@Composable
private fun HandTypeIcon(cat: HandCat, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(4.dp)) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (cat) {
            HandCat.TRAINER -> {
                // Poké Ball: círculo, línea media y botón central.
                val r = s * 0.46f
                drawCircle(tint, r, Offset(cx, cy), style = Stroke(width = s * 0.10f))
                drawLine(tint, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = s * 0.10f)
                drawCircle(tint, s * 0.15f, Offset(cx, cy), style = Stroke(width = s * 0.09f))
            }
            HandCat.ENERGY -> {
                // Estrella de 5 puntas.
                val path = Path()
                val outer = s * 0.48f
                val inner = s * 0.20f
                for (i in 0 until 10) {
                    val rr = if (i % 2 == 0) outer else inner
                    val ang = Math.toRadians((-90 + i * 36).toDouble())
                    val x = cx + (rr * Math.cos(ang)).toFloat()
                    val y = cy + (rr * Math.sin(ang)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, tint)
            }
            HandCat.POKEMON -> {
                // Silueta simple: cuerpo redondo + dos orejas triangulares.
                val r = s * 0.32f
                val bodyY = cy + s * 0.12f
                val ears = Path().apply {
                    moveTo(cx - r * 0.9f, bodyY - r * 0.7f)
                    lineTo(cx - r * 1.2f, bodyY - r * 1.8f)
                    lineTo(cx - r * 0.1f, bodyY - r * 1.0f)
                    close()
                    moveTo(cx + r * 0.9f, bodyY - r * 0.7f)
                    lineTo(cx + r * 1.2f, bodyY - r * 1.8f)
                    lineTo(cx + r * 0.1f, bodyY - r * 1.0f)
                    close()
                }
                drawPath(ears, tint)
                drawCircle(tint, r, Offset(cx, bodyY))
            }
        }
    }
}


/**
 * Un montón de la mano: copias IDÉNTICAS (mismo id IMPRESO, ignorando el sufijo de
 * instancia `#N`) colapsadas en una sola carta con contador, tal como TCG Live. `rep`
 * es la instancia representante (la primera): al arrastrarla/jugarla sale UNA copia y
 * el contador baja solo en la siguiente composición.
 */
data class HandStack(val rep: Card, val ids: List<CardId>) {
    val count: Int get() = ids.size
}

/**
 * Agrupa la mano (ya ordenada por supertipo con [sortedHandCards]) colapsando las copias
 * del mismo id IMPRESO en un solo montón, conservando el orden de primera aparición
 * (así el orden por supertipo se respeta y las copias quedan juntas).
 */
fun groupHandStacks(cards: List<Card>): List<HandStack> {
    val byPrint = LinkedHashMap<String, MutableList<Card>>()
    for (c in cards) byPrint.getOrPut(c.id.printed.raw) { mutableListOf() }.add(c)
    return byPrint.values.map { group -> HandStack(group.first(), group.map { it.id }) }
}

/** Mano en abanico, con cartas ligeramente rotadas y solapadas. El tamaño de carta
 *  se pasa desde el tablero (proporcional a su alto) para respetar la escala 1:1.
 *  Las copias idénticas se COLAPSAN en un montón con contador ([groupHandStacks]). */
@Composable
fun HandFan(
    cards: List<Card>,
    enabled: Boolean,
    onSelect: (Card) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: CardId? = null,
    cardW: androidx.compose.ui.unit.Dp = 76.dp,
    cardH: androidx.compose.ui.unit.Dp = 106.dp,
    // Arrastre de cartas (energía a un Pokémon, o evolución sobre su pre-evolución).
    // Posiciones en coords de RAÍZ. `canDrag` decide qué cartas son arrastrables.
    canDrag: (Card) -> Boolean = { false },
    onCardDragStart: ((Card, Offset) -> Unit)? = null,
    onCardDrag: ((Offset) -> Unit)? = null,
    onCardDragEnd: (() -> Unit)? = null,
    onCardDragCancel: (() -> Unit)? = null,
    // Filtro por tipo activo: si no es null, las cartas que NO cumplen se ATENÚAN (y no se
    // arrastran), para destacar el grupo elegido en los chips de la barra inferior.
    emphasize: ((Card) -> Boolean)? = null,
    // Auto-desplazamiento por categoría (barra blanca de TCG Live): al tocar un icono, la mano
    // se DESLIZA hasta donde EMPIEZA esa categoría (primer montón del grupo, pegado a la
    // izquierda). [focusNonce] cambia en cada toque para re-desplazar aunque la categoría repita.
    focusCategory: HandCat? = null,
    focusNonce: Int = 0,
    // Glow de jugabilidad por carta (azul = usable ahora, amarillo = evolución). null = sin glow.
    glowOf: (Card) -> HandGlow? = { null },
) {
    // El gesto de arrastre vive en un `pointerInput(card.id)` que captura sus lambdas
    // UNA sola vez (la clave no cambia). Sin esto, `onCardDragEnd` quedaba congelado en
    // la PRIMERA composición —cuando aún no se arrastra nada— y perdía los valores que
    // el tablero calcula por composición al arrastrar (objetivo de Objeto dirigido/
    // Herramienta, estado vigente). Resultado: la Poción y demás Objetos dirigidos no se
    // jugaban al soltarlos. `rememberUpdatedState` mantiene vivas las ÚLTIMAS lambdas.
    val latestOnDragStart by rememberUpdatedState(onCardDragStart)
    val latestOnDrag by rememberUpdatedState(onCardDrag)
    val latestOnDragEnd by rememberUpdatedState(onCardDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onCardDragCancel)
    // Copias idénticas → un solo montón con contador (réplica de TCG Live).
    val stacks = groupHandStacks(cards)
    val center = (stacks.size - 1) / 2f
    val maxAbs = center.coerceAtLeast(1f)
    // Solape moderado: cartas separadas y legibles como en TCG Live (ref_0040).
    val overlap = cardW * 0.24f
    // Scroll hoisteado para poder DESLIZAR la mano a una categoría (barra blanca).
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    // Al tocar un icono de la barra: animar el scroll hasta el primer montón de esa categoría,
    // dejándolo pegado al borde izquierdo (paso entre montones = cardW - overlap; +12dp de
    // padding inicial del Row). Si no hay cartas de esa categoría, no hace nada.
    LaunchedEffect(focusNonce) {
        val cat = focusCategory ?: return@LaunchedEffect
        val idx = stacks.indexOfFirst { handCatOf(it.rep) == cat }
        if (idx < 0) return@LaunchedEffect
        val targetPx = with(density) { (12.dp + (cardW - overlap) * idx).toPx() }
        scrollState.animateScrollTo(targetPx.toInt())
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(-overlap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        stacks.forEachIndexed { i, stack ->
            val card = stack.rep
            val delta = i - center
            val norm = delta / maxAbs                      // -1 (izq) .. 1 (der)
            val isSel = selectedId != null && selectedId in stack.ids
            // Abanico SUTIL como TCG Live: rotación pequeña con pivote en la BASE, y un
            // arco muy suave (colina) donde la carta central sube apenas y los extremos
            // quedan en la línea base (nada se hunde bajo el borde inferior).
            // Objetivos del abanico; se suavizan con RESORTE para que la selección/reflujo
            // asiente con inercia física en vez de saltar.
            val angleTarget = if (isSel) 0f else norm * 5f
            val hill = cardH * (0.05f * (1f - norm * norm))
            val lift = if (isSel) cardH * 0.16f else hill
            val cardScaleTarget = if (isSel) 1.14f else 1f
            val angle by animateFloatAsState(angleTarget, AnimationSprings.standard(), label = "handAngle")
            val cardScale by animateFloatAsState(cardScaleTarget, AnimationSprings.bouncy(), label = "handScale")
            // Filtro por tipo: las cartas que no cumplen se atenúan y no se arrastran.
            val matches = emphasize?.invoke(card) ?: true
            val cardAlpha = if (matches) 1f else 0.3f
            // Cartas arrastrables (energías / evoluciones) según `canDrag`.
            val draggable = enabled && matches && canDrag(card) && onCardDragStart != null
            var cardCoords by remember(card.id) { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier
                    .zIndex(if (isSel) 2f else 0f)
                    // Reubicación física: al reordenar/insertar la mano, la carta se DESLIZA
                    // con resorte a su nuevo hueco (incluida la subida al seleccionar).
                    .animatePlacement()
                    // Entrada al aparecer en la mano (aterrizaje con fade+escala+subida).
                    .motionAppear(card.id)
                    // Rotación/escala como capa de DIBUJO con pivote en la BASE de la carta
                    // → abanico limpio (bases convergen, puntas se despliegan) sin sumar
                    // alto de layout ni comprimir/deformar la carta.
                    .graphicsLayer {
                        rotationZ = angle
                        scaleX = cardScale; scaleY = cardScale
                        alpha = cardAlpha
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .offset(y = -lift)
                    .size(cardW, cardH)
                    // Sin clip en el Box exterior: así el badge de contador puede
                    // sobresalir por el borde superior (la imagen se recorta sola).
                    .border(
                        if (isSel) 2.dp else 1.dp,
                        if (isSel) BattleTheme.Gold else BattleTheme.Gold.copy(alpha = 0.5f),
                        RoundedCornerShape(7.dp),
                    )
                    .onGloballyPositioned { cardCoords = it }
                    // Tocar una carta SIEMPRE la inspecciona (también en el turno del
                    // rival), para poder leer las cartas de la mano en cualquier momento.
                    // El arrastre (jugar) sí queda restringido por `enabled`/`draggable`.
                    .then(
                        if (draggable) Modifier.pointerInput(card.id) {
                            // Solo el arrastre VERTICAL (hacia arriba, hacia el tablero)
                            // levanta la carta; el gesto horizontal NO se consume y lo recibe
                            // el horizontalScroll de la fila, permitiendo DESLIZAR la mano
                            // para ojearla sin arrastrar cartas.
                            detectVerticalDragGestures(
                                onDragStart = { local ->
                                    cardCoords?.let { latestOnDragStart?.invoke(card, it.localToRoot(local)) }
                                },
                                onVerticalDrag = { change, _ ->
                                    cardCoords?.let { latestOnDrag?.invoke(it.localToRoot(change.position)) }
                                },
                                onDragEnd = { latestOnDragEnd?.invoke() },
                                onDragCancel = { latestOnDragCancel?.invoke() },
                            )
                        } else Modifier,
                    )
                    .clickable { onSelect(card) },
            ) {
                // Todas las cartas (incluidas las de Energía) se muestran con su ARTE puro,
                // sin marco/fondo de color: la carta llena su hueco como cualquier otra.
                // HOLO real (foil de malie, brillo ambiental) igual que en colección/tablero.
                HoloCardImage(
                    imageUrl = card.artwork.small(true),
                    setCode = card.id.printed.raw.let { if (it.startsWith("energy")) "energy" else it.substringBeforeLast('-') },
                    cardNumber = card.id.printed.raw.substringAfterLast('-').toIntOrNull(),
                    rarity = card.rarity,
                    contentDescription = card.name.es,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
                )
                // Glow de jugabilidad PERSISTENTE (misma aura que el tablero): azul = usable ahora,
                // amarillo = evolución. Solo cuando la carta NO está atenuada por el filtro de tipo.
                if (matches) {
                    when (glowOf(card)) {
                        HandGlow.PLAYABLE -> PersistentGlow(AbilityGlowVisuals.Playable, Modifier.fillMaxSize(), edgeOnly = true)
                        HandGlow.EVOLVE -> PersistentGlow(AbilityGlowVisuals.Evolve, Modifier.fillMaxSize(), edgeOnly = true)
                        null -> {}
                    }
                }
                // Contador de copias: píldora blanca con el número, centrada sobre el
                // borde superior de la carta (solo cuando hay 2+ copias), como TCG Live.
                if (stack.count > 1) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = -(cardH * 0.09f))
                            .zIndex(3f)
                            .size(cardW * 0.34f)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFF20232B), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${stack.count}",
                            color = Color(0xFF20232B),
                            fontWeight = FontWeight.Black,
                            fontSize = (cardH.value * 0.15f).sp,
                        )
                    }
                }
            }
        }
    }
}

