package com.mineralord.tcg.engine.rules

import com.mineralord.tcg.engine.effects.EffectInterpreter
import com.mineralord.tcg.engine.effects.EffectSource
import com.mineralord.tcg.engine.model.PendingDecision
import com.mineralord.tcg.engine.events.GameEvent
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.EffectRegistry
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.EnergyCard
import com.mineralord.tcg.engine.model.EnergyProvision
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.GameState
import com.mineralord.tcg.engine.model.ModKind
import com.mineralord.tcg.engine.model.PassiveModifier
import com.mineralord.tcg.engine.model.Phase
import com.mineralord.tcg.engine.model.PlayerState
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonInPlay
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.model.SpecialEnergy
import com.mineralord.tcg.engine.model.Status
import com.mineralord.tcg.engine.model.EffectOp
import com.mineralord.tcg.engine.model.Target
import com.mineralord.tcg.engine.model.ToolTarget
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind

/**
 * Resultado de aplicar un intent: nuevo estado + eventos emitidos. Si [accepted]
 * es false, [state] es el estado SIN cambios y [rejection] explica por qué la
 * acción era ilegal (jamás se muta nada ante una acción inválida).
 */
data class EngineResult(
    val state: GameState,
    val events: List<GameEvent>,
    val accepted: Boolean = true,
    val rejection: String? = null,
    /**
     * Decisiones que el efecto del ataque dejó pendientes (elegir objetivo,
     * buscar en mazo, mover energía). Las ops deterministas ya quedaron aplicadas
     * en [state]; estas requieren que el jugador/IA elija antes de resolverlas.
     */
    val pending: List<PendingDecision> = emptyList(),
) {
    companion object {
        fun reject(state: GameState, reason: String) =
            EngineResult(state, emptyList(), accepted = false, rejection = reason)
    }
}

/**
 * Motor de reglas — Kotlin puro, determinista, headless.
 *
 * Transforma `(estado, intent) -> (estado', eventos)` sin mutar nada. Toda la
 * aleatoriedad pasa por [Rng], de modo que con la misma semilla la partida es
 * reproducible. Emite [GameEvent] para alimentar log, animaciones e IA (§0).
 *
 * Cobertura de Fase 1: turn-loop, robar, desplegar Básicos, evolucionar, unir
 * energía, retirar, atacar (con Debilidad/Resistencia), KO + premios, victoria
 * por premios/sin-Activo/deck-out, y daño de Veneno/Quemadura entre turnos. Los
 * efectos autorados por carta llegan con `:engine:effects`.
 */
class GameEngine(
    private val rng: Rng,
    private val effects: EffectRegistry = EffectsDb.registry,
    private val interpreter: EffectInterpreter = EffectInterpreter(),
) {

    /**
     * Lanzamiento de moneda GATEADO por [GameState.coinsAsTailsSide] (Psyduck — Cavilar): si el
     * jugador activo es el marcado y estamos en su turno marcado, la moneda se considera CRUZ
     * (false) sin consultar el [rng]. En cualquier otro caso, moneda normal. Las monedas del
     * turno activo (ataques/habilidades/coste-para-atacar) pasan por aquí.
     */
    private fun gatedFlip(s: GameState): Boolean =
        if (s.coinsAsTailsSide == s.activeSide && s.coinsAsTailsOnTurn == s.turn) false
        else rng.flipCoin()

    fun apply(state: GameState, intent: GameIntent): EngineResult {
        if (state.isOver) return EngineResult.reject(state, "La partida ha terminado")
        // Tras un KO hay que elegir el nuevo Activo ANTES que nada: mientras haya
        // promociones pendientes, solo se acepta PromoteActive (de un lado pendiente).
        if (state.awaitingPromotion) {
            return if (intent is GameIntent.PromoteActive) promoteActive(state, intent.benchTarget)
            else EngineResult.reject(state, "Debes elegir tu nuevo Pokémon Activo")
        }
        // Con una decisión pendiente, solo se puede resolverla.
        if (state.awaitingDecision && intent !is GameIntent.ResolveDecision) {
            return EngineResult.reject(state, "Hay una decisión pendiente por resolver")
        }
        return when (intent) {
            is GameIntent.PlayBasicToBench -> playBasicToBench(state, intent.card)
            is GameIntent.Evolve -> evolve(state, intent.evolution, intent.onto)
            is GameIntent.AttachEnergy -> attachEnergy(state, intent.energy, intent.to)
            is GameIntent.AttachTool -> attachTool(state, intent.tool, intent.target)
            is GameIntent.Retreat -> retreat(state, intent.benchTarget)
            is GameIntent.PromoteActive -> promoteActive(state, intent.benchTarget)
            is GameIntent.Attack -> attack(state, intent.attackName)
            is GameIntent.PlayTrainer -> playTrainer(state, intent.card)
            is GameIntent.UseAbility -> useAbility(state, intent.pokemon, intent.abilityName)
            is GameIntent.ResolveDecision -> resolveDecision(state, intent.chosen)
            GameIntent.EndTurn -> endTurn(state)
        }
    }

    // ---------------------------------------------------------------- despliegue

    private fun playBasicToBench(state: GameState, cardId: CardId): EngineResult {
        val me = state.activePlayer
        val card = me.hand.firstOrNull { it.id == cardId }
            ?: return EngineResult.reject(state, "La carta no está en la mano")
        if (card !is PokemonCard || !card.isBasic) {
            return EngineResult.reject(state, "Solo se puede poner un Pokémon Básico en la Banca")
        }
        if (me.bench.size >= BENCH_LIMIT) {
            return EngineResult.reject(state, "La Banca está llena")
        }
        val updated = me.copy(
            hand = me.hand - card,
            bench = me.bench + PokemonInPlay(card),
        )
        return EngineResult(
            withPlayer(state, updated),
            listOf(GameEvent.PokemonPlayed(state.activeSide, cardId, toBench = true)),
        )
    }

    private fun evolve(state: GameState, evoId: CardId, ontoId: CardId): EngineResult {
        val me = state.activePlayer
        val evo = me.hand.firstOrNull { it.id == evoId } as? PokemonCard
            ?: return EngineResult.reject(state, "La evolución no está en la mano")
        val target = me.allInPlay.firstOrNull { it.card.id == ontoId }
            ?: return EngineResult.reject(state, "El objetivo no está en juego")
        if (evo.evolvesFrom != target.card.name.en && evo.evolvesFrom != target.card.name.es) {
            return EngineResult.reject(state, "${evo.name.es} no evoluciona de ${target.card.name.es}")
        }
        // Regla oficial: no se puede evolucionar durante TU primer turno (turno 1 = 1er jugador;
        // turno 2 = primer turno del 2º jugador). Excepción: Spearow — Ventaja Evolutiva permite
        // evolucionarlo si vas segundo (turno 2), comprobando la habilidad del OBJETIVO (respeta el bloqueo).
        if (state.turn <= 2) {
            val exempt = state.turn == 2 &&
                abilityEffects(state, state.activeSide, target).any { it.evolvesFirstTurnIfSecond }
            if (!exempt) {
                return EngineResult.reject(state, "No puedes evolucionar durante tu primer turno")
            }
        }
        // Regla oficial: no se puede evolucionar un Pokémon el turno en que entró en juego
        // (turnsInPlay se pone a 0 al colocarlo/evolucionarlo; sube al iniciar tu turno).
        if (target.turnsInPlay < 1) {
            return EngineResult.reject(state, "No se puede evolucionar el mismo turno en que entró")
        }
        val evolved = target.copy(
            card = evo,
            evolutionStack = target.evolutionStack + target.card,
            statuses = emptySet(),       // evolucionar cura condiciones especiales
            // Recién evolucionado = "acaba de entrar": no puede volver a evolucionar este
            // turno (evita evolucionar dos veces en el mismo turno). Sube al iniciar tu turno.
            turnsInPlay = 0,
        )
        val updated = replaceInPlay(me, ontoId, evolved).copy(hand = me.hand - evo)
        var working = withPlayer(state, updated)
        val events = mutableListOf<GameEvent>(GameEvent.Evolved(state.activeSide, ontoId, evoId))
        // Habilidades "al evolucionar desde la mano" (Gyarados — Indomable descarta 5;
        // Hypno — Toma Hipnosis duerme al Activo rival). Se disparan AQUÍ, no vía UseAbility.
        // El Pokémon recién evolucionado conserva su posición pero ahora tiene id = evoId.
        val trigAbility = evo.abilities.firstOrNull { ab ->
            ab.effect?.let { effects[it] }?.triggerOnEvolve == true
        }
        val trigger = trigAbility?.effect?.let { effects[it] }
        if (trigger != null) {
            // Habilidad PASIVA disparada automáticamente: anuncio canónico (aura roja) sobre el evo.
            events += GameEvent.AbilityUsed(state.activeSide, evoId, trigAbility.name.es, manual = false)
            val res = interpreter.execute(
                trigger, EffectSource(state.activeSide, evoId), working,
                shuffle = { rng.shuffle(it) }, flip = { rng.flipCoin() },
            )
            working = res.state
            events += res.events
            // Habilidad INTERACTIVA al evolucionar (Gloom/Vileplume — mirar top N y unir
            // Energía): el efecto queda en pausa esperando la decisión de arrastre. Se
            // devuelve el `pending` para que UI/IA/netplay la resuelvan; NO cierra el turno
            // (endsTurnOnResolve queda false, a diferencia de un ataque).
            if (res.pending.isNotEmpty()) {
                return EngineResult(working, events, pending = res.pending)
            }
            working = handleKnockouts(working, state.activeSide.other(), events)
            working = handleKnockouts(working, state.activeSide, events)
        }
        return EngineResult(working, events)
    }

    private fun attachEnergy(state: GameState, energyId: CardId, toId: CardId): EngineResult {
        val me = state.activePlayer
        if (state.energyAttachedThisTurn) {
            return EngineResult.reject(state, "Ya uniste una energía este turno")
        }
        val energy = me.hand.firstOrNull { it.id == energyId } as? EnergyCard
            ?: return EngineResult.reject(state, "La energía no está en la mano")
        val target = me.allInPlay.firstOrNull { it.card.id == toId }
            ?: return EngineResult.reject(state, "El objetivo no está en juego")
        val updated = replaceInPlay(
            me,
            toId,
            target.copy(attachedEnergy = target.attachedEnergy + energy),
        ).copy(hand = me.hand - (energy as Card))
        return EngineResult(
            withPlayer(state, updated).copy(energyAttachedThisTurn = true),
            listOf(GameEvent.EnergyAttached(state.activeSide, energyId, toId)),
        )
    }

    /**
     * Ancla una Herramienta de la mano a un Pokémon. Reglas oficiales (rulebook):
     * "You can play as many … Pokémon Tool cards as you like" (sin límite por turno) y
     * un Pokémon "can only have one Pokémon Tool attached" (máx. 1). Se ancla a un
     * Pokémon PROPIO; si la Herramienta lo permite ([ToolTarget.ANY], p. ej. Team Flare
     * Hyper Gear) también al RIVAL. La carta queda anexada (se descarta con el Pokémon).
     */
    private fun attachTool(state: GameState, toolId: CardId, targetId: CardId): EngineResult {
        val me = state.activePlayer
        val card = me.hand.firstOrNull { it.id == toolId }
            ?: return EngineResult.reject(state, "La carta no está en la mano")
        if (card !is TrainerCard || card.kind !is TrainerKind.Tool) {
            return EngineResult.reject(state, "Esa carta no es una Herramienta")
        }
        val kind = card.kind as TrainerKind.Tool
        val foeSide = state.activeSide.other()
        val targetSide = when {
            me.allInPlay.any { it.card.id == targetId } -> state.activeSide
            kind.attachTo == ToolTarget.ANY &&
                state.sideState(foeSide).allInPlay.any { it.card.id == targetId } -> foeSide
            else -> return EngineResult.reject(state, "Objetivo no válido para la Herramienta")
        }
        val target = state.sideState(targetSide).allInPlay.first { it.card.id == targetId }
        // Regla oficial: máximo 1 Herramienta por Pokémon.
        if (target.attachedTools.isNotEmpty()) {
            return EngineResult.reject(state, "Ese Pokémon ya tiene una Herramienta anexada")
        }
        // Saca la carta de la mano del jugador activo y ánclala al objetivo.
        var working = withPlayer(state, me.copy(hand = me.hand - card), state.activeSide)
        val ps = working.sideState(targetSide)
        working = withPlayer(
            working,
            replaceInPlay(ps, targetId, target.copy(attachedTools = target.attachedTools + card)),
            targetSide,
        )
        return EngineResult(working, listOf(GameEvent.ToolAttached(state.activeSide, toolId, targetId)))
    }

    private fun retreat(state: GameState, benchTargetId: CardId): EngineResult {
        val me = state.activePlayer
        val active = me.active
            ?: return EngineResult.reject(state, "No hay Pokémon Activo")
        val benchMon = me.bench.firstOrNull { it.card.id == benchTargetId }
            ?: return EngineResult.reject(state, "El objetivo no está en la Banca")
        // Restricción "no puede retirarse este turno" (Amarrar/Rumble y similares).
        if (active.cannotRetreatOnTurn == state.turn) {
            return EngineResult.reject(state, "${active.card.name.es} no puede retirarse este turno")
        }
        // Bloqueo por habilidad rival (Omastar Tentáculos Primordiales).
        if (retreatBlockedByOpponent(state, state.activeSide)) {
            return EngineResult.reject(state, "${active.card.name.es} no puede retirarse")
        }
        // Coste efectivo: 0 si una habilidad lo anula (Flotación / Travesía Propulsión).
        val retreatCost = effectiveRetreatCost(state, state.activeSide)
        if (active.attachedEnergyCount < retreatCost) {
            return EngineResult.reject(state, "Energía insuficiente para retirarse")
        }
        // Descarta tantas energías como cueste retirarse.
        val toDiscard = active.attachedEnergy.take(retreatCost)
        val newActive = benchMon.copy(statuses = emptySet())   // retirarse cura condiciones
        val updated = me.copy(
            active = newActive,
            bench = me.bench - benchMon + active.copy(
                attachedEnergy = active.attachedEnergy - toDiscard.toSet(),
                // Deja el Puesto Activo → expira el override de Debilidad (Porygon — Conversión 4).
                weaknessOverrideType = null,
            ),
            discard = me.discard + toDiscard,
        )
        return EngineResult(
            withPlayer(state, updated),
            listOf(GameEvent.Retreated(state.activeSide, active.card.id, benchTargetId)),
        )
    }

    // ------------------------------------------------------------------- ataque

    private fun attack(state: GameState, attackName: String): EngineResult {
        val me = state.activePlayer
        val foeSide = state.activeSide.other()
        val foe = state.sideState(foeSide)
        val attacker = me.active
            ?: return EngineResult.reject(state, "No hay Pokémon Activo para atacar")
        val defender = foe.active
            ?: return EngineResult.reject(state, "El rival no tiene Pokémon Activo")
        val atk = attacker.card.attacks.firstOrNull { it.name.es == attackName || it.name.en == attackName }
            ?: return EngineResult.reject(state, "Ataque desconocido: $attackName")
        // Regla oficial: quien empieza (turno 1) no puede atacar en su primer turno.
        if (state.turn == 1) {
            return EngineResult.reject(state, "El jugador que empieza no puede atacar en su primer turno")
        }
        // Restricción "no puede atacar este turno" (Jet Wing y similares).
        if (attacker.cannotAttackOnTurn == state.turn) {
            return EngineResult.reject(state, "${attacker.card.name.es} no puede atacar este turno")
        }
        if (attacker.attachedEnergyCount < effectiveAttackCost(atk, attacker, state, state.activeSide)) {
            return EngineResult.reject(state, "Energía insuficiente para ${atk.name.es}")
        }

        val events = mutableListOf<GameEvent>()
        events += GameEvent.Attacked(state.activeSide, attacker.card.id, atk.name.es)

        // Seadra — Tinta Cegadora: si el Activo debe lanzar monedas para poder atacar este
        // turno, se lanzan ahora; si sale cruz en alguna, el ataque "no se lleva a cabo"
        // (sin daño ni efecto), pero el turno termina igualmente.
        if (attacker.flipsToAttackOnTurn == state.turn && attacker.flipsToAttackCount > 0) {
            val flips = (1..attacker.flipsToAttackCount).map { gatedFlip(state) }
            flips.forEach { events += GameEvent.CoinFlipped(state.activeSide, it) }
            if (flips.any { !it }) {
                val ended = endTurn(state)
                return EngineResult(ended.state, events + ended.events)
            }
        }

        // Daño base: si el efecto autora términos de daño (ataques "X+" con bonus
        // condicional) se usan ESOS (sumados) para que el bonus pase por Debilidad/
        // Resistencia; si no, el daño fijo de la carta. En ambos casos, un solo calc.
        val authored = effects[atk.effect]?.attackDamage.orEmpty()
        val base = if (authored.isNotEmpty()) {
            authored.filter { term ->
                when (val cond = term.condition) {
                    com.mineralord.tcg.engine.model.DamageCondition.Always -> true
                    com.mineralord.tcg.engine.model.DamageCondition.IfDefenderEvolved -> !defender.card.isBasic
                    com.mineralord.tcg.engine.model.DamageCondition.IfDefenderHasDamage -> defender.damage > 0
                    com.mineralord.tcg.engine.model.DamageCondition.IfSelfHasDamage -> attacker.damage > 0
                    com.mineralord.tcg.engine.model.DamageCondition.IfEmptyHand -> me.hand.isEmpty()
                    is com.mineralord.tcg.engine.model.DamageCondition.IfDefenderType -> cond.type in defender.card.types
                    com.mineralord.tcg.engine.model.DamageCondition.IfDefenderExOrV -> when (defender.card.mechanic) {
                        com.mineralord.tcg.engine.model.PokemonMechanic.ExLower,
                        com.mineralord.tcg.engine.model.PokemonMechanic.ExUpper,
                        com.mineralord.tcg.engine.model.PokemonMechanic.V,
                        com.mineralord.tcg.engine.model.PokemonMechanic.VMax,
                        com.mineralord.tcg.engine.model.PokemonMechanic.VStar -> true
                        else -> false
                    }
                    com.mineralord.tcg.engine.model.DamageCondition.IfSameHandSizeAsOpponent -> me.hand.size == foe.hand.size
                    com.mineralord.tcg.engine.model.DamageCondition.IfMorePrizesThanOpponent -> me.prizes.size > foe.prizes.size
                    is com.mineralord.tcg.engine.model.DamageCondition.IfSelfBenchHasName ->
                        me.bench.any { it.card.name.es.contains(cond.name, true) || it.card.name.en.contains(cond.name, true) }
                    com.mineralord.tcg.engine.model.DamageCondition.IfSupporterPlayedThisTurn -> state.supporterPlayedThisTurn
                    is com.mineralord.tcg.engine.model.DamageCondition.IfPlayedTrainerThisTurn ->
                        state.trainerNamesPlayedThisTurn.any { it.contains(cond.name, true) }
                }
            }.sumOf { it.amount + it.perDefenderCounter * (defender.damage / 10) }
        } else {
            (atk.baseDamage as? com.mineralord.tcg.engine.model.Damage.Fixed)?.value ?: 0
        }
        val atkEffect = effects[atk.effect]
        // Pidgeot — Vuelo (Fly): "Lanza 1 moneda. Si cruz, este ataque no hace nada." UNA moneda
        // gateada, tras `Attacked` y antes del daño, decide TODO el ataque: cara = procede (daño +
        // ops, p. ej. prevención); cruz = no hace nada. Se reutiliza el mecanismo `attackFizzles`.
        val coinGateHeads = if (atkEffect?.coinFlipOrNothing == true) {
            val f = gatedFlip(state)
            events += GameEvent.CoinFlipped(state.activeSide, f)
            f
        } else true
        // "Este ataque no hace nada si…" (Primeape: no Confundido; Slowbro: evolucionó este
        // turno; Pidgeot: moneda cruz). Anula daño Y efecto. El daño no es un efecto, pero "no hace
        // nada" cubre ambos.
        val attackFizzles =
            (atkEffect?.noEffectUnlessSelfConfused == true && Status.CONFUSED !in attacker.statuses) ||
            (atkEffect?.noEffectIfEvolvedThisTurn == true && attacker.turnsInPlay == 0) ||
            (atkEffect?.coinFlipOrNothing == true && !coinGateHeads)
        val dmgBase = if (attackFizzles) 0 else base
        // Bonus "próximo turno este Pokémon +X" (Golem ex Giro Dinámico, Hitmonchan Puño
        // Exaltado): se suma al daño base ANTES de Debilidad/Resistencia. Solo si hay daño.
        val selfBonus = if (dmgBase > 0 && attacker.attackBonusOnTurn == state.turn) attacker.attackBonusAmount else 0
        // Buff de aliado en Banca (Cubone — Ovación Ósea: tus Marowak +30 mientras Cubone
        // esté en tu Banca). Se suma ANTES de Debilidad/Resistencia. Respeta el bloqueo.
        val allyBoost = if (dmgBase > 0) me.bench.sumOf { ally ->
            abilityEffects(state, state.activeSide, ally)
                .filter { it.boostAlliedAttackerNamed != null && nameMatches(attacker, it.boostAlliedAttackerNamed!!) }
                .sumOf { it.boostAlliedAttackerAmount }
        } else 0
        // "El daño no se ve afectado por Debilidad/Resistencia" (Staryu, Golem ex).
        val ignoresDefEffects = atkEffect?.ignoresDefenderEffects == true
        // Override de Debilidad: Porygon (tipo, persistente en el Defensor) y Kabutops (multiplicador
        // ×4 aportado por una habilidad en juego del ATACANTE, respeta el bloqueo).
        val weaknessMultOverride = me.allInPlay.firstNotNullOfOrNull { pip ->
            abilityEffects(state, state.activeSide, pip).firstNotNullOfOrNull { it.overridesDefenderWeaknessMultiplier }
        }
        val dmg = Damage.calculate(
            dmgBase + selfBonus + allyBoost, attacker.card.types, defender,
            ignoreWeakness = atkEffect?.ignoresWeakness == true,
            ignoreResistance = atkEffect?.ignoresResistance == true,
            weaknessTypeOverride = defender.weaknessOverrideType,
            weaknessMultiplierOverride = weaknessMultOverride,
        )
        // Reducción de daño por Herramientas del defensor, aplicada DESPUÉS de
        // Debilidad/Resistencia (como manda la regla de "reduce el daño en X").
        // Staryu — Meteoros ignora TODOS los efectos del Activo rival (prevención,
        // reducción, Herramientas) → salta esas restas.
        val selfReduction = if (defender.damageReductionOnTurn == state.turn) defender.damageReductionAmount else 0
        // Prevención de daño: total (preventDamageOnTurn) o condicional a atacante Básico
        // (preventBasicDamageOnTurn, Nidoqueen — Prensa Real). Ambas se saltan si el ataque
        // ignora los efectos del Defensor (Staryu — Meteoros).
        // Escudo pasivo por paridad de Energía (Mr. Mime — Barrera Mímica): evita el daño si el
        // Defensor tiene la habilidad (no bloqueada) y ambos Activos tienen igual nº de Energías.
        val energyParityShield = !isAbilityLocked(state, foeSide, defender) &&
            defender.attachedEnergy.size == attacker.attachedEnergy.size &&
            defender.card.effectiveAbilities(false).any { ab ->
                ab.effect?.let { effects[it]?.preventsDamageIfEnergyParity } == true
            }
        val damagePrevented = energyParityShield ||
            defender.preventDamageOnTurn == state.turn ||
            (defender.preventBasicDamageOnTurn == state.turn && attacker.card.isBasic)
        val finalAmount = if (dmg.finalAmount > 0 && (ignoresDefEffects || !damagePrevented)) {
            if (ignoresDefEffects) dmg.finalAmount
            else (dmg.finalAmount - toolDamageReduction(defender) - selfReduction).coerceAtLeast(0)
        } else 0
        var newFoe = foe
        if (finalAmount > 0) {
            val damaged = defender.copy(damage = defender.damage + finalAmount)
            newFoe = foe.copy(active = damaged)
            events += GameEvent.DamageDealt(
                state.activeSide, defender.card.id, finalAmount,
                dmg.weaknessApplied, dmg.resistanceApplied,
            )
        }

        var working = withPlayer(state, newFoe, foeSide)

        // Efecto autorado del ataque (estados, daño extra/banca, robar, descartar
        // energía, recoil…). Las ops deterministas se aplican aquí; las de elección
        // (objetivo/búsqueda) viajan como [pending] para que el jugador/IA resuelva.
        var pending = emptyList<PendingDecision>()
        val effect = effects[atk.effect]
        // Kakuna — Manto de Capullo: el DEFENSOR es inmune a los EFECTOS del ataque (no al daño). Se
        // descartan los ops dirigidos al defensor. No aplica si el ataque ignora los efectos del Defensor.
        val defenderImmuneToEffects = !ignoresDefEffects &&
            abilityEffects(state, foeSide, defender).any { it.immuneToAttackEffects }
        val runEffect = if (effect != null && defenderImmuneToEffects)
            effect.copy(ops = effect.ops.filterNot { opTargetsDefender(it) }) else effect
        if (runEffect != null && !attackFizzles) {
            // endsTurnOnResolve = true: si el efecto deja una decisión, el turno se
            // cerrará al resolverla (ver resolveDecision), no aquí.
            val res = interpreter.execute(
                runEffect, EffectSource(state.activeSide, attacker.card.id), working,
                endsTurnOnResolve = true,
                shuffle = { rng.shuffle(it) }, flip = { gatedFlip(state) },
            )
            working = res.state
            events += res.events
            pending = res.pending
        }

        // Contragolpe del Defensor: habilidades que reaccionan a que este Activo sea dañado/
        // Noqueado por el ataque (Hitmonchan Contragolpe, Weezing A Pasarlo Bomba). Corre ANTES
        // de resolver KOs para que el Pokémon aún esté en juego ("incluso si queda Fuera de Combate").
        working = applyDefenderRetaliation(working, foeSide, finalAmount, events)

        // KO del rival y, si hubo recoil/auto-daño/contragolpe, también del atacante.
        // byAttack=true habilita los KO-triggers (Machamp Agallas, Raichu Toma de Tierra).
        // extraPrizes: Clefable — Más Luna coge 1 Premio más si este ataque noquea.
        val extraPrizes = if (finalAmount > 0 && atkEffect?.extraPrizeIfKo == true) 1 else 0
        working = handleKnockouts(working, foeSide, events, byAttack = true, extraPrizes = extraPrizes)
        working = handleKnockouts(working, state.activeSide, events, byAttack = true)

        if (working.isOver) return EngineResult(working.copy(interaction = null), events, pending = pending)

        // Si el efecto dejó una decisión pendiente, el turno sigue abierto hasta
        // que se resuelva; sólo entonces se cierra.
        if (working.awaitingDecision) return EngineResult(working, events, pending = pending)

        // Atacar termina el turno.
        val ended = endTurn(working)
        return EngineResult(ended.state, events + ended.events, pending = pending)
    }

    // ----------------------------------------------------- entrenador / habilidad

    private fun playTrainer(state: GameState, cardId: CardId): EngineResult {
        val me = state.activePlayer
        val card = me.hand.firstOrNull { it.id == cardId }
            ?: return EngineResult.reject(state, "La carta no está en la mano")
        if (card !is TrainerCard) return EngineResult.reject(state, "Esa carta no es un Entrenador")
        val kind = card.kind
        if (kind is TrainerKind.Stadium) return playStadium(state, card)
        if (kind !is TrainerKind.Supporter && kind !is TrainerKind.Item) {
            return EngineResult.reject(state, "Solo se pueden jugar Apoyos, Objetos o Estadios por ahora")
        }
        if (kind is TrainerKind.Supporter && state.supporterPlayedThisTurn) {
            return EngineResult.reject(state, "Ya jugaste un Apoyo este turno")
        }
        val effect = effects[card.effect]
            ?: return EngineResult.reject(state, "Esta carta aún no tiene efecto implementado")
        if (effect.requiresOwnKoLastTurn && state.activeSide !in state.koedLastOppTurn) {
            return EngineResult.reject(state, "Solo puedes jugar ${card.name.es} si te noquearon el turno pasado")
        }

        // La carta va al descarte al jugarse.
        val afterPlay = me.copy(hand = me.hand - card, discard = me.discard + card)
        var working = withPlayer(state, afterPlay, state.activeSide)
        if (kind is TrainerKind.Supporter) working = working.copy(supporterPlayedThisTurn = true)
        working = working.copy(
            trainerNamesPlayedThisTurn = working.trainerNamesPlayedThisTurn + card.name.es + card.name.en,
        )

        val events = mutableListOf<GameEvent>(GameEvent.TrainerPlayed(state.activeSide, cardId))
        val res = interpreter.execute(
            effect, EffectSource(state.activeSide, null), working,
            shuffle = { rng.shuffle(it) }, flip = { gatedFlip(state) },
        )
        // Si el efecto abre una elección de objetivo SIN candidatos (p. ej. Poción sin
        // ningún Pokémon dañado), la carta no haría nada → rechazar sin gastarla.
        if (res.pending.any { it is PendingDecision.ChooseTargets && it.candidates.isEmpty() }) {
            return EngineResult.reject(state, "No hay un objetivo válido para ${card.name.es}")
        }
        working = res.state
        events += res.events

        working = handleKnockouts(working, state.activeSide.other(), events)
        working = handleKnockouts(working, state.activeSide, events)
        if (working.isOver) return EngineResult(working.copy(interaction = null), events, pending = res.pending)
        return EngineResult(working, events, pending = res.pending)
    }

    /**
     * Juega un Estadio de la mano al campo (slot único compartido). Reglas oficiales:
     * no puedes jugar un Estadio con el MISMO nombre que el que ya está en juego; al poner
     * uno nuevo, el anterior se descarta a la pila de SU dueño. El Estadio NO va al descarte
     * al jugarse: queda en el campo hasta que otro lo reemplace. Su efecto/pasivo (p. ej. un
     * bloqueo de Habilidades tipo Camino hacia la Cima) lo leen los helpers de pasivos/bloqueo.
     */
    private fun playStadium(state: GameState, card: TrainerCard): EngineResult {
        // Fósil Hélix Antiguo — Oleaje Helicoidal: mientras esté en el Activo rival, no puedes jugar
        // Estadios. Respeta el bloqueo de Habilidades (habilidad suprimible).
        val foeActive = state.sideState(state.activeSide.other()).active
        if (foeActive != null && abilityPassives(state, state.activeSide.other(), foeActive)
                .any { it.mod == ModKind.BLOCK_OPPONENT_STADIUM }) {
            return EngineResult.reject(state, "No puedes jugar Estadios: el Activo rival lo impide")
        }
        val prev = state.stadium
        if (prev != null && (prev.name.en == card.name.en || prev.name.es == card.name.es)) {
            return EngineResult.reject(state, "Ya hay un Estadio con ese nombre en juego")
        }
        var working = state
        // El Estadio anterior va al descarte de su dueño.
        if (prev != null && state.stadiumOwner != null) {
            val owner = state.sideState(state.stadiumOwner!!)
            working = withPlayer(working, owner.copy(discard = owner.discard + prev), state.stadiumOwner!!)
        }
        // La carta sale de la mano del jugador activo y ocupa el slot de Estadio.
        val me = working.sideState(state.activeSide)
        working = withPlayer(working, me.copy(hand = me.hand - (card as com.mineralord.tcg.engine.model.Card)), state.activeSide)
        working = working.copy(
            stadium = card,
            stadiumOwner = state.activeSide,
            trainerNamesPlayedThisTurn = working.trainerNamesPlayedThisTurn + card.name.es + card.name.en,
        )
        return EngineResult(working, listOf(GameEvent.TrainerPlayed(state.activeSide, card.id)))
    }

    private fun useAbility(state: GameState, pokemonId: CardId, abilityName: String): EngineResult {
        val me = state.activePlayer
        val mon = me.allInPlay.firstOrNull { it.card.id == pokemonId }
            ?: return EngineResult.reject(state, "Ese Pokémon no está en juego")
        val ability = mon.card.abilities.firstOrNull { it.name.es == abilityName || it.name.en == abilityName }
            ?: return EngineResult.reject(state, "Habilidad desconocida: $abilityName")
        val effect = ability.effect?.let { effects[it] }
            ?: return EngineResult.reject(state, "Esta habilidad aún no tiene efecto implementado")
        if (effect.triggerOnEvolve) {
            return EngineResult.reject(state, "Esta habilidad se activa sola al evolucionar")
        }
        if (effect.triggerOnActiveDamaged || effect.triggerOnActiveKO) {
            return EngineResult.reject(state, "Esta habilidad se activa sola al recibir daño")
        }
        if (ability.kind.suppressibleByAbilityLock && isAbilityLocked(state, state.activeSide, mon)) {
            return EngineResult.reject(state, "Las Habilidades de ${mon.card.name.es} están bloqueadas")
        }
        if (effect.activeOnly && me.active?.card?.id != pokemonId) {
            return EngineResult.reject(state, "Esta habilidad solo puede usarla el Activo")
        }
        if (effect.oncePerTurn && pokemonId in state.abilitiesUsedThisTurn) {
            return EngineResult.reject(state, "Esta habilidad ya se usó este turno")
        }
        var working = state
        if (effect.oncePerTurn) {
            working = working.copy(abilitiesUsedThisTurn = working.abilitiesUsedThisTurn + pokemonId)
        }

        // Anuncio canónico ANTES del efecto: el rótulo/aura (dorado manual) precede a la resolución.
        val events = mutableListOf<GameEvent>(
            GameEvent.AbilityUsed(state.activeSide, pokemonId, abilityName, manual = true),
        )
        val res = interpreter.execute(
            effect, EffectSource(state.activeSide, pokemonId), working,
            shuffle = { rng.shuffle(it) }, flip = { gatedFlip(state) },
        )
        // Igual que en los Entrenadores: una habilidad que solo apunta a Pokémon dañados
        // (curación) no puede usarse si no hay ninguno con daño.
        if (res.pending.any { it is PendingDecision.ChooseTargets && it.candidates.isEmpty() }) {
            return EngineResult.reject(state, "No hay un objetivo válido para esa habilidad")
        }
        working = res.state
        events += res.events

        working = handleKnockouts(working, state.activeSide.other(), events)
        working = handleKnockouts(working, state.activeSide, events)
        if (working.isOver) return EngineResult(working.copy(interaction = null), events, pending = res.pending)
        return EngineResult(working, events, pending = res.pending)
    }

    private fun resolveDecision(state: GameState, chosen: List<CardId>): EngineResult {
        val interaction = state.interaction
            ?: return EngineResult.reject(state, "No hay ninguna decisión pendiente")
        if (interaction.side != state.activeSide) {
            return EngineResult.reject(state, "La decisión pendiente no es de quien juega")
        }
        validateChoice(interaction.decision, chosen)?.let { return EngineResult.reject(state, it) }

        val endsTurn = interaction.endsTurnOnResolve
        val res = interpreter.resolve(state, chosen, shuffle = { rng.shuffle(it) }, flip = { gatedFlip(state) })
        var working = res.state
        val events = res.events.toMutableList()

        // El efecto resuelto pudo noquear (daño dirigido a banca, etc.).
        working = handleKnockouts(working, state.activeSide.other(), events)
        working = handleKnockouts(working, state.activeSide, events)
        if (working.isOver) return EngineResult(working.copy(interaction = null), events, pending = res.pending)

        // Si encadenó otra decisión, seguimos esperando.
        if (working.awaitingDecision) return EngineResult(working, events, pending = res.pending)

        // Cadena agotada: si provino de un ataque, ahora se cierra el turno.
        if (endsTurn) {
            val ended = endTurn(working)
            return EngineResult(ended.state, events + ended.events)
        }
        return EngineResult(working, events)
    }

    private fun validateChoice(decision: PendingDecision, chosen: List<CardId>): String? = when (decision) {
        is PendingDecision.ChooseTargets ->
            when {
                chosen.size > decision.count -> "Demasiados objetivos elegidos"
                !decision.candidates.containsAll(chosen) -> "Objetivo no válido"
                else -> null
            }
        is PendingDecision.SearchCards ->
            when {
                chosen.size > decision.count -> "Demasiadas cartas elegidas"
                !decision.candidates.containsAll(chosen) -> "Carta no encontrada en la zona"
                else -> null
            }
        // PlaceCounters: `chosen` repite ids (uno por contador); no más de [count] en total
        // y todos deben ser candidatos.
        is PendingDecision.PlaceCounters ->
            when {
                chosen.size > decision.count -> "Demasiados contadores repartidos"
                !decision.candidates.containsAll(chosen.distinct()) -> "Objetivo no válido"
                else -> null
            }
        // CoinFlip: sin elección de cartas (chosen vacío); el motor lanza al resolver.
        is PendingDecision.CoinFlip -> null
        // CoinFlipThenSearch: sin elección; el motor lanza y encadena la búsqueda.
        is PendingDecision.CoinFlipThenSearch -> null
        // MoveEnergy: el intérprete ignora elecciones fuera de rango; validación ligera.
        is PendingDecision.MoveEnergy -> null
        // AttachFromRevealed: `chosen` son PARES (energía, destino); el intérprete valida
        // candidatos y tope. Validación ligera aquí.
        is PendingDecision.AttachFromRevealed -> null
        // ChooseEnergyType: exactamente 1 tipo, y debe estar entre los candidatos.
        is PendingDecision.ChooseEnergyType ->
            when {
                chosen.size != 1 -> "Debes elegir exactamente 1 tipo"
                chosen.single() !in decision.candidateIds -> "Tipo no válido"
                else -> null
            }
    }

    // --------------------------------------------------------------- KO / fin

    // --------------------------------------------------- pasivos de Herramienta
    // El motor aplica los PassiveModifier aportados por las Herramientas ancladas a un
    // Pokémon (HP extra, reducción de daño). Los pasivos de habilidades/energías aún no
    // se aplican de forma general (deuda documentada); esto se limita a Herramientas.

    private fun toolMods(pip: PokemonInPlay): List<PassiveModifier> =
        pip.attachedTools.flatMap { effects[it.effect]?.passives.orEmpty() }

    /**
     * HP máximo efectivo = HP impreso + EXTRA_HP de sus Herramientas + EXTRA_HP de sus
     * Habilidades cuya condición se cumple (Wigglytuff ex — Cuerpo Expansivo: +100 si
     * tiene Energía Especial unida). Los pasivos de habilidad respetan el bloqueo de
     * Habilidades (van por [abilityPassives]).
     */
    private fun effectiveMaxHp(state: GameState, side: Side, pip: PokemonInPlay): Int {
        val toolHp = toolMods(pip).filter { it.mod == ModKind.EXTRA_HP }.sumOf { it.amount }
        val abilityHp = abilityPassives(state, side, pip)
            .filter { it.mod == ModKind.EXTRA_HP && passiveEnergyConditionMet(pip, it) }
            .sumOf { it.amount }
        return pip.card.hp + toolHp + abilityHp
    }

    /**
     * ¿Se cumple la condición de energía de un pasivo condicional? Sin condición → true.
     * [PassiveModifier.requiresEnergyType]: ≥1 Energía que aporte ese tipo.
     * [PassiveModifier.requiresSpecialEnergy]: ≥1 Energía Especial unida.
     */
    private fun passiveEnergyConditionMet(pip: PokemonInPlay, mod: PassiveModifier): Boolean {
        if (mod.requiresEnergyType != null && !hasEnergyOfType(pip, mod.requiresEnergyType!!)) return false
        if (mod.requiresSpecialEnergy && pip.attachedEnergy.none { it is SpecialEnergy }) return false
        return true
    }

    /** Reducción de daño de ataques aportada por las Herramientas del defensor. */
    private fun toolDamageReduction(pip: PokemonInPlay): Int =
        toolMods(pip).filter { it.mod == ModKind.REDUCE_DAMAGE }.sumOf { it.amount }

    // --- Pasivos de HABILIDADES (Fase 7 del set 151: Flotación / Travesía / Tentáculos) ---

    /** Pasivos de TODAS las habilidades de un Pokémon SIN filtrar por bloqueo (uso interno:
     *  detección de fuentes de bloqueo, para no recursar). */
    private fun abilityPassivesRaw(pip: PokemonInPlay): List<PassiveModifier> =
        pip.card.abilities.mapNotNull { it.effect }.flatMap { effects[it]?.passives.orEmpty() }

    /**
     * Pasivos EFECTIVOS de las habilidades de [pip] del lado [side], respetando un posible
     * bloqueo de Habilidades en juego: si está bloqueado, sus habilidades [AbilityKind.ABILITY]
     * dejan de aportar pasivos (p. ej. Flotación se apaga), pero Poké-Powers/Poké-Bodies siguen.
     */
    private fun abilityPassives(state: GameState, side: Side, pip: PokemonInPlay): List<PassiveModifier> {
        val active = pip.card.effectiveAbilities(isAbilityLocked(state, side, pip))
        return active.mapNotNull { it.effect }.flatMap { effects[it]?.passives.orEmpty() }
    }

    /**
     * Efectos EFECTIVOS de las habilidades de [pip] (respetando el bloqueo de Habilidades).
     * Usado para pasivos autorados como flags de [Effect] (no como [PassiveModifier]):
     * Nidoking — Rey Entusiasta (ataque gratis) y Cubone — Ovación Ósea (buff a un aliado).
     */
    private fun abilityEffects(
        state: GameState,
        side: Side,
        pip: PokemonInPlay,
    ): List<com.mineralord.tcg.engine.model.Effect> {
        val active = pip.card.effectiveAbilities(isAbilityLocked(state, side, pip))
        return active.mapNotNull { it.effect }.mapNotNull { effects[it] }
    }

    /**
     * ¿Este op del ATAQUE aplica un EFECTO al Pokémon DEFENSOR (Target.OPP_ACTIVE o una restricción del
     * Defensor)? Se usa para la inmunidad de Kakuna — Manto de Capullo (el daño NO es un efecto, así que
     * los ops de daño no se filtran; sí los de condición especial, descarte de Energía y restricciones).
     */
    private fun opTargetsDefender(op: EffectOp): Boolean = when (op) {
        is EffectOp.ApplyStatus -> op.target == Target.OPP_ACTIVE
        is EffectOp.CoinFlipStatus -> op.target == Target.OPP_ACTIVE
        is EffectOp.ApplyStatusIfEmptyHand -> op.target == Target.OPP_ACTIVE
        is EffectOp.KoIfStatus -> op.target == Target.OPP_ACTIVE
        is EffectOp.DiscardEnergy -> op.target == Target.OPP_ACTIVE
        is EffectOp.RequireCoinsToAttackNextTurn -> op.target == Target.OPP_ACTIVE
        EffectOp.DefenderCannotRetreatNextTurn -> true
        EffectOp.DefenderCannotAttackNextTurn -> true
        is EffectOp.BumpDefenderRetreatCostNextTurn -> true
        is EffectOp.BumpDefenderAttackCostNextTurn -> true
        else -> false
    }

    private fun nameMatches(pip: PokemonInPlay, needle: String): Boolean =
        pip.card.name.es.contains(needle, true) || pip.card.name.en.contains(needle, true)

    // --- Bloqueo de Habilidades (Klefki, Camino hacia la Cima) — solo apaga AbilityKind.ABILITY ---

    private data class AbilityLockSource(val side: Side?, val sourceId: CardId?, val mod: PassiveModifier)

    /** Todas las fuentes de bloqueo de Habilidades en juego: Estadio + habilidades/Herramientas. */
    private fun abilityLockSources(state: GameState): List<AbilityLockSource> {
        val out = mutableListOf<AbilityLockSource>()
        state.stadium?.let { st ->
            effects[st.effect]?.passives.orEmpty().filter { it.mod == ModKind.BLOCK_ABILITY }
                .forEach { out += AbilityLockSource(state.stadiumOwner, null, it) }
        }
        for (side in listOf(Side.PLAYER, Side.OPPONENT)) {
            state.sideState(side).allInPlay.forEach { pip ->
                (abilityPassivesRaw(pip) + toolMods(pip)).filter { it.mod == ModKind.BLOCK_ABILITY }
                    .forEach { out += AbilityLockSource(side, pip.card.id, it) }
            }
        }
        return out
    }

    /** ¿Están BLOQUEADAS las Habilidades (modernas) de [pip], que está del lado [side]? */
    private fun isAbilityLocked(state: GameState, side: Side, pip: PokemonInPlay): Boolean {
        val sources = abilityLockSources(state)
        if (sources.isEmpty()) return false
        // Un Pokémon que él mismo aporta un bloqueo nunca queda bloqueado (fiel a "excepto este Pokémon").
        val selfProvides = (abilityPassivesRaw(pip) + toolMods(pip)).any { it.mod == ModKind.BLOCK_ABILITY }
        if (selfProvides) return false
        return sources.any { s ->
            val inScope = s.mod.blockBothSides || (s.side != null && side != s.side)
            val notSource = s.sourceId == null || s.sourceId != pip.card.id
            val ruleBoxOk = !s.mod.blockOnlyRuleBox || pip.card.mechanic.hasRuleBox
            inScope && notSource && ruleBoxOk
        }
    }

    /** ¿Tiene [pip] unida ≥1 Energía que aporte el tipo [type]? */
    private fun hasEnergyOfType(pip: PokemonInPlay, type: EnergyType): Boolean =
        pip.attachedEnergy.any { e ->
            when (e) {
                is BasicEnergy -> e.type == type
                is SpecialEnergy -> when (val p = e.provides) {
                    is EnergyProvision.Fixed -> type in p.types
                    EnergyProvision.ChooseOne -> true
                    is EnergyProvision.Conditional -> false
                }
            }
        }

    /**
     * Coste de retirada efectivo del Activo de [side], considerando pasivos de
     * habilidad que lo anulan: Flotación (SELF, condicional a energía del tipo)
     * y Travesía Propulsión (OWN_ALL, cualquier Pokémon propio en juego).
     */
    /**
     * Coste de energía efectivo de un ataque, con el recargo temporal "+{C} para atacar"
     * (Muk — Prisión Viscosa). Auto-expira por turno.
     */
    private fun effectiveAttackCost(
        atk: com.mineralord.tcg.engine.model.Attack,
        attacker: PokemonInPlay,
        state: GameState,
        side: Side,
    ): Int {
        // Nidoking — Rey Entusiasta: sus ataques no cuestan Energía si hay un aliado con el
        // nombre requerido (Nidoqueen) en juego. Respeta el bloqueo de Habilidades.
        val freeByAlly = abilityEffects(state, side, attacker).any { e ->
            e.freeAttackIfAllyNamed != null &&
                state.sideState(side).allInPlay.any { nameMatches(it, e.freeAttackIfAllyNamed!!) }
        }
        if (freeByAlly) return 0
        val bump = if (attacker.attackCostBumpOnTurn == state.turn) attacker.attackCostBumpAmount else 0
        return atk.convertedCost + bump
    }

    private fun effectiveRetreatCost(state: GameState, side: Side): Int {
        val player = state.sideState(side)
        val active = player.active ?: return 0
        // Recargo temporal "+{C} de Coste de Retirada" (Grimer/Muk). Auto-expira por turno.
        val bump = if (active.retreatCostBumpOnTurn == state.turn) active.retreatCostBumpAmount else 0
        val base = active.card.retreatCost.size
        if (base == 0 && bump == 0) return 0
        // Flotación: pasivo SELF condicional a energía del tipo requerido.
        val selfFree = abilityPassives(state, side, active).any {
            it.mod == ModKind.RETREAT_COST && it.appliesTo == Target.SELF &&
                (it.requiresEnergyType == null || hasEnergyOfType(active, it.requiresEnergyType!!))
        }
        if (selfFree) return 0
        // Travesía Propulsión: cualquier Pokémon propio en juego con pasivo OWN_ALL.
        val allFree = player.allInPlay.any { pip ->
            abilityPassives(state, side, pip).any { it.mod == ModKind.RETREAT_COST && it.appliesTo == Target.OWN_ALL }
        }
        return if (allFree) 0 else base + bump
    }

    /**
     * ¿El Activo de [side] tiene BLOQUEADA la retirada por una habilidad rival?
     * Omastar Tentáculos Primordiales: mientras su Omastar esté Activo, el Activo
     * rival no puede retirarse.
     */
    private fun retreatBlockedByOpponent(state: GameState, side: Side): Boolean {
        val oppActive = state.sideState(side.other()).active ?: return false
        return abilityPassives(state, side.other(), oppActive).any { it.mod == ModKind.NO_RETREAT && it.appliesTo == Target.OPP_ACTIVE }
    }

    /**
     * "Contragolpe" del Defensor: dispara las habilidades del Activo de [defenderSide] que
     * reaccionan a haber sido DAÑADO ([Effect.triggerOnActiveDamaged], Hitmonchan) o NOQUEADO
     * ([Effect.triggerOnActiveKO], Weezing) por el ataque recién resuelto. Se ejecuta con
     * `actingSide = defenderSide`, así que las ops que apuntan a `OPP_ACTIVE` golpean al
     * ATACANTE. Corre ANTES de [handleKnockouts] (el Pokémon aún está en juego). Respeta el
     * bloqueo de Habilidades. No hace nada si el Activo no recibió daño de este ataque.
     */
    private fun applyDefenderRetaliation(
        state: GameState,
        defenderSide: Side,
        receivedDamage: Int,
        events: MutableList<GameEvent>,
    ): GameState {
        if (receivedDamage <= 0) return state
        val active = state.sideState(defenderSide).active ?: return state
        val koed = active.damage >= effectiveMaxHp(state, defenderSide, active)
        var working = state

        // Reflejo de daño programado por un ataque previo (Mewtwo — Barrera Reflectante):
        // el Atacante recibe daño crudo igual al infligido a este Pokémon este turno.
        if (active.reflectDamageOnTurn == state.turn) {
            val attackerSide = defenderSide.other()
            val attackerState = working.sideState(attackerSide)
            val attacker = attackerState.active
            if (attacker != null) {
                working = withPlayer(
                    working,
                    attackerState.copy(active = attacker.copy(damage = attacker.damage + receivedDamage)),
                    attackerSide,
                )
                events += GameEvent.DamageDealt(defenderSide, attacker.card.id, receivedDamage, false, false)
            }
        }

        for (ab in active.card.effectiveAbilities(isAbilityLocked(state, defenderSide, active))) {
            val eff = ab.effect?.let { effects[it] } ?: continue
            if (!eff.triggerOnActiveDamaged && !(eff.triggerOnActiveKO && koed)) continue
            val res = interpreter.execute(
                eff, EffectSource(defenderSide, active.card.id), working,
                shuffle = { rng.shuffle(it) }, flip = { rng.flipCoin() },
            )
            working = res.state
            events += res.events
        }
        return working
    }

    private fun handleKnockouts(
        state: GameState,
        koSide: Side,
        events: MutableList<GameEvent>,
        byAttack: Boolean = false,
        extraPrizes: Int = 0,
    ): GameState {
        var target = state.sideState(koSide)
        var active = target.active ?: return state
        // KO por HP EFECTIVO (incluye HP extra de Herramientas tipo Capa/Amuleto y de
        // habilidades condicionales como Wigglytuff ex — Cuerpo Expansivo).
        val maxHp = effectiveMaxHp(state, koSide, active)
        if (active.damage < maxHp) return state

        val attackerSide = koSide.other()

        // Machamp — Agallas: "si fuese a quedar KO por el daño de un ATAQUE, moneda; cara → no
        // queda KO y sus PS restantes pasan a 10". Solo aplica a KO por ataque.
        if (byAttack && !isAbilityLocked(state, koSide, active) &&
            active.card.effectiveAbilities(false).any { ab ->
                ab.effect?.let { effects[it]?.survivesKoWithCoin } == true
            }
        ) {
            val heads = rng.flipCoin()
            events += GameEvent.CoinFlipped(koSide, heads)
            if (heads) {
                // PS restantes = 10 → damage = maxHp - 10.
                val survivor = active.copy(damage = (maxHp - 10).coerceAtLeast(0))
                return withPlayer(state, target.copy(active = survivor), koSide)
            }
        }

        // Raichu — Toma de Tierra: si el Activo queda KO por el ataque del RIVAL, un Raichu en
        // TU Banca puede mover 1 Energía Básica {L} del Noqueado hacia sí antes del descarte.
        if (byAttack && state.activeSide != koSide) {
            val benchIdx = target.bench.indexOfFirst { pip ->
                !isAbilityLocked(state, koSide, pip) &&
                    pip.card.effectiveAbilities(false).any { ab ->
                        ab.effect?.let { effects[it]?.pullsEnergyFromKoAllyType } != null
                    }
            }
            if (benchIdx >= 0) {
                val type = target.bench[benchIdx].card.effectiveAbilities(false)
                    .firstNotNullOf { ab -> ab.effect?.let { effects[it]?.pullsEnergyFromKoAllyType } }
                val energy = active.attachedEnergy.firstOrNull { it is BasicEnergy && it.type == type }
                if (energy != null) {
                    val raichu = target.bench[benchIdx]
                    val newBench = target.bench.toMutableList().apply {
                        this[benchIdx] = raichu.copy(attachedEnergy = raichu.attachedEnergy + energy)
                    }
                    active = active.copy(attachedEnergy = active.attachedEnergy - energy)
                    target = target.copy(active = active, bench = newBench)
                    events += GameEvent.EnergyAttached(koSide, energy.id, raichu.card.id)
                }
            }
        }

        events += GameEvent.KnockedOut(koSide, active.card.id)

        // El atacante toma premios (+ los extra de Clefable — Más Luna), sin exceder los que quedan.
        val prizesToTake = (active.card.prizeValue + extraPrizes)
            .coerceAtMost(state.sideState(attackerSide).prizes.size)
        val attacker = state.sideState(attackerSide)
        val taken = attacker.prizes.take(prizesToTake)
        val attackerAfter = attacker.copy(
            prizes = attacker.prizes - taken.toSet(),
            prizesRemaining = (attacker.prizesRemaining - prizesToTake).coerceAtLeast(0),
            hand = attacker.hand + taken,
        )
        if (prizesToTake > 0) events += GameEvent.PrizeTaken(attackerSide, prizesToTake)

        // El noqueado va al descarte (con lo que llevaba encima).
        val koalition = active.evolutionStack + active.card
        val targetAfter = target.copy(
            active = null,
            discard = target.discard + koalition + active.attachedEnergy + active.attachedTools,
        )

        var next = state
            .let { withPlayer(it, attackerAfter, attackerSide) }
            .let { withPlayer(it, targetAfter, koSide) }

        // Registrar KO "durante el turno del rival" (no cuenta el auto-KO por recoil
        // en el propio turno): habilita cartas condicionales como Mela.
        if (state.activeSide != koSide) {
            next = next.copy(koedLastOppTurn = next.koedLastOppTurn + koSide)
        }

        // Victoria por premios.
        if (attackerAfter.prizesRemaining <= 0) {
            events += GameEvent.GameWon(attackerSide)
            return next.copy(winner = attackerSide, phase = Phase.GAME_OVER)
        }
        // Sin Banca y sin Activo = derrota (no hay Pokémon con quien continuar).
        if (targetAfter.bench.isEmpty()) {
            events += GameEvent.GameWon(attackerSide)
            return next.copy(winner = attackerSide, phase = Phase.GAME_OVER)
        }
        // Con Banca: el lado Noqueado ELIGE su nuevo Activo (arrastrando una carta de la
        // Banca al centro). No se auto-promueve el primero: se marca la promoción pendiente
        // y el motor bloquea el resto de acciones hasta resolverla.
        return next.copy(pendingPromotion = next.pendingPromotion + koSide)
    }

    /**
     * Sube un Pokémon de la Banca al puesto Activo tras un KO. El lado se DEDUCE de
     * [GameState.pendingPromotion] ∩ (dueño de [benchTargetId]): así vale tanto si es
     * el jugador en turno como el rival (p. ej. te noquean en tu ataque por retroceso, o
     * en el chequeo entre turnos). Cura las condiciones especiales al subir (como retirar).
     */
    private fun promoteActive(state: GameState, benchTargetId: CardId): EngineResult {
        val side = state.pendingPromotion.firstOrNull { s ->
            state.sideState(s).bench.any { it.card.id == benchTargetId }
        } ?: return EngineResult.reject(state, "Ese Pokémon no puede subir al puesto Activo")
        val ps = state.sideState(side)
        val promoted = ps.bench.first { it.card.id == benchTargetId }
            .copy(statuses = emptySet())
        val updated = ps.copy(
            active = promoted,
            bench = ps.bench.filterNot { it.card.id == benchTargetId },
        )
        val next = withPlayer(state, updated, side)
            .copy(pendingPromotion = state.pendingPromotion - side)
        return EngineResult(next, listOf(GameEvent.Promoted(side, benchTargetId)))
    }

    // ------------------------------------------------------------- fin de turno

    private fun endTurn(state: GameState): EngineResult {
        val events = mutableListOf<GameEvent>()
        events += GameEvent.TurnEnded(state.activeSide)

        // Daño entre turnos (Veneno/Quemadura) al Activo del jugador que termina.
        var working = applyBetweenTurns(state, state.activeSide, events)
        // Efectos "al final de tu turno": daño diferido (Victreebel) y curación de
        // Herramientas (Restos) sobre el Activo del jugador que termina su turno.
        working = applyEndOfTurnEffects(working, state.activeSide, events)
        working = handleKnockouts(working, state.activeSide, events)
        if (working.isOver) return EngineResult(working, events)

        // Cambio de turno.
        val nextSide = working.activeSide.other()
        val nextTurn = working.turn + 1
        working = working.copy(
            activeSide = nextSide, turn = nextTurn, phase = Phase.DRAW,
            // Límites por turno se reinician al pasar el turno.
            supporterPlayedThisTurn = false,
            energyAttachedThisTurn = false,
            trainerNamesPlayedThisTurn = emptySet(),
            abilitiesUsedThisTurn = emptySet(),
            // El lado que ACABA su turno reinicia su ventana de "KO en turno rival":
            // durante el turno entrante volverá a poblarse si le noquean algo.
            koedLastOppTurn = working.koedLastOppTurn - state.activeSide,
        )
        events += GameEvent.TurnStarted(nextSide, nextTurn)

        // El jugador entrante roba; sin cartas = deck-out (pierde).
        val drawer = working.sideState(nextSide)
        if (drawer.deck.isEmpty()) {
            events += GameEvent.GameWon(nextSide.other())
            return EngineResult(
                working.copy(winner = nextSide.other(), phase = Phase.GAME_OVER),
                events,
            )
        }
        val drawn = drawer.deck.first()
        working = withPlayer(
            working,
            drawer.copy(deck = drawer.deck.drop(1), hand = drawer.hand + drawn),
            nextSide,
        )
        events += GameEvent.CardsDrawn(nextSide, 1)

        // Incrementa el contador de turnos-en-juego del nuevo Activo (permite evolucionar).
        val refreshed = working.sideState(nextSide)
        val tickedInPlay = refreshed.copy(
            active = refreshed.active?.let { it.copy(turnsInPlay = it.turnsInPlay + 1) },
            bench = refreshed.bench.map { it.copy(turnsInPlay = it.turnsInPlay + 1) },
        )
        working = withPlayer(working, tickedInPlay, nextSide).copy(phase = Phase.MAIN)

        return EngineResult(working, events)
    }

    /**
     * Efectos que se resuelven AL FINAL del turno del jugador [side], sobre su Activo:
     * - Daño diferido programado (Victreebel — Ácido de Acción Lenta): si
     *   `delayedDamageOnTurn == state.turn`, pone `delayedDamageAmount` de daño y expira.
     * - Curación de Herramientas (Restos): suma los `healSelfEndOfTurnIfActive` de sus
     *   Herramientas ancladas y reduce el daño (nunca por debajo de 0).
     * El posible KO por el daño diferido lo resuelve el `handleKnockouts` posterior.
     */
    private fun applyEndOfTurnEffects(
        state: GameState,
        side: Side,
        events: MutableList<GameEvent>,
    ): GameState {
        val ps = state.sideState(side)
        var active = ps.active ?: return state

        // 1) Daño diferido (Victreebel).
        if (active.delayedDamageOnTurn == state.turn && active.delayedDamageAmount > 0) {
            val amount = active.delayedDamageAmount
            active = active.copy(
                damage = active.damage + amount,
                delayedDamageOnTurn = null,
                delayedDamageAmount = 0,
            )
            events += GameEvent.DamageDealt(side, active.card.id, amount)
        }

        // 2) Curación de Herramientas al final de tu turno (Restos), solo en el Activo.
        val heal = active.attachedTools.sumOf { effects[it.effect]?.healSelfEndOfTurnIfActive ?: 0 }
        if (heal > 0 && active.damage > 0) {
            val healed = heal.coerceAtMost(active.damage)
            active = active.copy(damage = active.damage - healed)
            events += GameEvent.Healed(side, active.card.id, healed)
        }

        return withPlayer(state, ps.copy(active = active), side)
    }

    private fun applyBetweenTurns(
        state: GameState,
        side: Side,
        events: MutableList<GameEvent>,
    ): GameState {
        val ps = state.sideState(side)
        val active = ps.active ?: return state
        var extra = 0
        if (Status.POISONED in active.statuses) extra += POISON_DAMAGE
        if (Status.BURNED in active.statuses) extra += BURN_DAMAGE
        if (extra == 0) return state
        val damaged = active.copy(damage = active.damage + extra)
        events += GameEvent.DamageDealt(side, active.card.id, extra)
        return withPlayer(state, ps.copy(active = damaged), side)
    }

    // ------------------------------------------------------------------ helpers

    private fun Side.other(): Side = if (this == Side.PLAYER) Side.OPPONENT else Side.PLAYER

    private fun withPlayer(state: GameState, ps: PlayerState, side: Side = ps.side): GameState =
        if (side == Side.PLAYER) state.copy(player = ps) else state.copy(opponent = ps)

    private fun replaceInPlay(ps: PlayerState, id: CardId, replacement: PokemonInPlay): PlayerState =
        when {
            ps.active?.card?.id == id -> ps.copy(active = replacement)
            else -> ps.copy(bench = ps.bench.map { if (it.card.id == id) replacement else it })
        }

    // ------------------------------------------------------- jugadas legales

    /**
     * Enumera los intents legales para el lado en turno en [state]. Sirve a la UI
     * (qué ofrecer al jugador) y a la IA. Con una decisión pendiente devuelve
     * vacío: solo cabe [GameIntent.ResolveDecision], que se construye aparte a
     * partir de `state.interaction`.
     *
     * Es una aproximación conservadora basada en las mismas condiciones que
     * validan los handlers; el motor sigue siendo la autoridad (rechaza lo
     * ilegal), de modo que un falso positivo aquí nunca corrompe el estado.
     */
    fun legalIntents(state: GameState): List<GameIntent> {
        if (state.isOver) return emptyList()
        // Promoción pendiente: la única jugada legal es subir un Básico de la Banca del
        // lado Noqueado. Se ofrecen todas las opciones (la IA/UI elige cuál).
        if (state.awaitingPromotion) {
            return state.pendingPromotion.flatMap { s ->
                state.sideState(s).bench.map { GameIntent.PromoteActive(it.card.id) }
            }
        }
        if (state.awaitingDecision) return emptyList()
        val me = state.activePlayer
        val intents = mutableListOf<GameIntent>()

        // Poner Básicos en la Banca.
        if (me.bench.size < BENCH_LIMIT) {
            me.hand.filterIsInstance<PokemonCard>().filter { it.isBasic }
                .forEach { intents += GameIntent.PlayBasicToBench(it.id) }
        }

        // Evolucionar Pokémon en juego que lleven al menos un turno (no en el turno 1).
        if (state.turn > 1) {
            me.hand.filterIsInstance<PokemonCard>().filter { it.evolvesFrom != null }.forEach { evo ->
                me.allInPlay.filter {
                    it.turnsInPlay >= 1 &&
                        (evo.evolvesFrom == it.card.name.en || evo.evolvesFrom == it.card.name.es)
                }.forEach { target -> intents += GameIntent.Evolve(evo.id, target.card.id) }
            }
        }

        // Unir energía (una por turno) a cualquier Pokémon propio.
        if (!state.energyAttachedThisTurn) {
            me.hand.filterIsInstance<EnergyCard>().forEach { energy ->
                me.allInPlay.forEach { p -> intents += GameIntent.AttachEnergy(energy.id, p.card.id) }
            }
        }

        // Retirarse si hay Activo con energía suficiente (coste efectivo, considerando
        // habilidades de Flotación/Travesía) y no está bloqueado por una habilidad rival.
        val active = me.active
        if (active != null && active.cannotRetreatOnTurn != state.turn &&
            !retreatBlockedByOpponent(state, state.activeSide) &&
            active.attachedEnergyCount >= effectiveRetreatCost(state, state.activeSide)
        ) {
            me.bench.forEach { intents += GameIntent.Retreat(it.card.id) }
        }

        // Atacar con ataques pagables (salvo en el turno 1: quien empieza no ataca,
        // o si el Activo está restringido este turno por Jet Wing y similares).
        if (state.turn > 1 && active?.cannotAttackOnTurn != state.turn) {
            active?.card?.attacks?.filter { active.attachedEnergyCount >= effectiveAttackCost(it, active, state, state.activeSide) }
                ?.forEach { intents += GameIntent.Attack(it.name.es) }
        }

        // Jugar Entrenadores (Apoyo/Objeto) con efecto registrado.
        me.hand.filterIsInstance<TrainerCard>().forEach { trainer ->
            val kind = trainer.kind
            val effect = effects[trainer.effect]
            val playable = (kind is TrainerKind.Item) ||
                (kind is TrainerKind.Supporter && !state.supporterPlayedThisTurn)
            val conditionOk = effect != null &&
                (!effect.requiresOwnKoLastTurn || state.activeSide in state.koedLastOppTurn)
            if (playable && conditionOk) {
                intents += GameIntent.PlayTrainer(trainer.id)
            }
        }

        // Jugar un Estadio: si no hay ya uno del mismo nombre en campo. No requiere efecto registrado.
        me.hand.filterIsInstance<TrainerCard>().filter { it.kind is TrainerKind.Stadium }.forEach { st ->
            val sameName = state.stadium?.let { it.name.en == st.name.en || it.name.es == st.name.es } ?: false
            if (!sameName) intents += GameIntent.PlayTrainer(st.id)
        }

        // Anclar Herramientas: a un Pokémon PROPIO sin Herramienta (máx. 1/Pokémon); si
        // la Herramienta permite ANY, también a los del rival sin Herramienta.
        me.hand.filterIsInstance<TrainerCard>().forEach { trainer ->
            val kind = trainer.kind
            if (kind is TrainerKind.Tool) {
                me.allInPlay.filter { it.attachedTools.isEmpty() }
                    .forEach { intents += GameIntent.AttachTool(trainer.id, it.card.id) }
                if (kind.attachTo == ToolTarget.ANY) {
                    state.sideState(state.activeSide.other()).allInPlay
                        .filter { it.attachedTools.isEmpty() }
                        .forEach { intents += GameIntent.AttachTool(trainer.id, it.card.id) }
                }
            }
        }

        // Usar habilidades registradas que pasen activeOnly / oncePerTurn (y no estén bloqueadas).
        me.allInPlay.forEach { p ->
            val locked = isAbilityLocked(state, me.side, p)
            p.card.abilities.forEach { ability ->
                if (locked && ability.kind.suppressibleByAbilityLock) return@forEach
                val eff = ability.effect?.let { effects[it] }
                if (eff != null && eff.ops.isNotEmpty() &&   // ops vacías = pasivo: no se "activa"
                    !eff.triggerOnEvolve &&                  // se dispara sola al evolucionar
                    !eff.triggerOnActiveDamaged && !eff.triggerOnActiveKO &&  // se disparan solas al recibir daño
                    (!eff.activeOnly || me.active?.card?.id == p.card.id) &&
                    (!eff.oncePerTurn || p.card.id !in state.abilitiesUsedThisTurn)
                ) {
                    intents += GameIntent.UseAbility(p.card.id, ability.name.es)
                }
            }
        }

        intents += GameIntent.EndTurn
        return intents
    }

    companion object {
        const val BENCH_LIMIT = 5
        const val POISON_DAMAGE = 10
        const val BURN_DAMAGE = 20
    }
}
