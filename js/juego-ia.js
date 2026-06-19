/* =============================================================
   juego-ia.js — Rival automático para "Práctica vs IA"
   Dificultad seleccionable + toggle "randomiza" vs "sigue reglas".
   Decide acciones legales sobre el estado del motor (juego.js).
   Se implementa en la Fase 12. Fase 0: solo andamiaje.
   ============================================================= */
(function (global) {
  'use strict';

  const DIFICULTAD = Object.freeze({ FACIL: 'facil', MEDIO: 'medio', DIFICIL: 'dificil' });

  // Config por defecto; la UI la sobreescribe antes de empezar.
  const config = { dificultad: DIFICULTAD.MEDIO, modo: 'reglas' /* 'reglas' | 'random' */ };

  function configurar(opts) { Object.assign(config, opts || {}); }

  // Devuelve la siguiente acción de la IA (placeholder en Fase 0).
  function siguienteAccion(estado) { return null; }

  const API = { DIFICULTAD, config, configurar, siguienteAccion };
  global.JUEGO_IA = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
