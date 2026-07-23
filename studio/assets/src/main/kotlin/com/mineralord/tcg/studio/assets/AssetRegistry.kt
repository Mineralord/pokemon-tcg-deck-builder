package com.mineralord.tcg.studio.assets

/**
 * **Asset Registry: la fuente ÚNICA de verdad de los recursos del Studio.**
 *
 * Toda herramienta del Studio (empezando por la Animation Gallery) obtiene sus datos de aquí y de
 * ningún otro sitio: la Gallery, el Preview y las herramientas futuras **no mantienen listas
 * propias**. El registro es genérico sobre [Asset]; hoy sólo contiene [AnimationAsset], pero acepta
 * cualquier tipo futuro sin cambios.
 *
 * Se construye con la colección completa de assets (ensamblada en el composition root de la app) y
 * conserva el **orden de inserción** para categorías y variantes. Es inmutable: registrar nuevos
 * assets en caliente ("registro automático") es una evolución futura fuera del alcance de este
 * Sprint; la forma ya lo permite (bastaría una variante mutable o un builder).
 *
 * **Invariante curatorial:** a lo sumo **un** asset [AssetStatus.Canon] por cada par (tipo,
 * categoría). Las demás variantes conviven en cualquier otro estado —incluida [AssetStatus.Deprecated],
 * que preserva la historia— sin desaparecer.
 */
class AssetRegistry(assets: List<Asset>) {

    private val ordered: List<Asset> = assets.toList()

    init {
        val duplicateId = ordered.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }
        require(duplicateId == null) { "Id de asset duplicado en el registro: '${duplicateId?.key}'." }

        ordered.groupBy { it.type to it.category }.forEach { (key, group) ->
            val canon = group.count { it.status == AssetStatus.Canon }
            require(canon <= 1) {
                "La categoría '${key.second}' (${key.first.label}) no puede tener más de una variante Canon (tiene $canon)."
            }
        }
    }

    /** Todos los assets registrados, en orden de inserción. */
    val all: List<Asset> get() = ordered

    /** Assets de un tipo concreto (p. ej. todas las animaciones). */
    fun byType(type: AssetType): List<Asset> = ordered.filter { it.type == type }

    /** Categorías (carpetas) presentes en un tipo, en orden de aparición y sin repetir. */
    fun categories(type: AssetType): List<String> = byType(type).map { it.category }.distinct()

    /** Assets de una categoría dentro de un tipo, en orden de inserción. */
    fun byCategory(type: AssetType, category: String): List<Asset> =
        byType(type).filter { it.category == category }

    /** Asset por id, o `null` si no existe. */
    fun byId(id: String): Asset? = ordered.firstOrNull { it.id == id }

    /** La variante Canon de una categoría, si la hay (el juego usará esta automáticamente). */
    fun canonOf(type: AssetType, category: String): Asset? =
        byCategory(type, category).firstOrNull { it.status == AssetStatus.Canon }
}
