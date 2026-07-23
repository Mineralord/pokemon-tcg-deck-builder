package com.mineralord.tcg.engine.model

/**
 * Registro central de efectos, AGREGADOR por expansión. Cada set aporta sus
 * registros en su propio archivo (`Set151Effects.kt`, `AcademiaDecksEffects.kt`,
 * …) mediante una función de extensión `registerXxx()` sobre el mapa. Añadir una
 * expansión nueva = crear su archivo + una línea `registerXxx()` aquí; ninguna
 * expansión mezcla datos con otra.
 *
 * Claves de efecto (ver `EffectKeys.kt`): "<cardId>#atk:<nombreEn>",
 * "<cardId>#abi:<nombreEn>", o el id de carta a secas para Entrenador/Energía.
 */
object EffectsDb {

    /** Clave de ataque — expuesta para tests y llamadas externas. */
    fun atkKey(cardId: String, attackEn: String): EffectId =
        com.mineralord.tcg.engine.model.atkKey(cardId, attackEn)

    /** Clave de habilidad — expuesta para tests y llamadas externas. */
    fun abiKey(cardId: String, abilityEn: String): EffectId =
        com.mineralord.tcg.engine.model.abiKey(cardId, abilityEn)

    val registry: EffectRegistry = EffectRegistry(
        buildMap {
            registerAcademiaDecks()
            registerSet151()
        },
    )
}
