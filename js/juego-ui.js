/* =============================================================
   juego-ui.js — UI del Juego Virtual (tablero, HUD, animaciones)
   Fase 0: selector de modo (Cartas físicas | Juego virtual).
   Fase 2: tablero estático y bonito (mobile-first) pintado desde un estado
   de ejemplo (vista previa). Las fases siguientes lo conectan al motor.
   ============================================================= */
(function () {
  'use strict';

  const tx = (k, alt) => (typeof T === 'function' ? T(k) : (alt || k));
  const MODE_KEY = 'vs_modo'; // 'fisico' | 'virtual'

  function getModo() {
    try { return localStorage.getItem(MODE_KEY) === 'virtual' ? 'virtual' : 'fisico'; }
    catch (e) { return 'fisico'; }
  }

  // ---------- helpers de render ----------
  function esc(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // Una carta: imagen real o placeholder con nombre + HP.
  function cartaEl(c, opt) {
    opt = opt || {};
    if (opt.dorso) return '<div class="jv-card jv-card--back" aria-hidden="true"></div>';
    if (!c) return '<div class="jv-slot" aria-hidden="true"></div>';
    const cls = 'jv-card' + (opt.clickable ? ' clickable' : '');
    if (c.imagen) {
      return '<div class="' + cls + '" title="' + esc(c.nombre) + '">' +
        '<img src="' + esc(c.imagen) + '" alt="' + esc(c.nombre) + '" loading="lazy" decoding="async"></div>';
    }
    return '<div class="' + cls + ' jv-card--ph" title="' + esc(c.nombre) + '">' +
      '<span class="jv-ph-name">' + esc(c.nombre) + '</span>' +
      (c.hp ? '<span class="jv-ph-hp">' + c.hp + '</span>' : '') + '</div>';
  }

  function pileEl(n, dorso, labelKey, labelAlt) {
    const inner = dorso
      ? '<div class="jv-card jv-card--back" aria-hidden="true"></div>'
      : '<div class="jv-card jv-card--ph" aria-hidden="true"></div>';
    return '<div><div class="jv-pile">' + inner +
      (n != null ? '<span class="jv-count">' + n + '</span>' : '') + '</div>' +
      '<div class="jv-zlabel">' + tx(labelKey, labelAlt) + '</div></div>';
  }

  function prizesEl(restantes) {
    let cells = '';
    for (let i = 0; i < 6; i++) cells += '<div class="jv-mini' + (i >= restantes ? ' taken' : '') + '"></div>';
    return '<div><div class="jv-prizes-stack">' + cells + '</div>' +
      '<div class="jv-zlabel">' + tx('jv_z_prizes', 'Premios') + ' ' + restantes + '</div></div>';
  }

  function benchEl(banca) {
    let html = '';
    for (let i = 0; i < 5; i++) html += cartaEl(banca && banca[i] ? banca[i] : null, { clickable: !!(banca && banca[i]) });
    return '<div class="jv-bench">' + html + '</div>';
  }

  function pbarEl(p) {
    const av = p.foto ? '<img src="' + esc(p.foto) + '" alt="">' : esc((p.nombre || '?').charAt(0));
    return '<div class="jv-pbar"><span class="jv-av">' + av + '</span>' +
      '<span>' + esc(p.nombre) + '</span>' +
      '<span class="jv-prizecount">' + tx('jv_z_prizes', 'Premios') + ': ' + p.premios + '</span></div>';
  }

  function auxRow(p) {
    return '<div class="jv-row jv-row--aux">' +
      '<div class="jv-aux-group">' + prizesEl(p.premios) + '</div>' +
      '<div class="jv-aux-group">' +
        pileEl(p.descarte, false, 'jv_z_discard', 'Descarte') +
        pileEl(p.mazo, true, 'jv_z_deck', 'Mazo') +
      '</div></div>';
  }

  function renderTablero(b) {
    const rival = b.rival, yo = b.yo;
    const turnoTxt = b.turnoDe === 'yo'
      ? tx('jv_your_turn', 'Tu turno')
      : tx('jv_their_turn', 'Turno del rival');
    return '' +
      '<div class="jv-board">' +
      '<button class="jv-close" type="button" aria-label="' + esc(tx('jv_exit', 'Salir')) + '" title="' + esc(tx('jv_exit', 'Salir')) + '" onclick="setVersusMode(\'fisico\')">' +
        '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>' +
      '</button>' +
      '<span class="jv-demo-tag">' + tx('jv_preview', 'Vista previa') + '</span>' +

      // ----- RIVAL (arriba) -----
      '<div class="jv-side jv-side--rival">' +
        pbarEl(rival) +
        auxRow(rival) +
        benchEl(rival.banca) +
        '<div class="jv-active-wrap"><div class="jv-active">' + cartaEl(rival.activo, { clickable: !!rival.activo }) + '</div></div>' +
      '</div>' +

      // ----- CENTRO -----
      '<div class="jv-hud"><span class="jv-turn">' + turnoTxt + '</span></div>' +
      '<div class="jv-center">' +
        '<div class="jv-zlabel">' + tx('jv_z_stadium', 'Estadio') + '</div>' +
        '<div class="jv-zlabel">' + tx('jv_z_lost', 'Zona perdida') + '</div>' +
      '</div>' +

      // ----- TÚ (abajo) -----
      '<div class="jv-side jv-side--yo">' +
        '<div class="jv-active-wrap"><div class="jv-active">' + cartaEl(yo.activo, { clickable: !!yo.activo }) + '</div></div>' +
        benchEl(yo.banca) +
        auxRow(yo) +
        pbarEl(yo) +
      '</div>' +

      '</div>' +

      // ----- MANO PROPIA -----
      '<div class="jv-hand"><div class="jv-hand-inner">' +
        (yo.mano || []).map(function (c) { return cartaEl(c, { clickable: true }); }).join('') +
      '</div></div>';
  }

  // Estado de ejemplo para la vista previa, con cartas reales de la colección.
  function demoEstado() {
    const cartas = (typeof CARTAS_DB !== 'undefined' && CARTAS_DB.cartas) ? CARTAS_DB.cartas
      : (window.CARTAS_DB && window.CARTAS_DB.cartas) || [];
    const pk = cartas.filter(function (c) { return (c.supertipo || '').toLowerCase().indexOf('pok') >= 0; });
    function mk(v) { return v ? { nombre: v.nombre, hp: parseInt(v.ps, 10) || 0, tipo: (v.tipos || [])[0] || '', imagen: v.imagenChica || v.imagenGrande || null } : null; }
    function pick(i) { return pk.length ? mk(pk[(i * 7 + 3) % pk.length]) : { nombre: 'Pokémon', hp: 60, tipo: '', imagen: null }; }
    return {
      turnoDe: 'yo',
      rival: { nombre: tx('vs_player', 'Rival'), foto: '', premios: 5, mazo: 31, descarte: 4, manoN: 6, activo: pick(1), banca: [pick(2), pick(3), pick(4)] },
      yo: { nombre: tx('vs_you', 'Tú'), foto: '', premios: 6, mazo: 29, descarte: 3, activo: pick(5), banca: [pick(6), pick(7)], mano: [pick(8), pick(9), pick(10), pick(11), pick(12)] }
    };
  }

  function renderJuego() {
    const root = document.getElementById('juego-root');
    if (!root) return;
    root.innerHTML = renderTablero(demoEstado());
  }

  // ---------- Pantalla completa inmersiva ----------
  let _saliendo = false;
  function entrarFull() {
    const jr = document.getElementById('juego-root');
    document.body.classList.add('jv-full');
    if (jr) jr.classList.add('jv-full-root');
    // Fullscreen nativo como mejora (en iOS no aplica; el overlay CSS ya cubre todo).
    try {
      const el = document.documentElement;
      if (el.requestFullscreen && !document.fullscreenElement) el.requestFullscreen().catch(function () {});
    } catch (e) {}
  }
  function salirFull() {
    const jr = document.getElementById('juego-root');
    document.body.classList.remove('jv-full');
    if (jr) jr.classList.remove('jv-full-root');
    try {
      if (document.fullscreenElement && document.exitFullscreen) { _saliendo = true; document.exitFullscreen().catch(function () {}); }
    } catch (e) {}
  }

  // Cambia entre el tracker físico (#versus-root) y el juego virtual (#juego-root, full screen).
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
    if (modo === 'virtual') { renderJuego(); entrarFull(); }
    else salirFull();
  };

  // Si el usuario sale del fullscreen nativo (Esc), salimos también del modo inmersivo.
  document.addEventListener('fullscreenchange', function () {
    if (document.fullscreenElement) return;
    if (_saliendo) { _saliendo = false; return; }
    if (document.body.classList.contains('jv-full')) window.setVersusMode('fisico');
  });

  // Arranca SIEMPRE en físico (no forzamos pantalla completa sin gesto del usuario).
  window.juegoInitModo = function () {
    const jr = document.getElementById('juego-root');
    const vr = document.getElementById('versus-root');
    if (jr) jr.style.display = 'none';
    if (vr) vr.style.display = '';
    document.body.classList.remove('jv-full');
    if (jr) jr.classList.remove('jv-full-root');
  };

  document.addEventListener('DOMContentLoaded', function () {
    if (document.getElementById('juego-root')) window.juegoInitModo();
  });
})();
