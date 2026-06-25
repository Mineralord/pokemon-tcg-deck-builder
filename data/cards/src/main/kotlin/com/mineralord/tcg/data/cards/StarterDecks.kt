package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.CardId

/** Una entrada de baraja: una carta y cuántas copias. */
data class DeckEntry(val cardId: CardId, val count: Int)

/** Baraja preconstruida (60 cartas). */
data class StarterDeck(
    val id: String,
    val name: String,
    val headliner: CardId,
    val entries: List<DeckEntry>,
) {
    val totalCards: Int get() = entries.sumOf { it.count }

    /** Lista expandida de ids (con repeticiones), útil para sembrar la colección. */
    fun expandedCardIds(): List<CardId> = entries.flatMap { e -> List(e.count) { e.cardId } }
}

/**
 * Las tres barajas de la Academia de Combate 2024 (Battle Academy 2024), ya
 * desbloqueadas desde el inicio. Las listas son datos factuales tomados de la
 * decklist oficial del producto; cada id mapea a una carta del catálogo
 * (`:data:cards`). Las cartas que no estaban en el dataset se añadieron al
 * recurso desde pokemontcg.io con sus stats reales.
 */
object StarterDecks {

    private fun e(id: String, count: Int) = DeckEntry(CardId(id), count)

    val PIKACHU = StarterDeck(
        id = "ba2024-pikachu-ex",
        name = "Pikachu ex",
        headliner = CardId("svp-106"),
        entries = listOf(
            e("svp-106", 1), e("sv2-66", 4), e("sv2-67", 3), e("svp-107", 4),
            e("svp-108", 3), e("svp-109", 2), e("sv1-69", 1), e("sv1-77", 2),
            e("sv2-82", 1), e("svp-148", 1), e("sv1-170", 1), e("sv2-183", 4),
            e("sv1-175", 1), e("sv1-180", 4), e("sv1-181", 1), e("svp-114", 2),
            e("sv1-188", 2), e("sv1-194", 2), e("sv1-198", 3),
            e("energy-basic-lightning-energy", 18),
        ),
    )

    val ARMAROUGE = StarterDeck(
        id = "ba2024-armarouge-ex",
        name = "Armarouge ex",
        headliner = CardId("svp-105"),
        entries = listOf(
            e("sv1-33", 3), e("sv1-34", 2), e("sv1-35", 2), e("sv3-40", 2),
            e("sv3-41", 1), e("sv4-23", 4), e("sv2-36", 3), e("sv1-38", 1),
            e("sv2-39", 3), e("svp-105", 1), e("sv2-183", 4), e("sv1-175", 1),
            e("sv4-167", 1), e("sv1-180", 4), e("sv1-181", 1), e("svp-114", 2),
            e("sv1-188", 2), e("sv1-194", 2), e("sv1-198", 3),
            e("energy-basic-fire-energy", 18),
        ),
    )

    val DARKRAI = StarterDeck(
        id = "ba2024-darkrai-ex",
        name = "Darkrai ex",
        headliner = CardId("svp-110"),
        entries = listOf(
            e("sv2-137", 2), e("svp-110", 1), e("svp-111", 4), e("svp-112", 3),
            e("svp-113", 2), e("sv4-118", 2), e("sv3-139", 4), e("sv3-140", 3),
            e("sv1-164", 1), e("sv2-172", 1), e("sv2-183", 4), e("sv1-175", 1),
            e("sv1-180", 4), e("sv1-181", 1), e("svp-114", 2), e("sv1-188", 2),
            e("sv1-194", 2), e("sv1-198", 3),
            e("energy-basic-darkness-energy", 18),
        ),
    )

    /** Las tres barajas desbloqueadas de inicio. */
    val ALL: List<StarterDeck> = listOf(PIKACHU, ARMAROUGE, DARKRAI)
}
