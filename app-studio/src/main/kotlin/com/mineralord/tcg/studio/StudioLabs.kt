package com.mineralord.tcg.studio

import androidx.compose.runtime.Composable
import com.mineralord.tcg.studio.shell.Lab

/**
 * **Registro de Labs del Studio (APK 2).**
 *
 * Aquí —fuera del Shell— se declara qué Labs existen y en qué orden aparecen en el Rail. El Shell
 * solo los hospeda; la composición del catálogo vive en la cáscara de la app. La **Galería de
 * Animaciones** es un Lab NORMAL: se registra, carga y muestra con el mismo mecanismo que cualquier
 * otro. No recibe trato especial por ser el primero.
 *
 * **Fuente única de verdad:** los Labs que muestran datos los reciben desde el `AssetRegistry` de la
 * [session]; no mantienen listas propias. El Board Simulator recibe además [onEnterPresentation] para
 * activar Presentation Mode (que puentea el Shell, gestionado en `MainActivity`).
 *
 * Añadir un Lab futuro = añadir un `Lab(...)` más a esta lista con su propio `content`; el mecanismo
 * de conmutación del Shell ya lo cubre sin cambios.
 */
fun studioLabs(
    session: StudioSession,
    onEnterPresentation: () -> Unit,
    onEnterFullscreen: (@Composable () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
): List<Lab> = listOf(
    Lab(id = "event-lab", title = "Event Lab") {
        EventLabContent(session = session, onEnterPresentation = onEnterPresentation)
    },
    Lab(id = "match-builder", title = "Match Builder") {
        MatchBuilderContent(
            session = session,
            onEnterFullscreen = onEnterFullscreen,
            onExitFullscreen = onExitFullscreen,
        )
    },
    Lab(id = "animation-gallery", title = "Galería de Animaciones") {
        AnimationGalleryLabContent(session.registry)
    },
    Lab(id = "pack-simulator", title = "Simulador de Sobres") {
        PackSimulatorLabContent(
            onEnterFullscreen = onEnterFullscreen,
            onExitFullscreen = onExitFullscreen,
        )
    },
    Lab(id = "collection", title = "Colección de cartas") {
        CollectionLabContent(
            onEnterFullscreen = onEnterFullscreen,
            onExitFullscreen = onExitFullscreen,
        )
    },
)
