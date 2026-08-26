package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.Supertype
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import kotlin.random.Random

/**
 * Autocreación INTELIGENTE de mazos. Función pura (testeable) que, dado lo que el jugador
 * posee y 1-2 tipos de foco, construye un mazo legal de 60 cartas siguiendo el rulebook
 * (`docs/canon/rulebook.txt`) y los principios competitivos (`docs/canon/construccion-de-mazos.md`):
 *
 *  - 60 cartas exactas, máx. 4 por nombre (salvo Energía Básica), ≥1 Básico.
 *  - 1-2 Pokémon insignia (atacantes) + acompañantes de apoyo; líneas de evolución completas
 *    (4-3-2 / 4-3 / 3-2 / 2-2) limitadas por lo poseído.
 *  - Motor de Entrenadores priorizando robo/búsqueda/cambio (Regla de 4).
 *  - Energía básica del foco repartida por la distribución de tipos de los atacantes.
 *  - VARIEDAD: rota entre perfiles de ratio y varía la insignia con [rng].
 */
object AutoDeckBuilder {

    /** Perfil de ratios (rangos); el total siempre cuadra a 60. */
    data class Profile(
        val name: String,
        val pokemon: IntRange,
        val trainer: IntRange,
        val energy: IntRange,
    )

    val PROFILES = listOf(
        Profile("Competitivo", 12..18, 30..40, 8..12),
        Profile("Equilibrado", 18..22, 18..22, 18..22),
        Profile("Intermedio", 15..20, 24..32, 10..15),
    )

    /** Nombres (en inglés, coincidencia por prefijo) de Entrenadores de alto valor de motor. */
    private val STAPLE_PRIORITY = listOf(
        "Professor's Research", "Professor", "Iono", "Boss's Orders", "Ultra Ball",
        "Nest Ball", "Great Ball", "Poké Ball", "Pokémon Communication", "Rare Candy",
        "Switch", "Ordinary Rod", "Super Rod", "Earthen Vessel", "Buddy-Buddy Poffin",
    )

    private const val DECK_SIZE = 60
    private const val MAX_COPIES = 4

    /**
     * Construye el mazo. [owned] = cardId→copias poseídas. [all] = catálogo (para líneas de
     * evolución y energía básica). [focus] = 1-2 tipos elegidos. Devuelve las entradas del mazo.
     */
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

        // Tipos de foco efectivos: si vacío, inferir de los Pokémon poseídos.
        val focusTypes: List<EnergyType> = when {
            focus.isNotEmpty() -> focus.toList()
            else -> inferFocus(ownedCards.map { it.first })
        }.ifEmpty { listOf(EnergyType.COLORLESS) }

        val ownedPokemon = ownedCards.filter { it.first is PokemonCard }
            .map { it.first as PokemonCard to it.second }
        val ownedByNameEn: Map<String, PokemonCard> = ownedPokemon
            .sortedByDescending { it.second }
            .associate { it.first.name.en to it.first }

        // --- Perfil (variedad) ---
        val profile = PROFILES[rng.nextInt(PROFILES.size)]
        val energyTarget = profile.energy.random(rng)
        val pokemonTarget = profile.pokemon.random(rng)

        // Contador acumulado del mazo (por cardId) y por nombre (para la regla de 4).
        val counts = LinkedHashMap<String, Int>()
        val nameCount = HashMap<String, Int>()
        fun canAddName(card: Card): Boolean =
            card is BasicEnergy || (nameCount[card.name.en] ?: 0) < MAX_COPIES
        fun add(card: Card, n: Int) {
            if (n <= 0) return
            counts[card.id.raw] = (counts[card.id.raw] ?: 0) + n
            if (card !is BasicEnergy) nameCount[card.name.en] = (nameCount[card.name.en] ?: 0) + n
        }

        // ---------- 1) POKÉMON ----------
        // Candidatos a insignia: atacantes del foco, mejores primero, con algo de azar.
        val flagshipCandidates = ownedPokemon
            .map { it.first }
            .filter { it.types.any { t -> t in focusTypes } && it.attacks.isNotEmpty() }
            .distinctBy { it.name.en }
            .sortedByDescending { attackerScore(it) }

        // Elige 1-2 familias insignia (raíces distintas) barajando entre los mejores.
        val topPool = flagshipCandidates.take(6).shuffled(rng)
        val flagshipCount = if (topPool.size >= 2 && rng.nextBoolean()) 2 else 1
        val flagships = mutableListOf<PokemonCard>()
        val usedRoots = HashSet<String>()
        for (c in topPool) {
            val root = evolutionRoot(c, ownedByNameEn)?.name?.en ?: c.name.en
            if (root in usedRoots) continue
            flagships += c
            usedRoots += root
            if (flagships.size >= flagshipCount) break
        }

        // Construye la línea de cada insignia con ratios por fase.
        for (top in flagships) {
            val line = buildLine(top, ownedByNameEn, ownedCount, rng)
            for ((card, n) in line) {
                val allowed = minOf(n, MAX_COPIES - (nameCount[card.name.en] ?: 0), ownedCount[card.id.raw] ?: 0)
                add(card, allowed)
            }
        }

        // Acompañantes: Básicos de apoyo (foco/incoloro) con ataque o habilidad, hasta el objetivo.
        val companions = ownedPokemon.map { it.first }
            .filter { it.isBasic && (it.name.en !in usedRoots) }
            .filter { it.types.any { t -> t in focusTypes } || it.types.contains(EnergyType.COLORLESS) }
            .filter { it.attacks.isNotEmpty() || it.abilities.isNotEmpty() }
            .distinctBy { it.name.en }
            .sortedByDescending { supportScore(it) }
        var ci = 0
        while (pokemonCount(counts, byId) < pokemonTarget && ci < companions.size) {
            val c = companions[ci]; ci++
            if (!canAddName(c)) continue
            val n = minOf(2, ownedCount[c.id.raw] ?: 0)
            add(c, n)
        }

        // ---------- 2) ENERGÍA ----------
        // Reparte energyTarget entre los tipos de foco que tengan Energía Básica en el catálogo,
        // ponderando por la presencia de cada tipo entre los Pokémon ya incluidos.
        val basicEnergyByType: Map<EnergyType, BasicEnergy> = focusTypes.mapNotNull { t ->
            (all.firstOrNull { it is BasicEnergy && it.type == t } as? BasicEnergy)?.let { t to it }
        }.toMap()
        val energyTypes = basicEnergyByType.keys.toList().ifEmpty {
            // Sin energía básica del foco → usa los tipos de los atacantes que sí la tengan.
            (all.filterIsInstance<BasicEnergy>().map { it.type }.distinct()).take(1)
        }
        if (energyTypes.isNotEmpty()) {
            val weights = typeWeights(counts, byId, energyTypes)
            val split = distribute(energyTarget, energyTypes, weights, rng)
            for ((t, n) in split) {
                val e = basicEnergyByType[t] ?: (all.firstOrNull { it is BasicEnergy && it.type == t } as? BasicEnergy)
                if (e != null) add(e, n)
            }
        }

        // ---------- 3) ENTRENADORES ----------
        val trainerTarget = DECK_SIZE - total(counts)
        if (trainerTarget > 0) {
            val ownedTrainers = ownedCards.map { it.first }.filterIsInstance<TrainerCard>()
                .distinctBy { it.name.en }
            // Prioridad: staples de motor primero (en orden), luego resto por tipo.
            val ranked = ownedTrainers.sortedWith(
                compareBy(
                    { stapleRank(it) },
                    { trainerKindRank(it) },
                    { it.name.es.lowercase() },
                ),
            )
            var ti = 0
            while (total(counts) < DECK_SIZE && ti < ranked.size) {
                val t = ranked[ti]; ti++
                if (stapleRank(t) == Int.MAX_VALUE && t.kind is TrainerKind.Stadium &&
                    stadiumCount(counts, byId) >= 2
                ) continue
                val want = if (stapleRank(t) < STAPLE_PRIORITY.size) 4 else 2
                val n = minOf(want, MAX_COPIES, ownedCount[t.id.raw] ?: 0, DECK_SIZE - total(counts))
                add(t, n)
            }
        }

        // ---------- 4) RECONCILIAR A 60 EXACTAS ----------
        // El hueco restante se cubre con energía básica del foco (siempre disponible).
        var deficit = DECK_SIZE - total(counts)
        if (deficit > 0) {
            val fillType = energyTypes.firstOrNull() ?: EnergyType.COLORLESS
            val e = (all.firstOrNull { it is BasicEnergy && it.type == fillType } as? BasicEnergy)
                ?: all.filterIsInstance<BasicEnergy>().firstOrNull()
            if (e != null) add(e, deficit) else deficit = trimToSize(counts) // sin energía: recorta
        } else if (deficit < 0) {
            trimExcess(counts, -deficit)
        }

        return counts.filter { it.value > 0 }.map { (id, n) -> DeckEntry(CardId(id), n) }
    }

    // ---------------- helpers ----------------

    private fun inferFocus(cards: List<Card>): List<EnergyType> {
        val w = HashMap<EnergyType, Int>()
        for (c in cards) if (c is PokemonCard) for (t in c.types) if (t != EnergyType.COLORLESS) {
            w[t] = (w[t] ?: 0) + 1
        }
        return w.entries.sortedByDescending { it.value }.take(2).map { it.key }
    }

    private fun attackerScore(p: PokemonCard): Int {
        val stageBonus = when (p.stage) {
            Stage.Stage2 -> 40; Stage.Stage1 -> 20; else -> 0
        }
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

    /** Raíz Básica de la línea de evolución de [p] (siguiendo evolvesFrom entre lo poseído). */
    private fun evolutionRoot(p: PokemonCard, ownedByNameEn: Map<String, PokemonCard>): PokemonCard? {
        var cur: PokemonCard = p
        var guard = 0
        while (cur.evolvesFrom != null && guard++ < 4) {
            val pre = ownedByNameEn[cur.evolvesFrom] ?: return if (cur.isBasic) cur else null
            cur = pre
        }
        return cur
    }

    /**
     * Construye la línea de evolución de [top] con ratios por fase, limitada por lo poseído.
     * Si la línea no puede completarse (falta un eslabón poseído), devuelve vacío para que el
     * llamador escoja otra insignia.
     */
    private fun buildLine(
        top: PokemonCard,
        ownedByNameEn: Map<String, PokemonCard>,
        ownedCount: Map<String, Int>,
        rng: Random,
    ): List<Pair<PokemonCard, Int>> {
        // Cadena desde el top hacia abajo.
        val chain = ArrayList<PokemonCard>()
        var cur: PokemonCard? = top
        var guard = 0
        while (cur != null && guard++ < 4) {
            chain += cur
            val from = cur.evolvesFrom ?: break
            cur = ownedByNameEn[from] ?: return emptyList() // eslabón no poseído → línea inválida
        }
        chain.reverse() // [Básico, Fase1, (Fase2)]

        val ratios: List<Int> = when (chain.size) {
            1 -> listOf(if (top.mechanic.prizesWhenKO >= 2) 2 else 3) // Básico atacante (ex: 2-3)
            2 -> if (rng.nextBoolean()) listOf(4, 3) else listOf(3, 2)
            else -> listOf(4, 3, 2).let { base -> if (rng.nextBoolean()) listOf(4, 2, 3) else base }
        }
        return chain.mapIndexed { i, card ->
            card to minOf(ratios.getOrElse(i) { 1 }, ownedCount[card.id.raw] ?: 0, MAX_COPIES)
        }.filter { it.second > 0 }
    }

    private fun typeWeights(counts: Map<String, Int>, byId: Map<String, Card>, types: List<EnergyType>): Map<EnergyType, Int> {
        val w = types.associateWith { 1 }.toMutableMap() // suavizado: +1 a cada tipo
        for ((id, n) in counts) {
            val p = byId[id] as? PokemonCard ?: continue
            for (t in p.types) if (t in w) w[t] = (w[t] ?: 0) + n
        }
        return w
    }

    /** Reparte [total] entre [types] proporcional a [weights], repartiendo el resto por peso. */
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

    private fun total(counts: Map<String, Int>): Int = counts.values.sum()
    private fun pokemonCount(counts: Map<String, Int>, byId: Map<String, Card>): Int =
        counts.entries.filter { byId[it.key] is PokemonCard }.sumOf { it.value }
    private fun stadiumCount(counts: Map<String, Int>, byId: Map<String, Card>): Int =
        counts.entries.filter { (byId[it.key] as? TrainerCard)?.kind is TrainerKind.Stadium }.sumOf { it.value }

    private fun trimExcess(counts: LinkedHashMap<String, Int>, by: Int) {
        var left = by
        val it = counts.keys.toList().asReversed().iterator()
        while (left > 0 && it.hasNext()) {
            val k = it.next()
            val cur = counts[k] ?: 0
            val cut = minOf(cur, left)
            counts[k] = cur - cut; left -= cut
            if (counts[k] == 0) counts.remove(k)
        }
    }

    private fun trimToSize(counts: LinkedHashMap<String, Int>): Int {
        trimExcess(counts, total(counts) - DECK_SIZE)
        return 0
    }
}
