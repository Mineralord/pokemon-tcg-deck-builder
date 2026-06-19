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
  let _accion = null; // acción pendiente de objetivo: {tipo:'evo'|'energia'|'retirar', iid?}

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
  const ZAP = '<svg viewBox="0 0 24 24" width="9" height="9" fill="currentColor" aria-hidden="true"><path d="M13 2 4.5 13.5H11l-1 8.5L19.5 10H13z"/></svg>';
  function cardBadges(c) {
    if (!c) return '';
    let b = '';
    const ne = (c.energias || []).length;
    if (ne) b += '<span class="jv-badge jv-badge-e">' + ZAP + ne + '</span>';
    if (c.danio) b += '<span class="jv-badge jv-badge-d">' + c.danio + '</span>';
    return b ? ('<span class="jv-badges">' + b + '</span>') : '';
  }
  const COND_MAP = { poisoned: ['PSN', '#22c55e'], burned: ['BRN', '#f97316'], asleep: ['SLP', '#38bdf8'], paralyzed: ['PAR', '#eab308'], confused: ['CNF', '#a855f7'] };
  function condChips(c) {
    const cs = (c && c.condiciones) || [];
    if (!cs.length) return '';
    return '<span class="jv-conds">' + cs.map(function (x) {
      const m = COND_MAP[x] || [x, '#999']; return '<i class="jv-cond" style="background:' + m[1] + '">' + m[0] + '</i>';
    }).join('') + '</span>';
  }
  function cartaEl(c, opt) {
    opt = opt || {};
    const oc = opt.onclick ? (' onclick="' + opt.onclick + '"') : '';
    if (opt.dorso) return '<div class="jv-card jv-card--back" aria-hidden="true"></div>';
    if (!c) return '<div class="jv-slot" aria-hidden="true"></div>';
    const cls = 'jv-card' + (opt.clickable ? ' clickable' : '') + (opt.sel ? ' jv-sel' : '') +
      (opt.target ? ' jv-target' : '') + (opt.dim ? ' jv-dim' : '');
    const badges = opt.badges || '';
    const inner = c.imagen
      ? '<img src="' + esc(c.imagen) + '" alt="' + esc(c.nombre) + '" loading="lazy" decoding="async">'
      : '<span class="jv-ph-name">' + esc(c.nombre) + '</span>' + (c.hp ? '<span class="jv-ph-hp">' + c.hp + '</span>' : '');
    const phCls = c.imagen ? '' : ' jv-card--ph';
    return '<div class="' + cls + phCls + '" title="' + esc(c.nombre) + '"' + oc + '>' + inner + condChips(c) + badges + '</div>';
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
  function benchEl(banca, each) {
    let html = '';
    for (let i = 0; i < 5; i++) {
      const c = banca && banca[i] ? banca[i] : null;
      html += c ? cartaEl(c, each ? each(c) : {}) : cartaEl(null);
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
    const motivos = { deckout: tx('jv_by_deckout', 'por agotar el mazo del rival'), premios: tx('jv_by_prizes', 'por tomar todos los premios'), sinpokemon: tx('jv_by_nopokemon', 'sin Pokémon en juego') };
    const motivo = motivos[G.motivoFin] || '';
    return '<div class="jv-overlay"><div class="jv-overlay-card ' + (gane ? 'win' : 'lose') + '">' +
      '<h3>' + esc(titulo) + '</h3>' + (motivo ? '<p>' + esc(motivo) + '</p>' : '') +
      '<button class="jv-btn jv-btn-big" type="button" onclick="jvNueva()">' + esc(tx('jv_new_game', 'Nueva partida')) + '</button>' +
      '</div></div>';
  }

  function closeBtn() {
    return '<button class="jv-close" type="button" aria-label="' + esc(tx('jv_exit', 'Salir')) + '" title="' + esc(tx('jv_exit', 'Salir')) + '" onclick="jvSalir()">' +
      '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg></button>';
  }

  const TIPO_COLOR = { Grass: '#22c55e', Fire: '#f97316', Water: '#38bdf8', Lightning: '#eab308', Psychic: '#a855f7', Fighting: '#b45309', Darkness: '#1f2937', Metal: '#94a3b8', Fairy: '#ec4899', Dragon: '#ca8a04', Colorless: '#cbd5e1' };
  function costeHtml(coste) {
    if (!coste || !coste.length) return '<span class="jv-cost"><i style="background:#cbd5e1"></i></span>';
    return '<span class="jv-cost">' + coste.map(function (t) { return '<i style="background:' + (TIPO_COLOR[t] || '#cbd5e1') + '"></i>'; }).join('') + '</span>';
  }
  function attacksBar() {
    if (!G || G.ganador || G.turnoDe !== 'A' || !JUEGO.puedeAtacar(G)) return '';
    const at = G.lados.A.activo; if (!at || !at.ataques || !at.ataques.length) return '';
    const btns = at.ataques.map(function (a, i) {
      const pag = JUEGO.puedePagar(at, a.coste);
      return '<button class="jv-atk" type="button"' + (pag ? '' : ' disabled') + ' onclick="jvAtacar(' + i + ')">' +
        costeHtml(a.coste) + '<span class="jv-atk-name">' + esc(a.nombre) + '</span>' +
        (a.danioRaw ? '<span class="jv-atk-dmg">' + esc(a.danioRaw) + '</span>' : '') + '</button>';
    }).join('');
    return '<div class="jv-attacks"><div class="jv-attacks-h">' + esc(tx('jv_attack', 'Atacar')) + '</div>' + btns + '</div>';
  }

  function manoCard(iid) { return (G.lados.A.mano || []).find(function (c) { return c.iid === iid; }) || null; }
  // ¿La carta en juego `c` es objetivo válido de la acción pendiente?
  function esObjetivo(c, esActivo) {
    if (!_accion) return false;
    if (_accion.tipo === 'energia') return true;
    if (_accion.tipo === 'evo') { const e = manoCard(_accion.iid); return !!(e && c.nombre === e.evolucionaDe && G.turno > (c.enJuegoDesde || 0)); }
    if (_accion.tipo === 'retirar') return !esActivo; // objetivo = un banco
    return false;
  }
  function accionBanner() {
    if (!_accion) return '';
    const msg = _accion.tipo === 'energia' ? tx('jv_pick_energy_t', 'Elige a quién adjuntar la energía')
      : _accion.tipo === 'evo' ? tx('jv_pick_evo_t', 'Elige el Pokémon a evolucionar')
        : tx('jv_pick_retreat_t', 'Elige el Pokémon de banca que pasa a Activo');
    return '<div class="jv-actionbar"><span>' + esc(msg) + '</span>' +
      '<button class="jv-btn jv-btn-2" type="button" onclick="jvCancelar()">' + esc(tx('jv_cancel', 'Cancelar')) + '</button></div>';
  }

  function renderTablero() {
    const b = boardView();
    const setup = b.enSetup;
    const jugable = !setup && G.turnoDe === 'A' && G.fase === JUEGO.FASE.MAIN && !G.ganador;

    // ---- Mano propia ----
    const manoHtml = (b.yo.mano || []).map(function (c) {
      const basic = c.supertipo === 'Pokemon' && c.esBasico;
      if (setup) return cartaEl(c, { clickable: basic, dim: !basic, onclick: basic ? ("jvPlace('" + c.iid + "')") : '' });
      const sel = _accion && _accion.iid === c.iid;
      const jugarEsta = jugable && (basic || (c.supertipo === 'Pokemon' && c.evolucionaDe) || c.supertipo === 'Energy');
      return cartaEl(c, { clickable: jugable, sel: sel, dim: jugable && !jugarEsta, onclick: jugable ? ("jvManoClick('" + c.iid + "')") : '' });
    }).join('');

    // ---- Tu Activo / Banca ----
    let yoActivo, yoBanca;
    if (setup) {
      yoActivo = cartaEl(b.yo.activo, { clickable: !!b.yo.activo, onclick: b.yo.activo ? ("jvUnplace('" + b.yo.activo.iid + "')") : '' });
      yoBanca = benchEl(b.yo.banca, function (c) { return { clickable: true, onclick: "jvUnplace('" + c.iid + "')" }; });
    } else {
      yoActivo = cartaEl(b.yo.activo, b.yo.activo ? activoOpt(b.yo.activo, jugable) : {});
      yoBanca = benchEl(b.yo.banca, function (c) { return juegoOpt(c, false, jugable); });
    }
    const rivalBanca = benchEl(b.rival.banca, function (c) { return { badges: cardBadges(c) }; });
    const rivalActivo = cartaEl(b.rival.activo, { badges: cardBadges(b.rival.activo) });

    const centro = setup ? setupBar() : (accionBanner() + hudBar(b));

    return '' +
      '<div class="jv-board">' +
      closeBtn() +
      '<div class="jv-side jv-side--rival">' +
        pbarEl(b.rival) + auxRow(b.rival) + rivalBanca +
        '<div class="jv-active-wrap"><div class="jv-active">' + rivalActivo + '</div></div>' +
      '</div>' +
      centro +
      '<div class="jv-center"><div class="jv-zlabel">' + tx('jv_z_stadium', 'Estadio') + '</div>' +
        '<div class="jv-zlabel">' + tx('jv_z_lost', 'Zona perdida') + '</div></div>' +
      '<div class="jv-side jv-side--yo">' +
        '<div class="jv-active-wrap"><div class="jv-active">' + yoActivo + '</div></div>' +
        yoBanca + auxRow(b.yo) + pbarEl(b.yo) +
      '</div>' +
      '</div>' +
      attacksBar() +
      '<div class="jv-hand"><div class="jv-hand-inner">' + manoHtml + '</div></div>' +
      finOverlay();
  }

  // Opciones de render de un Pokémon en juego propio (banca o activo) en fase MAIN.
  function juegoOpt(c, esActivo, jugable) {
    const o = { badges: cardBadges(c) };
    if (!jugable) return o;
    if (_accion && esObjetivo(c, esActivo)) { o.target = true; o.clickable = true; o.onclick = "jvJuegoClick('" + c.iid + "')"; }
    return o;
  }
  function activoOpt(c, jugable) {
    const o = juegoOpt(c, true, jugable);
    // Sin acción pendiente: tocar el Activo inicia la retirada (si hay banca y no se usó).
    if (jugable && !_accion && G.lados.A.banca.length && !G.lados.A.retiroUsado) {
      o.clickable = true; o.onclick = "jvActivoClick()";
    }
    return o;
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
  window.jvManoClick = function (iid) {
    if (!G || G.turnoDe !== 'A' || G.fase !== JUEGO.FASE.MAIN || G.ganador) return;
    const A = G.lados.A; const c = A.mano.find(function (x) { return x.iid === iid; }); if (!c) return;
    if (c.supertipo === 'Pokemon' && c.esBasico) { JUEGO.ponerEnBanca(G, 'A', iid); _accion = null; }
    else if (c.supertipo === 'Pokemon' && c.evolucionaDe) { _accion = (_accion && _accion.iid === iid) ? null : { tipo: 'evo', iid: iid }; }
    else if (c.supertipo === 'Energy') { if (!A.energiaUsada) _accion = (_accion && _accion.iid === iid) ? null : { tipo: 'energia', iid: iid }; }
    else { _accion = null; } // entrenadores: Fase 8
    renderJuego();
  };
  window.jvJuegoClick = function (iid) {
    if (!G || !_accion) return;
    if (_accion.tipo === 'evo') JUEGO.evolucionar(G, 'A', _accion.iid, iid);
    else if (_accion.tipo === 'energia') JUEGO.adjuntarEnergia(G, 'A', _accion.iid, iid);
    else if (_accion.tipo === 'retirar') JUEGO.retirar(G, 'A', iid);
    _accion = null; renderJuego();
  };
  window.jvAtacar = function (i) { if (G && G.turnoDe === 'A' && !G.ganador) { _accion = null; JUEGO.atacar(G, 'A', i); renderJuego(); } };
  window.jvActivoClick = function () { if (G) { _accion = { tipo: 'retirar' }; renderJuego(); } };
  window.jvCancelar = function () { _accion = null; renderJuego(); };
  window.jvFinTurno = function () { if (G && G.turnoDe === 'A' && !G.ganador) { _accion = null; JUEGO.terminarTurno(G); renderJuego(); } };
  window.jvNueva = function () { _accion = null; G = null; renderJuego(); };
  window.jvSalir = function () { if (_rivalTimer) { clearTimeout(_rivalTimer); _rivalTimer = null; } _accion = null; G = null; window.setVersusMode('fisico'); };

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
