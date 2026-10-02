package com.mineralord.tcg.data.netplay

import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.rules.GameIntent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DTO serializable de un [GameIntent]. Los intents solo contienen ids/strings,
 * así que el mapeo es directo y reversible.
 */
@Serializable
sealed interface GameIntentDto {
    @Serializable data class PlayBasicToBench(val card: String) : GameIntentDto
    @Serializable data class Evolve(val evolution: String, val onto: String) : GameIntentDto
    @Serializable data class AttachEnergy(val energy: String, val to: String) : GameIntentDto
    @Serializable data class AttachTool(val tool: String, val target: String) : GameIntentDto
    @Serializable data class Retreat(val benchTarget: String) : GameIntentDto
    @Serializable data class PromoteActive(val benchTarget: String) : GameIntentDto
    @Serializable data class Attack(val attackName: String, val attacker: String? = null) : GameIntentDto
    @Serializable data class PlayTrainer(val card: String) : GameIntentDto
    @Serializable data class UseAbility(val pokemon: String, val abilityName: String) : GameIntentDto
    @Serializable data class ResolveDecision(val chosen: List<String>) : GameIntentDto
    @Serializable data class ResolveLuckyBonus(val chansey: String, val toBench: Boolean) : GameIntentDto
    @Serializable data class DiscardFossil(val target: String) : GameIntentDto
    @Serializable data class UseStadium(val energy: String) : GameIntentDto
    @Serializable data object EndTurn : GameIntentDto
}

fun GameIntent.toDto(): GameIntentDto = when (this) {
    is GameIntent.PlayBasicToBench -> GameIntentDto.PlayBasicToBench(card.raw)
    is GameIntent.Evolve -> GameIntentDto.Evolve(evolution.raw, onto.raw)
    is GameIntent.AttachEnergy -> GameIntentDto.AttachEnergy(energy.raw, to.raw)
    is GameIntent.AttachTool -> GameIntentDto.AttachTool(tool.raw, target.raw)
    is GameIntent.Retreat -> GameIntentDto.Retreat(benchTarget.raw)
    is GameIntent.PromoteActive -> GameIntentDto.PromoteActive(benchTarget.raw)
    is GameIntent.Attack -> GameIntentDto.Attack(attackName, attacker?.raw)
    is GameIntent.PlayTrainer -> GameIntentDto.PlayTrainer(card.raw)
    is GameIntent.UseAbility -> GameIntentDto.UseAbility(pokemon.raw, abilityName)
    is GameIntent.ResolveDecision -> GameIntentDto.ResolveDecision(chosen.map { it.raw })
    is GameIntent.ResolveLuckyBonus -> GameIntentDto.ResolveLuckyBonus(chansey.raw, toBench)
    is GameIntent.DiscardFossil -> GameIntentDto.DiscardFossil(target.raw)
    is GameIntent.UseStadium -> GameIntentDto.UseStadium(energy.raw)
    GameIntent.EndTurn -> GameIntentDto.EndTurn
}

fun GameIntentDto.toIntent(): GameIntent = when (this) {
    is GameIntentDto.PlayBasicToBench -> GameIntent.PlayBasicToBench(CardId(card))
    is GameIntentDto.Evolve -> GameIntent.Evolve(CardId(evolution), CardId(onto))
    is GameIntentDto.AttachEnergy -> GameIntent.AttachEnergy(CardId(energy), CardId(to))
    is GameIntentDto.AttachTool -> GameIntent.AttachTool(CardId(tool), CardId(target))
    is GameIntentDto.Retreat -> GameIntent.Retreat(CardId(benchTarget))
    is GameIntentDto.PromoteActive -> GameIntent.PromoteActive(CardId(benchTarget))
    is GameIntentDto.Attack -> GameIntent.Attack(attackName, attacker?.let { CardId(it) })
    is GameIntentDto.PlayTrainer -> GameIntent.PlayTrainer(CardId(card))
    is GameIntentDto.UseAbility -> GameIntent.UseAbility(CardId(pokemon), abilityName)
    is GameIntentDto.ResolveDecision -> GameIntent.ResolveDecision(chosen.map { CardId(it) })
    is GameIntentDto.ResolveLuckyBonus -> GameIntent.ResolveLuckyBonus(CardId(chansey), toBench)
    is GameIntentDto.DiscardFossil -> GameIntent.DiscardFossil(CardId(target))
    is GameIntentDto.UseStadium -> GameIntent.UseStadium(CardId(energy))
    GameIntentDto.EndTurn -> GameIntent.EndTurn
}

/**
 * DTO serializable de una señal de animación de combate (espejo de `FxCue` de la capa de UI, que este
 * módulo de datos no puede importar). El host mapea sus `FxCue` a estos DTO —VOLTEANDO el lado a la
 * perspectiva del invitado— y los envía en un [NetMessage.FxBatch]; el invitado los revive con el MISMO
 * pipeline de animación que el host y que el modo PvE, para que rótulos/ataques/daño/KO se vean igual.
 *
 * [side] es el nombre del enum `Side` ("PLAYER"/"OPPONENT") o null (moneda neutral compartida).
 */
@Serializable
sealed interface FxCueDto {
    @Serializable data class Attack(val side: String, val attackName: String, val attacker: String? = null) : FxCueDto
    @Serializable data class AbilityUse(val side: String, val pokemon: String, val manual: Boolean) : FxCueDto
    @Serializable data class Damage(val side: String, val amount: Int, val weakness: Boolean, val resistance: Boolean) : FxCueDto
    @Serializable data class Heal(val side: String, val amount: Int) : FxCueDto
    @Serializable data class Knockout(val side: String) : FxCueDto
    @Serializable data class Prize(val side: String, val count: Int) : FxCueDto
    @Serializable data class Coin(val side: String?, val heads: Boolean) : FxCueDto
    @Serializable data class StadiumPlaced(val side: String, val card: String) : FxCueDto
    @Serializable data class Cry(val side: String, val card: String) : FxCueDto
}

/**
 * Mensaje que viaja por el [MatchTransport] entre los dos dispositivos.
 * Host-autoritativo: el invitado envía [Intent]; el host responde con [Snapshot].
 */
@Serializable
sealed interface NetMessage {
    /** Handshake inicial: identidad + nombre de la baraja + mazo elegido (ids
     *  de carta expandidos). [deckName] es solo informativo (log/UI); la partida
     *  se arma con [deck]. */
    @Serializable
    data class Hello(
        val playerName: String,
        val deck: List<String>,
        val deckName: String = "Baraja",
    ) : NetMessage

    /** El invitado solicita una jugada. */
    @Serializable
    data class Intent(val intent: GameIntentDto) : NetMessage

    /** El host publica el estado tras aplicar una jugada. [seq] ordena snapshots.
     *  El estado ya viene CENSURADO y en la perspectiva del destinatario (ver
     *  [GameState.toDtoFor]). */
    @Serializable
    data class Snapshot(val seq: Int, val state: GameStateDto) : NetMessage

    /** Guest → host: pide que le reenvíen el snapshot ACTUAL (tras un corte o hueco).
     *  El host responde con un nuevo [Snapshot]; es idempotente en el guest, que
     *  descarta los de `seq` menor al último visto. Transport-agnóstico: indispensable
     *  para transportes NO durables (p. ej. Nearby, que pierde paquetes sin cola). */
    @Serializable
    data object RequestSnapshot : NetMessage

    // ---- Ceremonia inicial (host-autoritativo) ----

    /** Guest → host: el INVITADO llama cara (true) o cruz (false). El host la lanza. */
    @Serializable
    data class CoinCall(val heads: Boolean) : NetMessage

    /** Host → ambos: resultado del volado. [winnerIsHost] = ganó el host.
     *  El ganador decide el orden con [ChooseOrder]. */
    @Serializable
    data class CoinResult(val heads: Boolean, val winnerIsHost: Boolean) : NetMessage

    /** Ganador del volado → host: ¿empieza el que ganó? (si el guest ganó, lo envía él). */
    @Serializable
    data class ChooseOrder(val winnerGoesFirst: Boolean) : NetMessage

    /** Guest → host: su preparación (Activo + Banca), por id de instancia. */
    @Serializable
    data class SetupChoice(val activeId: String, val benchIds: List<String>) : NetMessage

    /** Host → guest: fase/marcha de la ceremonia (para que el guest muestre la UI adecuada).
     *  [phase] ∈ {COIN, DEAL, SETUP, REVEAL, PLAY}. */
    @Serializable
    data class Ceremony(val phase: String) : NetMessage

    /** Reparto animado para el guest: su mano inicial (ids de instancia). */
    @Serializable
    data class Deal(val hand: List<String>) : NetMessage

    /** Una línea ya localizada del registro de combate (para el invitado). */
    @Serializable
    data class LogLine(val text: String) : NetMessage

    /** Señal de animación puntual para el invitado (legado; la moneda de ceremonia). */
    @Serializable
    data class Fx(val kind: String, val side: String?, val amount: Int = 0) : NetMessage

    /** Lote ORDENADO de animaciones de combate para el invitado, ya en SU perspectiva (lados volteados
     *  por el host). El invitado lo reproduce con el mismo espaciado que el host/PvE. */
    @Serializable
    data class FxBatch(val cues: List<FxCueDto>) : NetMessage

    /** Fin de la conexión / abandono. */
    @Serializable
    data class Bye(val reason: String) : NetMessage
}

/** Codec JSON compartido (tolerante a campos extra para evolución del esquema). */
val NetJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "t"
}

fun NetMessage.encode(): String = NetJson.encodeToString(NetMessage.serializer(), this)
fun decodeNetMessage(raw: String): NetMessage = NetJson.decodeFromString(NetMessage.serializer(), raw)
