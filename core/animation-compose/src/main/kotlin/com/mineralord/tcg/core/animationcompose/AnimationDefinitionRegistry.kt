package com.mineralord.tcg.core.animationcompose

import com.mineralord.tcg.core.animation.AnimationDefinition
import com.mineralord.tcg.core.animation.AnimationRequest
import kotlin.reflect.KClass

/**
 * Fábrica de la [AnimationDefinition] para una [AnimationRequest] concreta. Es una
 * FUNCIÓN (no un mapa estático) porque la receta puede depender de los datos de la
 * request (p. ej. `CardDrawn.cardId`).
 */
fun interface AnimationDefinitionFactory<in R : AnimationRequest> {
    fun create(request: R): AnimationDefinition
}

/**
 * Contribuidor de recetas de animación de UN módulo/feature.
 *
 * Diseño DISTRIBUIDO: cada módulo registra SUS propias recetas en su propio código,
 * sin un god-object central. `core:animation-compose` sólo define el contrato y la
 * fusión; no conoce ninguna receta concreta (Clean Architecture: la dependencia
 * apunta hacia adentro).
 */
fun interface AnimationDefinitionContributor {
    fun contribute(builder: AnimationDefinitionRegistryBuilder)
}

/**
 * Constructor que acumula las contribuciones y detecta colisiones antes de sellar el
 * registro.
 */
class AnimationDefinitionRegistryBuilder {

    private val factories =
        HashMap<KClass<out AnimationRequest>, AnimationDefinitionFactory<AnimationRequest>>()

    /**
     * Registra la fábrica para un tipo de request. Clave por [KClass] del subtipo
     * `sealed` (seguridad de tipos). Falla RUIDOSAMENTE si dos módulos registran el
     * mismo tipo: una colisión es un bug de configuración, no algo a resolver en
     * silencio (fail-fast en arranque).
     */
    @Suppress("UNCHECKED_CAST")
    fun <R : AnimationRequest> register(type: KClass<R>, factory: AnimationDefinitionFactory<R>) {
        require(type !in factories) {
            "Definición de animación duplicada para ${type.simpleName}: dos contribuidores la registran."
        }
        factories[type] = factory as AnimationDefinitionFactory<AnimationRequest>
    }

    internal fun build(): AnimationDefinitionRegistry = AnimationDefinitionRegistry(factories.toMap())
}

/**
 * Registro sellado de recetas: resuelve [AnimationRequest] → [AnimationDefinition].
 *
 * Se construye con [from], que fusiona todos los contribuidores en el composition root.
 * El descubrimiento de contribuidores es EXPLÍCITO (lista inyectada), no por reflexión
 * mágica: mantiene el grafo auditable.
 */
class AnimationDefinitionRegistry internal constructor(
    private val factories: Map<KClass<out AnimationRequest>, AnimationDefinitionFactory<AnimationRequest>>,
) {

    /** Resuelve la definición de una request, o `null` si ningún módulo la aporta. */
    fun resolve(request: AnimationRequest): AnimationDefinition? =
        factories[request::class]?.create(request)

    /** ¿Hay una receta registrada para este tipo de request? */
    fun contains(type: KClass<out AnimationRequest>): Boolean = type in factories

    val size: Int get() = factories.size

    companion object {
        /** Fusiona los contribuidores dados en un registro sellado (fail-fast). */
        fun from(contributors: List<AnimationDefinitionContributor>): AnimationDefinitionRegistry {
            val builder = AnimationDefinitionRegistryBuilder()
            contributors.forEach { it.contribute(builder) }
            return builder.build()
        }
    }
}
