/* Tests de estadios/tools/recuperación autorados (Fase 7). node tests/pasivos7.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.EFECTOS_MOTOR = require('../js/efectos-motor.js');
const PAS = require('../js/efectos-pasivos.js'); global.EFECTOS_PASIVOS = PAS;
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');
global.window = {}; require('../data/cartas-db.js');
global.EFECTOS_DB = require('../data/efectos-db.js');

let pass = 0, fail = 0;
function ok(c, m) { if (c) pass++; else { fail++; console.error('  ✗ ' + m); } }
function eq(a, b, m) { ok(a === b, m + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

let _i = 0;
function card(o) { return Object.assign({ iid: 'c' + (_i++), supertipo: 'Pokemon', nombre: 'P', hp: 100, stage: 0, esBasico: true, tipos: ['Colorless'], danio: 0, energias: [], condiciones: [], tools: [], debilidad: null, resistencia: null, retirada: 1 }, o); }
function lado() { return { activo: null, banca: [], descarte: [], estadio: null, mazo: [], mano: [] }; }
function dos(a, b, extra) {
  const A = Object.assign(lado(), { activo: a }, (extra && extra.A) || {});
  const B = Object.assign(lado(), { activo: b }, (extra && extra.B) || {});
  return { seed: 1, rngN: 0, turno: 2, turnoDe: 'A', lados: { A: A, B: B } };
}

console.log('Estadio Animado — +30 PS a Básicos (ambos lados)');
let basico = card({ esBasico: true, stage: 0 });
let fase2 = card({ esBasico: false, stage: 2 });
let est = dos(basico, fase2, { A: { estadio: { id: 'sv8-180' } } });
eq(PAS.hpEf(est, basico), 130, 'Básico 100+30');
eq(PAS.hpEf(est, fase2), 100, 'Fase 2 sin cambio');

console.log('Montaña Gravedad — -30 PS a Fase 2');
est = dos(card({ esBasico: false, stage: 2, hp: 330 }), card({}), { A: { estadio: { id: 'sv8-177' } } });
eq(PAS.hpEf(est, est.lados.A.activo), 300, 'Fase 2 330-30');

console.log('Gafas Protectoras — Básico sin Debilidad');
let atk = card({ tipos: ['Fire'] });
let def = card({ esBasico: true, debilidad: { tipo: 'Fire', mult: 2 }, tools: [{ id: 'sv3pt5-164' }] });
est = dos(atk, def);
eq(J.danioEfectivo(atk, def, { danio: 50 }, est), 50, 'sin debilidad: 50 (no x2)');
def.tools = [];
eq(J.danioEfectivo(atk, def, { danio: 50 }, est), 100, 'con debilidad: 100');

console.log('Banda Rígida — Fase 1 recibe 30 menos');
def = card({ esBasico: false, stage: 1, tools: [{ id: 'sv3pt5-165' }] });
est = dos(card({ tipos: ['Water'] }), def);
eq(J.danioEfectivo(est.lados.A.activo, def, { danio: 80 }, est), 50, '80-30=50');

console.log('Gran Globo Aerostático — Fase 2 sin coste de retirada');
let g = card({ esBasico: false, stage: 2, retirada: 3, tools: [{ id: 'sv3pt5-155' }] });
est = dos(g, card({}));
eq(PAS.retiroEf(est, g), 0, 'retiro 0');

console.log('Camilla Nocturna — recupera 1 carta del descarte a la mano');
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
const rel = { id: 'rel', nombre: 'Reli', supertipo: 'Pokémon', fase: 'Basic', ps: '70', tipos: ['Colorless'], ataques: [{ name: 'Tap', cost: [], damage: '10', text: '' }], habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] };
let estP = J.crearPartida({ ladoA: { cartas: cartas(rel, 30) }, ladoB: { cartas: cartas(rel, 30) }, seed: 7 });
J.autoSetup(estP, 'A'); J.autoSetup(estP, 'B');
estP.inicia = 'A'; estP.turnoDe = 'A'; estP.fase = J.FASE.MAIN; estP.lados.A.turnosJugados = 1;
const DB = {}; global.window.CARTAS_DB.cartas.forEach(c => DB[c.id] = c);
const ns = J.cartaJuego(DB['sv8-251']); ns.iid = 'NS'; estP.lados.A.mano.push(ns);
const enDesc = J.cartaJuego(rel); enDesc.iid = 'DESC'; estP.lados.A.descarte.push(enDesc);
J.jugarEntrenador(estP, 'A', 'NS');
ok(!!estP.pendiente && estP.pendiente.tipo === 'buscarDescarte', 'pausa en buscarDescarte');
J.resolverEleccion(estP, 'A', ['DESC']);
ok(estP.lados.A.mano.some(c => c.iid === 'DESC'), 'la carta volvió a la mano');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
