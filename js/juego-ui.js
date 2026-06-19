/* =============================================================
   juego-ui.js — UI del Juego Virtual (tablero, HUD, animaciones)
   Fase 0: solo el selector de modo (Cartas físicas | Juego virtual) y un
   placeholder en #juego-root. Las fases siguientes pintarán el tablero real.
   ============================================================= */
(function () {
  'use strict';

  const tx = (k, alt) => (typeof T === 'function' ? T(k) : (alt || k));
  const MODE_KEY = 'vs_modo'; // 'fisico' | 'virtual'

  function getModo() {
    try { return localStorage.getItem(MODE_KEY) === 'virtual' ? 'virtual' : 'fisico'; }
    catch (e) { return 'fisico'; }
  }

  function renderJuego() {
    const root = document.getElementById('juego-root');
    if (!root) return;
    // Placeholder de la Fase 0. (Fase 2 sustituye esto por el tablero.)
    root.innerHTML = `
      <div class="jv-wip">
        <svg class="jv-wip-ico" width="40" height="40" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="m6.5 6.5 11 11"/><path d="m21 21-1-1"/><path d="m3 3 1 1"/>
          <path d="m18 22 4-4"/><path d="m2 6 4-4"/><path d="m3 10 7-7"/><path d="m14 21 7-7"/>
        </svg>
        <h3>${tx('jv_wip_title', 'Juego virtual en construcción')}</h3>
        <p>${tx('jv_wip_body', 'El tablero, las animaciones y el motor de reglas llegarán por fases. El modo de cartas físicas sigue disponible aquí al lado.')}</p>
      </div>`;
  }

  // Cambia entre el tracker físico (#versus-root) y el juego virtual (#juego-root).
  window.setVersusMode = function (modo) {
    modo = modo === 'virtual' ? 'virtual' : 'fisico';
    try { localStorage.setItem(MODE_KEY, modo); } catch (e) {}
    const vr = document.getElementById('versus-root');
    const jr = document.getElementById('juego-root');
    const bf = document.getElementById('vs-mode-fisico');
    const bv = document.getElementById('vs-mode-virtual');
    if (vr) vr.style.display = modo === 'fisico' ? '' : 'none';
    if (jr) jr.style.display = modo === 'virtual' ? '' : 'none';
    if (bf) { bf.classList.toggle('active', modo === 'fisico'); bf.setAttribute('aria-selected', modo === 'fisico'); }
    if (bv) { bv.classList.toggle('active', modo === 'virtual'); bv.setAttribute('aria-selected', modo === 'virtual'); }
    if (modo === 'virtual') renderJuego();
  };

  // Aplica el modo guardado al entrar a la pestaña Versus.
  window.juegoInitModo = function () { window.setVersusMode(getModo()); };

  document.addEventListener('DOMContentLoaded', function () {
    // Aplica el modo guardado por si la vista Versus ya está en el DOM.
    if (document.getElementById('juego-root')) window.juegoInitModo();
  });
})();
