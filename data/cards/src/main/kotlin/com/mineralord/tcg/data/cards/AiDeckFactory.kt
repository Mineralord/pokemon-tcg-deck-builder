package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EffectRegistry
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.TrainerCard
import kotlin.random.Random

/**
 * Genera el mazo del rival IA (PvE) de forma ALEATORIA pero CON LÓGICA, para que
 * ninguna partida contra la máquina se repita. En vez de usar siempre un mazo fijo
 * (antes: Pikachu ex de la Academia 2024), la IA "posee" toda carta con efectos
 * COMPLETOS (ataques/habilidades/entrenadores implementados en [EffectsDb]) y arma
 * una baraja legal de 60 con el mismo motor del editor de barajas ([AutoDeckBuilder]):
 * insignia + líneas de evolución por ratio, motor de Entrenadores y energía del foco.
 *
 * "Efectos completos" = la carta es realmente jugable por el motor:
 *  - Pokémon: todo efecto referenciado (ataque/habilidad) está registrado y tiene
 *    al menos un ataque USABLE (daño fijo sin efecto, o efecto registrado).
 *  - Entrenador: su efecto está registrado.
 *  - Energía básica: siempre.
 *  - Energía especial u otros no soportados: fuera (evita mazos rotos).
 */
object AiDeckFactory {

    /** La IA "posee" hasta 4 de cada carta y energía básica de sobra. */
    private const val VIRTUAL_COPIES = 4
    private const val VIRTUAL_ENERGY = 30

    /**
     * EXPANSIÓN PERMITIDA para la IA: de momento SOLO el set 151 (código `sv3pt5`), porque es el
     * único con sus efectos completamente implementados y probados. Brecha Paradójica (`sv4`) se
     * sumará aquí cuando TODOS sus efectos (ataques, habilidades y entrenadores) estén listos.
     * La Energía Básica se permite siempre (no pertenece a un set jugable concreto).
     */
    private const val ALLOWED_SET_PREFIX = "sv3pt5-"

    private fun inAllowedSet(card: Card): Boolean =
        card is BasicEnergy || card.id.raw.startsWith(ALLOWED_SET_PREFIX)

    /** Probabilidad (%) de que el mazo generado sea de DOS tipos en vez de uno. */
    private const val DUAL_TYPE_PERCENT = 40

    /**
     * Construye un mazo aleatorio (lista de entradas) usando únicamente cartas con
     * efectos completos. [all] es el catálogo entero (p. ej. `CardRepository.all`).
     * Si el pool jugable fuera insuficiente, cae al mazo Pikachu ex como red de seguridad.
     */
    fun buildRandomDeck(
        all: List<Card>,
        registry: EffectRegistry = EffectsDb.registry,
        rng: Random = Random.Default,
    ): List<DeckEntry> {
        // Solo cartas jugables (efectos completos) Y del set permitido (151 por ahora).
        val playable = all.filter { isPlayable(it, registry) && inAllowedSet(it) }
        if (playable.none { it is PokemonCard }) return StarterDecks.PIKACHU.entries

        // Colección virtual: la IA dispone de todo lo jugable, para que AutoDeckBuilder
        // tenga libertad de armar cualquier arquetipo implementado.
        val owned: Map<String, Int> = playable.associate { c ->
            c.id.raw to if (c is BasicEnergy) VIRTUAL_ENERGY else VIRTUAL_COPIES
        }

        val focus = pickFocus(playable, rng)
        val deck = AutoDeckBuilder.build(owned = owned, all = playable, focus = focus, rng = rng)
        // Garantía de legalidad mínima: al menos ~40 cartas y algún Pokémon. Si el foco
        // elegido resultó pobre, reintenta sin foco (deja que el builder infiera).
        val hasPokemon = deck.any { (playableById(playable)[it.cardId.raw]) is PokemonCard }
        return if (deck.sumOf { it.count } >= 40 && hasPokemon) deck
        else StarterDecks.PIKACHU.entries
    }

    /** Expande una lista de entradas a ids con repeticiones (para sembrar el mazo en juego). */
    fun expand(entries: List<DeckEntry>): List<CardId> =
        entries.flatMap { e -> List(e.count) { e.cardId } }

    // ---------------------------------------------------------------- internos

    private fun playableById(playable: List<Card>): Map<String, Card> =
        playable.associateBy { it.id.raw }

    /**
     * Elige 1 (o a veces 2) tipos elementales con suficientes atacantes jugables, para
     * que el mazo sea coherente y no un revoltijo. Vacío = deja que AutoDeckBuilder
     * infiera el foco (o caiga a Incoloro).
     */
    private fun pickFocus(playable: List<Card>, rng: Random): Set<EnergyType> {
        val attackersByType = HashMap<EnergyType, Int>()
        for (c in playable) if (c is PokemonCard && c.attacks.isNotEmpty()) {
            for (t in c.types) if (t != EnergyType.COLORLESS) {
                attackersByType[t] = (attackersByType[t] ?: 0) + 1
            }
        }
        val viable = attackersByType.filter { it.value >= 2 }.keys.toList()
        if (viable.isEmpty()) return emptySet()
        val primary = viable.random(rng)
        return if (viable.size >= 2 && rng.nextInt(100) < DUAL_TYPE_PERCENT) {
            setOf(primary, (viable - primary).random(rng))
        } else {
            setOf(primary)
        }
    }

    /** ¿La carta es realmente jugable por el motor (efectos completos)? */
    private fun isPlayable(card: Card, reg: EffectRegistry): Boolean = when (card) {
        is BasicEnergy -> true
        is TrainerCard -> reg.has(card.effect)
        is PokemonCard -> {
            val refsOk = card.attacks.all { it.effect.let { e -> e == null || reg.has(e) } } &&
                card.abilities.all { it.effect.let { e -> e == null || reg.has(e) } }
            val hasUsableAttack = card.attacks.any { atk ->
                val e = atk.effect
                (e != null && reg.has(e)) || (e == null && atk.baseDamage is Damage.Fixed)
            }
            refsOk && hasUsableAttack
        }
        else -> false // SpecialEnergy u otros aún sin soporte → fuera
    }
}
