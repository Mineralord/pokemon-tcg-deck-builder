/* =============================================================
   juego.js — Motor del Juego Virtual Pokémon TCG
   Reductor puro y transporte-agnóstico: aplicarAccion(estado, accion) -> estado.
   Sin DOM ni red, para que valga en online / pase y juega / IA y sea testeable
   en Node. Se va llenando por fases (ver plan).
   Fase 1: normalizador de carta (cartaJuego) + expansión de mazo + validación.
   ============================================================= */
(function (global) {
  'use strict';

  const VERSION = 8; // sube con cada fase del motor

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
  // Subtipo de Entrenador a partir del texto de reglas.
  function subEntrenador(reglas) {
    const r = (reglas || []).join(' ').toLowerCase();
    if (r.indexOf('supporter') >= 0) return 'supporter';
    if (r.indexOf('stadium') >= 0) return 'stadium';
    if (r.indexOf('pokémon tool') >= 0 || r.indexOf('pokemon tool') >= 0) return 'tool';
    return 'item';
  }

  // ---------- Normalizador: vista de carta -> carta de juego ----------
  function cartaJuego(view) {
    if (!view) return null;
    const poke = esPokemon(view);
    const energy = esEnergia(view);
    const trainer = esEntrenador(view);
    const tipos = (view.tipos || []).slice();
    return {
      texto: trainer ? ((view.reglas || [])[0] || '') : '',
      subTrainer: trainer ? subEntrenador(view.reglas) : null,
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
      estado: 'setup', mulligans: 0, energiaUsada: false,
      retiroUsado: false, supporterUsado: false, estadioUsado: false, turnosJugados: 0
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
      inicia: inicia, ganador: null, motivoFin: null, lados: { A: A, B: B }, seq: 0, log: []
    };
  }

  function _buscarMano(L, iid) { return L.mano.findIndex(function (c) { return c.iid === iid; }); }

  // Coloca un básico de la mano como Activo (durante SETUP).
  function colocarActivo(est, lado, iid) {
    const L = est.lados[lado]; if (!L || L.estado !== 'setup') return est;
    const i = _buscarMano(L, iid); if (i < 0) return est;
    const c = L.mano[i]; if (!esBasicoPoke(c)) return est;
    if (L.activo) L.mano.push(L.activo);
    c.enJuegoDesde = 0; L.activo = c; L.mano.splice(i, 1);
    return est;
  }
  // Añade un básico de la mano a la banca (máx 5) durante SETUP.
  function colocarBanca(est, lado, iid) {
    const L = est.lados[lado]; if (!L || L.estado !== 'setup') return est;
    if (L.banca.length >= 5) return est;
    const i = _buscarMano(L, iid); if (i < 0) return est;
    const c = L.mano[i]; if (!esBasicoPoke(c)) return est;
    c.enJuegoDesde = 0; L.banca.push(c); L.mano.splice(i, 1);
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
      est.turno = 1; est.turnoDe = est.inicia;
      _comenzarTurno(est);
    }
    return est;
  }

  // ---------- Estructura de turno (rulebook p.9-14) ----------
  // Inicia el turno del jugador `turnoDe`: roba 1 carta (derrota si no puede) y pasa a MAIN.
  function _comenzarTurno(est) {
    const lado = est.turnoDe; const L = est.lados[lado];
    if (!L.mazo.length) { est.ganador = (lado === 'A' ? 'B' : 'A'); est.fase = FASE.END; est.motivoFin = 'deckout'; return est; }
    L.mano.push(L.mazo.shift());           // robar del top
    L.energiaUsada = false; L.retiroUsado = false; L.supporterUsado = false; L.estadioUsado = false;
    est.fase = FASE.MAIN;
    return est;
  }
  // ¿Puede atacar ahora? El que empieza no ataca en su primer turno; Dormido/Paralizado lo impiden.
  function puedeAtacar(est) {
    if (est.ganador || est.fase !== FASE.MAIN) return false;
    const L = est.lados[est.turnoDe];
    if (est.turnoDe === est.inicia && L.turnosJugados === 0) return false;
    const c = (L.activo && L.activo.condiciones) || [];
    if (c.indexOf('asleep') >= 0 || c.indexOf('paralyzed') >= 0) return false;
    return true;
  }

  // ---------- Condiciones especiales (rulebook p.15-16) ----------
  const ROTATIVAS = ['asleep', 'confused', 'paralyzed']; // mutuamente excluyentes (gira la carta)
  function _tiene(card, c) { return card && (card.condiciones || []).indexOf(c) >= 0; }
  function _quitar(card, c) { if (card && card.condiciones) card.condiciones = card.condiciones.filter(function (x) { return x !== c; }); }
  // Moneda determinista por estado (serializable para online): true = cara.
  function _flip(est) {
    est.rngN = (est.rngN || 0) + 1;
    const f = rng32(((est.seed || 1) ^ Math.imul(est.rngN, 2654435761)) >>> 0);
    return f() < 0.5;
  }
  // Aplica una condición al Activo de `lado` (las rotativas se reemplazan entre sí).
  function aplicarCondicion(est, lado, cond) {
    const a = est.lados[lado] && est.lados[lado].activo; if (!a) return est;
    a.condiciones = a.condiciones || [];
    if (ROTATIVAS.indexOf(cond) >= 0) {
      a.condiciones = a.condiciones.filter(function (x) { return ROTATIVAS.indexOf(x) < 0; });
      a.condiciones.push(cond);
    } else if (a.condiciones.indexOf(cond) < 0) a.condiciones.push(cond);
    return est;
  }
  function _koSiProcede(est, lado) {
    const a = est.lados[lado] && est.lados[lado].activo;
    if (a && (a.danio || 0) >= a.hp) _noquear(est, lado, a);
  }
  // Chequeo Pokémon entre turnos: orden Veneno -> Quemado -> Dormido -> Paralizado.
  function chequeo(est, ladoQueTermina, flip) {
    const coin = flip || function () { return _flip(est); };
    ['A', 'B'].forEach(function (lado) {
      const a = est.lados[lado].activo; if (!a) return;
      if (_tiene(a, 'poisoned')) a.danio = (a.danio || 0) + 10;
      if (_tiene(a, 'burned')) { a.danio = (a.danio || 0) + 20; if (coin()) _quitar(a, 'burned'); }
      if (_tiene(a, 'asleep')) { if (coin()) _quitar(a, 'asleep'); }
    });
    // Paralizado: se recupera tras el turno de su dueño.
    const ap = est.lados[ladoQueTermina].activo; if (ap) _quitar(ap, 'paralyzed');
    _koSiProcede(est, 'A'); _koSiProcede(est, 'B');
    return est;
  }

  // Termina el turno: chequeo Pokémon y paso al rival.
  function terminarTurno(est, flip) {
    if (est.ganador) return est;
    const lado = est.turnoDe; est.lados[lado].turnosJugados++;
    est.fase = FASE.CHECKUP;
    chequeo(est, lado, flip);
    if (est.ganador) return est;
    est.turnoDe = (lado === 'A' ? 'B' : 'A'); est.turno++;
    return _comenzarTurno(est);
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

  // ---------- Acciones principales del turno (rulebook p.10-12) ----------
  function _enMano(L, iid) { const i = _buscarMano(L, iid); return i < 0 ? null : L.mano[i]; }
  function _refEnJuego(L, iid) {
    if (L.activo && L.activo.iid === iid) return { card: L.activo, pos: 'activo' };
    const i = L.banca.findIndex(function (c) { return c.iid === iid; });
    if (i >= 0) return { card: L.banca[i], pos: 'banca', i: i };
    return null;
  }
  function _esTurnoMain(est, lado) { return !est.ganador && est.fase === FASE.MAIN && est.turnoDe === lado; }

  // Poner un Básico de la mano en la Banca (las veces que quieras).
  function ponerEnBanca(est, lado, iid) {
    const L = est.lados[lado]; if (!_esTurnoMain(est, lado) || L.banca.length >= 5) return est;
    const c = _enMano(L, iid); if (!esBasicoPoke(c)) return est;
    c.enJuegoDesde = est.turno; L.banca.push(c); L.mano.splice(_buscarMano(L, iid), 1); return est;
  }

  // Adjuntar 1 energía por turno a un Pokémon en juego.
  function adjuntarEnergia(est, lado, iidMano, iidObjetivo) {
    const L = est.lados[lado]; if (!_esTurnoMain(est, lado) || L.energiaUsada) return est;
    const e = _enMano(L, iidMano); if (!e || e.supertipo !== 'Energy') return est;
    const r = _refEnJuego(L, iidObjetivo); if (!r) return est;
    r.card.energias.push(e); L.mano.splice(_buscarMano(L, iidMano), 1); L.energiaUsada = true; return est;
  }

  // Evolucionar: carta de la mano sobre un Pokémon cuyo nombre = evolucionaDe.
  function evolucionar(est, lado, iidMano, iidObjetivo) {
    const L = est.lados[lado]; if (!_esTurnoMain(est, lado)) return est;
    if (L.turnosJugados === 0) return est;                  // no en tu primer turno
    const evo = _enMano(L, iidMano); if (!evo || evo.supertipo !== 'Pokemon' || !evo.evolucionaDe) return est;
    const r = _refEnJuego(L, iidObjetivo); if (!r) return est;
    const base = r.card;
    if ((base.nombre || '') !== evo.evolucionaDe) return est;          // debe coincidir
    if (est.turno <= (base.enJuegoDesde || 0)) return est;            // en juego desde antes de este turno
    evo.energias = base.energias || []; evo.danio = base.danio || 0;
    evo.debajo = [base].concat(base.debajo || []);
    evo.condiciones = [];                                            // se limpian al evolucionar
    evo.enJuegoDesde = est.turno;
    if (r.pos === 'activo') L.activo = evo; else L.banca[r.i] = evo;
    L.mano.splice(_buscarMano(L, iidMano), 1); return est;
  }

  // Retirar el Activo (1 vez por turno): descarta energías del coste y cambia por un banco.
  function retirar(est, lado, iidBanca) {
    const L = est.lados[lado]; if (!_esTurnoMain(est, lado) || L.retiroUsado) return est;
    if (!L.activo || !L.banca.length) return est;
    const cond = L.activo.condiciones || [];
    if (cond.indexOf('asleep') >= 0 || cond.indexOf('paralyzed') >= 0) return est; // Fase 7
    const coste = L.activo.retirada || 0;
    if ((L.activo.energias || []).length < coste) return est;
    for (let k = 0; k < coste; k++) L.descarte.push(L.activo.energias.pop());
    const i = L.banca.findIndex(function (c) { return c.iid === iidBanca; });
    if (i < 0) return est;
    const nuevo = L.banca[i]; L.banca.splice(i, 1);
    const viejo = L.activo; viejo.condiciones = [];                 // limpia condiciones al ir a banca
    L.banca.push(viejo); L.activo = nuevo; L.retiroUsado = true; return est;
  }

  // Jugar un Entrenador (Item/Supporter/Estadio/Herramienta) con sus límites por turno.
  function jugarEntrenador(est, lado, iid) {
    const L = est.lados[lado]; if (!_esTurnoMain(est, lado)) return est;
    const i = _buscarMano(L, iid); if (i < 0) return est;
    const c = L.mano[i]; if (c.supertipo !== 'Trainer') return est;
    const sub = c.subTrainer || 'item';
    if (sub === 'supporter') {
      if (L.supporterUsado) return est;
      if (lado === est.inicia && L.turnosJugados === 0) return est; // el que empieza no juega Supporter en su 1er turno
    }
    if (sub === 'stadium' && L.estadioUsado) return est;
    // Efecto: handler codificado o auto-intérprete (de momento: robar N).
    const EF = global.JUEGO_EFECTOS;
    const reg = EF && EF.EFECTOS && EF.EFECTOS[c.id];
    if (reg && reg.jugar) { try { reg.jugar(est, lado, { carta: c }); } catch (e) {} }
    else {
      const md = /draw (\d+) cards?/i.exec(c.texto || '');
      if (md) for (let k = 0; k < parseInt(md[1], 10) && L.mazo.length; k++) L.mano.push(L.mazo.shift());
    }
    L.mano.splice(_buscarMano(L, iid), 1);
    if (sub === 'stadium') { if (L.estadio) L.descarte.push(L.estadio); L.estadio = c; L.estadioUsado = true; }
    else if (sub === 'tool') { if (L.activo) { L.activo.tools = L.activo.tools || []; L.activo.tools.push(c); } else L.descarte.push(c); }
    else { L.descarte.push(c); if (sub === 'supporter') L.supporterUsado = true; }
    return est;
  }

  // Usar una habilidad de un Pokémon propio en juego (handler codificado; si no, manual).
  function usarHabilidad(est, lado, iid, idx) {
    if (!_esTurnoMain(est, lado)) return est;
    const L = est.lados[lado]; const r = _refEnJuego(L, iid); if (!r) return est;
    const h = (r.card.habilidades || [])[idx]; if (!h) return est;
    const EF = global.JUEGO_EFECTOS;
    const reg = EF && EF.EFECTOS && EF.EFECTOS[r.card.id];
    const fn = reg && reg.habilidades && reg.habilidades[h.nombre];
    if (fn) { try { fn(est, lado, { card: r.card }); } catch (e) {} }
    return est;
  }

  // ---------- Ataque (rulebook p.13-14) ----------
  // ¿La carta tiene energía suficiente para pagar el coste? (tipado + incoloro)
  function puedePagar(card, coste) {
    const pool = (card.energias || []).map(function (e) { return e.energiaTipo || 'Colorless'; });
    const tipados = (coste || []).filter(function (t) { return t !== 'Colorless'; });
    for (let i = 0; i < tipados.length; i++) {
      const j = pool.indexOf(tipados[i]);
      if (j < 0) return false;
      pool.splice(j, 1);
    }
    const incoloro = (coste || []).filter(function (t) { return t === 'Colorless'; }).length;
    return pool.length >= incoloro;
  }

  function _tomarPremios(est, lado, n) {
    const L = est.lados[lado];
    for (let k = 0; k < n && L.premios.length; k++) L.mano.push(L.premios.pop());
    if (L.premios.length === 0) { est.ganador = lado; est.fase = FASE.END; est.motivoFin = 'premios'; }
    return est;
  }

  // Noquea una carta de `ladoDef`: descarta (con energías y cartas debajo), el rival toma premios,
  // y se repone el Activo desde la banca (auto). Sin banca -> el atacante gana.
  function _noquear(est, ladoDef, card) {
    const D = est.lados[ladoDef]; const ladoAtk = ladoDef === 'A' ? 'B' : 'A';
    D.descarte.push(card);
    (card.energias || []).forEach(function (e) { D.descarte.push(e); }); card.energias = [];
    (card.debajo || []).forEach(function (c) { D.descarte.push(c); }); card.debajo = [];
    if (D.activo === card) D.activo = null;
    else { const i = D.banca.indexOf(card); if (i >= 0) D.banca.splice(i, 1); }
    _tomarPremios(est, ladoAtk, card.premiosKO || 1);
    if (est.ganador) return est;
    if (!D.activo) {
      if (D.banca.length) D.activo = D.banca.shift();
      else { est.ganador = ladoAtk; est.fase = FASE.END; est.motivoFin = 'sinpokemon'; }
    }
    return est;
  }

  // Daño efectivo de un ataque sobre el defensor activo (debilidad/resistencia).
  function danioEfectivo(atacante, defensor, ataque) {
    let dmg = ataque.danio || 0;
    if (dmg <= 0) return 0;
    const t = (atacante.tipos || [])[0];
    if (t && defensor.debilidad && defensor.debilidad.tipo === t) {
      if (defensor.debilidad.mult) dmg *= defensor.debilidad.mult;
      else if (defensor.debilidad.suma) dmg += defensor.debilidad.suma;
    }
    if (t && defensor.resistencia && defensor.resistencia.tipo === t) {
      dmg = Math.max(0, dmg - (defensor.resistencia.resta || 0));
    }
    return dmg;
  }

  // Atacar con el ataque idx del Activo contra el Activo rival. Termina el turno.
  function atacar(est, lado, idxAtaque, flip) {
    if (!puedeAtacar(est) || est.turnoDe !== lado) return est;
    const L = est.lados[lado]; const op = lado === 'A' ? 'B' : 'A'; const O = est.lados[op];
    const at = L.activo, def = O.activo;
    if (!at || !def) return est;
    const ataque = (at.ataques || [])[idxAtaque]; if (!ataque) return est;
    if (!puedePagar(at, ataque.coste)) return est;
    // Confusión: moneda; si sale cruz, el ataque falla y se autoinflige 30.
    if (_tiene(at, 'confused')) {
      const cara = flip ? flip() : _flip(est);
      if (!cara) {
        at.danio = (at.danio || 0) + 30;
        est.ultimoAtaque = { lado: lado, nombre: ataque.nombre, dmg: 0, confuso: true };
        if ((at.danio || 0) >= at.hp) _noquear(est, lado, at);
        if (!est.ganador) terminarTurno(est);
        return est;
      }
    }
    const dmg = danioEfectivo(at, def, ataque);
    if (dmg > 0) def.danio = (def.danio || 0) + dmg;
    // Efectos: 1) handler codificado por carta (precedencia) 2) auto-intérprete de texto.
    let efectos = [];
    const EF = global.JUEGO_EFECTOS;
    const coded = EF && EF.efectoDe && EF.efectoDe(at.id, 'ataques', ataque.nombre);
    if (coded) { try { coded(est, lado, { at: at, def: def, ataque: ataque }); efectos = ['coded']; } catch (e) {} }
    else efectos = efectoAuto(est, lado, ataque);
    est.ultimoAtaque = { lado: lado, nombre: ataque.nombre, dmg: dmg, efectos: efectos };
    _koSiProcede(est, op); _koSiProcede(est, lado);
    if (!est.ganador) terminarTurno(est);
    return est;
  }

  // Auto-intérprete: aplica los efectos de texto más comunes de forma determinista.
  // coinFn opcional (para tests); por defecto usa la moneda serializable del estado.
  function efectoAuto(est, lado, ataque, coinFn) {
    const op = lado === 'A' ? 'B' : 'A';
    const me = est.lados[lado], rival = est.lados[op];
    const at = me.activo, def = rival.activo;
    const texto = ataque.texto || ''; const t = texto.toLowerCase();
    const coin = coinFn || function () { return _flip(est); };
    const out = []; let m;
    const addDef = function (n) { if (def && n > 0) { def.danio = (def.danio || 0) + n; } };

    // Condiciones incondicionales al Activo rival (salvo si dependen de moneda, que se trata aparte).
    const coinCond = /flip a coin\. if heads,[^.]*is now /i.test(texto);
    [['asleep', /now asleep|is asleep/], ['paralyzed', /now paralyzed|is paralyzed/],
     ['poisoned', /now poisoned|is poisoned/], ['burned', /now burned|is burned/], ['confused', /now confused|is confused/]
    ].forEach(function (p) { if (p[1].test(t) && !coinCond) { if (def) { aplicarCondicion(est, op, p[0]); out.push(p[0]); } } });

    if ((m = /flip a coin\. if heads, this attack does (\d+) more damage/i.exec(texto))) { if (coin()) { addDef(parseInt(m[1], 10)); out.push('coin+dmg'); } }
    if ((m = /flip a coin\. if heads,[^.]*is now (asleep|paralyzed|poisoned|burned|confused)/i.exec(texto))) { if (coin() && def) { aplicarCondicion(est, op, m[1].toLowerCase()); out.push('coin-cond'); } }
    if (/flip a coin\. if heads, discard an energy from your opponent's active/i.test(texto)) { if (coin() && def && def.energias.length) { rival.descarte.push(def.energias.pop()); out.push('coin-disc'); } }
    if ((m = /flip a coin until you get tails\. this attack does (\d+) damage for each heads/i.exec(texto))) { let h = 0; while (coin()) h++; addDef(h * parseInt(m[1], 10)); out.push('streak'); }
    if ((m = /flip (\d+) coins?\. this attack does (\d+) damage for each heads/i.exec(texto))) { let h = 0, n = parseInt(m[1], 10); for (let k = 0; k < n; k++) if (coin()) h++; addDef(h * parseInt(m[2], 10)); out.push('coins'); }
    if ((m = /does (\d+) more damage for each damage counter on this/i.exec(texto))) { if (at) addDef(parseInt(m[1], 10) * Math.floor((at.danio || 0) / 10)); out.push('perCounter'); }
    if ((m = /does (\d+) more damage for each of your opponent's benched/i.exec(texto))) { addDef(parseInt(m[1], 10) * rival.banca.length); out.push('perBench'); }
    if ((m = /does (\d+) more damage for each energy attached to your opponent's active/i.exec(texto))) { if (def) addDef(parseInt(m[1], 10) * (def.energias || []).length); out.push('perEnergy'); }
    if ((m = /also does (\d+) damage to itself/i.exec(texto))) { if (at) { at.danio = (at.danio || 0) + parseInt(m[1], 10); out.push('recoil'); } }
    if ((m = /discard (a|an|\d+) energy from this pokémon/i.exec(texto))) { const n = /^\d+$/.test(m[1]) ? parseInt(m[1], 10) : 1; for (let k = 0; k < n && at && at.energias.length; k++) me.descarte.push(at.energias.pop()); out.push('selfDiscard'); }
    if ((m = /heal (\d+) damage from this/i.exec(texto))) { if (at) { at.danio = Math.max(0, (at.danio || 0) - parseInt(m[1], 10)); out.push('heal'); } }
    if (/recovers from all special conditions/i.test(texto) && at) { at.condiciones = []; out.push('clearcond'); }
    if ((m = /draw (\d+) cards?/i.exec(texto))) { for (let k = 0; k < parseInt(m[1], 10) && me.mazo.length; k++) me.mano.push(me.mazo.shift()); out.push('draw'); }
    else if (/draw a card/i.test(texto)) { if (me.mazo.length) me.mano.push(me.mazo.shift()); out.push('draw'); }
    return out;
  }

  // ---------- Respaldo manual asistido (efectos no automatizados) ----------
  function manualDanioRival(est, lado, n) {
    const op = lado === 'A' ? 'B' : 'A'; const a = est.lados[op].activo;
    if (!_esTurnoMain(est, lado) || !a) return est;
    a.danio = (a.danio || 0) + n; _koSiProcede(est, op); return est;
  }
  function manualCurar(est, lado, n) {
    const a = est.lados[lado].activo; if (!_esTurnoMain(est, lado) || !a) return est;
    a.danio = Math.max(0, (a.danio || 0) - n); return est;
  }
  function manualRobar(est, lado, n) {
    const L = est.lados[lado]; if (!_esTurnoMain(est, lado)) return est;
    for (let k = 0; k < n && L.mazo.length; k++) L.mano.push(L.mazo.shift());
    if (!L.mazo.length) {} return est;
  }
  function manualCondicionRival(est, lado, cond) {
    const op = lado === 'A' ? 'B' : 'A'; if (!_esTurnoMain(est, lado)) return est;
    return aplicarCondicion(est, op, cond);
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
    terminarTurno, puedeAtacar,
    ponerEnBanca, adjuntarEnergia, evolucionar, retirar, jugarEntrenador, subEntrenador, usarHabilidad,
    puedePagar, danioEfectivo, atacar, efectoAuto,
    aplicarCondicion, chequeo, ROTATIVAS,
    manualDanioRival, manualCurar, manualRobar, manualCondicionRival,
    estadoInicial, aplicarAccion
  };

  global.JUEGO = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
