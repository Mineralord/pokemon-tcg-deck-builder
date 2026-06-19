/* =============================================================
   juego.js — Motor del Juego Virtual Pokémon TCG
   Reductor puro y transporte-agnóstico: aplicarAccion(estado, accion) -> estado.
   Sin DOM ni red, para que valga en online / pase y juega / IA y sea testeable
   en Node. Se irá llenando por fases (ver plan). Fase 0: solo andamiaje.
   ============================================================= */
(function (global) {
  'use strict';

  const VERSION = 0; // sube con cada fase del motor

  // Fases del juego (rulebook): preparación -> turnos -> fin.
  const FASE = Object.freeze({
    SETUP: 'setup',
    DRAW: 'draw',
    MAIN: 'main',
    ATTACK: 'attack',
    CHECKUP: 'checkup',
    END: 'end'
  });

  // Estado inicial vacío (se completará en Fase 1).
  function estadoInicial() {
    return {
      v: VERSION,
      fase: FASE.SETUP,
      turno: 0,
      turnoDe: null,         // uid/lado al que le toca
      jugadores: {},         // por lado: { mazo, mano, banca, activo, descarte, premios, ... }
      seq: 0,                // contador para sincronización online
      log: []                // historial de acciones (Fase 13)
    };
  }

  // Reductor puro. Por ahora devuelve el estado sin cambios (placeholder).
  function aplicarAccion(estado, accion) {
    return estado;
  }

  const API = { VERSION, FASE, estadoInicial, aplicarAccion };

  // Exponer en navegador y en Node (para tests).
  global.JUEGO = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
