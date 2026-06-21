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

    // Charizard ex (151) — Explosive Vortex: 330, descarta 3 energías de este Pokémon.
    'sv3pt5-6': {
      ataques: {
        'Explosive Vortex': { ops: [{ op: 'descartarEnergia', objetivo: 'esteP', cantidad: 3 }] }
      }
    },

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
    'sv8-172': { jugar: { ops: [{ op: 'curar', objetivo: 'propioActivo', filtro: { tipo: 'Dragon' }, cantidad: 60 }] } }   // Elixir de Dragón
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
