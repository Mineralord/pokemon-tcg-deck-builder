/* Tests de la máquina de elecciones (Fase 2). Ejecutar: node tests/elecciones.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
const M = require('../js/efectos-motor.js'); global.EFECTOS_MOTOR = M;
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

let _iid = 0;
function poke(o) {
  return Object.assign({ iid: 'k' + (_iid++), supertipo: 'Pokemon', nombre: 'P', hp: 100, stage: 0,
    esBasico: true, tipos: ['Colorless'], danio: 0, energias: [], condiciones: [] }, o || {});
}
function lado() { return { nombre: '?', mazo: [], mano: [], banca: [], activo: null, descarte: [], premios: [], lost: [], estadio: null, turnosJugados: 1 }; }
function estado(opts) {
  opts = opts || {}; const A = lado(), B = lado();
  A.activo = ('aActivo' in opts) ? opts.aActivo : poke({ nombre: 'Atacante' });
  B.activo = ('bActivo' in opts) ? opts.bActivo : poke({ nombre: 'Defensor' });
  if (opts.aBanca) A.banca = opts.aBanca; if (opts.aMazo) A.mazo = opts.aMazo; if (opts.aMano) A.mano = opts.aMano;
  return { seed: 3, rngN: 0, turno: 2, turnoDe: 'A', lados: { A: A, B: B } };
}

// ===== buscarMazo =====
console.log('buscarMazo — pausa, filtra y reanuda al robar');
let basico = poke({ nombre: 'Básico', iid: 'b1' });
let evo = poke({ nombre: 'Evo', iid: 'e1', esBasico: false, stage: 1 });
let e = estado({ aMazo: [evo, basico, poke({ iid: 'x1' })] });
let marcas = M.ejecutar(e, 'A', { ops: [{ op: 'buscarMazo', filtro: { esBasico: true }, cantidad: 1 }] });
ok(marcas.indexOf('pendiente') >= 0, 'devuelve marca pendiente');
ok(!!e.pendiente, 'est.pendiente fijado');
eq(e.pendiente.tipo, 'buscarMazo', 'tipo correcto');
eq(e.pendiente.opciones.length, 2, 'solo Básicos en opciones (b1, x1)');
M.resumir(e, ['b1']);
ok(!e.pendiente, 'pendiente limpiado tras reanudar');
ok(e.lados.A.mano.some(function (c) { return c.iid === 'b1'; }), 'Básico fue a la mano');
eq(e.lados.A.mazo.length, 2, 'mazo bajó a 2');

console.log('buscarMazo — cancelar (whiff) deja todo y termina');
e = estado({ aMazo: [poke({ iid: 'z1', esBasico: true })] });
M.ejecutar(e, 'A', { ops: [{ op: 'buscarMazo', filtro: { esBasico: true }, cantidad: 1 }] });
M.resumir(e, []); // cancela
ok(!e.pendiente, 'pendiente limpiado al cancelar');
eq(e.lados.A.mano.length, 0, 'no robó nada');

// ===== elegirObjetivo + curar elegido =====
console.log('elegirObjetivo — elige y la op siguiente actúa sobre lo elegido');
let herido = poke({ nombre: 'Herido', iid: 'h1', danio: 50 });
e = estado({ aActivo: poke({ iid: 'act', danio: 0 }), aBanca: [herido] });
M.ejecutar(e, 'A', { ops: [
  { op: 'elegirObjetivo', objetivo: 'propioTodos', cuantos: 1 },
  { op: 'curar', objetivo: 'elegido', cantidad: 60 }
] });
eq(e.pendiente.tipo, 'elegirObjetivo', 'pausa en elegirObjetivo');
eq(e.pendiente.opciones.length, 2, 'activo + 1 banca = 2 opciones');
M.resumir(e, ['h1']);
ok(!e.pendiente, 'sin pendiente tras curar');
eq(herido.danio, 0, 'curó 50 al elegido');

// ===== cambiarActivo =====
console.log('cambiarActivo — intercambia activo por banca elegida');
let banco = poke({ nombre: 'Banco', iid: 'bn1' });
e = estado({ aActivo: poke({ iid: 'act2' }), aBanca: [banco] });
M.ejecutar(e, 'A', { ops: [{ op: 'cambiarActivo' }] });
eq(e.pendiente.tipo, 'cambiarActivo', 'pausa en cambiarActivo');
M.resumir(e, ['bn1']);
eq(e.lados.A.activo.iid, 'bn1', 'nuevo activo es el elegido');
ok(e.lados.A.banca.some(function (c) { return c.iid === 'act2'; }), 'el viejo activo fue a banca');

// ===== ponerEnBanca =====
console.log('ponerEnBanca — mueve Básicos de la mano a la banca');
e = estado({ aMano: [poke({ iid: 'm1' }), poke({ iid: 'm2' }), poke({ iid: 'm3', esBasico: false, stage: 1 })] });
M.ejecutar(e, 'A', { ops: [{ op: 'ponerEnBanca', cantidad: 2 }] });
eq(e.pendiente.opciones.length, 2, 'solo 2 Básicos elegibles (m3 es evolución)');
M.resumir(e, ['m1', 'm2']);
eq(e.lados.A.banca.length, 2, '2 a la banca');

// ===== sin objetivo válido =====
console.log('sin objetivo válido — la op se omite, no hay pausa');
e = estado({ aActivo: null, bActivo: null });
let r = M.ejecutar(e, 'A', { ops: [
  { op: 'elegirObjetivo', objetivo: 'propioTodos', cuantos: 1 },
  { op: 'curar', objetivo: 'elegido', cantidad: 60 }
] });
ok(!e.pendiente, 'sin pendiente cuando no hay objetivos');

// ===== integración: ataque que busca difiere el fin de turno =====
console.log('integración — ataque con buscarMazo difiere terminarTurno');
global.EFECTOS_DB = { 'test-dig': { ataques: { 'Dig': { ops: [{ op: 'buscarMazo', filtro: { supertipo: 'Energy' }, cantidad: 1 }] } } } };
function vistaPoke(id, nombre, ataques) {
  return { id: id, nombre: nombre, supertipo: 'Pokémon', fase: 'Basic', ps: '200', tipos: ['Colorless'],
    evolucionaDe: null, ataques: ataques, habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] };
}
function cartas(view, n) { const out = []; for (let i = 0; i < n; i++) out.push(J.cartaJuego(view)); return out; }
const relleno = vistaPoke('relleno', 'Relleno', [{ name: 'Tap', cost: [], damage: '10', text: '' }]);
const est = J.crearPartida({ ladoA: { nombre: 'A', cartas: cartas(relleno, 30) }, ladoB: { nombre: 'B', cartas: cartas(relleno, 30) }, seed: 9 });
J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
const atk = J.cartaJuego(vistaPoke('test-dig', 'Digger', [{ name: 'Dig', cost: [], damage: '10', text: '' }]));
atk.iid = 'digger'; atk.danio = 0; atk.energias = []; atk.condiciones = []; atk.enJuegoDesde = 0;
est.lados.A.activo = atk;
const en = J.cartaJuego(J.energiaBasicaView('Lightning Energy')); en.iid = 'enr1'; est.lados.A.mazo.unshift(en);
J.atacar(est, 'A', 0);
ok(!!est.pendiente, 'pendiente tras atacar');
eq(est.turnoDe, 'A', 'el turno NO cambió (diferido)');
J.resolverEleccion(est, 'A', ['enr1']);
ok(!est.pendiente, 'pendiente resuelto');
ok(est.lados.A.mano.some(function (c) { return c.iid === 'enr1'; }) || est.turnoDe === 'B', 'energía robada');
eq(est.turnoDe, 'B', 'el turno avanzó tras resolver la elección');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
