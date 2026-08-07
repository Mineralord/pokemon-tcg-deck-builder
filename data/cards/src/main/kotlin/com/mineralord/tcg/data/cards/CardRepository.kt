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
        /** Índice de expansiones separadas (un archivo por set). */
        const val INDEX_RESOURCE = "/cards/index.json"
        /** Volcado monolítico legado (solo como respaldo si no existe el índice). */
        const val RESOURCE = "/cartas-db.json"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        private fun resource(path: String): String? =
            CardRepository::class.java.getResourceAsStream(path)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

        /**
         * Carga el catálogo desde los recursos del classpath. Cada expansión vive
         * en su propio archivo (`/cards/<setCode>.json`), listadas en
         * `/cards/index.json` — así ninguna expansión mezcla datos con otra. Si el
         * índice no existe (proyecto sin migrar), cae al volcado monolítico legado.
         */
        fun load(): CardRepository {
            val indexJson = resource(INDEX_RESOURCE)
            if (indexJson != null) {
                val index = json.decodeFromString(CardIndexDto.serializer(), indexJson)
                val files = index.sets.map { it.file } + listOfNotNull(index.energies)
                val cards = files.flatMap { rel ->
                    val body = resource("/cards/$rel")
                        ?: error("Falta el recurso /cards/$rel declarado en el índice")
                    json.decodeFromString(CardDbDto.serializer(), body).cartas
                }.map { CardMapper.map(it) }
                return CardRepository(cards)
            }
            val legacy = resource(RESOURCE)
                ?: error("No se encontró ni $INDEX_RESOURCE ni $RESOURCE en el classpath")
            return fromJson(legacy)
        }

        /** Carga desde una cadena JSON (útil para inyectar datos en tests). */
        fun fromJson(content: String): CardRepository {
            val db = json.decodeFromString(CardDbDto.serializer(), content)
            return CardRepository(db.cartas.map { CardMapper.map(it) })
        }
    }
}
