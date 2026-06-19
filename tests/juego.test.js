/* Tests del motor del juego (sin DOM). Ejecutar: node tests/juego.test.js */
'use strict';
const J = require('../js/juego.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

// --- Vistas de ejemplo (forma de data/cartas-db.js) ---
const pikachuEx = {
  id: 'svp-106', nombre: 'Pikachu ex', supertipo: 'Pokémon', fase: 'Basic, ex', ps: '200',
  tipos: ['Lightning'], evolucionaDe: null,
  ataques: [{ name: 'Thunderbolt', cost: ['Lightning', 'Lightning', 'Colorless'], convertedEnergyCost: 3, damage: '120', text: '' }],
  habilidades: [],
  debilidades: [{ type: 'Fighting', value: '×2' }],
  resistencias: [],
  costoRetirada: ['Colorless', 'Colorless'],
  reglas: ['Pokémon ex rule: When your Pokémon ex is Knocked Out, your opponent takes 2 Prize cards.'],
  imagenChica: 'x'
};
const raichu = {
  id: 'sv1-100', nombre: 'Raichu', supertipo: 'Pokémon', fase: 'Stage 1', ps: '120',
  tipos: ['Lightning'], evolucionaDe: 'Pikachu',
  ataques: [{ name: 'Zap', cost: ['Lightning'], damage: '30+', text: 'more' }],
  debilidades: [{ type: 'Fighting', value: '×2' }],
  resistencias: [{ type: 'Metal', value: '-30' }],
  costoRetirada: ['Colorless']
};
const lightningEnergy = { id: 'sve-4', nombre: 'Lightning Energy', supertipo: 'Energy', tipos: ['Lightning'] };

console.log('cartaJuego — Pokémon ex');
let p = J.cartaJuego(pikachuEx);
eq(p.supertipo, 'Pokemon', 'supertipo Pokemon');
eq(p.hp, 200, 'hp 200');
eq(p.esBasico, true, 'es básico');
eq(p.stage, 0, 'stage 0');
eq(p.ataques[0].costeN, 3, 'coste 3');
eq(p.ataques[0].danio, 120, 'daño 120');
eq(p.debilidad.tipo, 'Fighting', 'debilidad tipo');
eq(p.debilidad.mult, 2, 'debilidad ×2');
eq(p.retirada, 2, 'retirada 2');
eq(p.premiosKO, 2, 'premios 2 (ex desde reglas)');

console.log('cartaJuego — Stage 1 + resistencia + daño "30+"');
let r = J.cartaJuego(raichu);
eq(r.stage, 1, 'stage 1');
eq(r.esBasico, false, 'no básico (evoluciona)');
eq(r.evolucionaDe, 'Pikachu', 'evoluciona de Pikachu');
eq(r.resistencia.resta, 30, 'resistencia -30');
eq(r.ataques[0].danio, 30, 'daño base 30');
eq(r.ataques[0].mas, true, 'flag "+" en 30+');
eq(r.premiosKO, 1, 'premios 1 (normal)');

console.log('cartaJuego — Energía');
let e = J.cartaJuego(lightningEnergy);
eq(e.supertipo, 'Energy', 'energía');
eq(e.energiaTipo, 'Lightning', 'tipo de energía');

console.log('energía básica sintetizada');
eq(J.tipoEnergia('Fire Energy'), 'Fire', 'Fire');
eq(J.tipoEnergia('Energía Rayo'), 'Lightning', 'Rayo->Lightning');
eq(J.cartaJuego(J.energiaBasicaView('Water Energy')).energiaTipo, 'Water', 'Water básica');

console.log('expandirMazo + validarMazoJugable (60 cartas)');
const deck = {
  pokemon: [{ card: 'Pikachu ex', id: 'svp-106', qty: 4 }, { card: 'Raichu', id: 'sv1-100', qty: 4 }],
  trainers: [{ card: 'X', id: 't1', qty: 10 }],
  energies: [{ card: 'Lightning Energy', qty: 42 }]
};
const byId = { 'svp-106': pikachuEx, 'sv1-100': raichu, 't1': { id: 't1', nombre: 'X', supertipo: 'Trainer' } };
const resolver = function (c) { return byId[c.id] || null; };
const cartas = J.expandirMazo(deck, resolver);
eq(cartas.length, 60, 'expande a 60 cartas');
const val = J.validarMazoJugable(deck, resolver);
eq(val.total, 60, 'validación total 60');
ok(val.basicos >= 4, 'tiene básicos (' + val.basicos + ')');
eq(val.faltan.length, 0, 'sin Pokémon faltos de datos');
eq(val.ok, true, 'mazo jugable');

// ---------- Fase 3: preparación de partida ----------
function mazo60(resolver) {
  // 12 Pokémon básicos + resto entrenadores/energía hasta 60 (garantiza básicos en mano).
  return {
    pokemon: [{ card: 'Pikachu ex', id: 'svp-106', qty: 12 }],
    trainers: [{ card: 'X', id: 't1', qty: 18 }],
    energies: [{ card: 'Lightning Energy', qty: 30 }]
  };
}
const cartas60 = J.expandirMazo(mazo60(), resolver);
eq(cartas60.length, 60, 'mazo de prueba = 60');

console.log('crearPartida — reparto y determinismo');
const p1 = J.crearPartida({ ladoA: { nombre: 'Tú', cartas: cartas60 }, ladoB: { nombre: 'Rival', cartas: cartas60 }, seed: 42 });
const p2 = J.crearPartida({ ladoA: { nombre: 'Tú', cartas: cartas60 }, ladoB: { nombre: 'Rival', cartas: cartas60 }, seed: 42 });
eq(p1.lados.A.mano.length >= 7, true, 'mano inicial >= 7');
eq(p1.lados.A.mano[0].iid, p2.lados.A.mano[0].iid, 'misma semilla -> mismo reparto (determinista)');
eq(J.tieneBasico(p1.lados.A.mano), true, 'mano A tiene básico');
eq(J.totalCartasLado(p1.lados.A), 60, 'conservación: 60 cartas lado A');

console.log('colocar Activo / Banca + confirmar setup');
const bA = p1.lados.A.mano.filter(function (c) { return c.supertipo === 'Pokemon' && c.esBasico; });
J.colocarActivo(p1, 'A', bA[0].iid);
eq(p1.lados.A.activo && p1.lados.A.activo.iid, bA[0].iid, 'activo colocado');
if (bA[1]) J.colocarBanca(p1, 'A', bA[1].iid);
const manoAntes = p1.lados.A.mano.length;
J.quitarColocado(p1, 'A', bA[0].iid);
eq(p1.lados.A.activo, null, 'quitar activo lo devuelve a la mano');
eq(p1.lados.A.mano.length, manoAntes + 1, 'la mano recupera la carta');
J.colocarActivo(p1, 'A', bA[0].iid);
J.confirmarSetup(p1, 'A');
eq(p1.lados.A.premios.length, 6, 'A coloca 6 premios al confirmar');
eq(p1.lados.A.estado, 'ready', 'A queda listo');
eq(J.totalCartasLado(p1.lados.A), 60, 'conservación tras premios');

console.log('autoSetup del rival arranca la partida');
J.autoSetup(p1, 'B');
eq(p1.lados.B.estado, 'ready', 'B listo (auto)');
eq(p1.lados.B.premios.length, 6, 'B con 6 premios');
eq(p1.fase, J.FASE.MAIN, 'ambos listos -> arranca en MAIN (ya robó)');
ok(p1.turnoDe === 'A' || p1.turnoDe === 'B', 'turnoDe definido tras setup');
eq(p1.turno, 1, 'turno 1');

// ---------- Fase 4: estructura de turno ----------
console.log('estructura de turno');
const ini = p1.inicia, otro = ini === 'A' ? 'B' : 'A';
eq(p1.turnoDe, ini, 'empieza el ganador de la moneda');
eq(J.puedeAtacar(p1), false, 'el que empieza NO ataca en su primer turno');
const manoIniAntes = p1.lados[otro].mano.length;
J.terminarTurno(p1);
eq(p1.turnoDe, otro, 'terminarTurno pasa al rival');
eq(p1.turno, 2, 'turno 2');
eq(p1.fase, J.FASE.MAIN, 'el rival arranca en MAIN');
eq(p1.lados[otro].mano.length, manoIniAntes + 1, 'el rival robó 1 al empezar su turno');
eq(J.puedeAtacar(p1), true, 'el segundo jugador SÍ puede atacar en su turno 1');
J.terminarTurno(p1);
eq(p1.turnoDe, ini, 'vuelve al inicial');
eq(J.puedeAtacar(p1), true, 'el inicial ya puede atacar en su 2º turno');

console.log('derrota por mazo vacío (deck-out)');
const p3 = J.crearPartida({ ladoA: { nombre: 'Tú', cartas: cartas60 }, ladoB: { nombre: 'Rival', cartas: cartas60 }, seed: 7 });
J.autoSetup(p3, 'A'); J.autoSetup(p3, 'B');
const enTurno = p3.turnoDe, rival = enTurno === 'A' ? 'B' : 'A';
p3.lados[rival].mazo = []; // el rival se quedará sin cartas al empezar su turno
J.terminarTurno(p3);
eq(p3.ganador, enTurno, 'gana quien forzó el deck-out del rival');
eq(p3.fase, J.FASE.END, 'fase END al terminar');

// ---------- Fase 5: acciones principales ----------
console.log('acciones del turno: banca, energía, evolución, retirada');
const pikachu = { id: 'x-1', nombre: 'Pikachu', supertipo: 'Pokémon', fase: 'Basic', ps: '60', tipos: ['Lightning'], ataques: [{ name: 'a', cost: ['Lightning'], damage: '10' }], costoRetirada: ['Colorless'] };
let _id = 0;
function inst(view, iid) { const c = J.cartaJuego(view); c.iid = iid || ('i' + (_id++)); c.energias = []; c.danio = 0; c.condiciones = []; return c; }
function escena() {
  const e = J.crearPartida({ ladoA: { cartas: cartas60 }, ladoB: { cartas: cartas60 }, seed: 1 });
  J.autoSetup(e, 'A'); J.autoSetup(e, 'B');
  e.turnoDe = 'A'; e.fase = J.FASE.MAIN; e.turno = 3; e.ganador = null;
  e.lados.A.turnosJugados = 1; e.lados.A.energiaUsada = false; e.lados.A.retiroUsado = false;
  const act = inst(pikachu, 'act'); act.enJuegoDesde = 0;
  e.lados.A.activo = act; e.lados.A.banca = [];
  e.lados.A.mano = [inst(lightningEnergy, 'e1'), inst(raichu, 'rc1'), inst(pikachu, 'pk2'), inst(lightningEnergy, 'e2')];
  return e;
}

let e1 = escena();
J.ponerEnBanca(e1, 'A', 'pk2');
eq(e1.lados.A.banca.length, 1, 'poner básico en banca');
eq(e1.lados.A.mano.length, 3, 'la mano baja a 3');

let e2 = escena();
J.adjuntarEnergia(e2, 'A', 'e1', 'act');
eq(e2.lados.A.activo.energias.length, 1, 'energía adjuntada al activo');
eq(e2.lados.A.energiaUsada, true, 'energiaUsada=true');
J.adjuntarEnergia(e2, 'A', 'e2', 'act');
eq(e2.lados.A.activo.energias.length, 1, 'segunda energía bloqueada (1 por turno)');

let e3 = escena();
J.adjuntarEnergia(e3, 'A', 'e1', 'act');     // 1 energía en el activo Pikachu
J.evolucionar(e3, 'A', 'rc1', 'act');
eq(e3.lados.A.activo.nombre, 'Raichu', 'el activo evoluciona a Raichu');
eq(e3.lados.A.activo.energias.length, 1, 'la evolución conserva la energía');
eq((e3.lados.A.activo.debajo || []).length >= 1, true, 'queda Pikachu debajo');

let e4 = escena();
e4.lados.A.turnosJugados = 0;                // primer turno: no se evoluciona
J.evolucionar(e4, 'A', 'rc1', 'act');
eq(e4.lados.A.activo.nombre, 'Pikachu', 'no evoluciona en el primer turno');

let e5 = escena();
J.adjuntarEnergia(e5, 'A', 'e1', 'act');     // Pikachu retirada=1 -> necesita 1 energía
e5.lados.A.banca = [inst(pikachu, 'b1')];
J.retirar(e5, 'A', 'b1');
eq(e5.lados.A.activo.iid, 'b1', 'el de banca pasa a Activo al retirar');
eq(e5.lados.A.retiroUsado, true, 'retiroUsado=true');
eq(e5.lados.A.descarte.length, 1, 'se descarta 1 energía por el coste de retirada');

// ---------- Fase 6: ataque ----------
console.log('ataque: coste, debilidad/resistencia, KO, premios, victoria');
function combate(defView, bancaDef) {
  const e = escena();
  const atk = inst(pikachu, 'atk'); atk.enJuegoDesde = 0; atk.energias = [inst(lightningEnergy, 'le')];
  e.lados.A.activo = atk; e.lados.A.turnosJugados = 1;
  const def = inst(defView, 'def'); e.lados.B.activo = def;
  e.lados.B.banca = bancaDef || [];
  return e;
}
// puedePagar
ok(J.puedePagar({ energias: [{ energiaTipo: 'Lightning' }] }, ['Lightning']), 'paga coste tipado');
ok(!J.puedePagar({ energias: [] }, ['Lightning']), 'sin energía no paga');
ok(J.puedePagar({ energias: [{ energiaTipo: 'Water' }] }, ['Colorless']), 'incoloro lo paga cualquiera');

// debilidad ×2 -> 10 daño *2 = 20, KO sobre 20 HP
let c1 = combate(Object.assign({}, pikachu, { ps: '20', debilidades: [{ type: 'Lightning', value: '×2' }] }), [inst(pikachu, 'bb')]);
J.atacar(c1, 'A', 0);
eq(c1.lados.B.activo.iid, 'bb', 'tras KO, sube el de banca a Activo');
eq(c1.lados.A.premios.length, 5, 'el atacante toma 1 premio (6->5)');
eq(c1.lados.B.descarte.length >= 1, true, 'el KO va al descarte');
eq(c1.turnoDe, 'B', 'atacar termina el turno');

// sin debilidad: 10 daño, sin KO sobre 100 HP
let c2 = combate(Object.assign({}, pikachu, { ps: '100' }), [inst(pikachu, 'bb')]);
J.atacar(c2, 'A', 0);
eq(c2.lados.B.activo.danio, 10, 'daño base 10 aplicado');
eq(c2.lados.B.activo.iid, 'def', 'sin KO sigue el mismo Activo');

// resistencia -20 con un ataque de 30
const golpe30 = { id: 'g', nombre: 'Golpeador', supertipo: 'Pokémon', fase: 'Basic', ps: '90', tipos: ['Lightning'], ataques: [{ name: 'big', cost: ['Lightning'], damage: '30' }] };
let c3 = escena();
const a3 = inst(golpe30, 'a3'); a3.enJuegoDesde = 0; a3.energias = [inst(lightningEnergy, 'le3')];
c3.lados.A.activo = a3; c3.lados.A.turnosJugados = 1;
c3.lados.B.activo = inst(Object.assign({}, pikachu, { ps: '100', resistencias: [{ type: 'Lightning', value: '-20' }] }), 'd3');
c3.lados.B.banca = [inst(pikachu, 'bb3')];
J.atacar(c3, 'A', 0);
eq(c3.lados.B.activo.danio, 10, 'resistencia -20 reduce 30 a 10');

// energía insuficiente -> no hace nada
let c4 = combate(Object.assign({}, pikachu, { ps: '100' }), []);
c4.lados.A.activo.energias = []; // quitamos energía
J.atacar(c4, 'A', 0);
eq(c4.lados.B.activo.danio || 0, 0, 'sin energía no ataca');
eq(c4.turnoDe, 'A', 'no se gasta el turno si el ataque es ilegal');

// victoria por premios: 1 premio restante, KO da el último
let c5 = combate(Object.assign({}, pikachu, { ps: '20', debilidades: [{ type: 'Lightning', value: '×2' }] }), [inst(pikachu, 'bb5')]);
c5.lados.A.premios = c5.lados.A.premios.slice(0, 1);
J.atacar(c5, 'A', 0);
eq(c5.ganador, 'A', 'gana al tomar el último premio');
eq(c5.fase, J.FASE.END, 'fase END');

// victoria por dejar al rival sin Pokémon
let c6 = combate(Object.assign({}, pikachu, { ps: '20', debilidades: [{ type: 'Lightning', value: '×2' }] }), []);
J.atacar(c6, 'A', 0);
eq(c6.ganador, 'A', 'gana si el rival se queda sin Pokémon');
eq(c6.motivoFin, 'sinpokemon', 'motivo sinpokemon');

console.log('\n' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
