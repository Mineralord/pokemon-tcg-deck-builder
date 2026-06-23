package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import kotlinx.serialization.json.Json

/**
 * Carga el catálogo de cartas desde el JSON empaquetado (volcado de la base de
 * datos web) y lo expone como modelo de dominio fuertemente tipado.
 *
 * Es Kotlin puro: lee el recurso del classpath, así que sirve igual en tests
 * JVM y en la app Android (el recurso se empaqueta en el AAR/APK).
 */
class CardRepository private constructor(private val cards: List<Card>) {

    val all: List<Card> get() = cards
    val size: Int get() = cards.size
    private val byId: Map<CardId, Card> by lazy { cards.associateBy { it.id } }

    operator fun get(id: CardId): Card? = byId[id]
    fun byIds(ids: Collection<CardId>): List<Card> = ids.mapNotNull { byId[it] }

    companion object {
        const val RESOURCE = "/cartas-db.json"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Carga desde el recurso del classpath (uso normal en app y tests). */
        fun load(): CardRepository {
            val stream = CardRepository::class.java.getResourceAsStream(RESOURCE)
                ?: error("No se encontró el recurso $RESOURCE en el classpath")
            return fromJson(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }

        /** Carga desde una cadena JSON (útil para inyectar datos en tests). */
        fun fromJson(content: String): CardRepository {
            val db = json.decodeFromString(CardDbDto.serializer(), content)
            return CardRepository(db.cartas.map { CardMapper.map(it) })
        }
    }
}
