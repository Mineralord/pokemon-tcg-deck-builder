/* =============================================================
   juego.js — Motor del Juego Virtual Pokémon TCG
   Reductor puro y transporte-agnóstico: aplicarAccion(estado, accion) -> estado.
   Sin DOM ni red, para que valga en online / pase y juega / IA y sea testeable
   en Node. Se va llenando por fases (ver plan).
   Fase 1: normalizador de carta (cartaJuego) + expansión de mazo + validación.
   ============================================================= */
(function (global) {
  'use strict';

  const VERSION = 3; // sube con cada fase del motor

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

  // ---------- RNG determinista + baraja ----------
  function rng32(seed) {
    let a = (seed >>> 0) || 1;
    return function () {
      a |= 0; a = (a + 0x6D2B79F5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }
  function barajar(arr, rnd) {
    const a = arr.slice();
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(rnd() * (i + 1));
      const t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }
  function tieneBasico(cartas) {
    return cartas.some(function (c) { return c.supertipo === 'Pokemon' && c.esBasico; });
  }
  function esBasicoPoke(c) { return c && c.supertipo === 'Pokemon' && c.esBasico; }

  // ---------- Preparación de partida (rulebook p.8) ----------
  function _lado(nombre) {
    return {
      nombre: nombre || '?', mazo: [], mano: [], banca: [], activo: null,
      descarte: [], premios: [], lost: [], estadio: null,
      estado: 'setup', mulligans: 0, energiaUsada: false
    };
  }

  // opts: { ladoA:{nombre,cartas}, ladoB:{nombre,cartas}, seed }
  function crearPartida(opts) {
    opts = opts || {};
    const seed = (opts.seed != null) ? opts.seed : (Date.now() >>> 0);
    const rnd = rng32(seed);
    let iid = 0;
    function inst(cartas) {
      return (cartas || []).map(function (c) {
        const o = Object.assign({}, c); o.iid = 'c' + (iid++); o.danio = 0; o.energias = []; o.condiciones = []; return o;
      });
    }
    const A = _lado(opts.ladoA && opts.ladoA.nombre || 'A');
    const B = _lado(opts.ladoB && opts.ladoB.nombre || 'B');
    A.mazo = barajar(inst(opts.ladoA && opts.ladoA.cartas), rnd);
    B.mazo = barajar(inst(opts.ladoB && opts.ladoB.cartas), rnd);
    function reparte(L) { L.mano = L.mazo.splice(0, 7); }
    function mulligan(L) { L.mazo = barajar(L.mazo.concat(L.mano), rnd); L.mano = L.mazo.splice(0, 7); }
    reparte(A); reparte(B);
    let guard = 0;
    while (!tieneBasico(A.mano) && A.mazo.length && guard++ < 50) { mulligan(A); A.mulligans++; }
    guard = 0;
    while (!tieneBasico(B.mano) && B.mazo.length && guard++ < 50) { mulligan(B); B.mulligans++; }
    // Cartas extra: cada lado roba tantas como mulligans hizo el rival (regla opcional, automática).
    if (B.mulligans) A.mano = A.mano.concat(A.mazo.splice(0, B.mulligans));
    if (A.mulligans) B.mano = B.mano.concat(B.mazo.splice(0, A.mulligans));
    const inicia = rnd() < 0.5 ? 'A' : 'B';
    return {
      v: VERSION, fase: FASE.SETUP, seed: seed, turno: 0, turnoDe: null,
      inicia: inicia, primerTurno: true, lados: { A: A, B: B }, seq: 0, log: []
    };
  }

  function _buscarMano(L, iid) { return L.mano.findIndex(function (c) { return c.iid === iid; }); }

  // Coloca un básico de la mano como Activo (durante SETUP).
  function colocarActivo(est, lado, iid) {
    const L = est.lados[lado]; if (!L || L.estado !== 'setup') return est;
    const i = _buscarMano(L, iid); if (i < 0) return est;
    const c = L.mano[i]; if (!esBasicoPoke(c)) return est;
    if (L.activo) L.mano.push(L.activo);
    L.activo = c; L.mano.splice(i, 1);
    return est;
  }
  // Añade un básico de la mano a la banca (máx 5) durante SETUP.
  function colocarBanca(est, lado, iid) {
    const L = est.lados[lado]; if (!L || L.estado !== 'setup') return est;
    if (L.banca.length >= 5) return est;
    const i = _buscarMano(L, iid); if (i < 0) return est;
    const c = L.mano[i]; if (!esBasicoPoke(c)) return est;
    L.banca.push(c); L.mano.splice(i, 1);
    return est;
  }
  // Devuelve a la mano un Pokémon colocado (activo o de banca) durante SETUP.
  function quitarColocado(est, lado, iid) {
    const L = est.lados[lado]; if (!L || L.estado !== 'setup') return est;
    if (L.activo && L.activo.iid === iid) { L.mano.push(L.activo); L.activo = null; return est; }
    const i = L.banca.findIndex(function (c) { return c.iid === iid; });
    if (i >= 0) { L.mano.push(L.banca[i]); L.banca.splice(i, 1); }
    return est;
  }
  // Confirma la colocación de un lado: requiere Activo; coloca 6 premios; si ambos listos, arranca.
  function confirmarSetup(est, lado) {
    const L = est.lados[lado]; if (!L || L.estado !== 'setup' || !L.activo) return est;
    if (!L.premios.length) L.premios = L.mazo.splice(0, 6);
    L.estado = 'ready';
    const A = est.lados.A, B = est.lados.B;
    if (A.estado === 'ready' && B.estado === 'ready') {
      est.fase = FASE.DRAW; est.turno = 1; est.turnoDe = est.inicia;
    }
    return est;
  }

  // Coloca automáticamente un lado (rival de práctica): 1 activo + banca con básicos.
  function autoSetup(est, lado) {
    const L = est.lados[lado]; if (!L) return est;
    const basicos = L.mano.filter(esBasicoPoke);
    if (basicos[0]) colocarActivo(est, lado, basicos[0].iid);
    for (let i = 1; i < basicos.length && L.banca.length < 5; i++) colocarBanca(est, lado, basicos[i].iid);
    return confirmarSetup(est, lado);
  }

  function totalCartasLado(L) {
    return L.mazo.length + L.mano.length + L.banca.length + (L.activo ? 1 : 0) +
      L.descarte.length + L.premios.length + L.lost.length;
  }

  // ---------- Estado / reductor ----------
  function estadoInicial() {
    return { v: VERSION, fase: FASE.SETUP, turno: 0, turnoDe: null, lados: {}, seq: 0, log: [] };
  }
  function aplicarAccion(estado, accion) { return estado; } // se implementa desde la Fase 4

  const API = {
    VERSION, FASE,
    cartaJuego, expandirMazo, validarMazoJugable,
    energiaBasicaView, tipoEnergia,
    rng32, barajar, tieneBasico,
    crearPartida, colocarActivo, colocarBanca, quitarColocado, confirmarSetup, autoSetup, totalCartasLado,
    estadoInicial, aplicarAccion
  };

  global.JUEGO = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
