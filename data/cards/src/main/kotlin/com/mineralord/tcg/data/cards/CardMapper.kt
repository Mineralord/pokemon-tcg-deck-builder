package com.mineralord.tcg.data.cards

import com.mineralord.tcg.engine.model.Ability
import com.mineralord.tcg.engine.model.AbilityKind
import com.mineralord.tcg.engine.model.AncientTrait
import com.mineralord.tcg.engine.model.ArtworkRefs
import com.mineralord.tcg.engine.model.Attack
import com.mineralord.tcg.engine.model.BasicEnergy
import com.mineralord.tcg.engine.model.Card
import com.mineralord.tcg.engine.model.CardId
import com.mineralord.tcg.engine.model.Damage
import com.mineralord.tcg.engine.model.EffectId
import com.mineralord.tcg.engine.model.EffectsDb
import com.mineralord.tcg.engine.model.EnergyProvision
import com.mineralord.tcg.engine.model.EnergyType
import com.mineralord.tcg.engine.model.LocalizedText
import com.mineralord.tcg.engine.model.PokemonCard
import com.mineralord.tcg.engine.model.PokemonMechanic
import com.mineralord.tcg.engine.model.Rarity
import com.mineralord.tcg.engine.model.SetInfo
import com.mineralord.tcg.engine.model.SpecialEnergy
import com.mineralord.tcg.engine.model.Stage
import com.mineralord.tcg.engine.model.ToolTarget
import com.mineralord.tcg.engine.model.TrainerCard
import com.mineralord.tcg.engine.model.TrainerKind
import com.mineralord.tcg.engine.model.TypeModifier

/**
 * Traduce los [CardDto] del dataset al modelo de dominio. Concentra todas las
 * heurísticas de interpretación del texto del dataset (fase → etapa/mecánica,
 * rareza, tipos, daño), de modo que el resto del código trabaja con tipos
 * fuertes y nunca con strings sueltas.
 */
object CardMapper {

    fun map(dto: CardDto): Card = when (dto.supertipo) {
        "Pokémon" -> pokemon(dto)
        "Trainer" -> trainer(dto)
        "Energy" -> energy(dto)
        else -> error("Supertipo desconocido: ${dto.supertipo} (${dto.id})")
    }

    // ------------------------------------------------------------------ común

    private fun name(dto: CardDto) = LocalizedText(es = dto.es?.nombre ?: dto.nombre, en = dto.nombre)

    private fun artwork(dto: CardDto) = ArtworkRefs(
        smallEs = dto.es?.imagenChica,
        largeEs = dto.es?.imagenGrande,
        smallEn = dto.imagenChica ?: "",
        largeEn = dto.imagenGrande ?: "",
    )

    private fun setInfo(dto: CardDto) = SetInfo(
        code = dto.set?.nombre?.takeIf { it.isNotBlank() } ?: "unknown",
        name = LocalizedText(dto.set?.nombre ?: "", dto.set?.nombre ?: ""),
        series = dto.set?.serie ?: "",
    )

    fun energyType(raw: String): EnergyType = when (raw.trim().lowercase()) {
        "grass" -> EnergyType.GRASS
        "fire" -> EnergyType.FIRE
        "water" -> EnergyType.WATER
        "lightning" -> EnergyType.LIGHTNING
        "psychic" -> EnergyType.PSYCHIC
        "fighting" -> EnergyType.FIGHTING
        "darkness" -> EnergyType.DARKNESS
        "metal" -> EnergyType.METAL
        "fairy" -> EnergyType.FAIRY
        "dragon" -> EnergyType.DRAGON
        else -> EnergyType.COLORLESS
    }

    fun rarity(raw: String?): Rarity = when (raw?.trim()) {
        "Common" -> Rarity.COMMON
        "Uncommon" -> Rarity.UNCOMMON
        "Rare" -> Rarity.RARE
        "Double Rare" -> Rarity.DOUBLE_RARE
        "Ultra Rare" -> Rarity.ULTRA_RARE
        "Illustration Rare" -> Rarity.ILLUSTRATION_RARE
        "Special Illustration Rare" -> Rarity.SPECIAL_ILLUSTRATION_RARE
        "Hyper Rare" -> Rarity.HYPER_RARE
        "ACE SPEC Rare" -> Rarity.RARE          // ACE SPEC: rareza propia; se trata como Rara a efectos de pool
        "Promo" -> Rarity.PROMO
        else -> Rarity.COMMON
    }

    private fun damage(raw: String?): Damage {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return Damage.None
        val n = s.takeWhile { it.isDigit() }.toIntOrNull()
        return when {
            n == null -> Damage.None
            s.any { it == '+' || it == '×' || it == 'x' || it == '-' } -> Damage.Variable
            else -> Damage.Fixed(n)
        }
    }

    // --------------------------------------------------------------- Pokémon

    private fun pokemon(dto: CardDto): PokemonCard {
        val tokens = (dto.fase ?: "").split(",").map { it.trim() }
        return PokemonCard(
            id = CardId(dto.id),
            name = name(dto),
            set = setInfo(dto),
            rarity = rarity(dto.rareza),
            regulationMark = dto.marcaRegulacion,
            artwork = artwork(dto),
            stage = stageOf(tokens),
            mechanic = mechanicOf(tokens),
            hp = dto.ps?.toIntOrNull() ?: 0,
            types = dto.tipos.map { energyType(it) },
            evolvesFrom = dto.evolucionaDe,
            abilities = dto.habilidades.mapIndexed { i, it ->
                // Nombre/texto ES del dataset (bloque `es.habilidades`) si existe; si no, el inglés.
                val esa = dto.es?.habilidades?.getOrNull(i)
                val esName = esa?.name?.takeIf { s -> s.isNotBlank() } ?: it.name
                val esText = esa?.text?.takeIf { s -> s.isNotBlank() } ?: (it.text ?: "")
                Ability(
                    name = LocalizedText(esName, it.name),
                    text = LocalizedText(esText, it.text ?: ""),
                    effect = EffectsDb.abiKey(dto.id, it.name).takeIf { id -> EffectsDb.registry.has(id) },
                    kind = abilityKind(it.type),
                )
            },
            ancientTrait = dto.rasgoAntiguo?.let {
                AncientTrait(
                    name = LocalizedText(it.nombreEs?.takeIf { s -> s.isNotBlank() } ?: it.name, it.name),
                    text = LocalizedText(it.textoEs ?: it.text ?: "", it.text ?: ""),
                )
            },
            attacks = dto.ataques.mapIndexed { i, a ->
                // Nombre ES del dataset (si existe y no está en blanco); si falta, se usa
                // el mapa de traducción [AttackNames]; como último recurso, el inglés.
                val rawEs = dto.es?.ataques?.getOrNull(i)?.name?.takeIf { it.isNotBlank() }
                val esName = rawEs ?: AttackNames.es(a.name)
                Attack(
                    name = LocalizedText(esName, a.name),
                    cost = a.cost.map { energyType(it) },
                    convertedCost = a.convertedEnergyCost,
                    baseDamage = damage(a.damage),
                    // Puntero al efecto autorado (clave por id+nombre); null si no hay comportamiento.
                    effect = EffectsDb.atkKey(dto.id, a.name).takeIf { id -> EffectsDb.registry.has(id) },
                )
            },
            weaknesses = dto.debilidades.map { TypeModifier(energyType(it.type), it.value) },
            resistances = dto.resistencias.map { TypeModifier(energyType(it.type), it.value) },
            retreatCost = dto.costoRetirada.map { energyType(it) },
            rulesText = dto.reglas.map { LocalizedText(it, it) },
        )
    }

    /**
     * Clasifica el poder impreso por su etiqueta de época. Acepta variantes de acento
     * y guion del scraper. Ausente/desconocido = [AbilityKind.ABILITY] (SV moderno).
     */
    fun abilityKind(raw: String?): AbilityKind {
        val s = raw?.trim()?.lowercase()?.replace("é", "e")?.replace("-", " ") ?: return AbilityKind.ABILITY
        return when {
            "poke power" in s || "pokemon power" in s -> AbilityKind.POKE_POWER
            "poke body" in s -> AbilityKind.POKE_BODY
            else -> AbilityKind.ABILITY
        }
    }

    private fun stageOf(tokens: List<String>): Stage = when {
        tokens.any { it.startsWith("Stage 2") } -> Stage.Stage2
        tokens.any { it.startsWith("Stage 1") } -> Stage.Stage1
        else -> Stage.Basic
    }

    private fun mechanicOf(tokens: List<String>): PokemonMechanic = when {
        tokens.any { it.equals("Tera", true) } -> PokemonMechanic.Tera()
        tokens.any { it.equals("ex", true) } -> PokemonMechanic.ExLower
        else -> PokemonMechanic.Normal
    }

    // ------------------------------------------------------------- Entrenador

    private fun trainer(dto: CardDto): TrainerCard {
        val fase = dto.fase ?: ""
        val ace = fase.contains("ACE SPEC", ignoreCase = true)
        // Reconoce el subtipo tanto en INGLÉS (fuente tcgdex habitual) como en ESPAÑOL, por si
        // una carta se añade a mano con el `fase` localizado. Un subtipo DESCONOCIDO falla-rápido
        // (error) en vez de degradar silenciosamente a Objeto: así una expansión futura con un
        // valor inesperado se detecta en carga/tests, no se clasifica mal sin avisar.
        val f = fase.trimStart()
        fun startsAny(vararg prefixes: String) = prefixes.any { f.startsWith(it, ignoreCase = true) }
        val kind = when {
            startsAny("Supporter", "Partidario") -> TrainerKind.Supporter(ace)
            startsAny("Stadium", "Estadio") -> TrainerKind.Stadium(ace)
            startsAny("Pokémon Tool", "Pokemon Tool", "Herramienta") -> TrainerKind.Tool(ToolTarget.OWN_POKEMON, ace)
            startsAny("Item", "Objeto") -> TrainerKind.Item(ace)
            else -> error("Subtipo de Entrenador desconocido: '$fase' (${dto.id})")
        }
        val text = dto.reglas.firstOrNull() ?: ""
        return TrainerCard(
            id = CardId(dto.id),
            name = name(dto),
            set = setInfo(dto),
            rarity = rarity(dto.rareza),
            regulationMark = dto.marcaRegulacion,
            artwork = artwork(dto),
            kind = kind,
            text = LocalizedText(text, text),
            effect = EffectId(dto.id),       // el comportamiento se autora por id
        )
    }

    // ---------------------------------------------------------------- Energía

    private fun energy(dto: CardDto): Card {
        val type = dto.tipos.firstOrNull()?.let { energyType(it) } ?: EnergyType.COLORLESS
        val isBasic = (dto.fase ?: "").contains("Basic Energy", ignoreCase = true)
        return if (isBasic) {
            BasicEnergy(
                id = CardId(dto.id), name = name(dto), set = setInfo(dto),
                rarity = rarity(dto.rareza), regulationMark = dto.marcaRegulacion, artwork = artwork(dto),
                type = type,
            )
        } else {
            SpecialEnergy(
                id = CardId(dto.id), name = name(dto), set = setInfo(dto),
                rarity = rarity(dto.rareza), regulationMark = dto.marcaRegulacion, artwork = artwork(dto),
                provides = EnergyProvision.Fixed(listOf(type)),
                stateModifiers = emptyList(),
                effect = EffectId(dto.id),
            )
        }
    }
}

/**
 * Traducción ES de nombres de ataque para las cartas cuyo dataset NO trae el nombre
 * en español (41 ataques, sobre todo de las barajas de inicio SV1). Se usa como
 * respaldo en [CardMapper] cuando `es.ataques[i].name` falta o está en blanco, para
 * que el panel de combate nunca muestre el ataque en inglés. Clave = nombre en inglés.
 */
private object AttackNames {
    private val ES = mapOf(
        "Armor Cannon" to "Cañón Blindado",
        "Bite" to "Mordisco",
        "Blazing Shout" to "Grito Ardiente",
        "Collect" to "Recolectar",
        "Concentrated Fire" to "Fuego Concentrado",
        "Cross-Cut" to "Corte Cruzado",
        "Cut" to "Corte",
        "Dark Edge" to "Filo Oscuro",
        "Elbow Strike" to "Codazo",
        "Electric Claws" to "Garras Eléctricas",
        "Electro Ball" to "Bola Eléctrica",
        "Fire Blast" to "Llamarada",
        "Flame Cloak" to "Manto Llameante",
        "Flare" to "Fogonazo",
        "Gentle Slap" to "Palmada Suave",
        "Glide" to "Planear",
        "Headbutt" to "Cabezazo",
        "Heat Blast" to "Ráfaga de Calor",
        "Hyper Voice" to "Vozarrón",
        "Jet Wing" to "Ala a Reacción",
        "Lightning Ball" to "Bola Rayo",
        "Linear Attack" to "Ataque Lineal",
        "Live Coal" to "Brasa",
        "Mach Bolt" to "Rayo Mach",
        "Magnum Punch" to "Puño Magnum",
        "Passionate Singing" to "Canto Apasionado",
        "Peck" to "Picotazo",
        "Pierce" to "Perforación",
        "Poltergeist" to "Poltergeist",
        "Ram" to "Embestida",
        "Rolling Tackle" to "Placaje Rodante",
        "Rollout" to "Rodar",
        "Scratch" to "Arañazo",
        "Sharp Fang" to "Colmillo Afilado",
        "Slicing Blade" to "Hoja Cortante",
        "Speed Attack" to "Ataque Rápido",
        "Stampede" to "Estampida",
        "Steady Firebreathing" to "Aliento Ígneo Constante",
        "Suffocating Gas" to "Gas Asfixiante",
        "Take Down" to "Derribo",
        "Touring" to "Gira",
    )

    /** Nombre ES del ataque [en] (o el propio inglés si no hay traducción registrada). */
    fun es(en: String): String = ES[en] ?: en
}
