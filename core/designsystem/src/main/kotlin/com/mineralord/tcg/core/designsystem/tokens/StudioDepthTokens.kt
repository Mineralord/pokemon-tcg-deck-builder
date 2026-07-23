package com.mineralord.tcg.core.designsystem.tokens

/**
 * Depth tokens del **ecosistema Pokémon TCG** (Studio, Launcher, Card Viewer, herramientas)
 * — Fase 9.4. Definen SOLO la **percepción espacial**: planos, nivel de elevación y z-index.
 *
 * NO definen radius, border, blur, corner shape ni surface styling: eso es la Fase 9.5 (Surface).
 * En particular, el `level` es **ordinal**; el *shadow* concreto que representa cada nivel lo
 * materializará Surface. Aquí solo se fija *qué está sobre qué* y *qué significa*.
 *
 * Fuente de verdad: `docs/developer-studio/tokens/tokens.depth.json`. Regenerar desde el JSON.
 *
 * Ley rectora: **Depth rige Elevation y Z-Index** (DC-TA5). No existen z-index libres; todo
 * apilamiento sale de esta escala de 6 planos (VG-D2). Subir de plano es afirmar interrupción o
 * reclamo de atención (VG-D3 / VG-PROF); nunca es decoración. Ver Anexo 9.4.
 */
object StudioDepthTokens {

    /** Paso entre planos; deja holgura para apilamiento intra-plano en runtime (p. ej. toasts). */
    const val ZStep: Float = 100f

    /**
     * Escala semántica de profundidad: 6 planos fijos, del contenido base a la alerta topmost.
     * Un componente elige un plano por su **significado**, nunca un z-index suelto.
     *
     * @property level ordinal de elevación (0 = base). Surface (9.5) lo mapea a un shadow concreto.
     * @property z valor para `Modifier.zIndex(...)` de Compose.
     */
    enum class Plane(val level: Int, val z: Float) {
        /** Base: aquí vive el contenido (Preview, paneles, dock, inspector, timeline). VG-D2 · VL-CONT. */
        Content(0, 0f),
        /** Elevado-persistente, adherido al contenido: header pegado, carta seleccionada, base de arrastre. */
        Raised(1, 100f),
        /** Flotante anclado a un disparador y transitorio: tooltip, popover, menú, ghost de arrastre. */
        Floating(2, 200f),
        /** Interrupción con scrim: diálogos/modales. VG-D (interrupción). */
        Modal(3, 300f),
        /** Feedback del sistema no bloqueante: toasts. Nunca queda oculto bajo un modal. */
        Notification(4, 400f),
        /** Alerta crítica topmost reservada (pérdida de trabajo / bloqueo). VL modo Alerta. */
        Alert(5, 500f),
    }

    /**
     * Mapa de contextos a plano. Aliases semánticos; no introducen valores nuevos. El chrome
     * estructural (dock, sidebar, inspector, timeline, preview) vive en [Plane.Content]; solo
     * los elementos que interrumpen o reclaman atención suben de plano.
     */
    object Context {
        val Dock: Plane = Plane.Content
        val DockFlyout: Plane = Plane.Floating
        val Sidebar: Plane = Plane.Content
        val Inspector: Plane = Plane.Content
        val Timeline: Plane = Plane.Content
        /** Telón del preview en base; sus controles apenas se elevan (protagonismo, VL-CONT). */
        val Preview: Plane = Plane.Content
        val PreviewControls: Plane = Plane.Raised
        /** Elevar lo seleccionado = reclamo de atención (VG-D3). */
        val SelectedCard: Plane = Plane.Raised
        val DragGhost: Plane = Plane.Floating
        val Tooltip: Plane = Plane.Floating
        val Popover: Plane = Plane.Floating
        val Menu: Plane = Plane.Floating
        val Dialog: Plane = Plane.Modal
        val Toast: Plane = Plane.Notification
        val CriticalAlert: Plane = Plane.Alert
    }
}
