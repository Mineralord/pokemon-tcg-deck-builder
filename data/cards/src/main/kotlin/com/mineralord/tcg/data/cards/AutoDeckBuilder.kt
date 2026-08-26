package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.random.Random

/**
 * Autocreación INTELIGENTE de mazos. Función pura (testeable) que, dado lo que el jugador
 * posee y 1-2 tipos de foco, construye un mazo legal de 60 cartas siguiendo el rulebook
 * (`docs/canon/rulebook.txt`) y los principios competitivos (`docs/canon/construccion-de-mazos.md`).
 *
 * Reglas de lógica de construcción (permanentes, válidas para cartas futuras):
 *  1. **60 exactas, máx. 4 por nombre** (salvo Energía Básica), **≥1 Básico**.
 *  2. **Sólo cartas poseídas**: nunca se incluyen más copias (incluida ENERGÍA) de las que el
 *     jugador tiene en su colección. La energía básica no tiene límite de 4, pero SÍ está
 *     limitada por lo poseído.
 *  3. **Afinidad de tipo de los Entrenadores**: un Entrenador cuyo texto sólo beneficia a un
 *     tipo (p.ej. Melo→Fuego, Generador Eléctrico→Rayo) se EXCLUYE si ese tipo no está en el
 *     foco. La afinidad se DERIVA del texto de la carta (símbolos {R}/{L}… o "Fire Energy",
 *     "Lightning Pokémon"…), así funciona automáticamente al añadir cartas nuevas.
 *  4. **Incoloro sólo de relleno**: si el jugador elige tipos elementales, el mazo se llena con
 *     Pokémon de esos tipos; los Pokémon Incoloros sólo entran si NO hay suficientes del foco.
 *     Si el jugador elige COLORLESS como foco, los Incoloros son de primera clase.
 *  5. **1-2 insignia + acompañantes**, líneas de evolución por ratio (4-3-2/4-3/3-2/2-2),
 *     motor de Entrenadores (robo/búsqueda/cambio, Regla de 4), energía baja del foco.
 *  6. **VARIEDAD**: rota entre perfiles de ratio y varía la insignia con [rng].
 */
object AutoDeckBuilder {

    data class Profile(val name: String, val pokemon: IntRange, val trainer: IntRange, val energy: IntRange)

    val PROFILES = listOf(
        Profile("Competitivo", 12..18, 30..40, 8..12),
        Profile("Equilibrado", 18..22, 18..22, 18..22),
        Profile("Intermedio", 15..20, 24..32, 10..15),
    )

    private val STAPLE_PRIORITY = listOf(
        "Professor's Research", "Professor", "Iono", "Boss's Orders", "Ultra Ball",
        "Nest Ball", "Great Ball", "Poké Ball", "Pokémon Communication", "Rare Candy",
        "Switch", "Ordinary Rod", "Super Rod", "Earthen Vessel", "Buddy-Buddy Poffin",
    )

    private const val DECK_SIZE = 60
    private const val MAX_COPIES = 4

    fun build(
        owned: Map<String, Int>,
        all: List<Card>,
        focus: Set<EnergyType>,
        rng: Random = Random.Default,
    ): List<DeckEntry> {
        val byId: Map<String, Card> = all.associateBy { it.id.raw }
        val ownedCards: List<Pair<Card, Int>> = owned.mapNotNull { (id, n) ->
            if (n <= 0) null else byId[id]?.let { it to n }
        }
        val ownedCount: Map<String, Int> = ownedCards.associate { it.first.id.raw to it.second }

        val focusTypes: List<EnergyType> = when {
            focus.isNotEmpty() -> focus.toList()
            else -> inferFocus(ownedCards.map { it.first })
        }.ifEmpty { listOf(EnergyType.COLORLESS) }
        val wantColorless = EnergyType.COLORLESS in focusTypes

        val ownedPokemon = ownedCards.map { it.first }.filterIsInstance<PokemonCard>()
        val ownedByNameEn: Map<String, PokemonCard> = ownedPokemon
            .sortedByDescending { ownedCount[it.id.raw] ?: 0 }
            .associateBy { it.name.en }

        val profile = PROFILES[rng.nextInt(PROFILES.size)]
        val energyTarget = profile.energy.random(rng)
        val pokemonTarget = profile.pokemon.random(rng)

        val counts = LinkedHashMap<String, Int>()
        val nameCount = HashMap<String, Int>()

        /** Añade hasta [want] copias respetando: poseídas, regla de 4 (salvo energía básica) y 60. */
        fun addCapped(card: Card, want: Int): Int {
            if (want <= 0) return 0
            val ownedRem = (ownedCount[card.id.raw] ?: 0) - (counts[card.id.raw] ?: 0)
            val nameRem = if (card is BasicEnergy) Int.MAX_VALUE else MAX_COPIES - (nameCount[card.name.en] ?: 0)
            val sizeRem = DECK_SIZE - counts.values.sum()
            val n = minOf(want, ownedRem, nameRem, sizeRem)
            if (n <= 0) return 0
            counts[card.id.raw] = (counts[card.id.raw] ?: 0) + n
            if (card !is BasicEnergy) nameCount[card.name.en] = (nameCount[card.name.en] ?: 0) + n
            return n
        }
        fun total() = counts.values.sum()
        fun pokemonCount() = counts.entries.filter { byId[it.key] is PokemonCard }.sumOf { it.value }

        // ---------- 1) POKÉMON ----------
        val onFocus: (PokemonCard) -> Boolean = { p -> p.types.any { it in focusTypes } }

        val flagshipCandidates = ownedPokemon
            .filter { onFocus(it) && it.attacks.isNotEmpty() }
            .distinctBy { it.name.en }
            .sortedByDescending { attackerScore(it) }

        val topPool = flagshipCandidates.take(6).shuffled(rng)
        val flagshipCount = if (topPool.size >= 2 && rng.nextBoolean()) 2 else 1
        val flagships = mutableListOf<PokemonCard>()
        val usedRoots = HashSet<String>()
        for (c in topPool) {
            val root = evolutionRoot(c, ownedByNameEn)?.name?.en ?: c.name.en
            if (root in usedRoots) continue
            flagships += c; usedRoots += root
            if (flagships.size >= flagshipCount) break
        }
        for (top in flagships) {
            for ((card, n) in buildLine(top, ownedByNameEn, ownedCount, rng)) addCapped(card, n)
        }

        // Acompañantes DEL FOCO primero (Básicos de apoyo con ataque o habilidad).
        val focusCompanions = ownedPokemon
            .filter { it.isBasic && it.name.en !in usedRoots && onFocus(it) }
            .filter { it.attacks.isNotEmpty() || it.abilities.isNotEmpty() }
            .distinctBy { it.name.en }
            .sortedByDescending { supportScore(it) }
        for (c in focusCompanions) {
            if (pokemonCount() >= pokemonTarget) break
            addCapped(c, 2)
        }

        // Incoloro SÓLO como relleno si el foco es elemental y aún faltan Pokémon.
        if (!wantColorless && pokemonCount() < pokemonTarget) {
            val colorlessFill = ownedPokemon
                .filter { it.isBasic && it.name.en !in usedRoots && it.types.all { t -> t == EnergyType.COLORLESS } }
                .filter { it.attacks.isNotEmpty() || it.abilities.isNotEmpty() }
                .distinctBy { it.name.en }
                .sortedByDescending { supportScore(it) }
            for (c in colorlessFill) {
                if (pokemonCount() >= pokemonTarget) break
                addCapped(c, 2)
            }
        }

        // ---------- 2) ENERGÍA (del foco, respetando lo poseído) ----------
        // Mejor carta de energía básica poseída por tipo de foco.
        val energyCardByType: Map<EnergyType, BasicEnergy> = focusTypes.mapNotNull { t ->
            all.filterIsInstance<BasicEnergy>().filter { it.type == t }
                .maxByOrNull { ownedCount[it.id.raw] ?: 0 }
                ?.takeIf { (ownedCount[it.id.raw] ?: 0) > 0 }
                ?.let { t to it }
        }.toMap()
        val energyTypes = energyCardByType.keys.toList()
        if (energyTypes.isNotEmpty()) {
            val weights = typeWeights(counts, byId, energyTypes)
            for ((t, want) in distribute(energyTarget, energyTypes, weights, rng)) {
                energyCardByType[t]?.let { addCapped(it, want) }
            }
        }

        // ---------- 3) ENTRENADORES (excluyendo afinidad fuera del foco) ----------
        val ownedTrainers = ownedCards.map { it.first }.filterIsInstance<TrainerCard>()
            .distinctBy { it.name.en }
            .filter { affinityAllowed(it, focusTypes) }
            .sortedWith(compareBy({ stapleRank(it) }, { trainerKindRank(it) }, { it.name.es.lowercase() }))
        for (t in ownedTrainers) {
            if (total() >= DECK_SIZE) break
            if (stapleRank(t) == Int.MAX_VALUE && t.kind is TrainerKind.Stadium &&
                stadiumCount(counts, byId) >= 2
            ) continue
            val want = if (stapleRank(t) < STAPLE_PRIORITY.size) 4 else 2
            addCapped(t, want)
        }

        // ---------- 4) RELLENO HASTA 60 (sólo con cartas poseídas) ----------
        // Orden: más Entrenadores → copias extra de Pokémon del mazo → más energía del foco →
        // cualquier otra energía poseída. Nunca inventa copias.
        if (total() < DECK_SIZE) {
            val fillers = ArrayList<Card>()
            fillers += ownedTrainers
            fillers += counts.keys.mapNotNull { byId[it] as? PokemonCard }
            fillers += energyTypes.mapNotNull { energyCardByType[it] as Card? }
            fillers += ownedCards.map { it.first }.filterIsInstance<BasicEnergy>()
            var progressed = true
            while (total() < DECK_SIZE && progressed) {
                progressed = false
                for (c in fillers) {
                    if (total() >= DECK_SIZE) break
                    if (addCapped(c, 1) > 0) progressed = true
                }
            }
        }

        return counts.filter { it.value > 0 }.map { (id, n) -> DeckEntry(CardId(id), n) }
    }

    // ---------------- afinidad de tipo de Entrenadores (data-driven) ----------------

    private val SYMBOL_TYPE = mapOf(
        "{G}" to EnergyType.GRASS, "{R}" to EnergyType.FIRE, "{W}" to EnergyType.WATER,
        "{L}" to EnergyType.LIGHTNING, "{P}" to EnergyType.PSYCHIC, "{F}" to EnergyType.FIGHTING,
        "{D}" to EnergyType.DARKNESS, "{M}" to EnergyType.METAL, "{N}" to EnergyType.DRAGON,
        "{Y}" to EnergyType.FAIRY,
    )
    private val EN_TYPE = mapOf(
        "Grass" to EnergyType.GRASS, "Fire" to EnergyType.FIRE, "Water" to EnergyType.WATER,
        "Lightning" to EnergyType.LIGHTNING, "Psychic" to EnergyType.PSYCHIC, "Fighting" to EnergyType.FIGHTING,
        "Darkness" to EnergyType.DARKNESS, "Metal" to EnergyType.METAL, "Dragon" to EnergyType.DRAGON,
        "Fairy" to EnergyType.FAIRY,
    )

    /**
     * Tipos a los que el Entrenador está "atado" según su texto (símbolos {R}… o
     * "<Tipo> Energy/Pokémon"). Ignora COLORLESS (genérico). Vacío = genérico/sin restricción.
     */
    private fun trainerAffinity(t: TrainerCard): Set<EnergyType> {
        val es = t.text.es
        val en = t.text.en
        val found = HashSet<EnergyType>()
        for ((sym, ty) in SYMBOL_TYPE) if (es.contains(sym) || en.contains(sym)) found += ty
        for ((word, ty) in EN_TYPE) {
            if (Regex("\\b$word\\s+(Energy|Pok)", RegexOption.IGNORE_CASE).containsMatchIn(en)) found += ty
        }
        found.remove(EnergyType.COLORLESS)
        return found
    }

    /** Permitido si es genérico o si alguna de sus afinidades está en el foco. */
    private fun affinityAllowed(t: TrainerCard, focus: List<EnergyType>): Boolean {
        val aff = trainerAffinity(t)
        return aff.isEmpty() || aff.any { it in focus }
    }

    // ---------------- otros helpers ----------------

    private fun inferFocus(cards: List<Card>): List<EnergyType> {
        val w = HashMap<EnergyType, Int>()
        for (c in cards) if (c is PokemonCard) for (t in c.types) if (t != EnergyType.COLORLESS) {
            w[t] = (w[t] ?: 0) + 1
        }
        return w.entries.sortedByDescending { it.value }.take(2).map { it.key }
    }

    private fun attackerScore(p: PokemonCard): Int {
        val stageBonus = when (p.stage) { Stage.Stage2 -> 40; Stage.Stage1 -> 20; else -> 0 }
        val exBonus = if (p.mechanic.prizesWhenKO >= 2) 50 else 0
        val dmg = p.attacks.maxOfOrNull { (it.baseDamage as? Damage.Fixed)?.value ?: (it.convertedCost * 20) } ?: 0
        return p.hp + stageBonus + exBonus + dmg
    }

    private fun supportScore(p: PokemonCard): Int {
        val abilityBonus = if (p.abilities.isNotEmpty()) 40 else 0
        val exBonus = if (p.mechanic.prizesWhenKO >= 2) 20 else 0
        val lowRetreat = if (p.retreatCost.size <= 1) 15 else 0
        return p.hp / 10 + abilityBonus + exBonus + lowRetreat
    }

    private fun evolutionRoot(p: PokemonCard, ownedByNameEn: Map<String, PokemonCard>): PokemonCard? {
        var cur = p; var guard = 0
        while (cur.evolvesFrom != null && guard++ < 4) {
            cur = ownedByNameEn[cur.evolvesFrom] ?: return if (cur.isBasic) cur else null
        }
        return cur
    }

    private fun buildLine(
        top: PokemonCard,
        ownedByNameEn: Map<String, PokemonCard>,
        ownedCount: Map<String, Int>,
        rng: Random,
    ): List<Pair<PokemonCard, Int>> {
        val chain = ArrayList<PokemonCard>()
        var cur: PokemonCard? = top; var guard = 0
        while (cur != null && guard++ < 4) {
            chain += cur
            val from = cur.evolvesFrom ?: break
            cur = ownedByNameEn[from] ?: return emptyList()
        }
        chain.reverse()
        val ratios: List<Int> = when (chain.size) {
            1 -> listOf(if (top.mechanic.prizesWhenKO >= 2) 2 else 3)
            2 -> if (rng.nextBoolean()) listOf(4, 3) else listOf(3, 2)
            else -> if (rng.nextBoolean()) listOf(4, 2, 3) else listOf(4, 3, 2)
        }
        return chain.mapIndexed { i, card ->
            card to minOf(ratios.getOrElse(i) { 1 }, ownedCount[card.id.raw] ?: 0, MAX_COPIES)
        }.filter { it.second > 0 }
    }

    private fun typeWeights(counts: Map<String, Int>, byId: Map<String, Card>, types: List<EnergyType>): Map<EnergyType, Int> {
        val w = types.associateWith { 1 }.toMutableMap()
        for ((id, n) in counts) {
            val p = byId[id] as? PokemonCard ?: continue
            for (t in p.types) if (t in w) w[t] = (w[t] ?: 0) + n
        }
        return w
    }

    private fun distribute(total: Int, types: List<EnergyType>, weights: Map<EnergyType, Int>, rng: Random): Map<EnergyType, Int> {
        if (types.size == 1) return mapOf(types[0] to total)
        val sum = types.sumOf { weights[it] ?: 1 }.coerceAtLeast(1)
        val base = types.associateWith { (total * (weights[it] ?: 1)) / sum }.toMutableMap()
        var rem = total - base.values.sum()
        val order = types.sortedByDescending { weights[it] ?: 1 }
        var i = 0
        while (rem > 0) { base[order[i % order.size]] = (base[order[i % order.size]] ?: 0) + 1; rem--; i++ }
        return base
    }

    private fun stapleRank(t: TrainerCard): Int {
        val idx = STAPLE_PRIORITY.indexOfFirst { t.name.en.startsWith(it, ignoreCase = true) }
        return if (idx >= 0) idx else Int.MAX_VALUE
    }

    private fun trainerKindRank(t: TrainerCard): Int = when (t.kind) {
        is TrainerKind.Supporter -> 0
        is TrainerKind.Item -> 1
        is TrainerKind.Tool -> 2
        is TrainerKind.Stadium -> 3
    }

    private fun stadiumCount(counts: Map<String, Int>, byId: Map<String, Card>): Int =
        counts.entries.filter { (byId[it.key] as? TrainerCard)?.kind is TrainerKind.Stadium }.sumOf { it.value }
}
