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

@Serializable
data class CardDto(
    val id: String,
    val nombre: String,
    val supertipo: String,
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
    val rareza: String? = null,
    val marcaRegulacion: String? = null,
    val reglas: List<String> = emptyList(),
    val imagenChica: String? = null,
    val imagenGrande: String? = null,
    val set: SetDto? = null,
    val es: EsDto? = null,
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
    val imagenChica: String? = null,
    val imagenGrande: String? = null,
)

@Serializable
data class EsAttackDto(
    val name: String? = null,
    val text: String? = null,
)
