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
    @Serializable data class Retreat(val benchTarget: String) : GameIntentDto
    @Serializable data class Attack(val attackName: String) : GameIntentDto
    @Serializable data class PlayTrainer(val card: String) : GameIntentDto
    @Serializable data class UseAbility(val pokemon: String, val abilityName: String) : GameIntentDto
    @Serializable data class ResolveDecision(val chosen: List<String>) : GameIntentDto
    @Serializable data object EndTurn : GameIntentDto
}

fun GameIntent.toDto(): GameIntentDto = when (this) {
    is GameIntent.PlayBasicToBench -> GameIntentDto.PlayBasicToBench(card.raw)
    is GameIntent.Evolve -> GameIntentDto.Evolve(evolution.raw, onto.raw)
    is GameIntent.AttachEnergy -> GameIntentDto.AttachEnergy(energy.raw, to.raw)
    is GameIntent.Retreat -> GameIntentDto.Retreat(benchTarget.raw)
    is GameIntent.Attack -> GameIntentDto.Attack(attackName)
    is GameIntent.PlayTrainer -> GameIntentDto.PlayTrainer(card.raw)
    is GameIntent.UseAbility -> GameIntentDto.UseAbility(pokemon.raw, abilityName)
    is GameIntent.ResolveDecision -> GameIntentDto.ResolveDecision(chosen.map { it.raw })
    GameIntent.EndTurn -> GameIntentDto.EndTurn
}

fun GameIntentDto.toIntent(): GameIntent = when (this) {
    is GameIntentDto.PlayBasicToBench -> GameIntent.PlayBasicToBench(CardId(card))
    is GameIntentDto.Evolve -> GameIntent.Evolve(CardId(evolution), CardId(onto))
    is GameIntentDto.AttachEnergy -> GameIntent.AttachEnergy(CardId(energy), CardId(to))
    is GameIntentDto.Retreat -> GameIntent.Retreat(CardId(benchTarget))
    is GameIntentDto.Attack -> GameIntent.Attack(attackName)
    is GameIntentDto.PlayTrainer -> GameIntent.PlayTrainer(CardId(card))
    is GameIntentDto.UseAbility -> GameIntent.UseAbility(CardId(pokemon), abilityName)
    is GameIntentDto.ResolveDecision -> GameIntent.ResolveDecision(chosen.map { CardId(it) })
    GameIntentDto.EndTurn -> GameIntent.EndTurn
}

/**
 * Mensaje que viaja por el [MatchTransport] entre los dos dispositivos.
 * Host-autoritativo: el invitado envía [Intent]; el host responde con [Snapshot].
 */
@Serializable
sealed interface NetMessage {
    /** Handshake inicial: identidad + mazo elegido (ids de carta expandidos). */
    @Serializable
    data class Hello(val playerName: String, val deck: List<String>) : NetMessage

    /** El invitado solicita una jugada. */
    @Serializable
    data class Intent(val intent: GameIntentDto) : NetMessage

    /** El host publica el estado tras aplicar una jugada. [seq] ordena snapshots. */
    @Serializable
    data class Snapshot(val seq: Int, val state: GameStateDto) : NetMessage

    /** Una línea ya localizada del registro de combate (para el invitado). */
    @Serializable
    data class LogLine(val text: String) : NetMessage

    /** Señal de animación para el invitado (ver FxCue en :feature:game). */
    @Serializable
    data class Fx(val kind: String, val side: String?, val amount: Int = 0) : NetMessage

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
