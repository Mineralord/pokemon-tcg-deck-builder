package com.mineralord.tcg.engine.model

/**
 * Regla oficial de coste de ataque (Web Rulebook 2026, "CHECK the Energy attached"):
 *
 *  · Cada símbolo de un TIPO específico del coste (Agua, Rayo, Fuego…) debe pagarse con una Energía
 *    UNIDA que aporte ESE tipo. No basta con tener el número total de energías: los tipos importan.
 *  · Cada símbolo Incoloro `{C}` se paga con CUALQUIER Energía (rulebook: "any type of Energy can be
 *    used for Colorless").
 *  · Cada Energía unida paga como máximo UN símbolo del coste.
 *
 * Así, "1 Agua + 1 Rayo" (p. ej. Pulso Dragón de Dragonite) NO se puede pagar con 2 Agua: falta el Rayo.
 *
 * Vale para TODOS los ataques, presentes y futuros, porque opera sobre [Attack.cost] (la lista tipada
 * del coste) sin conocer la carta concreta. [extraColorless] cubre recargos temporales "+{C} para atacar".
 *
 * Aporte de tipos por Energía: [BasicEnergy] aporta su único tipo; [SpecialEnergy] según [EnergyProvision]
 * (Fija = sus tipos; ElegirUno = comodín que sirve para cualquier tipo; Condicional = sin tipo garantizado).
 */
fun canPayEnergyCost(
    attached: List<EnergyCard>,
    cost: List<EnergyType>,
    extraColorless: Int = 0,
): Boolean {
    val specific = cost.filter { it != EnergyType.COLORLESS }
    val colorless = cost.count { it == EnergyType.COLORLESS } + extraColorless
    // Cota rápida: nunca alcanza si hay menos energías unidas que símbolos totales del coste.
    if (attached.size < specific.size + colorless) return false

    // Conjunto de tipos que puede aportar cada Energía unida.
    val provides: List<Set<EnergyType>> = attached.map { e ->
        when (e) {
            is BasicEnergy -> setOf(e.type)
            is SpecialEnergy -> when (val p = e.provides) {
                is EnergyProvision.Fixed -> p.types.toSet()
                EnergyProvision.ChooseOne -> EnergyType.entries.toSet()   // comodín (arcoíris)
                is EnergyProvision.Conditional -> emptySet()
            }
        }
    }

    // Emparejamiento bipartito (camino aumentante): cada símbolo de tipo específico se asigna a una
    // Energía DISTINTA que lo aporte. Con tamaños pequeños (energías/coste ≤ ~10) es de sobra eficiente.
    val slotForEnergy = IntArray(attached.size) { -1 }   // energía i -> símbolo específico asignado (o -1)
    fun assign(slot: Int, seen: BooleanArray): Boolean {
        val req = specific[slot]
        for (i in attached.indices) {
            if (!seen[i] && req in provides[i]) {
                seen[i] = true
                if (slotForEnergy[i] == -1 || assign(slotForEnergy[i], seen)) {
                    slotForEnergy[i] = slot
                    return true
                }
            }
        }
        return false
    }
    for (slot in specific.indices) {
        if (!assign(slot, BooleanArray(attached.size))) return false
    }

    // Las Energías no usadas por símbolos específicos cubren los Incoloros (aceptan cualquier tipo).
    val usedForSpecific = slotForEnergy.count { it != -1 }
    return attached.size - usedForSpecific >= colorless
}
