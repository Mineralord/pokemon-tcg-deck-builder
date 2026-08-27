package com.mineralord.tcg.feature.carddetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mineralord.tcg.core.designsystem.HoloCardImage
import com.mineralord.tcg.core.designsystem.InteractiveHoloCard
import com.mineralord.tcg.data.profile.CraftingRules
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.TrainerCard

// ─────────────────────────────────────────────────────────────────────────────
// Detalle de carta COMPARTIDO (colección + editor de barajas). Réplica 1:1 del
// visor de "Mis cartas" de TCG Live: visor holo interactivo, copias, Fabricar/
// Destruir, tabla de datos, ataques, cartas relacionadas, ¡La quiero!, idiomas.
// ─────────────────────────────────────────────────────────────────────────────

// Paleta neumórfica clara (idéntica a la colección).
private val BgTop = Color(0xFFEDF1F8)
private val Panel = Color(0xFFFFFFFF)
private val Ink = Color(0xFF2B3346)
private val Muted = Color(0xFF8A93A6)
private val Accent = Color(0xFF35C4E8)

/** Carta resuelta para el detalle compartido. Conserva el [Card] del motor para la tabla/ataques. */
data class CardDetailUi(
    val card: Card,
    val number: Int,
    val name: String,
    val rarity: Rarity,
    val owned: Boolean,
    val count: Int,
    val cap: Int,
    val setCode: String,
    val imageEs: String?,
    val imageLarge: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Economía (Canon Fase 5). Pública para que los llamadores calculen coste/valor/playset.
// ─────────────────────────────────────────────────────────────────────────────

fun tierOf(r: Rarity): Int = when (r) {
    Rarity.COMMON -> 1
    Rarity.UNCOMMON -> 2
    Rarity.RARE -> 3
    Rarity.RARE_HOLO -> 4
    Rarity.DOUBLE_RARE -> 5
    Rarity.ULTRA_RARE -> 6
    Rarity.ILLUSTRATION_RARE -> 7
    Rarity.SPECIAL_ILLUSTRATION_RARE -> 8
    Rarity.HYPER_RARE -> 9
    Rarity.PROMO -> 1
}

fun playsetSize(card: Card): Int = when {
    ProfileRepository.isEnergyId(card.id.raw) -> ProfileRepository.ENERGY_CAP
    card is TrainerCard && card.kind.isAceSpec -> 1
    card is PokemonCard && card.mechanic == PokemonMechanic.Radiant -> 1
    else -> 4
}

fun craftCostOf(card: Card): Int = CraftingRules.craftCost(tierOf(card.rarity))

fun destroyValueOf(card: Card): Int = CraftingRules.recycleFichas(tierOf(card.rarity))

// ─────────────────────────────────────────────────────────────────────────────
// Hoja de detalle.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CardDetailSheet(
    card: CardDetailUi,
    allCards: List<CardDetailUi>,
    isFavorite: Boolean,
    isWished: Boolean,
    fichas: Int,
    onToggleFavorite: () -> Unit,
    onToggleWish: () -> Unit,
    onCraft: () -> Unit,
    onDestroy: () -> Unit,
    onOpenRelated: (CardDetailUi) -> Unit,
    onDismiss: () -> Unit,
) {
    var showLangs by remember(card) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFDCD8F0), Color(0xFFE9ECF4))))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DropPill("▦ 0")
                Spacer(Modifier.width(8.dp))
                Box(Modifier.noRippleClick { showLangs = !showLangs }) { DropPill("🌐 1") }
                Spacer(Modifier.weight(1f))
                Txt("•••", 16.sp, Muted, FontWeight.Black)
                Spacer(Modifier.width(14.dp))
                Txt(if (isFavorite) "★" else "☆", 20.sp, if (isFavorite) Color(0xFFE7B10A) else Muted, FontWeight.Black, Modifier.noRippleClick(onToggleFavorite))
                Spacer(Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.noRippleClick(onToggleWish)) {
                    Txt(if (isWished) "♥" else "♡", 20.sp, if (isWished) Color(0xFFEC5F94) else Muted, FontWeight.Black)
                    Txt("¡La quiero!", 9.sp, Muted, FontWeight.Bold)
                }
            }

            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth(0.96f).aspectRatio(0.72f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (card.owned) {
                        InteractiveHoloCard(
                            imageUrl = card.imageLarge, setCode = card.setCode, cardNumber = card.number,
                            rarity = card.rarity, contentDescription = card.name,
                            modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(12.dp), clip = false).clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().shadow(10.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF2A3550), Color(0xFF161E30)))),
                            contentAlignment = Alignment.Center,
                        ) { Txt("No la tienes", 13.sp, Color(0xCCFFFFFF), FontWeight.Black) }
                    }
                }
            }

            if (card.owned) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CountChip("▤", "${card.count}")
                    CountChip("▣", "0")
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
                            .border(1.5.dp, Accent, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Txt("✦", 14.sp, Accent, FontWeight.Black)
                        Txt("Obtener efecto visual", 12.sp, Ink, FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Txt(card.name, 24.sp, Ink, FontWeight.Black, Modifier.fillMaxWidth(), align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                RaritySymbol(card.rarity, height = 20.dp)
                Spacer(Modifier.width(8.dp))
                Txt(rarityEs(card.rarity), 13.sp, Muted, FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            CraftDestroyRow(card = card, fichas = fichas, onCraft = onCraft, onDestroy = onDestroy)
            Spacer(Modifier.height(14.dp))

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp),
            ) {
                DetailTable(card.card)
                (card.card as? PokemonCard)?.let { p ->
                    if (p.abilities.isNotEmpty() || p.attacks.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Txt("Ataques", 14.sp, Ink, FontWeight.Black)
                        }
                        Spacer(Modifier.height(12.dp))
                        AttacksSection(p)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Txt("Cartas relacionadas", 14.sp, Ink, FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                RelatedCards(card, allCards, onOpenRelated)
            }
            Spacer(Modifier.height(90.dp))
        }

        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).size(54.dp)
                .shadow(4.dp, CircleShape).clip(CircleShape).background(Panel).noRippleClick(onDismiss),
            contentAlignment = Alignment.Center,
        ) { Txt("✕", 20.sp, Muted, FontWeight.Black) }

        if (showLangs) LanguagePopup(ownedCount = card.count, onDismiss = { showLangs = false })
    }
}

/**
 * Muestra la [CardDetailSheet] a pantalla completa dentro de un [Dialog] (para abrirla
 * como overlay desde cualquier pantalla, p. ej. el editor de barajas con long-press).
 */
@Composable
fun CardDetailDialogSheet(
    card: CardDetailUi,
    allCards: List<CardDetailUi>,
    isFavorite: Boolean,
    isWished: Boolean,
    fichas: Int,
    onToggleFavorite: () -> Unit,
    onToggleWish: () -> Unit,
    onCraft: () -> Unit,
    onDestroy: () -> Unit,
    onOpenRelated: (CardDetailUi) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CardDetailSheet(
            card = card, allCards = allCards, isFavorite = isFavorite, isWished = isWished, fichas = fichas,
            onToggleFavorite = onToggleFavorite, onToggleWish = onToggleWish, onCraft = onCraft,
            onDestroy = onDestroy, onOpenRelated = onOpenRelated, onDismiss = onDismiss,
        )
    }
}

@Composable
private fun CraftDestroyRow(card: CardDetailUi, fichas: Int, onCraft: () -> Unit, onDestroy: () -> Unit) {
    val playset = playsetSize(card.card)
    val cost = craftCostOf(card.card)
    val value = destroyValueOf(card.card)
    val canCraft = card.count < playset && fichas >= cost
    val canDestroy = card.count > playset
    val isEnergy = ProfileRepository.isEnergyId(card.card.id.raw)
    var confirmDestroy by remember(card) { mutableStateOf(false) }
    val destroyColor = Color(0xFFE0564E)
    val floorLabel = if (isEnergy) "Colección" else "Playset"

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Txt("$floorLabel ${card.count.coerceAtMost(playset)}/$playset" + if (card.count > playset) "  ·  ${card.count - playset} excedente(s)" else "", 12.sp, Muted, FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionPill("Fabricar", "$cost 🧩", Accent, enabled = canCraft, onClick = onCraft)
            ActionPill("Destruir", "+$value 🧩", destroyColor, enabled = canDestroy, onClick = { confirmDestroy = true })
        }
    }

    if (confirmDestroy) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)).noRippleClick { confirmDestroy = false }, contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(28.dp).clip(RoundedCornerShape(20.dp)).background(Panel).padding(22.dp).noRippleClick { },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt("¿Destruir 1 copia?", 18.sp, Ink, FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Txt("Recibirás $value Fichas. Conservarás " + (if (isEnergy) "$playset energías" else "tu playset ($playset)") + ". No se puede deshacer.", 13.sp, Muted, FontWeight.Medium, Modifier.fillMaxWidth(), align = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).border(1.5.dp, Muted, RoundedCornerShape(12.dp))
                            .noRippleClick { confirmDestroy = false }.padding(horizontal = 20.dp, vertical = 10.dp),
                    ) { Txt("Cancelar", 13.sp, Ink, FontWeight.Bold) }
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(destroyColor)
                            .noRippleClick { confirmDestroy = false; onDestroy() }.padding(horizontal = 20.dp, vertical = 10.dp),
                    ) { Txt("Destruir", 13.sp, Color.White, FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, sub: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) accent else accent.copy(alpha = 0.28f)
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(bg)
            .then(if (enabled) Modifier.noRippleClick(onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(label, 14.sp, Color.White, FontWeight.Black)
        Txt(sub, 11.sp, Color(0xE6FFFFFF), FontWeight.Bold)
    }
}

@Composable
private fun DropPill(text: String) {
    Row(
        Modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(text, 13.sp, Ink, FontWeight.Black)
        Txt("▾", 10.sp, Muted, FontWeight.Black)
    }
}

@Composable
private fun CountChip(glyph: String, value: String) {
    Row(
        Modifier.shadow(1.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(glyph, 13.sp, Muted, FontWeight.Black)
        Txt(value, 14.sp, Ink, FontWeight.Black)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tabla de datos + ataques.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailTable(card: Card) {
    val p = card as? PokemonCard
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TableRow("N.º", "%04d".format(numberOf(card.id.raw)))
        if (p != null) {
            TableRow("Pokémon", stageLabel(p.stage))
            TableEnergyRow("Tipo", p.types)
            TableRow("PS", "${p.hp}")
            TableEnergyRow("Debilidad", p.weaknesses.map { it.type }, suffix = p.weaknesses.firstOrNull()?.value)
            TableEnergyRow("Coste de Retirada", p.retreatCost)
        }
        TableRow("Serie", seriesEs(card.set.series))
        TableRow("Expansión", expansionEs(card.set.name.es))
    }
}

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

@Composable
private fun EnergyIcon(t: EnergyType, size: Dp = 18.dp) {
    val res = energyIconRes(t)
    if (res != null) {
        Image(painterResource(res), energyEs(t), Modifier.size(size))
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Color(0xFFE9A3D6)), contentAlignment = Alignment.Center) {
            Txt("H", (size.value * 0.55f).sp, Color.White, FontWeight.Black)
        }
    }
}

@Composable
private fun AttacksSection(p: PokemonCard) {
    if (p.abilities.isEmpty() && p.attacks.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        p.abilities.forEach { ability -> AbilityRow(ability) }
        p.attacks.forEach { atk -> AttackRow(atk) }
    }
}

@Composable
private fun AbilityRow(ability: Ability) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgTop).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE04A6B)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Txt("Habilidad", 10.sp, Color.White, FontWeight.Black)
            }
            Txt(ability.name.es.ifBlank { ability.name.en }, 14.sp, Ink, FontWeight.Black)
        }
        val text = ability.text.es.ifBlank { ability.text.en }
        if (text.isNotBlank()) { Spacer(Modifier.height(6.dp)); Txt(text, 12.sp, Muted, FontWeight.SemiBold, align = TextAlign.Start) }
    }
}

@Composable
private fun AttackRow(atk: Attack) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgTop).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (atk.cost.isEmpty()) Txt("—", 14.sp, Muted, FontWeight.Black)
                else atk.cost.forEach { EnergyIcon(it, 20.dp) }
            }
            Spacer(Modifier.width(10.dp))
            Txt(atk.name.es.ifBlank { atk.name.en }, 15.sp, Ink, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Start)
            val dmg = (atk.baseDamage as? Damage.Fixed)?.value?.takeIf { it > 0 }
            if (dmg != null) Txt("$dmg", 18.sp, Ink, FontWeight.Black)
        }
        val text = atk.text.es.ifBlank { atk.text.en }
        if (text.isNotBlank()) { Spacer(Modifier.height(6.dp)); Txt(text, 12.sp, Muted, FontWeight.SemiBold, align = TextAlign.Start) }
    }
}

@Composable
private fun TableRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, 13.sp, Muted, FontWeight.Bold, Modifier.weight(1f), align = TextAlign.Start)
        Txt(value, 13.sp, Ink, FontWeight.Black)
    }
}

@Composable
private fun TableEnergyRow(label: String, types: List<EnergyType>, suffix: String? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BgTop).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, 13.sp, Muted, FontWeight.Bold, Modifier.weight(1f), align = TextAlign.Start)
        if (types.isEmpty()) {
            Txt("—", 13.sp, Ink, FontWeight.Black)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                types.forEach { EnergyIcon(it, 20.dp) }
                if (suffix != null) Txt(" $suffix", 13.sp, Ink, FontWeight.Black)
            }
        }
    }
}

@Composable
private fun RelatedCards(card: CardDetailUi, all: List<CardDetailUi>, onOpen: (CardDetailUi) -> Unit) {
    val related = remember(card) { relatedFamily(card, all) }
    if (related.isEmpty()) {
        Txt("—", 13.sp, Muted, FontWeight.Bold, Modifier.fillMaxWidth(), align = TextAlign.Center)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        related.take(3).forEach { r ->
            Box(Modifier.weight(1f).aspectRatio(0.72f).clip(RoundedCornerShape(8.dp)).noRippleClick { onOpen(r) }) {
                if (r.owned) {
                    HoloCardImage(
                        imageUrl = r.imageEs ?: r.imageLarge, setCode = r.setCode, cardNumber = r.number,
                        rarity = r.rarity, contentDescription = r.name, enabled = false, modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = r.imageEs ?: r.imageLarge, contentDescription = r.name,
                            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                        )
                        Box(Modifier.fillMaxSize().background(Color(0x99B0B6C2)))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePopup(ownedCount: Int, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x22000000)).noRippleClick(onDismiss), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.padding(top = 60.dp).fillMaxWidth(0.7f).shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)).background(Panel).noRippleClick { }.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("Vista previa", 12.sp, Muted, FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val langs = listOf("ING" to ownedCount, "ES-ES" to 0, "FRA" to 0, "ALE" to 0, "ITA" to 0, "PT-BR" to 0)
            langs.forEachIndexed { i, (lang, n) ->
                val active = i == 0
                Row(
                    Modifier.fillMaxWidth(0.85f).padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(50)).background(if (active) Ink else BgTop)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(lang, 14.sp, if (active) Color.White else Muted, FontWeight.Black, Modifier.weight(1f), align = TextAlign.Start)
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (active) Color(0x33FFFFFF) else Panel).padding(horizontal = 10.dp, vertical = 2.dp)) {
                        Txt("$n", 12.sp, if (active) Color.White else Muted, FontWeight.Black)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lógica de familia / etiquetas / rareza.
// ─────────────────────────────────────────────────────────────────────────────

private fun numberOf(id: String): Int = id.substringAfterLast('-').toIntOrNull() ?: 0

private fun relatedFamily(card: CardDetailUi, all: List<CardDetailUi>): List<CardDetailUi> {
    val p = card.card as? PokemonCard ?: return emptyList()
    val nameEn = p.name.en
    val evo = p.evolvesFrom
    return all.filter { d ->
        val pc = d.card as? PokemonCard ?: return@filter false
        pc.name.en == nameEn || pc.evolvesFrom == nameEn || (evo != null && pc.name.en == evo)
    }.distinctBy { (it.card as? PokemonCard)?.name?.en ?: it.card.id.raw }
        .sortedBy { stageOrder((it.card as? PokemonCard)?.stage) }
}

private fun stageOrder(stage: Stage?): Int = when (stage) {
    Stage.Basic -> 0; Stage.Stage1 -> 1; Stage.Stage2 -> 2; else -> 3
}

private fun stageLabel(stage: Stage): String = when (stage) {
    Stage.Basic -> "Básico"; Stage.Stage1 -> "Fase 1"; Stage.Stage2 -> "Fase 2"; Stage.BabyRestored -> "Restaurado"
}

private fun seriesEs(series: String): String = when (series) {
    "Scarlet & Violet" -> "Escarlata y Púrpura"
    "Mega Evolution" -> "Megaevolución"
    "Sword & Shield" -> "Espada y Escudo"
    "Sun & Moon" -> "Sol y Luna"
    "XY" -> "XY"
    "Black & White" -> "Negro y Blanco"
    else -> series
}

private fun expansionEs(name: String): String = when (name) {
    "151" -> "151"
    "Scarlet & Violet" -> "Escarlata y Púrpura"
    "Paldea Evolved" -> "Evoluciones en Paldea"
    "Obsidian Flames" -> "Llamas Obsidianas"
    "Paradox Rift" -> "Brecha Paradójica"
    "Temporal Forces" -> "Fuerzas Temporales"
    "Twilight Masquerade" -> "Mascarada Crepuscular"
    "Shrouded Fable" -> "Fábula Sombría"
    "Stellar Crown" -> "Corona Astral"
    "Surging Sparks" -> "Chispas Fulgurantes"
    "Destined Rivals" -> "Rivales Predestinados"
    "Mega Evolution" -> "Megaevolución"
    else -> name
}

private fun energyEs(t: EnergyType?): String = when (t) {
    EnergyType.GRASS -> "Planta"; EnergyType.FIRE -> "Fuego"; EnergyType.WATER -> "Agua"
    EnergyType.LIGHTNING -> "Rayo"; EnergyType.PSYCHIC -> "Psíquico"; EnergyType.FIGHTING -> "Lucha"
    EnergyType.DARKNESS -> "Oscuro"; EnergyType.METAL -> "Metálico"; EnergyType.FAIRY -> "Hada"
    EnergyType.DRAGON -> "Dragón"; EnergyType.COLORLESS -> "Incoloro"; null -> "—"
}

private fun rarityRes(r: Rarity): Int = when (r) {
    Rarity.COMMON -> R.drawable.rarity_common
    Rarity.UNCOMMON -> R.drawable.rarity_uncommon
    Rarity.RARE -> R.drawable.rarity_rare
    Rarity.RARE_HOLO -> R.drawable.rarity_rare
    Rarity.DOUBLE_RARE -> R.drawable.rarity_double_rare
    Rarity.ULTRA_RARE -> R.drawable.rarity_ultra_rare
    Rarity.ILLUSTRATION_RARE -> R.drawable.rarity_illustration_rare
    Rarity.SPECIAL_ILLUSTRATION_RARE -> R.drawable.rarity_special_illustration_rare
    Rarity.HYPER_RARE -> R.drawable.rarity_hyper_rare
    Rarity.PROMO -> R.drawable.rarity_rare
}

private fun rarityEs(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"
    Rarity.UNCOMMON -> "Poco común"
    Rarity.RARE -> "Rara"
    Rarity.RARE_HOLO -> "Rara holográfica"
    Rarity.DOUBLE_RARE -> "Doble rara"
    Rarity.ULTRA_RARE -> "Ultra rara"
    Rarity.ILLUSTRATION_RARE -> "Rara ilustración"
    Rarity.SPECIAL_ILLUSTRATION_RARE -> "Rara ilustración especial"
    Rarity.HYPER_RARE -> "Hiperrara"
    Rarity.PROMO -> "Promocional"
}

@Composable
private fun RaritySymbol(r: Rarity, height: Dp = 16.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(rarityRes(r)), contentDescription = rarityEs(r),
        contentScale = ContentScale.Fit, modifier = modifier.height(height),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers de bajo nivel.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Txt(
    text: String,
    size: TextUnit,
    color: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = align),
    )
}

private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
