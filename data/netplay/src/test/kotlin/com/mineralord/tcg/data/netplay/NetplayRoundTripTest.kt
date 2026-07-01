package com.mineralord.tcg.data.netplay

import com.mineralord.tcg.data.cards.CardRepository
import com.mineralord.tcg.data.cards.StarterDecks
import com.mineralord.tcg.engine.model.Side
import com.mineralord.tcg.engine.rules.GameIntent
import com.mineralord.tcg.engine.rules.GameSetup
import com.mineralord.tcg.engine.rules.SeededRng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica que el estado e intents sobreviven el viaje por la red:
 * GameState -> DTO -> JSON -> DTO -> GameState (rehidratado con el repo).
 */
class NetplayRoundTripTest {

    private val repo = CardRepository.load()

    private fun freshState() = GameSetup.start(
        playerDeck = StarterDecks.ALL[0].expandedCardIds().mapNotNull { repo[it] },
        opponentDeck = StarterDecks.ALL[1].expandedCardIds().mapNotNull { repo[it] },
        rng = SeededRng(42),
    )

    @Test
    fun `game state survives json round trip`() {
        val original = freshState()

        val json = NetJson.encodeToString(GameStateDto.serializer(), original.toDto())
        val restored = NetJson.decodeFromString(GameStateDto.serializer(), json).toGameState(repo)

        assertEquals(original.turn, restored.turn)
        assertEquals(original.activeSide, restored.activeSide)
        assertEquals(original.phase, restored.phase)
        assertEquals(original.player.active?.card?.id, restored.player.active?.card?.id)
        assertEquals(original.player.hand.map { it.id }, restored.player.hand.map { it.id })
        assertEquals(original.player.deck.size, restored.player.deck.size)
        assertEquals(original.player.prizesRemaining, restored.player.prizesRemaining)
        assertEquals(original.opponent.bench.map { it.card.id }, restored.opponent.bench.map { it.card.id })
    }

    @Test
    fun `dynamic pokemon state survives round trip`() {
        val base = freshState()
        // Inyecta daño + energía + estado en el activo para probar el estado dinámico.
        val active = base.player.active!!
        val withDamage = base.copy(
            player = base.player.copy(
                active = active.copy(
                    damage = 40,
                    statuses = setOf(com.mineralord.tcg.engine.model.Status.POISONED),
                    turnsInPlay = 3,
                ),
            ),
        )

        val restored = withDamage.toDto()
            .let { NetJson.encodeToString(GameStateDto.serializer(), it) }
            .let { NetJson.decodeFromString(GameStateDto.serializer(), it) }
            .toGameState(repo)

        val r = restored.player.active!!
        assertEquals(40, r.damage)
        assertEquals(3, r.turnsInPlay)
        assertTrue(com.mineralord.tcg.engine.model.Status.POISONED in r.statuses)
    }

    @Test
    fun `all intents survive round trip`() {
        val intents = listOf(
            GameIntent.PlayBasicToBench(repo.all.first().id),
            GameIntent.AttachEnergy(repo.all[1].id, repo.all[2].id),
            GameIntent.Attack("Trueno"),
            GameIntent.Retreat(repo.all[3].id),
            GameIntent.UseAbility(repo.all[4].id, "Carga"),
            GameIntent.ResolveDecision(listOf(repo.all[5].id, repo.all[6].id)),
            GameIntent.EndTurn,
        )
        for (intent in intents) {
            val msg: NetMessage = NetMessage.Intent(intent.toDto())
            val restored = decodeNetMessage(msg.encode())
            assertTrue(restored is NetMessage.Intent)
            assertEquals(intent, (restored as NetMessage.Intent).intent.toIntent())
        }
    }

    @Test
    fun `snapshot message survives round trip`() {
        val msg = NetMessage.Snapshot(seq = 7, state = freshState().toDto())
        val restored = decodeNetMessage(msg.encode())
        assertTrue(restored is NetMessage.Snapshot)
        assertEquals(7, (restored as NetMessage.Snapshot).seq)
    }
}
