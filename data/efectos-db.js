/* =============================================================
   efectos-db.js — Base de efectos AUTORADA por carta (DSL, Fase 7)
   EFECTOS_DB[cardId] = { ataques:{nombre:entrada}, habilidades:{}, jugar, pasivos }
   donde `entrada` = { ops:[...] } | { pasivos:[...] } | { efectoJS:fn }.
   Spec: docs/efectos-dsl.md. El bridge js/juego-efectos.js lo ejecuta vía
   js/efectos-motor.js. Si una carta no está aquí, el motor cae al
   auto-intérprete de texto y luego al respaldo manual asistido.
   Se irá llenando carta por carta (Fase 7). Estas son entradas semilla.
   ============================================================= */
(function (global) {
  'use strict';

  const EFECTOS_DB = {
    // Venusaur ex (151) — Dangerous Toxwhip: 150, rival Confundido + Envenenado.
    'sv3pt5-3': {
      ataques: {
        'Dangerous Toxwhip': { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: ['confused', 'poisoned'] }] }
      },
      habilidades: {
        // Tranquil Flower: elige 1 de tus Pokémon y cúrale 60 (Fase 2: elegirObjetivo + curar).
        'Tranquil Flower': { ops: [
          { op: 'elegirObjetivo', objetivo: 'propioTodos', cuantos: 1, prompt: 'Elige un Pokémon para curar 60' },
          { op: 'curar', objetivo: 'elegido', cantidad: 60 }
        ] }
      }
    },

    // Charizard ex (151) — Brave Wing: 60 (+100 si tiene daño); Explosive Vortex: 330, descarta 3 energías.
    'sv3pt5-6': {
      ataques: {
        'Brave Wing': { ops: [{ op: 'danioExtra', cantidad: 100, condicion: { tipo: 'tieneDanio', objetivo: 'esteP' } }] },
        'Explosive Vortex': { ops: [{ op: 'descartarEnergia', objetivo: 'esteP', cantidad: 3 }] }
      }
    },

    // ---------- Ataques de Pokémon ex (Fase 7) ----------
    'sv3pt5-38': { ataques: { 'Heat Wave': { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: 'burned' }] } } }, // Ninetales ex
    'sv3pt5-65': { ataques: { 'Mind Jack': { ops: [{ op: 'danioExtra', porCada: { op: 'cuenta', objetivo: 'rivalBanca', multiplica: 30 } }] } } }, // Alakazam ex
    'sv3pt5-115': { ataques: { 'Triple Draw': { ops: [{ op: 'robar', cantidad: 3 }] } } }, // Kangaskhan ex
    'sv3pt5-124': { ataques: { 'Icy Wind': { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: 'asleep' }] } } }, // Jynx ex
    'sv8-42': { ataques: { 'Hypno Splash': { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: 'asleep' }] } } }, // Milotic ex
    'sv8-37': { ataques: { 'Spicy Rage': { ops: [{ op: 'danioExtra', porCada: { op: 'contadores', objetivo: 'esteP', multiplica: 70 } }] } } }, // Scovillain ex
    'sv8-48': { ataques: { 'Black Frost': { ops: [{ op: 'recoil', cantidad: 30 }] } } }, // Black Kyurem ex
    'sv3pt5-145': { ataques: { 'Multishot Lightning': { ops: [                                  // Zapdos ex
      { op: 'elegirObjetivo', objetivo: 'rivalBanca', cuantos: 1, prompt: 'Elige un Pokémon de la Banca rival (90 de daño)' },
      { op: 'danio', objetivo: 'elegido', cantidad: 90 }
    ] } } },
    'sv8-119': { ataques: { 'Obsidian': { ops: [                                                  // Hydreigon ex
      { op: 'elegirObjetivo', objetivo: 'rivalBanca', cuantos: 2, prompt: 'Elige 2 Pokémon de la Banca rival (130 de daño)' },
      { op: 'danio', objetivo: 'elegido', cantidad: 130 }
    ] } } },

    // ---------- Entrenadores (Fase 7) ----------
    'sv1-180': { jugar: { ops: [{ op: 'robar', cantidad: 3 }] } },                               // Nemona
    'sv3pt5-158': { jugar: { ops: [{ op: 'robar', cantidad: 2 }] } },                            // Ayuda de Daisy
    'sv1-181': { jugar: { ops: [{ op: 'buscarMazo', filtro: { esBasico: true }, destino: 'banca', cantidad: 1, prompt: 'Busca un Pokémon Básico para tu Banca' }] } }, // Nido Ball
    'sv2-183': { jugar: { ops: [{ op: 'buscarMazo', filtro: { supertipo: 'Pokemon' }, destino: 'mano', cantidad: 1, prompt: 'Busca un Pokémon' }] } }, // Super Ball
    'sv8-175': { jugar: { ops: [{ op: 'buscarMazo', filtro: { supertipo: 'Pokemon' }, destino: 'mano', cantidad: 1, prompt: 'Busca un Pokémon' }] } }, // Bola Ocaso
    'sv1-188': { jugar: { ops: [                                                                  // Poción
      { op: 'elegirObjetivo', objetivo: 'propioTodos', cuantos: 1, prompt: 'Elige un Pokémon para curarle 30' },
      { op: 'curar', objetivo: 'elegido', cantidad: 30 }
    ] } },
    'sv3pt5-206': { jugar: { ops: [{ op: 'cambiarActivo', prompt: 'Elige tu nuevo Pokémon Activo' }] } }, // Cambio
    'me1-114': { jugar: { ops: [{ op: 'cambiarActivo', lado: 'rival', prompt: 'Elige el Pokémon de la Banca rival que pasa a Activo' }] } }, // Órdenes de Jefes
    'sv1-198': { jugar: { ops: [{ op: 'barajarManoEnMazo' }, { op: 'robar', cantidad: 5 }] } },  // Joven
    'sv8-173': { jugar: { ops: [{ op: 'barajarManoEnMazo' }, { op: 'robar', cantidad: 4 }, { op: 'robar', cantidad: 4, condicion: { tipo: 'coin' } }] } }, // Drasna
    'sv8-167': { jugar: { ops: [{ op: 'curar', objetivo: 'propioTodos', filtro: { tipo: 'Lightning' }, cantidad: 60 }] } }, // Ingenio de Clavel (Clemont)
    'sv8-172': { jugar: { ops: [{ op: 'curar', objetivo: 'propioActivo', filtro: { tipo: 'Dragon' }, cantidad: 60 }] } },  // Elixir de Dragón

    // ---------- Estadios (pasivos de campo) ----------
    'sv8-180': { pasivos: [{ mod: 'hpExtra', cantidad: 30, a: 'todos', filtro: { esBasico: true } }] }, // Estadio Animado: +30 PS a Básicos
    'sv8-177': { pasivos: [{ mod: 'hpExtra', cantidad: -30, a: 'todos', filtro: { etapa: 2 } }] },       // Montaña Gravedad: -30 PS a Fase 2

    // ---------- Herramientas Pokémon (pasivos al portador) ----------
    'sv3pt5-164': { pasivos: [{ mod: 'noWeakness', a: 'esteP', filtro: { esBasico: true } }] },          // Gafas Protectoras: Básico sin Debilidad
    'sv3pt5-155': { pasivos: [{ mod: 'costoRetiro', set: 0, a: 'esteP', filtro: { etapa: 2 } }] },        // Gran Globo Aerostático: Fase 2 sin coste de retirada
    'sv3pt5-165': { pasivos: [{ mod: 'reduceDanio', cantidad: 30, a: 'esteP', filtro: { etapa: 1 } }] },  // Banda Rígida: Fase 1 recibe 30 menos

    // ---------- Recuperación de descarte ----------
    'sv8-251': { jugar: { ops: [{ op: 'buscarDescarte', cantidad: 1, destino: 'mano', prompt: 'Elige una carta de tu descarte para volver a tu mano' }] } }, // Camilla Nocturna

    // ---------- Más entrenadores (búsqueda) ----------
    'sv3pt5-156': { jugar: { ops: [{ op: 'mirarTopN', n: 8, filtro: { supertipo: 'Pokemon' }, cantidad: 3, destino: 'mano', prompt: 'Mira las 8 de arriba: elige Pokémon para tu mano' }] } }, // Transferencia de Bill
    'sv1-175': { jugar: { ops: [{ op: 'buscarMazo', filtro: { supertipo: 'Pokemon', esBasico: false }, cantidad: 2, destino: 'mano', prompt: 'Busca hasta 2 Pokémon de Evolución' }] } }, // Cinio (Jacq)
    'sv8-170': { jugar: { ops: [{ op: 'buscarMazo', filtro: { supertipo: 'Pokemon', premiosMin: 2 }, cantidad: 3, destino: 'mano', prompt: 'Busca hasta 3 Pokémon ex' }] } }, // Cyrano
    'sv8-189': { jugar: { ops: [{ op: 'buscarMazo', filtro: { tera: true }, cantidad: 1, destino: 'mano', prompt: 'Busca un Pokémon Tera' }] } }, // Tera Orbe

    // ---------- Habilidades (Fase 7) ----------
    // Blastoise ex — Coraza Sólida: recibe 30 menos de daño (pasivo al portador).
    'sv3pt5-9': { habilidades: { 'Solid Shell': { pasivos: [{ mod: 'reduceDanio', cantidad: 30, a: 'esteP' }] } } },
    // Dodrio — Robo Veloz: 1 vez por turno, pon 1 contador de daño en este y roba 1.
    'sv3pt5-85': { habilidades: { 'Zooming Draw': { unaVezPorTurno: true, soloActivo: false, ops: [{ op: 'danio', objetivo: 'esteP', cantidad: 10 }, { op: 'robar', cantidad: 1 }] } } },
    // Starmie — Cometa Misterioso: 1 vez por turno, 2 contadores a 1 Pokémon rival elegido.
    'sv3pt5-121': { habilidades: { 'Mysterious Comet': { unaVezPorTurno: true, ops: [{ op: 'elegirObjetivo', objetivo: 'rivalTodos', cuantos: 1, prompt: 'Elige un Pokémon rival (20 de daño)' }, { op: 'danio', objetivo: 'elegido', cantidad: 20 }] } } }
  };

  // Validación temprana en desarrollo: avisa si alguna entrada está mal formada.
  if (global.EFECTOS_DSL && global.EFECTOS_DSL.validar) {
    Object.keys(EFECTOS_DB).forEach(function (id) {
      const r = global.EFECTOS_DSL.validar(EFECTOS_DB[id]);
      if (!r.ok && typeof console !== 'undefined') console.warn('[efectos-db] ' + id + ': ' + r.errores.join('; '));
    });
  }

  global.EFECTOS_DB = EFECTOS_DB;
  if (typeof module !== 'undefined' && module.exports) module.exports = EFECTOS_DB;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
