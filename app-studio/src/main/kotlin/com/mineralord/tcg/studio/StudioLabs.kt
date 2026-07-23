package com.mineralord.tcg.studio

import com.mineralord.tcg.studio.shell.Lab

/**
 * **Registro de Labs del Studio (APK 2).**
 *
 * Aquí —fuera del Shell— se declara qué Labs existen y en qué orden aparecen en el Rail. El Shell
 * solo los hospeda; la composición del catálogo vive en la cáscara de la app. La **Galería de
 * Animaciones** es un Lab NORMAL: se registra, carga y muestra con el mismo mecanismo que cualquier
 * otro. No recibe trato especial por ser el primero.
 *
 * Añadir un Lab futuro = añadir un `Lab(...)` más a esta lista con su propio `content`; el mecanismo
 * de conmutación del Shell ya lo cubre sin cambios.
 */
fun studioLabs(): List<Lab> = listOf(
    Lab(id = "animation-gallery", title = "Galería de Animaciones") { AnimationGalleryLabContent() },
)
