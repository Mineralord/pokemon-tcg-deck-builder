package com.mineralord.tcg.core.animationcompose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/**
 * Identificador tipado de una "ranura" visual (una celda del tablero, un slot de
 * carta…). Value class para no mezclar ids de ranura con otros `String`.
 */
@JvmInline
value class SlotId(val value: String)

/**
 * Información de render de una ranura: SÓLO geometría, nunca Composables.
 *
 * @param positionInRoot posición de la esquina superior-izquierda relativa a la raíz
 *        del árbol Compose (coordenadas estables entre pantallas).
 * @param size tamaño en píxeles de la ranura.
 */
data class SlotBounds(
    val positionInRoot: Offset,
    val size: IntSize,
) {
    /** Centro de la ranura, útil para calcular trayectorias de vuelo. */
    val center: Offset
        get() = Offset(
            positionInRoot.x + size.width / 2f,
            positionInRoot.y + size.height / 2f,
        )

    /** Rectángulo equivalente en coordenadas de raíz. */
    fun toRect(): Rect = Rect(positionInRoot, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
}

/**
 * Registro de coordenadas: la **frontera lógico↔pantalla** del framework.
 *
 * El mundo puro (`core:animation`) referencia objetivos por `String`; el mundo Compose
 * necesita rectángulos reales. Este registro traduce [SlotId] → [SlotBounds].
 *
 * Responsabilidad ÚNICA: almacenar y entregar información de render. NO guarda
 * Composables, NO dibuja, NO conoce animaciones.
 *
 * Seguridad frente a recomposición: el almacén es un `SnapshotStateMap`. Las lecturas
 * ([bounds]) son observables (quien lea dentro de una composición se recompone al
 * cambiar la ranura) y las escrituras son seguras bajo el sistema de snapshots de
 * Compose. Marcado [Stable] para que Compose pueda saltar recomposiciones cuando la
 * referencia no cambia.
 */
@Stable
class CoordinateRegistry {

    private val slots = mutableStateMapOf<SlotId, SlotBounds>()

    /** Número de ranuras registradas. */
    val size: Int get() = slots.size

    /**
     * Registra o actualiza (upsert) la geometría de una ranura. Registrar por primera
     * vez y actualizar comparten semántica: la última posición gana.
     */
    fun place(id: SlotId, bounds: SlotBounds) {
        slots[id] = bounds
    }

    /** Elimina el registro de una ranura (p. ej. al salir de composición). */
    fun remove(id: SlotId) {
        slots.remove(id)
    }

    /** Consulta la geometría actual de una ranura, o `null` si no está registrada. */
    fun bounds(id: SlotId): SlotBounds? = slots[id]

    /** ¿Está registrada la ranura? */
    fun contains(id: SlotId): Boolean = slots.containsKey(id)

    /** Vacía el registro por completo. */
    fun clear() {
        slots.clear()
    }
}
