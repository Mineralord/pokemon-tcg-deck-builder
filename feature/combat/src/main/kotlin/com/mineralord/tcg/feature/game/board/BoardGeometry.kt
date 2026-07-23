package com.mineralord.tcg.feature.game.board

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometría centralizada del tablero.
 *
 * Antes, las medidas del tablero estaban dispersas como literales `.dp` en
 * `GameScreen.kt` (ActiveW/H, BenchW/H) y `BattleBoard.kt` (carril central, hexes
 * de premio). Aquí se concentran para poder:
 *   1. anclarlas a un lienzo de referencia device-native (1080x2400), el mismo
 *      viewport de los drawables vectoriales y de las capturas de referencia; y
 *   2. verificarlas con el pipeline de comparación (`tools/`), que mide offsets en
 *      píxeles sobre ese mismo lienzo.
 *
 * Las constantes en dp conservan los valores actuales (sin cambio visual). Las
 * fracciones documentan la posición/tamaño relativo al lienzo de referencia y son
 * la base para migrar a un layout proporcional con `BoxWithConstraints`.
 */
object BoardGeometry {

    /** Lienzo de referencia (px) — coincide con el viewport de los drawables. */
    const val RefW = 1080f
    const val RefH = 2400f

    // --- Cartas activas / banca (valores actuales, sin cambio visual) ---------
    val ActiveW: Dp = 106.dp
    val ActiveH: Dp = 148.dp
    val BenchW: Dp = 72.dp
    val BenchH: Dp = 100.dp

    // --- Carril central del honeycomb (fracciones de ancho) -------------------
    // Usado por MatBackground: lane en x = w*LaneStartFrac, ancho = w*LaneWidthFrac.
    const val LaneStartFrac = 0.30f
    const val LaneWidthFrac = 0.40f
    /** Tamaño del hexágono del honeycomb como fracción del ancho. */
    const val HexSizeFrac = 0.045f

    // --- Riel derecho (ancho reservado para premios/temporizador/fin de turno) -
    val RightRailWidth: Dp = 40.dp

    // --- Fracciones de referencia (para layout proporcional futuro) -----------
    // Alto de cada elemento relativo a RefH, medido sobre los frames de referencia.
    const val ActiveHFrac = 148f / 2400f
    const val BenchHFrac = 100f / 2400f
    const val ActiveWFrac = 106f / 1080f
    const val BenchWFrac = 72f / 1080f

    /** Resuelve una fracción a Dp dado el tamaño total del eje correspondiente. */
    fun frac(total: Dp, f: Float): Dp = total * f

    /**
     * Caja normalizada [x, y, w, h] en 0..1 (origen arriba-izquierda) relativa al área
     * de tablero (bloqueada a 1080:2400). Es el contrato posicional 1:1: cada elemento
     * del clon se coloca y dimensiona con su NBox, y el arnés `tools/` mide su caja real
     * y la compara contra el spec de referencia `tools/refspec/board_start.json`.
     *
     * IMPORTANTE: estos valores son el ESPEJO de board_start.json (única fuente de verdad
     * de la maquetación). Si se refina el spec (con `tcgtools annotate`), sincronizar aquí.
     */
    data class NBox(val x: Float, val y: Float, val w: Float, val h: Float) {
        val cx: Float get() = x + w / 2f
        val cy: Float get() = y + h / 2f
    }

    /** Relación de aspecto del tablero (ancho/alto) para el bloqueo proporcional. */
    const val Aspect = RefW / RefH

    /**
     * TAMAÑO ESTÁNDAR de carta del tablero (ancho como fracción del ancho del tablero). TODAS
     * las cartas del tablero miden exactamente igual —premios, mazo y descarte de ambos lados—
     * EXCEPTO las manos y los Pokémon activos. Es el tamaño de la carta de PREMIOS AZUL, la
     * referencia acordada. El alto sale del aspecto (h = w / CardAspect).
     */
    const val BoardCardWFrac = 0.144f

    // Cajas por elemento — RETUNEADAS 1:1 midiendo cartas reales en referencias_live/combate1
    // (ref_0040 combate limpio + board_start_ref tablero vacío). Todas las cartas comparten
    // CardAspect (0.716). Cajas de Activo/Banca == huella exacta de la carta (la carta llena
    // la caja). Medidas: Activo 0.22×0.14; Banca 0.172×0.108; Mano ~0.28×0.176.
    val TopAvatar    = NBox(0.010f, 0.038f, 0.130f, 0.025f)
    // Botón de ajustes CUADRADO anclado a la esquina superior derecha. Su borde inferior
    // llega al borde superior del descarte rival (y≈0.072): así tapa el trozo de engranaje
    // horneado que asoma por encima del descarte (el resto lo cubre la propia pila). Se
    // dibuja ANTES que el descarte, por lo que este se superpone a su mitad inferior.
    val TopSettings  = NBox(0.872f, 0.016f, 0.124f, 0.056f)

    val OppHand      = NBox(0.150f, 0.000f, 0.700f, 0.055f)
    // Premios/mazo/descarte: SLOTS de carta VERTICALES (retrato) con PESTAÑA NUMÉRICA,
    // medidos por detección de componentes en el tablero vacío auténtico (board_start_ref).
    // Cada slot es una huella de carta que la carta LLENA (usa el aspecto de la caja).
    // El lado del RIVAL va ESCORZADO por la perspectiva 3D del tablero (la caja es más
    // ancha/achatada, ratio≈1.0), por eso sus cartas parecen tumbadas aunque son verticales.
    // Premios RIVAL: MISMO TAMAÑO DE CARTA que los premios del jugador (BoardCardWFrac): la
    // carta mide igual en todo el tablero. El slot rojo horneado es un paralelogramo escorzado
    // (más ancho), pero la carta NO se agranda para llenarlo; conserva el tamaño estándar. La
    // PESTAÑA queda debajo (y≈0.215–0.239) reservada para el contador.
    val OppPrizes    = NBox(0.000f, 0.074f, BoardCardWFrac, 0.141f)
    val OppBench     = NBox(0.060f, 0.152f, 0.880f, 0.108f)
    // MAZO y DESCARTE del RIVAL: ESPEJO del jugador dentro de su SLOT ROJO horneado (top-right,
    // interior x≈0.825–0.955, y≈0.072–0.214, PESTAÑA debajo). Van APILADOS en vertical con el
    // orden invertido respecto al jugador: el DESCARTE arriba y el MAZO abajo (justo sobre la
    // pestaña, como en ref_0040: mazo "46" en y≈0.127–0.214). Mismo TAMAÑO de carta que el
    // resto del tablero (mazo/descarte del jugador): ancho 0.130, alto 0.081 (aspecto exacto).
    val OppDiscard   = NBox(0.867f, 0.072f, 0.130f, 0.081f)
    val OppDeck      = NBox(0.867f, 0.133f, 0.130f, 0.081f)
    /** Pestaña del contador del MAZO rival, bajo el mazo (espejo de MeDeckTab). */
    val OppDeckTab   = NBox(0.837f, 0.199f, 0.078f, 0.036f)
    val OppActive    = NBox(0.390f, 0.268f, 0.220f, 0.140f)

    val Stadium      = NBox(0.010f, 0.420f, 0.130f, 0.060f)

    val MeActive     = NBox(0.390f, 0.415f, 0.220f, 0.140f)
    val MeBench      = NBox(0.060f, 0.563f, 0.880f, 0.108f)
    val MePrizes     = NBox(0.000f, 0.591f, BoardCardWFrac, 0.208f)
    // MAZO y DESCARTE del jugador: comparten un SLOT vertical horneado en el mat
    // (interior x≈0.868–1.0, y≈0.598–0.802, borde azul del slot). Dentro van APILADOS en
    // vertical: el MAZO arriba (justo bajo la pestaña) y el DESCARTE debajo, con solo un hueco
    // fino entre ambos. IMPORTANTE: las cartas se dibujan con su ASPECTO EXACTO (CardAspect,
    // nunca deformadas): al ancho del slot (~0.130) les corresponde un alto de ~0.081. Las
    // cajas conservan ese aspecto para que la carta las llene sin estirarse.
    val MeDeck       = NBox(0.868f, 0.598f, 0.130f, 0.081f)
    val MeDiscard    = NBox(0.868f, 0.687f, 0.130f, 0.081f)
    /** Pestaña del contador del MAZO: se dibuja sobre el "0" horneado del mat (centro
     *  aprox. x=0.888 y=0.604) para mostrar el conteo real de la baraja. */
    val MeDeckTab    = NBox(0.852f, 0.585f, 0.078f, 0.036f)
    val MeHand       = NBox(0.000f, 0.845f, 1.000f, 0.200f)

    // Chips simples heredados (pantalla CLÁSICA GameScreen).
    val RailPrizeOpp = NBox(0.880f, 0.270f, 0.115f, 0.050f)
    val RailPrizeMe  = NBox(0.880f, 0.400f, 0.115f, 0.043f)

    // --- Columna derecha 1:1 con TCG Live (Combate 1, medidas sobre at_0300) -----
    // Dos pestañas de estado docked al borde derecho (rival arriba, jugador abajo, espejo
    // vertical) + botón de registro de batalla debajo. Cada pestaña: barra de turno dorada
    // (borde exterior), chevrones ▲▼, temporizador mm:ss y contador de premios (rojo/azul).
    val RailStatusOpp = NBox(0.898f, 0.307f, 0.102f, 0.102f)
    val RailStatusMe  = NBox(0.898f, 0.415f, 0.102f, 0.101f)
    val RailLog       = NBox(0.900f, 0.520f, 0.098f, 0.046f)
    // Botón FIN de turno (afimación propia del clon; TCG Live lo resuelve con un aviso
    // inferior). Reubicado al carril IZQUIERDO para no invadir la columna derecha fiel.
    val RailEndTurn   = NBox(0.020f, 0.455f, 0.150f, 0.050f)

    val LeftCancel   = NBox(0.020f, 0.300f, 0.100f, 0.050f)
    val LeftEmote    = NBox(0.020f, 0.360f, 0.100f, 0.050f)

    /** Banner "Pulsa Listo…" + botón LISTO de la preparación (sobre el carril central). */
    val SetupBanner  = NBox(0.030f, 0.335f, 0.940f, 0.085f)

    /** Panel GRIS curvo que divide los dos lados (el "carril" central con el lente).
     *  Zona de soltado para jugar Entrenadores (Partidario/Objeto): arrastrar la carta
     *  aquí ejecuta su efecto y la manda al descarte. */
    val CenterPanel  = NBox(0.000f, 0.395f, 1.000f, 0.175f)

    /** Relación ancho/alto de una carta en juego (para preservar su forma). */
    const val CardAspect = 106f / 148f

    /** Relación ancho/alto de una carta APAISADA (premios/mazo/descarte, que en TCG
     *  Live van tumbadas en horizontal). Es la carta girada 90°: 148/106 ≈ 1.40. */
    const val SideCardAspect = 148f / 106f

    /** Alto de una carta de BANCA como fracción del alto del tablero (medido en ref_0040:
     *  banca ~0.172w → 0.108h). Las cartas se reparten en fila centrada dentro del panel. */
    const val BenchCardHFrac = 0.108f

    /** Alto de una carta de MANO como fracción del alto del tablero (medido en ref_0040:
     *  mano ~0.28w → 0.176h). El abanico las solapa; sobresalen por debajo del borde. */
    const val HandCardHFrac = 0.176f

    /** Alto de un dorso de la MANO DEL RIVAL como fracción del alto del tablero. */
    const val OppHandCardHFrac = 0.048f

    // --- BANCA en PANAL (honeycomb) 3+2 ---------------------------------------
    // TCG Live muestra la banca LLENA como DOS filas escalonadas: 3 cartas en la
    // fila pegada al Pokémon activo y 2 cartas ANIDADAS en los huecos, hacia la
    // mano (jugador) / borde (rival). CADA carta tiene su celda: las filas NO se
    // solapan (el paso vertical entre filas supera el alto de la carta, dejando un
    // hueco fino) y dentro de una fila las cartas quedan tangentes con un margen.
    //
    // Centros medidos 1:1 sobre la referencia de banca llena (REFERENCIAS POKEMON
    // EN BANCA, lienzo 1080x2400), midiendo topes/bases por columnas en los huecos:
    //   Jugador: fila A (junto al activo) cy≈0.628 en x {0.31, 0.50, 0.69};
    //            fila B (hacia la mano)   cy≈0.751 en x {0.40, 0.60}.  Alto ≈0.112.
    //   Rival (ESCORZADO por la perspectiva 3D → cartas más chicas):
    //            fila cercana (3) cy≈0.207 en x {0.323, 0.50, 0.678};
    //            fila lejana  (2) cy≈0.100 en x {0.44, 0.59}.           Alto ≈0.092.
    // NOTA: por ahora solo el estado LLENO (5) está afinado; con <5 cartas se
    // colocan por índice (fila de 3 primero) — los estados intermedios se refinarán.

    /** Alto de carta de banca por lado (frac de alto de tablero). El RIVAL va más
     *  chico por el escorzo de la perspectiva del mat. */
    const val MeBenchCardHFrac = 0.112f
    const val OppBenchCardHFrac = 0.092f

    /** Construye la caja de un slot de banca centrada en (cx, cy) con el alto dado
     *  (el ancho sale del aspecto exacto de carta, sin deformar). */
    fun benchSlot(cx: Float, cy: Float, hf: Float): NBox {
        val wf = hf * (RefH / RefW) * CardAspect
        return NBox(cx - wf / 2f, cy - hf / 2f, wf, hf)
    }

    /** 5 slots del panal del JUGADOR (orden de llenado: fila de 3 izq→der, luego fila de 2). */
    val MeBenchSlots: List<NBox> = listOf(
        benchSlot(0.31f, 0.628f, MeBenchCardHFrac), benchSlot(0.50f, 0.628f, MeBenchCardHFrac), benchSlot(0.69f, 0.628f, MeBenchCardHFrac),
        benchSlot(0.40f, 0.751f, MeBenchCardHFrac), benchSlot(0.60f, 0.751f, MeBenchCardHFrac),
    )

    /** 5 slots del panal del RIVAL (fila cercana de 3 + fila lejana de 2, cartas escorzadas). */
    val OppBenchSlots: List<NBox> = listOf(
        benchSlot(0.323f, 0.207f, OppBenchCardHFrac), benchSlot(0.50f, 0.207f, OppBenchCardHFrac), benchSlot(0.678f, 0.207f, OppBenchCardHFrac),
        benchSlot(0.44f, 0.100f, OppBenchCardHFrac), benchSlot(0.59f, 0.100f, OppBenchCardHFrac),
    )
}
