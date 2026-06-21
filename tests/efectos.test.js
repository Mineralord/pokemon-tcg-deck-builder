/* Tests del DSL de efectos (Fase 1). Ejecutar: node tests/efectos.test.js */
'use strict';
const DSL = require('../js/efectos-dsl.js');
const M = require('../js/efectos-motor.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

// ---------- Helpers de estado mínimo ----------
let _iid = 0;
function poke(o) {
  return Object.assign({ iid: 'k' + (_iid++), supertipo: 'Pokemon', nombre: 'P', hp: 100, stage: 0,
    esBasico: true, tipos: ['Colorless'], danio: 0, energias: [], condiciones: [] }, o || {});
}
function energia(t) { return { supertipo: 'Energy', energiaTipo: t || 'Colorless', nombre: t + ' Energy' }; }
function lado() {
  return { nombre: '?', mazo: [], mano: [], banca: [], activo: null, descarte: [], premios: [], lost: [], estadio: null,
    turnosJugados: 1 };
}
function estado(opts) {
  opts = opts || {};
  const A = lado(), B = lado();
  A.activo = opts.aActivo || poke({ nombre: 'Atacante' });
  B.activo = opts.bActivo || poke({ nombre: 'Defensor' });
  if (opts.aBanca) A.banca = opts.aBanca; if (opts.bBanca) B.banca = opts.bBanca;
  if (opts.aMazo) A.mazo = opts.aMazo; if (opts.aMano) A.mano = opts.aMano;
  return { seed: 1, rngN: 0, turno: 2, turnoDe: 'A', lados: { A: A, B: B } };
}
// Moneda fija para tests deterministas.
function coinSeq(seq) { let i = 0; return function () { return seq[i++ % seq.length]; }; }
function run(est, entrada, extra) { return M.ejecutar(est, 'A', entrada, extra); }

// ============ Validación del esquema (Fase 1b) ============
console.log('DSL.validar — entradas correctas');
ok(DSL.validar({ ataques: { X: { ops: [{ op: 'danioExtra', cantidad: 30 }] } } }).ok, 'danioExtra válido');
ok(DSL.validar({ ataques: { X: { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: ['confused', 'poisoned'] } ] } } }).ok, 'estado múltiple válido');
ok(DSL.validar({ pasivos: [{ mod: 'reduceDanio', cantidad: 20, a: 'propiaBanca' }] }).ok, 'pasivo válido');

console.log('DSL.validar — detecta errores');
ok(!DSL.validar({ ataques: { X: { ops: [{ op: 'noExiste' }] } } }).ok, 'op desconocida rechazada');
ok(!DSL.validar({ ataques: { X: { ops: [{ op: 'danioExtra' }] } } }).ok, 'falta cantidad/porCada');
ok(!DSL.validar({ ataques: { X: { ops: [{ op: 'danioExtra', cantidad: 10, porCada: { op: 'cuenta', objetivo: 'rivalBanca', multiplica: 1 } }] } } }).ok, 'cantidad y porCada a la vez');
ok(!DSL.validar({ ataques: { X: { ops: [{ op: 'estado', estado: 'roto' }] } } }).ok, 'estado desconocido');
ok(!DSL.validar({ pasivos: [{ mod: 'xx' }] }).ok, 'mod desconocido');

console.log('DSL.esEjecutableF1');
ok(DSL.esEjecutableF1({ ops: [{ op: 'danioExtra', cantidad: 10 }] }), 'ops F1 ejecutable');
ok(!DSL.esEjecutableF1({ ops: [{ op: 'buscarMazo' }] }), 'op F2 no ejecutable en F1');
ok(DSL.esEjecutableF1({ efectoJS: function () {} }), 'efectoJS ejecutable');

// ============ Ejecución de ops (Fase 1c) ============
console.log('op estado — confundido + envenenado al rival');
let e = estado();
run(e, { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: ['confused', 'poisoned'] }] });
ok(e.lados.B.activo.condiciones.indexOf('confused') >= 0, 'rival confundido');
ok(e.lados.B.activo.condiciones.indexOf('poisoned') >= 0, 'rival envenenado');

console.log('op danioExtra — incondicional');
e = estado();
run(e, { ops: [{ op: 'danioExtra', cantidad: 50 }] });
eq(e.lados.B.activo.danio, 50, 'def +50');

console.log('op danioExtra — condicion tieneDanio(esteP)');
e = estado({ aActivo: poke({ nombre: 'A', danio: 20 }) });
run(e, { ops: [{ op: 'danioExtra', cantidad: 100, condicion: { tipo: 'tieneDanio', objetivo: 'esteP' } }] });
eq(e.lados.B.activo.danio, 100, 'def +100 porque atacante tiene daño');
e = estado(); // sin daño en atacante
run(e, { ops: [{ op: 'danioExtra', cantidad: 100, condicion: { tipo: 'tieneDanio', objetivo: 'esteP' } }] });
eq(e.lados.B.activo.danio, 0, 'sin daño extra si atacante sano');

console.log('op danioExtra — porCada banca rival');
e = estado({ bBanca: [poke({}), poke({}), poke({})] });
run(e, { ops: [{ op: 'danioExtra', porCada: { op: 'cuenta', objetivo: 'rivalBanca', multiplica: 30 } }] });
eq(e.lados.B.activo.danio, 90, '3 banca × 30 = 90');

console.log('op danioExtra — porCada contadores propios');
e = estado({ aActivo: poke({ danio: 50 }) });
run(e, { ops: [{ op: 'danioExtra', porCada: { op: 'contadores', objetivo: 'esteP', multiplica: 10 } }] });
eq(e.lados.B.activo.danio, 50, '5 contadores × 10 = 50');

console.log('op danioExtra — porCada energías rival');
e = estado({ bActivo: poke({ energias: [energia('Water'), energia('Water')] }) });
run(e, { ops: [{ op: 'danioExtra', porCada: { op: 'energias', objetivo: 'rivalActivo', multiplica: 20 } }] });
eq(e.lados.B.activo.danio, 40, '2 energías × 20 = 40');

console.log('op recoil');
e = estado();
run(e, { ops: [{ op: 'recoil', cantidad: 30 }] });
eq(e.lados.A.activo.danio, 30, 'atacante se autoinflige 30');

console.log('op descartarEnergia propia');
e = estado({ aActivo: poke({ energias: [energia('Fire'), energia('Fire'), energia('Fire'), energia('Fire')] }) });
run(e, { ops: [{ op: 'descartarEnergia', objetivo: 'esteP', cantidad: 3 }] });
eq(e.lados.A.activo.energias.length, 1, 'quedan 1 energía');
eq(e.lados.A.descarte.length, 3, '3 al descarte propio');

console.log('op curar');
e = estado({ aActivo: poke({ danio: 80 }) });
run(e, { ops: [{ op: 'curar', objetivo: 'esteP', cantidad: 60 }] });
eq(e.lados.A.activo.danio, 20, 'curado a 20');
run(e, { ops: [{ op: 'curar', objetivo: 'esteP', todo: true }] });
eq(e.lados.A.activo.danio, 0, 'curado del todo');

console.log('op quitarEstado todas');
e = estado({ aActivo: poke({ condiciones: ['burned', 'confused'] }) });
run(e, { ops: [{ op: 'quitarEstado', objetivo: 'esteP', todas: true }] });
eq(e.lados.A.activo.condiciones.length, 0, 'sin condiciones');

console.log('op robar');
e = estado({ aMazo: [poke({}), poke({}), poke({})] });
run(e, { ops: [{ op: 'robar', cantidad: 2 }] });
eq(e.lados.A.mano.length, 2, 'robó 2');
eq(e.lados.A.mazo.length, 1, 'mazo queda 1');

console.log('op estado — con condicion coin (cara)');
e = estado();
run(e, { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: 'asleep', condicion: { tipo: 'coin' } }] }, { flip: coinSeq([true]) });
ok(e.lados.B.activo.condiciones.indexOf('asleep') >= 0, 'dormido con cara');
e = estado();
run(e, { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: 'asleep', condicion: { tipo: 'coin' } }] }, { flip: coinSeq([false]) });
ok(e.lados.B.activo.condiciones.indexOf('asleep') < 0, 'no dormido con cruz');

console.log('porCada carasHastaCruz (3 caras seguidas)');
e = estado();
run(e, { ops: [{ op: 'danioExtra', porCada: { op: 'carasHastaCruz', multiplica: 20 } }] }, { flip: coinSeq([true, true, true, false]) });
eq(e.lados.B.activo.danio, 60, '3 caras × 20 = 60');

console.log('porCada carasDe N monedas');
e = estado();
run(e, { ops: [{ op: 'danioExtra', porCada: { op: 'carasDe', monedas: 4, multiplica: 50 } }] }, { flip: coinSeq([true, false, true, false]) });
eq(e.lados.B.activo.danio, 100, '2 caras de 4 × 50 = 100');

console.log('efectoJS escape');
e = estado();
let r = run(e, { efectoJS: function (ctx) { ctx.def.danio = (ctx.def.danio || 0) + 7; } });
eq(e.lados.B.activo.danio, 7, 'efectoJS aplicado');
eq(r[0], 'efectoJS', 'marca efectoJS');

console.log('entrada vacía -> manual');
eq(run(estado(), { ops: [] })[0], 'manual', 'ops vacío = manual');
eq(run(estado(), { ops: [{ op: 'buscarMazo' }] })[0], 'manual', 'op F2 = manual');

// ---------- Resultado ----------
console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
