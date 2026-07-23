package com.mineralord.tcg.studio.assets

/**
 * **Asset: cualquier recurso curado del Studio (fuente única de verdad).**
 *
 * El Asset Registry no pertenece a las animaciones: pertenece al Studio. Este contrato es el
 * denominador común de *todo* recurso que el Studio pueda curar a lo largo de los años. Hoy sólo
 * existe un subtipo ([AnimationAsset]); mañana podrán existir Audio, Partículas, Shaders, Modelos,
 * UI, Fuentes, Temas… **sin rediseñar** este sistema: cada nuevo tipo es un valor de [AssetType] y un
 * subtipo de `Asset` que añade su propia *referencia al recurso real*.
 *
 * Los metadatos comunes están pensados para **crecer**: no es necesario rellenarlos todos hoy, pero
 * la arquitectura ya los admite. Los campos específicos del tipo (p. ej. la `request` de una
 * animación) viven en el subtipo, no aquí.
 */
interface Asset {
    /** Identificador estable y único del asset (p. ej. "EVO_001"). Clave de referencia permanente. */
    val id: String

    /** Nombre legible para herramientas y galerías. */
    val name: String

    /** Tipo de recurso. Determina qué herramientas lo consumen y cómo se reproduce/usa. */
    val type: AssetType

    /** Categoría (carpeta) dentro del tipo (p. ej. "Evolución", "Robar carta"). */
    val category: String

    /** Estado de curación. Ver [AssetStatus]: la historia nunca se borra (existe `Deprecated`). */
    val status: AssetStatus

    /** Versión del asset; permite iterar sin perder el histórico. */
    val version: Int

    /** Autor/origen de la creación (persona, equipo o "I+D AAA"). */
    val author: String

    /** Fecha de creación en ISO-8601 (`YYYY-MM-DD`). String a propósito: sin acoplar a `java.time`. */
    val createdAt: String

    /** Referencia/inspiración AAA de la que nace el recurso (Hearthstone, LoR, Marvel Snap…). */
    val inspiration: String

    /** Descripción funcional/visual del recurso. */
    val description: String

    /** Etiquetas libres para futura búsqueda/filtrado (fuera de alcance en este Sprint). */
    val tags: List<String>

    /** Duración en milisegundos **cuando aplique** (animaciones, audio…); `null` si no aplica. */
    val durationMillis: Long?
}

/**
 * Tipos de Asset. Hoy sólo [Animation]. Los demás quedan documentados como el crecimiento natural
 * previsto (no se implementan todavía): añadir uno = añadir una entrada aquí y su subtipo de [Asset].
 */
enum class AssetType(val label: String) {
    Animation("Animación"),
    // Futuro (no implementar aún): Audio, Music, Particles, Shaders, Models, Backgrounds,
    // Ui, Iconography, Fonts, Layouts, Themes, …
}

/**
 * Estado de curación de un Asset dentro de su categoría.
 *
 * **Nunca se elimina historia.** Una variante puede dejar de ser [Canon] pasando a [Deprecated], sin
 * desaparecer: queda disponible para comparar y consultar.
 */
enum class AssetStatus(val label: String) {
    /** Prototipo de I+D; no elegible para el juego. */
    Experimental("Experimental"),

    /** Candidata seria a convertirse en Canon. */
    Candidate("Candidata"),

    /** Elegida como oficial. Sólo una por categoría (invariante del registro). */
    Canon("Canon"),

    /** Fue relevante (a veces Canon); se conserva por historia, ya no se usa. */
    Deprecated("Obsoleta"),
}
