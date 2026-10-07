package com.mineralord.tcg.data.cards

import kotlinx.serialization.Serializable

/**
 * DTOs que reflejan la estructura de `data/cartas-db.js` (volcado a JSON en
 * resources). Son intencionadamente laxos (campos nullable) porque el dataset
 * mezcla Pokémon/Entrenador/Energía con campos opcionales. El mapeo al modelo
 * de dominio (`:engine:model`) vive en [CardMapper].
 */
@Serializable
data class CardDbDto(
    val totalCartas: Int = 0,
    val cartas: List<CardDto> = emptyList(),
)

/**
 * Índice de expansiones separadas (`/cards/index.json`). Cada expansión vive en su
 * propio archivo bajo `cards/<serie>/<expansion>.json`; las energías básicas van
 * aparte (no son expansión). El `officialCode` es el código impreso por Pokémon
 * Company (MEW=151, SSP=Surging Sparks…) — SOLO metadato; los `id` internos de las
 * cartas no lo usan.
 */
@Serializable
data class CardIndexDto(
    val sets: List<CardSetRefDto> = emptyList(),
    val energies: String? = null,
)

@Serializable
data class CardSetRefDto(
    val code: String = "",
    val officialCode: String? = null,
    val serie: String? = null,
    val expansion: String? = null,
    val file: String,
)

@Serializable
data class CardDto(
    val id: String,
    val nombre: String,
    val supertipo: String,
    /** Subtipos de la carta (p. ej. "Pasado"/"Futuro" de los Pokémon Paradoja). Vacío = ninguno. */
    val subtipos: List<String> = emptyList(),
    val fase: String? = null,
    val evolucionaDe: String? = null,
    val ps: String? = null,
    val tipos: List<String> = emptyList(),
    val habilidades: List<AbilityDto> = emptyList(),
    val ataques: List<AttackDto> = emptyList(),
    val debilidades: List<TypeModDto> = emptyList(),
    val resistencias: List<TypeModDto> = emptyList(),
    val costoRetirada: List<String> = emptyList(),
    val numeroCarta: String? = null,
    /** Número(s) de Pokédex NACIONAL de la especie (para gritos, ordenación, etc.). */
    val numeroPokedex: List<Int> = emptyList(),
    val rareza: String? = null,
    val marcaRegulacion: String? = null,
    val reglas: List<String> = emptyList(),
    val imagenChica: String? = null,
    val imagenGrande: String? = null,
    val set: SetDto? = null,
    val es: EsDto? = null,
    /** Rasgo Antiguo (era XY). Ausente en la inmensa mayoría de cartas (SV no los usa). */
    val rasgoAntiguo: AncientTraitDto? = null,
)

@Serializable
data class AttackDto(
    val name: String = "",
    val cost: List<String> = emptyList(),
    val convertedEnergyCost: Int = 0,
    val damage: String? = null,
    val text: String? = null,
)

@Serializable
data class AbilityDto(
    val name: String = "",
    val text: String? = null,
    /**
     * Clase impresa del poder: "Ability" (moderno), "Poké-Power"/"Pokémon Power" o
     * "Poké-Body" (pre-2011). null/ausente = se asume "Ability" (cartas modernas SV).
     */
    val type: String? = null,
)

@Serializable
data class AncientTraitDto(
    val name: String = "",
    val text: String? = null,
    val nombreEs: String? = null,
    val textoEs: String? = null,
)

@Serializable
data class TypeModDto(
    val type: String = "",
    val value: String = "",
)

@Serializable
data class SetDto(
    val nombre: String? = null,
    val serie: String? = null,
    val simbolo: String? = null,
    val logo: String? = null,
)

@Serializable
data class EsDto(
    val nombre: String? = null,
    val ataques: List<EsAttackDto> = emptyList(),
    val habilidades: List<EsAbilityDto> = emptyList(),
    val imagenChica: String? = null,
    val imagenGrande: String? = null,
)

@Serializable
data class EsAbilityDto(
    val name: String? = null,
    val text: String? = null,
)

@Serializable
data class EsAttackDto(
    val name: String? = null,
    val text: String? = null,
)
