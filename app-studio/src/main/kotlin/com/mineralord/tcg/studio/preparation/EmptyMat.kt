package com.mineralord.tcg.studio.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.mineralord.tcg.feature.game.board.BoardGeometry
import com.mineralord.tcg.feature.game.combat.CombatMat

/**
 * **C2 · Escenario OFICIAL vacío: la superficie sobre la que se reproducen los eventos de framing
 * (V0.1, V0.2).**
 *
 * Escenario ÚNICO (canon): no hay tapete alternativo ni mockup. Esta superficie renderiza el MISMO
 * [CombatMat] oficial que usa el juego (fondo, tapete, lente, rieles, texturas — todo por fracciones),
 * en estado neutro/vacío (`litSide = null`, `activeType = null`). Sin cartas, sin HUD, sin GameState,
 * sin lógica de partida: sólo la capa de ESCENARIO, desacoplada de la PARTIDA. La cámara y los canales
 * de animación se aplican por fuera (los `cameraChannels`/`EventOverlay` del Event Player).
 */
@Composable
fun EmptyMat(modifier: Modifier = Modifier) {
    // Fondo del recinto idéntico al de CombatScreen (fuera del tapete aspecto-bloqueado).
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF06080D))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Bloqueo de aspecto 1080:2400 (idéntico a CombatScreen): el tapete conserva su forma.
            val availW = maxWidth
            val availH = maxHeight
            val boardW: Dp
            val boardH: Dp
            if (availW <= availH * BoardGeometry.Aspect) {
                boardW = availW; boardH = availW / BoardGeometry.Aspect
            } else {
                boardH = availH; boardW = availH * BoardGeometry.Aspect
            }
            Box(modifier = Modifier.size(boardW, boardH).align(Alignment.Center)) {
                CombatMat(Modifier.matchParentSize(), litSide = null, activeType = null)
            }
        }
    }
}
