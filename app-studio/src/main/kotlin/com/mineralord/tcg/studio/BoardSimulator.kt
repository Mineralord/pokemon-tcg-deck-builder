package com.mineralord.tcg.studio

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * **Board Simulator — modelo del Sandbox (sin reglas).**
 *
 * Estado puramente MANUAL de un tapete libre: cartas colocables donde el usuario quiera, superponibles
 * y recolocables, sin turnos, validaciones ni motor de combate. Este archivo NO conoce animaciones ni
 * variantes concretas: sólo describe el escenario y el catálogo de acciones. El pipeline real y el
 * Asset Registry se conectan en la capa de UI ([BoardSimulatorLabContent]).
 *
 * Es la base pensada para crecer hasta el **laboratorio central del Studio**: nuevas capacidades
 * (partículas, sonidos, cámaras, shaders…) se añadirán como nuevas [SandboxAction] sobre el MISMO
 * escenario, sin rediseñar el modelo.
 */

/** Categoría de Asset que la acción Evolution consulta en el Registry. El Sandbox sólo conoce esto. */
const val EVOLUTION_CATEGORY = "Evolución"

/** Zonas visuales del tapete (sólo presentación; sin semántica de reglas). */
enum class BoardZone(val label: String) {
    Prizes("Premios"),
    Active("Activo"),
    Bench("Banca"),
    Deck("Mazo"),
    Discard("Descarte"),
    Hand("Mano"),
}

/**
 * Acción genérica del panel. Es el punto de extensión del laboratorio: declarar una acción = añadir
 * una entrada aquí. Sólo [implemented] = true hace algo hoy (Evolution); el resto es infraestructura
 * de UI lista para conectarse en Sprints futuros. [category] indica de qué categoría del Asset
 * Registry se nutre la acción cuando aplica (Evolution → [EVOLUTION_CATEGORY]).
 */
data class SandboxAction(
    val id: String,
    val label: String,
    val category: String? = null,
    val implemented: Boolean = false,
)

/** Catálogo genérico de acciones (sólo Evolution está implementada en este Sprint). */
val SandboxActions: List<SandboxAction> = listOf(
    SandboxAction("draw", "Draw Card"),
    SandboxAction("evolution", "Evolution", category = EVOLUTION_CATEGORY, implemented = true),
    SandboxAction("play", "Play Pokémon"),
    SandboxAction("energy", "Attach Energy"),
    SandboxAction("tool", "Attach Tool"),
    SandboxAction("retreat", "Retreat"),
    SandboxAction("ko", "Knock Out"),
    SandboxAction("prize", "Prize"),
    SandboxAction("shuffle", "Shuffle"),
    SandboxAction("coin", "Coin"),
    SandboxAction("dice", "Dice"),
    SandboxAction("ability", "Ability"),
    SandboxAction("attack", "Attack"),
)

/**
 * Una carta física en el tapete. Su [offset] (en píxeles, coords del tablero) es libre y mutable:
 * el usuario la arrastra y la suelta donde quiera. Identidad de carta real y fija (Bulbasaur…), sin
 * dependencia de los datos del juego (Arquitectura Dual: el Studio no enlaza `data:cards`).
 */
@Stable
class SandboxCard(
    val id: String,
    val name: String,
    initialOffset: Offset,
) {
    var offset by mutableStateOf(initialOffset)
}

/** Petición pendiente de evolución: [evolvingId] soltada sobre [baseId]. Abre el selector de variantes. */
data class PendingEvolution(val baseId: String, val evolvingId: String)

/**
 * Controlador de estado del tapete (hoisted). Mantiene las cartas, el orden de apilamiento (la última
 * arrastrada queda al frente), la acción armada y la evolución pendiente. Sin reglas: sólo bookkeeping
 * visual del Sandbox.
 */
@Stable
class BoardController {
    val cards: MutableList<SandboxCard> = mutableStateListOf()

    /** Orden Z: ids de fondo→frente. La carta arrastrada sube al frente. */
    val zOrder: MutableList<String> = mutableStateListOf()

    var armedActionId by mutableStateOf<String?>(null)
    var pendingEvolution by mutableStateOf<PendingEvolution?>(null)

    fun bringToFront(id: String) {
        zOrder.remove(id)
        zOrder.add(id)
    }

    fun zIndexOf(id: String): Float = zOrder.indexOf(id).toFloat()

    fun cardById(id: String): SandboxCard? = cards.firstOrNull { it.id == id }
}
