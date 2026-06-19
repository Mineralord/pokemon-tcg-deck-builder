/* =============================================================
   sonido.js — Efectos de sonido + háptica del juego virtual
   API estable: SONIDO.play('attack'), SONIDO.vibrate(ms), SONIDO.setMute(b).
   Fase 0: andamiaje con mute persistente y vibración. Los clips/síntesis
   concretos se añaden en la Fase 9.
   ============================================================= */
(function (global) {
  'use strict';

  const MUTE_KEY = 'jv_mute';
  let muted = false;
  try { muted = localStorage.getItem(MUTE_KEY) === '1'; } catch (e) {}

  function isMuted() { return muted; }
  function setMute(b) {
    muted = !!b;
    try { localStorage.setItem(MUTE_KEY, muted ? '1' : '0'); } catch (e) {}
  }

  // Reproduce un efecto por nombre (no-op en Fase 0; se implementa en Fase 9).
  function play(/* name */) { if (muted) return; }

  // Háptica suave (respeta el silencio del usuario como proxy de "sin feedback").
  function vibrate(ms) {
    if (muted) return;
    try { if (navigator.vibrate) navigator.vibrate(ms || 10); } catch (e) {}
  }

  const API = { play, vibrate, isMuted, setMute };
  global.SONIDO = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
