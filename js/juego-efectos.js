/* =============================================================
   juego-efectos.js — Registro de efectos por carta (el "100% automático")
   EFECTOS[cardId] = { ataques:{}, habilidades:{}, jugar:fn, ... }.
   Si una carta no tiene handler -> el motor cae al "respaldo manual asistido".
   Se irá llenando en la Fase 8, carta por carta de la colección del usuario.
   ============================================================= */
(function (global) {
  'use strict';

  const EFECTOS = {}; // handlers JS legados por id (p.ej. "svp-106")

  // Devuelve el handler JS legado de un efecto o null (=> intenta DSL / manual).
  function efectoDe(cardId, tipo, nombre) {
    const e = EFECTOS[cardId];
    if (!e || !tipo) return null;
    const grupo = e[tipo];
    if (!grupo) return null;
    return (nombre && grupo[nombre]) || null;
  }

  function registrar(cardId, def) { EFECTOS[cardId] = def; }

  // --- Puente con el DSL autorado (data/efectos-db.js) + intérprete ---
  function _entradaDSL(cardId, tipo, nombre) {
    const DB = global.EFECTOS_DB; if (!DB || !cardId) return null;
    const def = DB[cardId]; if (!def) return null;
    if (tipo === 'jugar') return def.jugar || null;
    const grupo = def[tipo]; if (!grupo) return null;
    return (nombre && grupo[nombre]) || null;
  }
  // Ejecuta el efecto DSL de un ataque/habilidad. Devuelve marcas o null si no hay entrada.
  function resolverDSL(est, lado, cardId, tipo, nombre, ctxExtra) {
    const ent = _entradaDSL(cardId, tipo, nombre);
    if (!ent || !global.EFECTOS_MOTOR) return null;
    return global.EFECTOS_MOTOR.ejecutar(est, lado, ent, ctxExtra);
  }
  // Ejecuta el efecto DSL de jugar un Entrenador. Devuelve marcas o null.
  function resolverDSLJugar(est, lado, cardId, ctxExtra) {
    const ent = _entradaDSL(cardId, 'jugar', null);
    if (!ent || !global.EFECTOS_MOTOR) return null;
    return global.EFECTOS_MOTOR.ejecutar(est, lado, ent, ctxExtra);
  }

  const API = { EFECTOS, efectoDe, registrar, resolverDSL, resolverDSLJugar };
  global.JUEGO_EFECTOS = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
