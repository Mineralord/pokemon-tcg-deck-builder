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
eq(p1.fase, J.FASE.DRAW, 'ambos listos -> fase DRAW');
ok(p1.turnoDe === 'A' || p1.turnoDe === 'B', 'turnoDe definido tras setup');
eq(p1.turno, 1, 'turno 1');

console.log('\n' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
