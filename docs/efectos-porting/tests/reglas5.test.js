/* Tests de la Fase 5 (ex, VSTAR, Terastal). node tests/reglas5.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
const M = require('../js/efectos-motor.js'); global.EFECTOS_MOTOR = M;
global.EFECTOS_PASIVOS = require('../js/efectos-pasivos.js');
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

global.EFECTOS_DB = {
  'vab': { habilidades: { 'Star Engine': { vstar: true, ops: [{ op: 'robar', cantidad: 1 }] } } }
};

function vista(id, nombre, fase, ataques, habilidades) {
  return { id: id, nombre: nombre, supertipo: 'Pokémon', fase: fase || 'Basic', ps: '200', tipos: ['Colorless'],
    evolucionaDe: null, ataques: ataques || [{ name: 'Tap', cost: [], damage: '10', text: '' }],
    habilidades: habilidades || [], debilidades: [], resistencias: [],
    reglas: /ex/i.test(fase || '') ? ['Pokémon ex rule: When your Pokémon ex is Knocked Out, your opponent takes 2 Prize cards.'] : [], costoRetirada: [] };
}

console.log('Pokémon ex — otorga 2 premios');
const exC = J.cartaJuego(vista('exmon', 'Exmon', 'Basic, ex'));
eq(exC.premiosKO, 2, 'premiosKO 2');

console.log('Terastal — flag tera desde la fase');
const teraC = J.cartaJuego(vista('teramon', 'Teramon', 'Basic, Tera ex'));
eq(teraC.tera, true, 'tera detectado');
eq(teraC.premiosKO, 2, 'tera ex sigue dando 2 premios');

console.log('Terastal — sin daño en Banca, sí en Activo');
function poke(o) { return Object.assign({ iid: 'k', supertipo: 'Pokemon', nombre: 'P', hp: 200, danio: 0, tipos: ['Colorless'], energias: [], condiciones: [], tera: false }, o); }
function lado() { return { mazo: [], mano: [], banca: [], activo: null, descarte: [], premios: [], lost: [], estadio: null }; }
function est2(a, b) { const A = lado(), B = lado(); A.activo = a; B.activo = b; return { seed: 1, rngN: 0, turno: 2, turnoDe: 'A', lados: { A: A, B: B } }; }
let tb = poke({ iid: 'tb', tera: true });
let e = est2(poke({ iid: 'a' }), poke({ iid: 'b' }));
e.lados.B.banca = [tb];
M.ejecutar(e, 'A', { ops: [{ op: 'danio', objetivo: 'rivalBanca', cantidad: 100 }] });
eq(tb.danio, 0, 'Tera en banca no recibe daño');
// Como Activo sí recibe:
e = est2(poke({ iid: 'a' }), poke({ iid: 'tb2', tera: true }));
M.ejecutar(e, 'A', { ops: [{ op: 'danio', objetivo: 'rivalActivo', cantidad: 100 }] });
eq(e.lados.B.activo.danio, 100, 'Tera Activo sí recibe daño');

console.log('Poder VSTAR — una vez por partida (no se reinicia entre turnos)');
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
const rel = vista('rel', 'Rel');
let est = J.crearPartida({ ladoA: { nombre: 'A', cartas: cartas(rel, 30) }, ladoB: { nombre: 'B', cartas: cartas(rel, 30) }, seed: 4 });
J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
const star = J.cartaJuego(vista('vab', 'Star', 'Basic', [{ name: 'Tap', cost: [], damage: '10', text: '' }], [{ name: 'Star Engine', text: '' }]));
star.iid = 'star'; star.danio = 0; star.energias = []; star.condiciones = [];
est.lados.A.activo = star;
let m0 = est.lados.A.mano.length;
J.usarHabilidad(est, 'A', 'star', 0);
eq(est.lados.A.mano.length, m0 + 1, 'VSTAR usado: roba 1');
ok(est.lados.A.vstarUsado === true, 'vstarUsado marcado');
// Avanza un turno completo y vuelve a A:
J.terminarTurno(est); est.fase = J.FASE.MAIN; est.turnoDe = 'B'; J.terminarTurno(est);
est.fase = J.FASE.MAIN; est.turnoDe = 'A';
m0 = est.lados.A.mano.length;
J.usarHabilidad(est, 'A', 'star', 0);
eq(est.lados.A.mano.length, m0, 'VSTAR ya usado: bloqueado aunque sea otro turno');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
