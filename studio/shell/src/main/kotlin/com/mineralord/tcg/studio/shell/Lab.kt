package com.mineralord.tcg.studio.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * **Lab: unidad hospedable del Studio (Shell Navigation v1).**
 *
 * Un `Lab` es una herramienta autocontenida que el [StudioShell] puede alojar en su Host (R3) y
 * ofrecer en el Rail (R2). El Shell **no conoce el interior de ningún Lab**: solo su [id] (clave de
 * selección), su [title] (etiqueta del Rail y de la ruta de contexto) y su [content] (el composable
 * que se dibuja en el Host cuando el Lab está activo).
 *
 * Esta es la ÚNICA responsabilidad del Shell respecto a los Labs: hospedarlos y conmutar entre ellos.
 * Cualquier Lab futuro (incluida la Galería de Animaciones, que es solo el primero registrado) se
 * expone mediante este mismo mecanismo, sin acoplar el Shell a ninguno en concreto.
 */
@Immutable
class Lab(
    val id: String,
    val title: String,
    val content: @Composable () -> Unit,
)
