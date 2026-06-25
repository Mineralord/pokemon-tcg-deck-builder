/* Tests de la capa de pasivos (Fase 3). Ejecutar: node tests/pasivos.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.EFECTOS_MOTOR = require('../js/efectos-motor.js');
const PAS = require('../js/efectos-pasivos.js'); global.EFECTOS_PASIVOS = PAS;
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

// Base de efectos con pasivos de prueba.
global.EFECTOS_DB = {
  'p-armor': { pasivos: [{ mod: 'reduceDanio', cantidad: 20, a: 'esteP' }] },
  'p-bighp': { pasivos: [{ mod: 'hpExtra', cantidad: 30, a: 'esteP' }] },
  'p-float': { pasivos: [{ mod: 'costoRetiro', set: 0, a: 'esteP' }] },
  'p-veil': { habilidades: { 'Veil': { pasivos: [{ mod: 'inmuneEstado', a: 'propioTodos' }] } } },
  'stad-mist': { pasivos: [{ mod: 'reduceDanio', cantidad: 30, a: 'todos' }] }
};

let _i = 0;
function card(o) {
  return Object.assign({ iid: 'c' + (_i++), id: null, nombre: 'P', supertipo: 'Pokemon', hp: 100, danio: 0,
    tipos: ['Colorless'], energias: [], condiciones: [], debilidad: null, resistencia: null, retirada: 1, esBasico: true }, o || {});
}
function dos(a, b, extra) {
  const A = Object.assign({ activo: a, banca: [], descarte: [], estadio: null }, (extra && extra.A) || {});
  const B = Object.assign({ activo: b, banca: [], descarte: [], estadio: null }, (extra && extra.B) || {});
  return { lados: { A: A, B: B } };
}

console.log('reduceDanio — el defensor recibe menos daño');
let at = card({ tipos: ['Fire'] });
let def = card({ id: 'p-armor', hp: 100 });
let est = dos(at, def);
eq(J.danioEfectivo(at, def, { danio: 50 }, est), 30, '50 - 20 = 30');
eq(J.danioEfectivo(at, def, { danio: 10 }, est), 0, 'no baja de 0');
// Sin est (compat) no aplica pasivos:
eq(J.danioEfectivo(at, def, { danio: 50 }), 50, 'sin est -> 50 (sin regresión)');

console.log('hpExtra — sube el umbral de KO');
def = card({ id: 'p-bighp', hp: 100 });
est = dos(at, def);
eq(PAS.hpEf(est, def), 130, 'HP efectivo 130');

console.log('costoRetiro — fijado a 0 permite retirar sin energía');
const floatC = card({ id: 'p-float', retirada: 2, condiciones: [] });
const banco = card({ nombre: 'Banco' });
est = { fase: J.FASE.MAIN, turnoDe: 'A', ganador: null,
  lados: { A: { activo: floatC, banca: [banco], descarte: [], estadio: null, retiroUsado: false },
           B: { activo: card({}), banca: [], descarte: [], estadio: null } } };
J.retirar(est, 'A', banco.iid);
eq(est.lados.A.activo.iid, banco.iid, 'retiró con costo 0 (nuevo activo es el banco)');

console.log('inmuneEstado — bloquea condiciones');
const veil = card({ id: 'p-veil' });
est = dos(veil, card({}));
ok(PAS.cartaInmune(est, veil.iid), 'inmune por habilidad pasiva');
J.aplicarCondicion(est, 'A', 'poisoned');
eq(est.lados.A.activo.condiciones.length, 0, 'aplicarCondicion no afecta a inmune');
// op estado del intérprete también lo respeta:
global.EFECTOS_MOTOR.ejecutar(est, 'B', { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: 'asleep' }] }, { at: est.lados.B.activo, def: est.lados.A.activo });
eq(est.lados.A.activo.condiciones.length, 0, 'op estado tampoco afecta a inmune');

console.log('estadio de campo — reduce daño a AMBOS activos (a: todos)');
at = card({ tipos: ['Fire'] }); def = card({ hp: 100 });
est = dos(at, def, { A: { estadio: { id: 'stad-mist' } } });
eq(J.danioEfectivo(at, def, { danio: 50 }, est), 20, 'estadio -30 al rival');
eq(J.danioEfectivo(def, at, { danio: 50 }, est), 20, 'estadio -30 también al propio');

console.log('combinado — tool/habilidad (reduceDanio) + estadio se suman');
def = card({ id: 'p-armor', hp: 100 }); // -20 propio
est = dos(at, def, { A: { estadio: { id: 'stad-mist' } } }); // -30 todos
eq(J.danioEfectivo(at, def, { danio: 100 }, est), 50, '100 - 20 - 30 = 50');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
