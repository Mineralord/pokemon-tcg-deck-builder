/* =============================================================
   juego-ui.js — UI del Juego Virtual (tablero, HUD, setup)
   Fase 0: selector de modo. Fase 2: tablero. Fase 2.1: pantalla completa.
   Fase 3: pantalla de inicio + reparto + colocación de Activo/Banca (setup),
   renderizando el tablero desde el estado real del motor (juego.js).
   ============================================================= */
(function () {
  'use strict';

  const tx = (k, alt) => (typeof T === 'function' ? T(k) : (alt || k));
  const MODE_KEY = 'vs_modo';
  let G = null; // partida actual (estado del motor) o null si estamos en la pantalla de inicio

  function esc(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // Resuelve una referencia de mazo {card,id} a una vista de carta de la app.
  function resolverCarta(ref) {
    if (!ref) return null;
    if (ref.id && typeof cardRegistry !== 'undefined' && cardRegistry[ref.id]) return cardRegistry[ref.id];
    if (typeof getCardData === 'function') { const v = getCardData(ref.card || ref.name); if (v) return v; }
    return null;
  }
  function misMazos() { return (typeof savedDecks !== 'undefined' && savedDecks) ? savedDecks : []; }

  // ---------- Componentes ----------
  function cartaEl(c, opt) {
    opt = opt || {};
    const oc = opt.onclick ? (' onclick="' + opt.onclick + '"') : '';
    if (opt.dorso) return '<div class="jv-card jv-card--back" aria-hidden="true"></div>';
    if (!c) return '<div class="jv-slot" aria-hidden="true"></div>';
    const cls = 'jv-card' + (opt.clickable ? ' clickable' : '') + (opt.sel ? ' jv-sel' : '') + (opt.dim ? ' jv-dim' : '');
    if (c.imagen) {
      return '<div class="' + cls + '" title="' + esc(c.nombre) + '"' + oc + '>' +
        '<img src="' + esc(c.imagen) + '" alt="' + esc(c.nombre) + '" loading="lazy" decoding="async"></div>';
    }
    return '<div class="' + cls + ' jv-card--ph" title="' + esc(c.nombre) + '"' + oc + '>' +
      '<span class="jv-ph-name">' + esc(c.nombre) + '</span>' +
      (c.hp ? '<span class="jv-ph-hp">' + c.hp + '</span>' : '') + '</div>';
  }
  function pileEl(n, dorso, labelKey, labelAlt) {
    const inner = dorso ? '<div class="jv-card jv-card--back" aria-hidden="true"></div>'
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
  function benchEl(banca, opt) {
    opt = opt || {}; let html = '';
    for (let i = 0; i < 5; i++) {
      const c = banca && banca[i] ? banca[i] : null;
      html += cartaEl(c, c ? { clickable: opt.clickable, onclick: opt.onclick ? opt.onclick(c) : '' } : {});
    }
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

  // ---------- Vista del tablero desde el estado ----------
  function boardView() {
    const A = G.lados.A, B = G.lados.B;
    return {
      enSetup: A.estado === 'setup',
      turnoDe: (G.fase === JUEGO.FASE.SETUP) ? null : (G.turnoDe === 'A' ? 'yo' : 'rival'),
      rival: { nombre: B.nombre, premios: B.premios.length, mazo: B.mazo.length, descarte: B.descarte.length, manoN: B.mano.length, activo: B.activo, banca: B.banca },
      yo: { nombre: A.nombre, premios: A.premios.length, mazo: A.mazo.length, descarte: A.descarte.length, activo: A.activo, banca: A.banca, mano: A.mano }
    };
  }

  function setupBar() {
    const A = G.lados.A;
    const instr = !A.activo
      ? tx('jv_setup_active', 'Toca un Pokémon Básico de tu mano para ponerlo como Activo')
      : tx('jv_setup_bench', 'Añade Básicos a tu Banca (opcional) y pulsa Confirmar');
    const dis = A.activo ? '' : ' disabled';
    return '<div class="jv-setupbar">' +
      '<div class="jv-setup-instr">' + esc(instr) + '</div>' +
      '<div class="jv-setup-actions">' +
        '<button class="jv-btn jv-btn-2" type="button" onclick="jvAuto()">' + esc(tx('jv_auto', 'Automático')) + '</button>' +
        '<button class="jv-btn" type="button"' + dis + ' onclick="jvConfirm()">' + esc(tx('jv_confirm', 'Confirmar')) + '</button>' +
      '</div></div>';
  }

  function hudBar(b) {
    const miTurno = (G.turnoDe === 'A') && !G.ganador;
    const turnoTxt = b.turnoDe === 'yo' ? tx('jv_your_turn', 'Tu turno') : tx('jv_their_turn', 'Turno del rival');
    let html = '<div class="jv-hud">' +
      '<span class="jv-turn">' + esc(tx('jv_turn', 'Turno')) + ' ' + (G.turno || 1) + ' · ' + esc(turnoTxt) + '</span>';
    if (miTurno) {
      html += '<button class="jv-btn jv-btn-2" type="button" onclick="jvFinTurno()">' + esc(tx('jv_end_turn', 'Terminar turno')) + '</button>';
    } else if (!G.ganador) {
      html += '<span class="jv-thinking">' + esc(tx('jv_rival_thinking', 'El rival juega…')) + '</span>';
    }
    html += '</div>' +
      '<div class="jv-hint">' + esc(tx('jv_actions_soon', 'Las acciones de turno (energía, evolución, ataque…) llegan en las próximas fases.')) + '</div>';
    return html;
  }

  function finOverlay() {
    if (!G || !G.ganador) return '';
    const gane = G.ganador === 'A';
    const titulo = gane ? tx('jv_win', '¡Ganaste!') : tx('jv_lose', 'Perdiste');
    const motivo = G.motivoFin === 'deckout' ? tx('jv_by_deckout', 'por agotar el mazo del rival') : '';
    return '<div class="jv-overlay"><div class="jv-overlay-card ' + (gane ? 'win' : 'lose') + '">' +
      '<h3>' + esc(titulo) + '</h3>' + (motivo ? '<p>' + esc(motivo) + '</p>' : '') +
      '<button class="jv-btn jv-btn-big" type="button" onclick="jvNueva()">' + esc(tx('jv_new_game', 'Nueva partida')) + '</button>' +
      '</div></div>';
  }

  function closeBtn() {
    return '<button class="jv-close" type="button" aria-label="' + esc(tx('jv_exit', 'Salir')) + '" title="' + esc(tx('jv_exit', 'Salir')) + '" onclick="jvSalir()">' +
      '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg></button>';
  }

  function renderTablero() {
    const b = boardView();
    const setup = b.enSetup;
    // Mano propia: en setup, las cartas básicas son clicables para colocarlas.
    const manoHtml = (b.yo.mano || []).map(function (c) {
      const basic = c.supertipo === 'Pokemon' && c.esBasico;
      return cartaEl(c, { clickable: setup && basic, dim: setup && !basic, onclick: (setup && basic) ? ("jvPlace('" + c.iid + "')") : '' });
    }).join('');
    const yoActivo = cartaEl(b.yo.activo, { clickable: setup && !!b.yo.activo, onclick: (setup && b.yo.activo) ? ("jvUnplace('" + b.yo.activo.iid + "')") : '' });
    const yoBanca = benchEl(b.yo.banca, { clickable: setup, onclick: setup ? function (c) { return "jvUnplace('" + c.iid + "')"; } : null });

    const centro = setup ? setupBar() : hudBar(b);

    return '' +
      '<div class="jv-board">' +
      closeBtn() +
      // RIVAL
      '<div class="jv-side jv-side--rival">' +
        pbarEl(b.rival) + auxRow(b.rival) + benchEl(b.rival.banca) +
        '<div class="jv-active-wrap"><div class="jv-active">' + cartaEl(b.rival.activo) + '</div></div>' +
      '</div>' +
      // CENTRO
      centro +
      '<div class="jv-center"><div class="jv-zlabel">' + tx('jv_z_stadium', 'Estadio') + '</div>' +
        '<div class="jv-zlabel">' + tx('jv_z_lost', 'Zona perdida') + '</div></div>' +
      // TÚ
      '<div class="jv-side jv-side--yo">' +
        '<div class="jv-active-wrap"><div class="jv-active">' + yoActivo + '</div></div>' +
        yoBanca + auxRow(b.yo) + pbarEl(b.yo) +
      '</div>' +
      '</div>' +
      // MANO
      '<div class="jv-hand"><div class="jv-hand-inner">' + manoHtml + '</div></div>' +
      finOverlay();
  }

  // ---------- Pantalla de inicio de partida ----------
  function pantallaInicio() {
    const decks = misMazos();
    if (!decks.length) {
      return '<div class="jv-board"><div class="jv-start">' + closeBtn() +
        '<h3>' + esc(tx('jv_start_title', 'Juego virtual')) + '</h3>' +
        '<p>' + esc(tx('jv_no_decks', 'Primero crea o guarda un mazo en "Construir mazo" o "Mazos".')) + '</p></div></div>';
    }
    const opts = decks.map(function (d, i) { return '<option value="' + i + '">' + esc(d.name || ('Mazo ' + (i + 1))) + '</option>'; }).join('');
    return '<div class="jv-board"><div class="jv-start">' + closeBtn() +
      '<h3>' + esc(tx('jv_start_title', 'Juego virtual')) + '</h3>' +
      '<label class="jv-field"><span>' + esc(tx('jv_your_deck', 'Tu mazo')) + '</span><select id="jv-deck-yo" class="control-select">' + opts + '</select></label>' +
      '<label class="jv-field"><span>' + esc(tx('jv_rival_deck', 'Mazo del rival (práctica)')) + '</span><select id="jv-deck-rival" class="control-select">' + opts + '</select></label>' +
      '<p class="jv-note">' + esc(tx('jv_start_note', 'Práctica local: tú colocas tus Pokémon; el rival se prepara solo. Los turnos llegarán en la siguiente fase.')) + '</p>' +
      '<button class="jv-btn jv-btn-big" type="button" onclick="jvEmpezar()">' + esc(tx('jv_start_btn', 'Repartir y empezar')) + '</button>' +
      '</div></div>';
  }

  let _rivalTimer = null;
  function renderJuego() {
    const root = document.getElementById('juego-root');
    if (!root) return;
    root.innerHTML = G ? renderTablero() : pantallaInicio();
    // Turno del rival (sin IA todavía): auto-pasa para que el ciclo sea observable.
    if (_rivalTimer) { clearTimeout(_rivalTimer); _rivalTimer = null; }
    if (G && !G.ganador && G.fase !== JUEGO.FASE.SETUP && G.turnoDe === 'B') {
      _rivalTimer = setTimeout(function () {
        _rivalTimer = null;
        if (G && G.turnoDe === 'B' && !G.ganador) { JUEGO.terminarTurno(G); renderJuego(); }
      }, 850);
    }
  }

  // ---------- Acciones (globales para onclick) ----------
  function cartasDeMazo(deck) {
    return (typeof JUEGO !== 'undefined') ? JUEGO.expandirMazo(deck, resolverCarta) : [];
  }
  window.jvEmpezar = function () {
    const decks = misMazos(); if (!decks.length) return;
    const iy = parseInt((document.getElementById('jv-deck-yo') || {}).value, 10) || 0;
    const ir = parseInt((document.getElementById('jv-deck-rival') || {}).value, 10) || 0;
    const dy = decks[iy], dr = decks[ir] || decks[iy];
    G = JUEGO.crearPartida({
      ladoA: { nombre: tx('vs_you', 'Tú'), cartas: cartasDeMazo(dy) },
      ladoB: { nombre: tx('jv_rival', 'Rival'), cartas: cartasDeMazo(dr) }
    });
    JUEGO.autoSetup(G, 'B');
    renderJuego();
  };
  window.jvPlace = function (iid) {
    if (!G) return;
    const A = G.lados.A;
    if (!A.activo) JUEGO.colocarActivo(G, 'A', iid);
    else JUEGO.colocarBanca(G, 'A', iid);
    renderJuego();
  };
  window.jvUnplace = function (iid) { if (G) { JUEGO.quitarColocado(G, 'A', iid); renderJuego(); } };
  window.jvAuto = function () { if (G) { JUEGO.autoSetup(G, 'A'); renderJuego(); } };
  window.jvConfirm = function () { if (G) { JUEGO.confirmarSetup(G, 'A'); renderJuego(); } };
  window.jvFinTurno = function () { if (G && G.turnoDe === 'A' && !G.ganador) { JUEGO.terminarTurno(G); renderJuego(); } };
  window.jvNueva = function () { G = null; renderJuego(); };
  window.jvSalir = function () { if (_rivalTimer) { clearTimeout(_rivalTimer); _rivalTimer = null; } G = null; window.setVersusMode('fisico'); };

  // ---------- Pantalla completa inmersiva (solo overlay CSS) ----------
  function entrarFull() {
    const jr = document.getElementById('juego-root');
    document.body.classList.add('jv-full');
    if (jr) jr.classList.add('jv-full-root');
  }
  function salirFull() {
    const jr = document.getElementById('juego-root');
    document.body.classList.remove('jv-full');
    if (jr) jr.classList.remove('jv-full-root');
  }

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

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && document.body.classList.contains('jv-full')) window.setVersusMode('fisico');
  });

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
