package com.mineralord.tcg.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Permite a pantallas profundas (p. ej. el visor de información de carta) OCULTAR la barra de
 * navegación del chrome de la app mientras están visibles, sin acoplarse a la navegación.
 *
 * [AppShell] (módulo app) PROVEE la implementación real; cualquier composable que la consuma llama
 * `LocalBottomBarHider.current(true)` al aparecer y `(false)` al desaparecer. El visor de carta la usa
 * para que la barra de navegación NUNCA tape su botón de cerrar (X). Por defecto no hace nada.
 */
val LocalBottomBarHider = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
