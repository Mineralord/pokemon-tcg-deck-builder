package com.mineralord.tcg.feature.game

import com.mineralord.tcg.feature.game.combat.CombatSceneController

/**
 * Contrato del lado **JUEGO** para conducir la pantalla de combate compartida. Es simplemente el
 * contrato neutral [CombatSceneController] (del que depende `CombatScreen`), especializado como
 * alias para los controladores del juego: `GameViewModel` (PvE) y `OnlineGameController` (PvP).
 *
 * La pantalla NO conoce este tipo: sólo el contrato neutral. Así el Studio puede conducir la MISMA
 * pantalla con su propio `SandboxController : CombatSceneController`, sin depender del juego.
 */
interface GameController : CombatSceneController
