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
                // Nombre/texto ES del dataset (bloque `es.habilidades`) si existe; si no, el nombre
                // OFICIAL en español ([AbilityNames]); como último recurso, el inglés impreso.
                val esa = dto.es?.habilidades?.getOrNull(i)
                val esName = esa?.name?.takeIf { s -> s.isNotBlank() } ?: AbilityNames.es(it.name)
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
                // Texto de regla IMPRESO (ES del dataset si existe; si no, el inglés). Los ataques
                // sin texto quedan en blanco → la UI no les pinta descripción.
                val enText = a.text ?: ""
                val esText = dto.es?.ataques?.getOrNull(i)?.text?.takeIf { it.isNotBlank() } ?: enText
                Attack(
                    name = LocalizedText(esName, a.name),
                    cost = a.cost.map { energyType(it) },
                    convertedCost = a.convertedEnergyCost,
                    baseDamage = damage(a.damage),
                    // Puntero al efecto autorado (clave por id+nombre); null si no hay comportamiento.
                    effect = EffectsDb.atkKey(dto.id, a.name).takeIf { id -> EffectsDb.registry.has(id) },
                    text = LocalizedText(esText, enText),
                )
            },
            weaknesses = dto.debilidades.map { TypeModifier(energyType(it.type), it.value) },
            resistances = dto.resistencias.map { TypeModifier(energyType(it.type), it.value) },
            retreatCost = dto.costoRetirada.map { energyType(it) },
            rulesText = dto.reglas.map { LocalizedText(it, it) },
            nationalDex = dto.numeroPokedex.firstOrNull(),
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
        // Objeto jugado como Pokémon (Fósiles Antiguos): un Entrenador con PS impresos. Se sintetiza
        // el Pokémon Básico {C} en que se convierte reutilizando el mapeo de Pokémon (PS + Habilidad
        // ya cableados), forzando tipo Incoloro (los fósiles traen `tipos` vacío) y Básico/Normal.
        val playsAs = if (dto.ps != null) pokemon(dto).copy(
            types = listOf(EnergyType.COLORLESS),
            stage = Stage.Basic,
            mechanic = PokemonMechanic.Normal,
        ) else null
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
            playsAs = playsAs,
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
 * Nombres OFICIALES en español de los ataques cuyo dataset NO trae el bloque `es.ataques[i].name`
 * (cartas de barajas de inicio y promos SV). Se usan como respaldo en [CardMapper] para que el
 * panel/rótulo de combate muestre el nombre IMPRESO en la carta española, no una traducción
 * inventada ni el inglés. Fuente: TCGdex (localización ES oficial). Clave = nombre impreso en inglés.
 */
private object AttackNames {
    private val ES = mapOf(
        "Armor Cannon" to "Cañón Armadura",
        "Bite" to "Mordisco",
        "Blazing Shout" to "Grito Abrasador",
        "Collect" to "Coleccionar",
        "Concentrated Fire" to "Fuego Concentrado",
        "Cross-Cut" to "Atajar",
        "Cut" to "Corte",
        "Dark Edge" to "Filo Siniestro",
        "Elbow Strike" to "Codazo",
        "Electric Claws" to "Garras Eléctricas",
        "Electro Ball" to "Bola Voltio",
        "Fire Blast" to "Llamarada",
        "Flame Cloak" to "Manto Ígneo",
        "Flare" to "Llama",
        "Gentle Slap" to "Bofetada Gentil",
        "Glide" to "Planeo",
        "Headbutt" to "Golpe Cabeza",
        "Heat Blast" to "Explosión de Calor",
        "Hyper Voice" to "Vozarrón",
        "Jet Wing" to "Ala Propulsión",
        "Lightning Ball" to "Bola Relámpago",
        "Linear Attack" to "Ataque Lineal",
        "Live Coal" to "Carbón Activado",
        "Mach Bolt" to "Rayo Mach",
        "Magnum Punch" to "Puño Mágnum",
        "Passionate Singing" to "Canto Apasionado",
        "Peck" to "Picotazo",
        "Pierce" to "Perforar",
        "Ram" to "Apisonar",
        "Rolling Tackle" to "Placaje Giro",
        "Rollout" to "Rodar",
        "Scratch" to "Arañazo",
        "Sharp Fang" to "Colmillo Afilado",
        "Slicing Blade" to "Cuchilla Cortante",
        "Speed Attack" to "Ataque Fugaz",
        "Stampede" to "Estampida",
        "Steady Firebreathing" to "Lanzallamas Continuo",
        "Suffocating Gas" to "Gas Sofocante",
        "Take Down" to "Derribo",
        "Touring" to "De Gira",
    )

    /** Nombre ES oficial del ataque [en] (o el propio inglés si no hay ninguno registrado). */
    fun es(en: String): String = ES[en] ?: en
}

/**
 * Nombres OFICIALES en español de las Habilidades cuyo dataset NO trae `es.habilidades[i].name`
 * (fósiles antiguos de 151). Evita mostrarlas en inglés. Fuente: TCGdex (localización ES oficial).
 * Clave = nombre impreso en inglés.
 */
private object AbilityNames {
    private val ES = mapOf(
        "Domed Armor" to "Caparazón Domo",
        "Helical Swell" to "Oleaje Helicoidal",
        "Amber Protection" to "Protección Ámbar",
    )

    /** Nombre ES oficial de la Habilidad [en] (o el propio inglés si no hay ninguno registrado). */
    fun es(en: String): String = ES[en] ?: en
}
