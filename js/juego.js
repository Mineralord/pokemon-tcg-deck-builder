/* =============================================================
   juego.js — Motor del Juego Virtual Pokémon TCG
   Reductor puro y transporte-agnóstico: aplicarAccion(estado, accion) -> estado.
   Sin DOM ni red, para que valga en online / pase y juega / IA y sea testeable
   en Node. Se va llenando por fases (ver plan).
   Fase 1: normalizador de carta (cartaJuego) + expansión de mazo + validación.
   ============================================================= */
(function (global) {
  'use strict';

  const VERSION = 1; // sube con cada fase del motor

  // Fases del juego (rulebook): preparación -> turnos -> fin.
  const FASE = Object.freeze({
    SETUP: 'setup',
    DRAW: 'draw',
    MAIN: 'main',
    ATTACK: 'attack',
    CHECKUP: 'checkup',
    END: 'end'
  });

  // ---------- Utilidades de parseo ----------
  function num(x) { const n = parseInt(x, 10); return isNaN(n) ? 0 : n; }
  function st(view) { return (view && view.supertipo || '').toLowerCase(); }
  function esPokemon(view) { return st(view).indexOf('pok') >= 0; }
  function esEnergia(view) { return st(view).indexOf('energ') >= 0; }
  function esEntrenador(view) { return st(view).indexOf('train') >= 0 || st(view).indexOf('entren') >= 0; }

  // Etapa de evolución: 0 básico, 1 Stage 1, 2 Stage 2 (según texto de "fase").
  function stageDe(view) {
    const f = (view.fase || '').toLowerCase();
    if (f.indexOf('stage 2') >= 0 || f.indexOf('etapa 2') >= 0) return 2;
    if (f.indexOf('stage 1') >= 0 || f.indexOf('etapa 1') >= 0) return 1;
    return 0;
  }

  // Premios que entrega al ser noqueado (ex/V/GX/VSTAR=2; VMAX/TAG TEAM=3; resto=1).
  function premiosDe(view) {
    const reglas = (view.reglas || []).join(' ');
    let m = /takes?\s+(\d+)\s+prize/i.exec(reglas) || /(\d+)\s+cartas?\s+de\s+premio/i.exec(reglas);
    if (m) return Math.max(1, parseInt(m[1], 10));
    const f = (view.fase || '').toLowerCase();
    if (f.indexOf('vmax') >= 0 || f.indexOf('tag team') >= 0) return 3;
    if (/\bex\b/.test(f) || /\bgx\b/.test(f) || f.indexOf('vstar') >= 0 ||
        f.indexOf('v-union') >= 0 || /\bv\b/.test(f)) return 2;
    return 1;
  }

  function parseDebilidad(arr) {
    if (!arr || !arr[0] || !arr[0].type) return null;
    const d = arr[0]; const v = String(d.value || '×2');
    const m = /(-?\d+)/.exec(v); const n = m ? parseInt(m[1], 10) : 2;
    if (/[×x]/i.test(v)) return { tipo: d.type, mult: n || 2 };
    return { tipo: d.type, suma: n };
  }
  function parseResistencia(arr) {
    if (!arr || !arr[0] || !arr[0].type) return null;
    const d = arr[0]; const m = /(-?\d+)/.exec(String(d.value || '-20'));
    return { tipo: d.type, resta: Math.abs(m ? parseInt(m[1], 10) : 20) };
  }
  function parseAtaque(a) {
    const dmg = String(a.damage || '');
    const m = /(\d+)/.exec(dmg);
    const coste = (a.cost || []).slice();
    return {
      nombre: a.name || '',
      coste: coste,
      costeN: (a.convertedEnergyCost != null) ? a.convertedEnergyCost : coste.length,
      danio: m ? parseInt(m[1], 10) : 0,
      danioRaw: dmg,
      mas: /\+/.test(dmg),       // "20+"
      por: /[×x]/.test(dmg),     // "30×"
      texto: a.text || ''
    };
  }

  // Energía básica (no suele estar en la DB): se sintetiza desde el nombre.
  const E_MAP = [
    ['grass', 'Grass'], ['fire', 'Fire'], ['water', 'Water'], ['lightning', 'Lightning'],
    ['psychic', 'Psychic'], ['fighting', 'Fighting'], ['darkness', 'Darkness'], ['metal', 'Metal'],
    ['fairy', 'Fairy'],
    ['planta', 'Grass'], ['fuego', 'Fire'], ['agua', 'Water'], ['rayo', 'Lightning'],
    ['eléctric', 'Lightning'], ['electric', 'Lightning'], ['psíquic', 'Psychic'], ['psiquic', 'Psychic'],
    ['lucha', 'Fighting'], ['oscur', 'Darkness'], ['metál', 'Metal'], ['metal', 'Metal'], ['acero', 'Metal'],
    ['hada', 'Fairy']
  ];
  function tipoEnergia(name) {
    const n = (name || '').toLowerCase();
    for (let i = 0; i < E_MAP.length; i++) if (n.indexOf(E_MAP[i][0]) >= 0) return E_MAP[i][1];
    return 'Colorless';
  }
  function energiaBasicaView(name) {
    return { nombre: name, supertipo: 'Energy', tipos: [tipoEnergia(name)], esEnergiaBasica: true };
  }

  // ---------- Normalizador: vista de carta -> carta de juego ----------
  function cartaJuego(view) {
    if (!view) return null;
    const poke = esPokemon(view);
    const energy = esEnergia(view);
    const tipos = (view.tipos || []).slice();
    return {
      id: view.id || null,
      nombre: view.nombre || view.name || '',
      supertipo: poke ? 'Pokemon' : (energy ? 'Energy' : (esEntrenador(view) ? 'Trainer' : '?')),
      tipos: tipos,
      hp: poke ? num(view.ps) : 0,
      stage: poke ? stageDe(view) : 0,
      evolucionaDe: view.evolucionaDe || null,
      esBasico: poke && !view.evolucionaDe,
      ataques: poke ? (view.ataques || []).map(parseAtaque) : [],
      habilidades: poke ? (view.habilidades || []).map(function (h) { return { nombre: h.name, texto: h.text }; }) : [],
      debilidad: poke ? parseDebilidad(view.debilidades) : null,
      resistencia: poke ? parseResistencia(view.resistencias) : null,
      retirada: poke ? ((view.retiradaConvertida != null) ? view.retiradaConvertida : (view.costoRetirada || []).length) : 0,
      premiosKO: poke ? premiosDe(view) : 0,
      // energías
      esEnergiaBasica: !!view.esEnergiaBasica,
      energiaTipo: energy ? (tipos[0] || 'Colorless') : null,
      imagen: view.imagenChica || view.imagenGrande || null,
      es: view.es || null,
      raw: view
    };
  }

  // ---------- Expansión de mazo -> lista runtime de cartas ----------
  // resolver(ref) -> vista de carta (en navegador: cardRegistry/getCardData).
  function expandirMazo(deck, resolver) {
    const out = [];
    resolver = resolver || function () { return null; };
    function add(view, n) { for (let i = 0; i < (n || 1); i++) out.push(cartaJuego(view)); }
    (deck.pokemon || []).concat(deck.trainers || []).forEach(function (c) {
      const v = resolver(c); if (v) add(v, c.qty);
    });
    (deck.energies || []).forEach(function (c) {
      const v = resolver(c) || energiaBasicaView(c.card); add(v, c.qty);
    });
    return out;
  }

  // Valida que el mazo sea jugable: 60 cartas y Pokémon con HP/ataques.
  function validarMazoJugable(deck, resolver) {
    const cartas = expandirMazo(deck, resolver);
    const total = cartas.length;
    const faltan = [];
    cartas.forEach(function (c) {
      if (c.supertipo === 'Pokemon' && (!c.hp || !c.ataques.length)) faltan.push(c.nombre || c.id);
    });
    const basicos = cartas.filter(function (c) { return c.supertipo === 'Pokemon' && c.esBasico; }).length;
    return {
      ok: total === 60 && basicos > 0 && faltan.length === 0,
      total: total,
      basicos: basicos,
      faltan: faltan
    };
  }

  // ---------- Estado ----------
  function estadoInicial() {
    return {
      v: VERSION,
      fase: FASE.SETUP,
      turno: 0,
      turnoDe: null,
      jugadores: {},
      seq: 0,
      log: []
    };
  }

  // Reductor puro (se irá implementando desde la Fase 3).
  function aplicarAccion(estado, accion) { return estado; }

  const API = {
    VERSION, FASE,
    cartaJuego, expandirMazo, validarMazoJugable,
    energiaBasicaView, tipoEnergia,
    estadoInicial, aplicarAccion
  };

  global.JUEGO = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
