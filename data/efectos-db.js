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
        // Tranquil Flower: curar 60 a 1 Pokémon (requiere elección → Fase 2; aquí queda manual).
        'Tranquil Flower': { ops: [{ op: 'curar', objetivo: 'elegido', cantidad: 60 }] }
      }
    },

    // Charizard ex (151) — Explosive Vortex: 330, descarta 3 energías de este Pokémon.
    'sv3pt5-6': {
      ataques: {
        'Explosive Vortex': { ops: [{ op: 'descartarEnergia', objetivo: 'esteP', cantidad: 3 }] }
      }
    }
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
