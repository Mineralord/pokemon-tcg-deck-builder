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
  let _modo = 'local'; // 'local' (vs IA) | 'pase' | 'online'
  let _miLado = 'A';   // lado del motor que controla ESTE cliente
  let _handoff = false, _handoffA = null; // relevo de dispositivo en "pase y juega"

  function MI() { return _miLado; }
  function OP() { return _miLado === 'A' ? 'B' : 'A'; }
  function L() { return G.lados[_miLado]; }
  // Serializa la partida sin el campo pesado `raw` para sincronizar por Firestore.
  function slimG() { return JSON.parse(JSON.stringify(G, function (k, v) { return k === 'raw' ? undefined : v; })); }
  function pushOnline() {
    if (_modo !== 'online' || !G) return;
    G.seq = (G.seq || 0) + 1;
    if (typeof window.salaSetPartida === 'function') window.salaSetPartida(slimG());
  }
  const SAVE_KEY = 'jv_save';
  let _hist = [];      // historial de estados (para deshacer; local/pase)
  let _zoom = null;    // { side, iid } carta ampliada
  let _sel = [];       // iids seleccionados para la elección pendiente (Fase 2)
  function histPush() { if (_modo === 'online' || !G) return; try { _hist.push(JSON.stringify(slimG())); if (_hist.length > 25) _hist.shift(); } catch (e) {} }
  function autosave() {
    try {
      if (_modo !== 'online' && G && !G.ganador) localStorage.setItem(SAVE_KEY, JSON.stringify({ modo: _modo, miLado: _miLado, g: slimG() }));
      else localStorage.removeItem(SAVE_KEY);
    } catch (e) {}
  }
  function haySave() { try { return !!localStorage.getItem(SAVE_KEY); } catch (e) { return false; } }

  // Tras una mutación local: sincroniza (si online) y re-renderiza.
  function trasMutar() { pushOnline(); renderJuego(); maybeHandoff(0); histPush(); autosave(); }
  // En "pase y juega": muestra el relevo cuando cambia el jugador en turno o en el setup.
  function maybeHandoff(delay) {
    if (_modo !== 'pase' || !G || G.ganador) return;
    let target = null;
    if (G.fase === JUEGO.FASE.SETUP) {
      if (G.lados[_miLado].estado === 'ready' && G.lados[OP()].estado === 'setup') target = OP();
    } else if (G.turnoDe !== _miLado) target = G.turnoDe;
    if (!target) return;
    setTimeout(function () { _handoffA = target; _handoff = true; renderJuego(); }, delay || 0);
  }

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
    const aria = opt.clickable ? (' role="button" tabindex="0" aria-label="' + esc(c.nombre) + '"') : '';
    return '<div class="' + cls + phCls + '" title="' + esc(c.nombre) + '"' + oc + aria + '>' + inner + condChips(c) + badges + '</div>';
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
    const A = G.lados[MI()], B = G.lados[OP()];
    return {
      enSetup: A.estado === 'setup',
      turnoDe: (G.fase === JUEGO.FASE.SETUP) ? null : (G.turnoDe === MI() ? 'yo' : 'rival'),
      rival: { nombre: B.nombre, premios: B.premios.length, mazo: B.mazo.length, descarte: B.descarte.length, manoN: B.mano.length, activo: B.activo, banca: B.banca },
      yo: { nombre: A.nombre, premios: A.premios.length, mazo: A.mazo.length, descarte: A.descarte.length, activo: A.activo, banca: A.banca, mano: A.mano }
    };
  }

  function setupBar() {
    const A = L();
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
    const miTurno = (G.turnoDe === MI()) && !G.ganador;
    const turnoTxt = b.turnoDe === 'yo' ? tx('jv_your_turn', 'Tu turno') : tx('jv_their_turn', 'Turno del rival');
    let html = '<div class="jv-hud">' +
      '<span class="jv-turn' + (miTurno ? ' jv-turn-mio' : '') + '">' + esc(tx('jv_turn', 'Turno')) + ' ' + (G.turno || 1) + ' · ' + esc(turnoTxt) + '</span>';
    if (miTurno) {
      html += '<button class="jv-btn jv-btn-2" type="button" onclick="jvFinTurno()">' + esc(tx('jv_end_turn', 'Terminar turno')) + '</button>';
    } else if (!G.ganador) {
      html += '<span class="jv-thinking">' + esc(tx('jv_rival_thinking', 'El rival juega…')) + '</span>';
    }
    html += '</div>';
    if (miTurno) html += '<div class="jv-hint">' + esc(tx('jv_hint_play', 'Toca cartas de tu mano para jugarlas, tu Activo para retirarte, y abajo para atacar.')) + '</div>';
    // Barra de utilidades: deshacer (local/pase) + rendirse.
    let utils = '';
    if (_modo !== 'online' && _hist.length > 1) utils += '<button class="jv-util" type="button" onclick="jvDeshacer()">' + esc(tx('jv_undo', 'Deshacer')) + '</button>';
    if (!G.ganador) utils += '<button class="jv-util jv-util-danger" type="button" onclick="jvRendirse()">' + esc(tx('jv_concede', 'Rendirse')) + '</button>';
    if (utils) html += '<div class="jv-utils">' + utils + '</div>';
    return html;
  }

  function finOverlay() {
    if (!G || !G.ganador) return '';
    const empate = G.ganador === 'empate';
    const gane = !empate && G.ganador === MI();
    const titulo = empate ? tx('jv_draw', 'Empate') : (gane ? tx('jv_win', '¡Ganaste!') : tx('jv_lose', 'Perdiste'));
    const motivos = { deckout: tx('jv_by_deckout', 'por agotar el mazo del rival'), premios: tx('jv_by_prizes', 'por tomar todos los premios'), sinpokemon: tx('jv_by_nopokemon', 'sin Pokémon en juego'), rendicion: tx('jv_by_concede', 'por rendición'), empate: tx('jv_by_draw', 'ambos sin Pokémon a la vez') };
    const motivo = motivos[G.motivoFin] || '';
    return '<div class="jv-overlay"><div class="jv-overlay-card ' + (empate ? 'draw' : (gane ? 'win' : 'lose')) + '">' +
      '<h3>' + esc(titulo) + '</h3>' + (motivo ? '<p>' + esc(motivo) + '</p>' : '') +
      '<button class="jv-btn jv-btn-big" type="button" onclick="jvNueva()">' + esc(tx('jv_new_game', 'Nueva partida')) + '</button>' +
      '</div></div>';
  }

  function closeBtn() {
    return '<button class="jv-close" type="button" aria-label="' + esc(tx('jv_exit', 'Salir')) + '" title="' + esc(tx('jv_exit', 'Salir')) + '" onclick="jvSalir()">' +
      '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg></button>' +
      muteBtn();
  }
  function muteBtn() {
    const m = (typeof SONIDO !== 'undefined') && SONIDO.isMuted();
    const ico = m
      ? '<path d="M11 5 6 9H2v6h4l5 4z"/><line x1="22" y1="9" x2="16" y2="15"/><line x1="16" y1="9" x2="22" y2="15"/>'
      : '<path d="M11 5 6 9H2v6h4l5 4z"/><path d="M15.5 8.5a5 5 0 0 1 0 7"/><path d="M19 5a9 9 0 0 1 0 14"/>';
    return '<button class="jv-mute" type="button" aria-label="' + esc(tx('jv_sound', 'Sonido')) + '" title="' + esc(tx('jv_sound', 'Sonido')) + '" onclick="jvMute()">' +
      '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + ico + '</svg></button>';
  }

  // ---------- Efectos visuales/sonido ----------
  function snd(n) { if (typeof SONIDO !== 'undefined') SONIDO.play(n); }
  function fxHit(sel, dmg, ko) {
    setTimeout(function () {
      const wrap = document.querySelector(sel);
      const card = wrap && wrap.querySelector('.jv-card');
      if (card) { card.classList.add('jv-shake'); setTimeout(function () { card.classList.remove('jv-shake'); }, 360); }
      if (wrap) {
        const f = document.createElement('div'); f.className = 'jv-dmgfloat';
        f.textContent = ko ? 'KO' : ('-' + (dmg > 0 ? dmg : 0));
        wrap.appendChild(f); setTimeout(function () { f.remove(); }, 750);
      }
      snd(ko ? 'ko' : 'hit');
      if (ko && typeof SONIDO !== 'undefined') { snd('prize'); SONIDO.vibrate([20, 40, 30]); }
    }, 170);
  }
  function fxAtaque(dmg, ko) { snd('attack'); if (typeof SONIDO !== 'undefined') SONIDO.vibrate(15); fxHit('#juego-root .jv-side--rival .jv-active', dmg, ko); }
  function fxDefensa(dmg, ko) { snd('attack'); if (typeof SONIDO !== 'undefined') SONIDO.vibrate(20); fxHit('#juego-root .jv-side--yo .jv-active', dmg, ko); }

  const TIPO_COLOR = { Grass: '#22c55e', Fire: '#f97316', Water: '#38bdf8', Lightning: '#eab308', Psychic: '#a855f7', Fighting: '#b45309', Darkness: '#1f2937', Metal: '#94a3b8', Fairy: '#ec4899', Dragon: '#ca8a04', Colorless: '#cbd5e1' };
  function costeHtml(coste) {
    if (!coste || !coste.length) return '<span class="jv-cost"><i style="background:#cbd5e1"></i></span>';
    return '<span class="jv-cost">' + coste.map(function (t) { return '<i style="background:' + (TIPO_COLOR[t] || '#cbd5e1') + '"></i>'; }).join('') + '</span>';
  }
  function attacksBar() {
    if (!G || G.ganador || G.turnoDe !== MI() || !JUEGO.puedeAtacar(G)) return '';
    const at = L().activo; if (!at || !at.ataques || !at.ataques.length) return '';
    const btns = at.ataques.map(function (a, i) {
      const pag = JUEGO.puedePagar(at, a.coste);
      return '<button class="jv-atk" type="button"' + (pag ? '' : ' disabled') + ' onclick="jvAtacar(' + i + ')">' +
        '<span class="jv-atk-top">' + costeHtml(a.coste) + '<span class="jv-atk-name">' + esc(a.nombre) + '</span>' +
        (a.danioRaw ? '<span class="jv-atk-dmg">' + esc(a.danioRaw) + '</span>' : '') + '</span>' +
        (a.texto ? '<span class="jv-atk-fx">' + esc(a.texto) + '</span>' : '') + '</button>';
    }).join('');
    return '<div class="jv-attacks"><div class="jv-attacks-h">' + esc(tx('jv_attack', 'Atacar')) + '</div>' + btns + '</div>';
  }

  // Panel de habilidades de tus Pokémon en juego.
  function habilidadesPanel() {
    if (!G || G.ganador || G.turnoDe !== MI() || G.fase !== JUEGO.FASE.MAIN) return '';
    const A = L(); const enJuego = (A.activo ? [A.activo] : []).concat(A.banca);
    const rows = [];
    enJuego.forEach(function (c) {
      (c.habilidades || []).forEach(function (h, idx) {
        rows.push('<div class="jv-hab"><div class="jv-hab-t"><b>' + esc(c.nombre) + '</b> · ' + esc(h.nombre) + '</div>' +
          '<div class="jv-hab-x">' + esc(h.texto || '') + '</div>' +
          '<button class="jv-mbtn" type="button" onclick="jvHab(\'' + c.iid + '\',' + idx + ')">' + esc(tx('jv_use', 'Usar')) + '</button></div>');
      });
    });
    if (!rows.length) return '';
    return '<details class="jv-manual"><summary>' + esc(tx('jv_abilities', 'Habilidades')) + '</summary><div class="jv-habs">' + rows.join('') + '</div></details>';
  }

  // Panel de acciones manuales (respaldo para efectos no automatizados).
  function manualPanel() {
    if (!G || G.ganador || G.turnoDe !== MI() || G.fase !== JUEGO.FASE.MAIN) return '';
    function b(fn, label) { return '<button class="jv-mbtn" type="button" onclick="' + fn + '">' + esc(label) + '</button>'; }
    return '<details class="jv-manual"><summary>' + esc(tx('jv_manual', 'Acciones manuales (efectos)')) + '</summary>' +
      '<div class="jv-manual-grid">' +
        b('jvMD(10)', tx('jv_m_dmg', 'Daño rival') + ' +10') + b('jvMD(20)', tx('jv_m_dmg', 'Daño rival') + ' +20') + b('jvMD(30)', tx('jv_m_dmg', 'Daño rival') + ' +30') +
        b('jvMC(10)', tx('jv_m_heal', 'Curar') + ' 10') + b('jvMC(20)', tx('jv_m_heal', 'Curar') + ' 20') + b('jvMR()', tx('jv_m_draw', 'Robar 1')) +
        b("jvMK('poisoned')", 'PSN') + b("jvMK('burned')", 'BRN') + b("jvMK('asleep')", 'SLP') + b("jvMK('paralyzed')", 'PAR') + b("jvMK('confused')", 'CNF') +
      '</div></details>';
  }

  function manoCard(iid) { return (L().mano || []).find(function (c) { return c.iid === iid; }) || null; }
  function findCard(side, iid) {
    const Z = G.lados[side]; if (!Z) return null;
    if (Z.activo && Z.activo.iid === iid) return Z.activo;
    return Z.banca.find(function (c) { return c.iid === iid; }) || (Z.mano || []).find(function (c) { return c.iid === iid; }) || null;
  }
  function zoomOverlay() {
    if (!_zoom) return '';
    const c = findCard(_zoom.side, _zoom.iid); if (!c) return '';
    const img = c.imagen ? '<img src="' + esc(c.imagen) + '" alt="' + esc(c.nombre) + '">' : '<div class="jv-zoom-noimg">' + esc(c.nombre) + '</div>';
    let info = '<div class="jv-zoom-info"><div class="jv-zoom-name">' + esc(c.nombre) + (c.hp ? ' · ' + c.hp + ' HP' : '') + '</div>';
    (c.ataques || []).forEach(function (a) {
      info += '<div class="jv-zoom-atk">' + costeHtml(a.coste) + '<b>' + esc(a.nombre) + '</b>' + (a.danioRaw ? ' <span>' + esc(a.danioRaw) + '</span>' : '') +
        (a.texto ? '<div class="jv-zoom-t">' + esc(a.texto) + '</div>' : '') + '</div>';
    });
    (c.habilidades || []).forEach(function (h) { info += '<div class="jv-zoom-atk"><b>' + esc(h.nombre) + '</b><div class="jv-zoom-t">' + esc(h.texto || '') + '</div></div>'; });
    info += '</div>';
    return '<div class="jv-zoomwrap" onclick="jvZoomClose()"><div class="jv-zoom" onclick="event.stopPropagation()">' +
      '<button class="jv-zoom-x" type="button" aria-label="' + esc(tx('jv_close', 'Cerrar')) + '" onclick="jvZoomClose()">✕</button>' +
      '<div class="jv-zoom-img">' + img + '</div>' + info + '</div></div>';
  }
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

  // ---------- Overlay de elección (Fase 2: est.pendiente) ----------
  function eleccionOverlay() {
    const p = G && G.pendiente; if (!p) return '';
    if (p.lado !== MI()) {
      return '<div class="jv-overlay"><div class="jv-overlay-card"><div class="jv-overlay-sub">' +
        esc(tx('jv_rival_elige', 'El rival está eligiendo…')) + '</div></div></div>';
    }
    const ops = (p.opciones || []).map(function (o) {
      const on = _sel.indexOf(o.iid) >= 0;
      const img = o.imagen ? '<img src="' + esc(o.imagen) + '" alt="' + esc(o.nombre) + '">' : '<span>' + esc(o.nombre) + '</span>';
      return '<button type="button" class="jv-pick' + (on ? ' jv-pick-on' : '') + '" onclick="jvSel(\'' + o.iid + '\')">' + img + '</button>';
    }).join('');
    const puede = _sel.length >= (p.min || 0);
    const conf = '<button class="jv-btn" type="button" ' + (puede ? '' : 'disabled ') + 'onclick="jvElegir()">' +
      esc(tx('jv_confirm', 'Confirmar')) + ' (' + _sel.length + '/' + p.max + ')</button>';
    const canc = p.cancelable ? '<button class="jv-btn jv-btn-2" type="button" onclick="jvElegirCancelar()">' + esc(tx('jv_skip', 'Pasar')) + '</button>' : '';
    return '<div class="jv-zoomwrap"><div class="jv-choice"><div class="jv-choice-h">' + esc(p.prompt || tx('jv_elige', 'Elige')) + '</div>' +
      '<div class="jv-choice-grid">' + (ops || '<div class="jv-choice-empty">' + esc(tx('jv_sin_opciones', 'Sin opciones')) + '</div>') + '</div>' +
      '<div class="jv-choice-foot">' + conf + canc + '</div></div></div>';
  }

  function renderTablero() {
    const b = boardView();
    const setup = b.enSetup;
    const jugable = !setup && G.turnoDe === MI() && G.fase === JUEGO.FASE.MAIN && !G.ganador;

    // ---- Mano propia ----
    const manoHtml = (b.yo.mano || []).map(function (c) {
      const basic = c.supertipo === 'Pokemon' && c.esBasico;
      if (setup) return cartaEl(c, { clickable: basic, dim: !basic, onclick: basic ? ("jvPlace('" + c.iid + "')") : '' });
      const sel = _accion && _accion.iid === c.iid;
      const jugarEsta = jugable && (basic || (c.supertipo === 'Pokemon' && c.evolucionaDe) || c.supertipo === 'Energy' || c.supertipo === 'Trainer');
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
    const rivalBanca = benchEl(b.rival.banca, function (c) { return { badges: cardBadges(c), clickable: true, onclick: "jvZoom('" + OP() + "','" + c.iid + "')" }; });
    const rivalActivo = cartaEl(b.rival.activo, b.rival.activo ? { badges: cardBadges(b.rival.activo), clickable: true, onclick: "jvZoom('" + OP() + "','" + b.rival.activo.iid + "')" } : {});

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
      attacksBar() + habilidadesPanel() + manualPanel() +
      '<div class="jv-hand"><div class="jv-hand-inner">' + manoHtml + '</div></div>' +
      finOverlay() + zoomOverlay() + eleccionOverlay();
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
    if (jugable && !_accion && L().banca.length && !L().retiroUsado) {
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
    const resume = haySave() ? '<button class="jv-btn jv-btn-big" type="button" onclick="jvReanudar()">' + esc(tx('jv_resume', 'Reanudar partida')) + '</button><div class="jv-or">' + esc(tx('jv_or', 'o')) + '</div>' : '';
    return '<div class="jv-board"><div class="jv-start">' + closeBtn() +
      '<h3>' + esc(tx('jv_start_title', 'Juego virtual')) + '</h3>' +
      resume +
      '<label class="jv-field"><span>' + esc(tx('jv_your_deck', 'Tu mazo')) + '</span><select id="jv-deck-yo" class="control-select">' + opts + '</select></label>' +
      '<label class="jv-field"><span>' + esc(tx('jv_rival_deck', 'Mazo del rival (IA / Jugador 2)')) + '</span><select id="jv-deck-rival" class="control-select">' + opts + '</select></label>' +
      '<label class="jv-field"><span>' + esc(tx('jv_ai_diff', 'Dificultad de la IA')) + '</span><select id="jv-ai-diff" class="control-select">' +
        '<option value="facil">' + esc(tx('jv_easy', 'Fácil')) + '</option>' +
        '<option value="medio" selected>' + esc(tx('jv_med', 'Media')) + '</option>' +
        '<option value="dificil">' + esc(tx('jv_hard', 'Difícil')) + '</option></select></label>' +
      '<label class="jv-field"><span>' + esc(tx('jv_ai_mode', 'Comportamiento de la IA')) + '</span><select id="jv-ai-mode" class="control-select">' +
        '<option value="reglas" selected>' + esc(tx('jv_rules_b', 'Sigue reglas')) + '</option>' +
        '<option value="random">' + esc(tx('jv_random_b', 'Aleatorio')) + '</option></select></label>' +
      '<p class="jv-note">' + esc(tx('jv_start_note', 'Práctica local: tú colocas tus Pokémon; la IA se prepara y juega sola.')) + '</p>' +
      '<button class="jv-btn jv-btn-big" type="button" onclick="jvEmpezar()">' + esc(tx('jv_start_btn', 'Repartir y empezar')) + '</button>' +
      '<div class="jv-or">' + esc(tx('jv_or', 'o')) + '</div>' +
      '<button class="jv-btn jv-btn-2 jv-btn-big" type="button" onclick="jvPase()">' + esc(tx('jv_pase', 'Pase y juega (mismo dispositivo)')) + '</button>' +
      '<button class="jv-btn jv-btn-2 jv-btn-big" type="button" onclick="jvOnline()">' + esc(tx('jv_online', 'Jugar online (2 jugadores)')) + '</button>' +
      '<p class="jv-note">' + esc(tx('jv_online_note', 'Online: los dos jugadores deben tener Versus abierto. Juegas con el mazo de arriba.')) + '</p>' +
      '</div></div>';
  }
  function pantallaEsperaOnline() {
    return '<div class="jv-board"><div class="jv-start">' + closeBtn() + muteBtn() +
      '<h3>' + esc(tx('jv_online', 'Juego online')) + '</h3>' +
      '<p class="jv-note">' + esc(tx('jv_waiting_online', 'Esperando al otro jugador… (debe elegir su mazo y pulsar "Jugar online").')) + '</p>' +
      '<button class="jv-btn jv-btn-2 jv-btn-big" type="button" onclick="jvNueva()">' + esc(tx('jv_cancel', 'Cancelar')) + '</button>' +
      '</div></div>';
  }
  function pantallaHandoff() {
    const nom = (G.lados[_handoffA] && G.lados[_handoffA].nombre) || tx('jv_player', 'Jugador');
    return '<div class="jv-board"><div class="jv-handoff">' +
      '<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect width="14" height="20" x="5" y="2" rx="2"/><path d="M12 18h.01"/></svg>' +
      '<h3>' + esc(tx('jv_pass_device', 'Pasa el dispositivo')) + '</h3>' +
      '<p>' + esc(tx('jv_pass_to', 'Le toca a')) + ' <b>' + esc(nom) + '</b></p>' +
      '<button class="jv-btn jv-btn-big" type="button" onclick="jvHandoff()">' + esc(tx('jv_im_ready', 'Estoy listo')) + '</button>' +
      '</div></div>';
  }

  let _rivalTimer = null, _finSonado = false;
  function renderJuego() {
    const root = document.getElementById('juego-root');
    if (!root) return;
    if (_handoff && G) { root.innerHTML = pantallaHandoff(); return; }
    root.innerHTML = G ? renderTablero() : (_modo === 'online' ? pantallaEsperaOnline() : pantallaInicio());
    if (G && G.ganador) { if (!_finSonado) { _finSonado = true; snd(G.ganador === MI() ? 'win' : 'lose'); } }
    else _finSonado = false;
    // Turno del rival (sin IA todavía): auto-pasa para que el ciclo sea observable.
    if (_rivalTimer) { clearTimeout(_rivalTimer); _rivalTimer = null; }
    if (_modo !== 'online' && G && !G.ganador && G.fase !== JUEGO.FASE.SETUP && G.turnoDe === OP()) {
      _rivalTimer = setTimeout(function () {
        _rivalTimer = null;
        if (!(G && G.turnoDe === OP() && !G.ganador)) return;
        const ya = G.lados[MI()].activo; const idA = ya && ya.iid; const dA = ya ? (ya.danio || 0) : 0;
        if (typeof JUEGO_IA !== 'undefined' && JUEGO_IA.jugarTurno) JUEGO_IA.jugarTurno(G);
        else JUEGO.terminarTurno(G);
        autoResolverIA();
        const aa = G.lados[MI()].activo;
        let ko = false, dmg = 0;
        if (!aa || aa.iid !== idA) ko = (idA != null); else dmg = (aa.danio || 0) - dA;
        renderJuego();
        if (ko || dmg > 0) fxDefensa(dmg, ko);
      }, 750);
    }
  }

  // Auto-resuelve elecciones pendientes del lado IA (local). Delega en el motor de IA.
  function autoResolverIA() {
    if (typeof JUEGO_IA !== 'undefined' && JUEGO_IA.resolverPendientes) { JUEGO_IA.resolverPendientes(G, OP()); return; }
    let guard = 0;
    while (G && G.pendiente && G.pendiente.lado === OP() && guard++ < 20) {
      const p = G.pendiente;
      const n = Math.max(p.min || 0, Math.min(p.max || 1, 1));
      const sel = (p.opciones || []).slice(0, n).map(function (o) { return o.iid; });
      JUEGO.resolverEleccion(G, OP(), sel);
    }
  }

  // ---------- Acciones (globales para onclick) ----------
  function cartasDeMazo(deck) {
    return (typeof JUEGO !== 'undefined') ? JUEGO.expandirMazo(deck, resolverCarta) : [];
  }
  window.jvEmpezar = function () {
    const decks = misMazos(); if (!decks.length) return;
    _modo = 'local'; _miLado = 'A';
    const iy = parseInt((document.getElementById('jv-deck-yo') || {}).value, 10) || 0;
    const ir = parseInt((document.getElementById('jv-deck-rival') || {}).value, 10) || 0;
    const dy = decks[iy], dr = decks[ir] || decks[iy];
    if (typeof JUEGO_IA !== 'undefined' && JUEGO_IA.configurar) {
      JUEGO_IA.configurar({
        dificultad: ((document.getElementById('jv-ai-diff') || {}).value) || 'medio',
        modo: ((document.getElementById('jv-ai-mode') || {}).value) || 'reglas'
      });
    }
    G = JUEGO.crearPartida({
      ladoA: { nombre: tx('vs_you', 'Tú'), cartas: cartasDeMazo(dy) },
      ladoB: { nombre: tx('jv_rival', 'Rival'), cartas: cartasDeMazo(dr) }
    });
    JUEGO.autoSetup(G, 'B');
    _hist = []; renderJuego(); histPush(); autosave();
  };
  // Inicia / se une a una partida online (2 jugadores reales por Firestore).
  window.jvOnline = function () {
    if (typeof window.salaUid !== 'function' || !window.salaUid()) {
      if (typeof showToast === 'function') showToast(tx('jv_login_first', 'Inicia sesión y abre Versus primero'), 'error');
      return;
    }
    const decks = misMazos(); if (!decks.length) return;
    const iy = parseInt((document.getElementById('jv-deck-yo') || {}).value, 10) || 0;
    const slim = cartasDeMazo(decks[iy]).map(function (c) { const o = Object.assign({}, c); delete o.raw; delete o.es; return o; });
    _modo = 'online';
    _miLado = (typeof window.salaEsDueno === 'function' && window.salaEsDueno()) ? 'A' : 'B';
    G = null;
    if (typeof window.salaSetMiDeck === 'function') window.salaSetMiDeck(slim);
    renderJuego(); // muestra "esperando"; juegoOnSala crea/recibe la partida
  };
  // Recibe cambios de la sala: adopta la partida remota o (anfitrión) la crea.
  window.juegoOnSala = function (sala) {
    if (_modo !== 'online' || !sala) return;
    if (sala.partida) {
      let p = null; try { p = JSON.parse(sala.partida); } catch (e) {}
      if (p && (!G || (p.seq || 0) > (G.seq || 0))) {
        const aBefore = G && G.lados[MI()].activo; const idA = aBefore && aBefore.iid; const dA = aBefore ? (aBefore.danio || 0) : 0;
        G = p; renderJuego();
        // fx si mi Activo recibió daño en el turno del rival
        const aa = G.lados[MI()].activo;
        if (aBefore) { if (!aa || aa.iid !== idA) fxDefensa(0, true); else if ((aa.danio || 0) > dA) fxDefensa((aa.danio || 0) - dA, false); }
      }
      return;
    }
    if (G) { G = null; renderJuego(); return; } // la partida fue limpiada
    if (_miLado === 'A') {
      const miD = window.salaGetDeck && window.salaGetDeck('yo');
      const suD = window.salaGetDeck && window.salaGetDeck('otro');
      if (miD && suD) {
        G = JUEGO.crearPartida({ ladoA: { nombre: tx('vs_owner', 'Anfitrión'), cartas: miD }, ladoB: { nombre: tx('vs_guest', 'Invitada'), cartas: suD } });
        JUEGO.autoSetup(G, 'A'); JUEGO.autoSetup(G, 'B');
        pushOnline(); renderJuego();
      }
    }
  };
  // Pase y juega: 2 jugadores en el mismo dispositivo, por turnos con relevo.
  window.jvPase = function () {
    const decks = misMazos(); if (!decks.length) return;
    _modo = 'pase'; _miLado = 'A'; _handoff = false; _accion = null;
    const iy = parseInt((document.getElementById('jv-deck-yo') || {}).value, 10) || 0;
    const ir = parseInt((document.getElementById('jv-deck-rival') || {}).value, 10) || 0;
    G = JUEGO.crearPartida({
      ladoA: { nombre: tx('jv_p1', 'Jugador 1'), cartas: cartasDeMazo(decks[iy]) },
      ladoB: { nombre: tx('jv_p2', 'Jugador 2'), cartas: cartasDeMazo(decks[ir] || decks[iy]) }
    });
    _hist = []; renderJuego(); histPush(); autosave(); // el Jugador 1 (lado A) coloca primero
  };
  window.jvHandoff = function () { _miLado = _handoffA; _handoff = false; _accion = null; renderJuego(); };

  window.jvPlace = function (iid) {
    if (!G) return;
    if (!L().activo) JUEGO.colocarActivo(G, MI(), iid);
    else JUEGO.colocarBanca(G, MI(), iid);
    trasMutar();
  };
  window.jvUnplace = function (iid) { if (G) { JUEGO.quitarColocado(G, MI(), iid); trasMutar(); } };
  window.jvAuto = function () { if (G) { JUEGO.autoSetup(G, MI()); trasMutar(); } };
  window.jvConfirm = function () { if (G) { JUEGO.confirmarSetup(G, MI()); trasMutar(); } };
  window.jvManoClick = function (iid) {
    if (!G || G.turnoDe !== MI() || G.fase !== JUEGO.FASE.MAIN || G.ganador) return;
    const A = L(); const c = A.mano.find(function (x) { return x.iid === iid; }); if (!c) return;
    if (c.supertipo === 'Pokemon' && c.esBasico) { JUEGO.ponerEnBanca(G, MI(), iid); _accion = null; snd('button'); trasMutar(); }
    else if (c.supertipo === 'Pokemon' && c.evolucionaDe) { _accion = (_accion && _accion.iid === iid) ? null : { tipo: 'evo', iid: iid }; renderJuego(); }
    else if (c.supertipo === 'Energy') { if (!A.energiaUsada) _accion = (_accion && _accion.iid === iid) ? null : { tipo: 'energia', iid: iid }; renderJuego(); }
    else if (c.supertipo === 'Trainer') { JUEGO.jugarEntrenador(G, MI(), iid); _accion = null; snd('button'); trasMutar(); }
    else { _accion = null; renderJuego(); }
  };
  window.jvJuegoClick = function (iid) {
    if (!G || !_accion) return;
    if (_accion.tipo === 'evo') { JUEGO.evolucionar(G, MI(), _accion.iid, iid); snd('evolve'); }
    else if (_accion.tipo === 'energia') { JUEGO.adjuntarEnergia(G, MI(), _accion.iid, iid); snd('energy'); }
    else if (_accion.tipo === 'retirar') { JUEGO.retirar(G, MI(), iid); snd('button'); }
    _accion = null; trasMutar();
  };
  window.jvAtacar = function (i) {
    if (!G || G.turnoDe !== MI() || G.ganador) return;
    _accion = null;
    const rb = G.lados[OP()].activo; const idB = rb && rb.iid; const dB = rb ? (rb.danio || 0) : 0;
    JUEGO.atacar(G, MI(), i);
    const ra = G.lados[OP()].activo;
    let ko = false, dmg = 0;
    if (!ra || ra.iid !== idB) ko = true; else dmg = (ra.danio || 0) - dB;
    pushOnline(); renderJuego();
    fxAtaque(dmg, ko);
    histPush(); autosave();
    if (_modo === 'pase') maybeHandoff(900); // deja ver el impacto antes del relevo
  };
  window.jvMute = function () { if (typeof SONIDO !== 'undefined') { SONIDO.setMute(!SONIDO.isMuted()); if (!SONIDO.isMuted()) snd('button'); renderJuego(); } };
  // Limpieza: la rotación CSS opcional se retiró (no funcionaba en algunos móviles).
  try { localStorage.removeItem('jv_rotar'); } catch (e) {}
  try { document.body.classList.remove('jv-rotar'); } catch (e) {}
  window.jvMD = function (n) { if (G) { JUEGO.manualDanioRival(G, MI(), n); trasMutar(); } };
  window.jvMC = function (n) { if (G) { JUEGO.manualCurar(G, MI(), n); trasMutar(); } };
  window.jvMR = function () { if (G) { JUEGO.manualRobar(G, MI(), 1); trasMutar(); } };
  window.jvMK = function (c) { if (G) { JUEGO.manualCondicionRival(G, MI(), c); trasMutar(); } };
  window.jvHab = function (iid, idx) { if (G) { JUEGO.usarHabilidad(G, MI(), iid, idx); trasMutar(); } };
  window.jvActivoClick = function () { if (G) { _accion = { tipo: 'retirar' }; renderJuego(); } };
  window.jvCancelar = function () { _accion = null; renderJuego(); };
  window.jvRendirse = function () {
    if (!G || G.ganador) return;
    if (typeof confirm === 'function' && !confirm(tx('jv_concede_q', '¿Seguro que quieres rendirte?'))) return;
    JUEGO.rendirse(G, MI()); snd('lose'); trasMutar();
  };
  window.jvDeshacer = function () {
    if (_modo === 'online' || _hist.length < 2) return;
    _hist.pop(); const prev = _hist[_hist.length - 1];
    try { G = JSON.parse(prev); } catch (e) { return; }
    _accion = null; _handoff = false; autosave(); renderJuego();
  };
  window.jvZoom = function (side, iid) { _zoom = { side: side, iid: iid }; renderJuego(); };
  window.jvZoomClose = function () { _zoom = null; renderJuego(); };
  // --- Elección pendiente (Fase 2) ---
  window.jvSel = function (iid) {
    const p = G && G.pendiente; if (!p) return;
    const i = _sel.indexOf(iid);
    if (i >= 0) _sel.splice(i, 1);
    else if (p.max === 1) _sel = [iid];
    else if (_sel.length < p.max) _sel.push(iid);
    snd('button'); renderJuego();
  };
  window.jvElegir = function () {
    const p = G && G.pendiente; if (!p || p.lado !== MI()) return;
    if (_sel.length < (p.min || 0)) return;
    const sel = _sel.slice(); _sel = [];
    JUEGO.resolverEleccion(G, MI(), sel); trasMutar();
  };
  window.jvElegirCancelar = function () {
    const p = G && G.pendiente; if (!p || p.lado !== MI() || !p.cancelable) return;
    _sel = []; JUEGO.resolverEleccion(G, MI(), []); trasMutar();
  };
  window.jvReanudar = function () {
    try {
      const s = JSON.parse(localStorage.getItem(SAVE_KEY)); if (!s || !s.g) return;
      _modo = s.modo || 'local'; _miLado = s.miLado || 'A'; G = s.g; _hist = [JSON.stringify(s.g)]; _handoff = false; _accion = null;
      renderJuego();
    } catch (e) {}
  };
  window.jvFinTurno = function () { if (G && G.turnoDe === MI() && !G.ganador) { _accion = null; JUEGO.terminarTurno(G); trasMutar(); } };
  function borrarSave() { try { localStorage.removeItem(SAVE_KEY); } catch (e) {} }
  window.jvNueva = function () { _accion = null; _handoff = false; _zoom = null; _sel = []; _hist = []; borrarSave(); if (_modo === 'online' && typeof window.salaResetPartida === 'function') window.salaResetPartida(); G = null; _modo = 'local'; renderJuego(); };
  window.jvSalir = function () { if (_rivalTimer) { clearTimeout(_rivalTimer); _rivalTimer = null; } _accion = null; _handoff = false; _zoom = null; G = null; _modo = 'local'; window.setVersusMode('fisico'); };

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
    if (e.key === 'Escape') {
      if (_zoom) { window.jvZoomClose(); return; }
      if (document.body.classList.contains('jv-full')) window.setVersusMode('fisico');
      return;
    }
    // Activación por teclado de cartas/role=button dentro del juego.
    if ((e.key === 'Enter' || e.key === ' ') && document.activeElement) {
      const el = document.activeElement;
      if (el.getAttribute && el.getAttribute('role') === 'button' && el.closest && el.closest('#juego-root')) {
        e.preventDefault(); el.click();
      }
    }
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
