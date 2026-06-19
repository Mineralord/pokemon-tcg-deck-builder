/* =============================================================
   juego-efectos.js — Registro de efectos por carta (el "100% automático")
   EFECTOS[cardId] = { ataques:{}, habilidades:{}, jugar:fn, ... }.
   Si una carta no tiene handler -> el motor cae al "respaldo manual asistido".
   Se irá llenando en la Fase 8, carta por carta de la colección del usuario.
   ============================================================= */
(function (global) {
  'use strict';

  const EFECTOS = {}; // por id de carta pokemontcg (p.ej. "svp-106")

  // Devuelve el handler de un efecto o null (=> respaldo manual).
  function efectoDe(cardId, tipo, nombre) {
    const e = EFECTOS[cardId];
    if (!e || !tipo) return null;
    const grupo = e[tipo];
    if (!grupo) return null;
    return (nombre && grupo[nombre]) || null;
  }

  function registrar(cardId, def) { EFECTOS[cardId] = def; }

  const API = { EFECTOS, efectoDe, registrar };
  global.JUEGO_EFECTOS = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
