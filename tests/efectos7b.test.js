/* Tests de la autoría Fase 7 (lote 2: búsquedas + habilidades). node tests/efectos7b.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.EFECTOS_MOTOR = require('../js/efectos-motor.js');
global.EFECTOS_PASIVOS = require('../js/efectos-pasivos.js');
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');
global.window = {}; require('../data/cartas-db.js');
global.EFECTOS_DB = require('../data/efectos-db.js');

let pass = 0, fail = 0;
function ok(c, m) { if (c) pass++; else { fail++; console.error('  ✗ ' + m); } }
function eq(a, b, m) { ok(a === b, m + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

const DB = {}; global.window.CARTAS_DB.cartas.forEach(c => DB[c.id] = c);
const rel = { id: 'rel', nombre: 'Reli', supertipo: 'Pokémon', fase: 'Basic', ps: '70', tipos: ['Colorless'], ataques: [{ name: 'Tap', cost: [], damage: '10', text: '' }], habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] };
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
function partida() {
  const est = J.crearPartida({ ladoA: { cartas: cartas(rel, 30) }, ladoB: { cartas: cartas(rel, 30) }, seed: 31 });
  J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
  est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
  return est;
}
function meterEntrenador(est, id) { const c = J.cartaJuego(DB[id]); c.iid = 'TR'; est.lados.A.mano.push(c); return c; }
function ponerActivoA(est, id) { const c = J.cartaJuego(DB[id]); c.iid = 'ACT'; c.danio = 0; c.energias = []; c.condiciones = []; c.enJuegoDesde = 0; est.lados.A.activo = c; return c; }

console.log('Transferencia de Bill — mirarTopN (Pokémon de las 8 de arriba)');
let est = partida(); meterEntrenador(est, 'sv3pt5-156');
J.jugarEntrenador(est, 'A', 'TR');
ok(!!est.pendiente && est.pendiente.tipo === 'mirarTopN', 'pausa en mirarTopN');
const o1 = est.pendiente.opciones[0].iid;
const mano0 = est.lados.A.mano.length;
J.resolverEleccion(est, 'A', [o1]);
eq(est.lados.A.mano.length, mano0 + 1, 'añadió 1 Pokémon a la mano');

console.log('Tera Orbe — busca un Pokémon Tera');
est = partida();
const tera = J.cartaJuego({ id: 'tx', nombre: 'TeraMon', supertipo: 'Pokémon', fase: 'Basic, Tera ex', ps: '200', tipos: ['Colorless'], ataques: [{ name: 'a', cost: [], damage: '10', text: '' }], debilidades: [], resistencias: [], costoRetirada: [] });
tera.iid = 'TERA'; est.lados.A.mazo.unshift(tera);
meterEntrenador(est, 'sv8-189');
J.jugarEntrenador(est, 'A', 'TR');
ok(!!est.pendiente && est.pendiente.opciones.some(o => o.iid === 'TERA'), 'el Tera aparece como opción');
J.resolverEleccion(est, 'A', ['TERA']);
ok(est.lados.A.mano.some(c => c.iid === 'TERA'), 'Tera fue a la mano');

console.log('Blastoise ex — Coraza Sólida: 30 menos de daño');
est = { lados: { A: { activo: J.cartaJuego({ id: 'atk', nombre: 'A', supertipo: 'Pokémon', tipos: ['Colorless'], ps: '100', ataques: [], debilidades: [], resistencias: [], costoRetirada: [] }), banca: [] },
  B: { activo: (function () { const c = J.cartaJuego(DB['sv3pt5-9']); c.iid = 'BLAS'; c.danio = 0; c.energias = []; c.condiciones = []; return c; })(), banca: [] } } };
eq(J.danioEfectivo(est.lados.A.activo, est.lados.B.activo, { danio: 80 }, est), 50, '80 - 30 = 50');

console.log('Dodrio — Robo Veloz: +1 contador a sí mismo y roba 1 (1/turno)');
est = partida(); const dod = ponerActivoA(est, 'sv3pt5-85');
const m0 = est.lados.A.mano.length;
J.usarHabilidad(est, 'A', 'ACT', 0);
eq(dod.danio, 10, 'se puso 1 contador (10) a sí mismo');
eq(est.lados.A.mano.length, m0 + 1, 'robó 1');
J.usarHabilidad(est, 'A', 'ACT', 0);
eq(est.lados.A.mano.length, m0 + 1, 'no se repite en el mismo turno');

console.log('Starmie — Cometa Misterioso: 20 a un Pokémon rival elegido');
est = partida(); ponerActivoA(est, 'sv3pt5-121');
const rb = est.lados.B.banca[0];
J.usarHabilidad(est, 'A', 'ACT', 0);
ok(!!est.pendiente && est.pendiente.tipo === 'elegirObjetivo', 'pausa para elegir objetivo');
J.resolverEleccion(est, 'A', [rb.iid]);
eq(rb.danio, 20, 'el Pokémon rival elegido recibió 20');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
