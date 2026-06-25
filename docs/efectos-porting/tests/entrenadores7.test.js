/* Tests de entrenadores autorados (Fase 7). node tests/entrenadores7.test.js */
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
function vistaBasico(id) { return { id: id, nombre: 'Reli ' + id, supertipo: 'Pokémon', fase: 'Basic', ps: '70', tipos: ['Colorless'], evolucionaDe: null, ataques: [{ name: 'Tap', cost: [], damage: '10', text: '' }], habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] }; }
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
function partida() {
  const est = J.crearPartida({ ladoA: { cartas: cartas(vistaBasico('rel'), 30) }, ladoB: { cartas: cartas(vistaBasico('rel'), 30) }, seed: 11 });
  J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
  est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
  return est;
}
function meter(est, id) { const c = J.cartaJuego(DB[id]); c.iid = 'TR'; c.danio = 0; c.energias = []; c.condiciones = []; est.lados.A.mano.push(c); return c; }

console.log('Nemona — roba 3');
let est = partida(); let mz = est.lados.A.mazo.length; meter(est, 'sv1-180');
J.jugarEntrenador(est, 'A', 'TR');
eq(est.lados.A.mazo.length, mz - 3, 'mazo bajó 3 (robó 3)');

console.log('Nido Ball — busca Básico a la Banca');
est = partida(); est.lados.A.banca = []; meter(est, 'sv1-181'); const bancaN = est.lados.A.banca.length;
J.jugarEntrenador(est, 'A', 'TR');
ok(!!est.pendiente && est.pendiente.tipo === 'buscarMazo', 'pausa en buscarMazo');
const opt = est.pendiente.opciones[0].iid;
J.resolverEleccion(est, 'A', [opt]);
eq(est.lados.A.banca.length, bancaN + 1, 'Básico añadido a la Banca');

console.log('Cambio — intercambia tu Activo');
est = partida(); const actViejo = est.lados.A.activo.iid; meter(est, 'sv3pt5-206');
J.jugarEntrenador(est, 'A', 'TR');
ok(!!est.pendiente && est.pendiente.tipo === 'cambiarActivo', 'pausa en cambiarActivo');
J.resolverEleccion(est, 'A', [est.pendiente.opciones[0].iid]);
ok(est.lados.A.activo.iid !== actViejo, 'el Activo cambió');

console.log('Órdenes de Jefes — saca un Pokémon de la Banca rival al Activo');
est = partida(); const rivalViejo = est.lados.B.activo.iid; meter(est, 'me1-114');
J.jugarEntrenador(est, 'A', 'TR');
ok(!!est.pendiente && est.pendiente.tipo === 'cambiarActivo', 'pausa cambiarActivo (rival)');
J.resolverEleccion(est, 'A', [est.pendiente.opciones[0].iid]);
ok(est.lados.B.activo.iid !== rivalViejo, 'el Activo rival cambió');

console.log('Poción — cura 30 al Pokémon elegido');
est = partida(); est.lados.A.activo.danio = 50; const objIid = est.lados.A.activo.iid; meter(est, 'sv1-188');
J.jugarEntrenador(est, 'A', 'TR');
ok(!!est.pendiente && est.pendiente.tipo === 'elegirObjetivo', 'pausa elegirObjetivo');
J.resolverEleccion(est, 'A', [objIid]);
eq(est.lados.A.activo.danio, 20, 'curado de 50 a 20');

console.log('Joven — baraja tu mano en el mazo y roba 5');
est = partida(); meter(est, 'sv1-198');
// vaciar mano salvo el entrenador para medir
est.lados.A.mano = est.lados.A.mano.filter(c => c.iid === 'TR');
J.jugarEntrenador(est, 'A', 'TR');
eq(est.lados.A.mano.length, 5, 'mano de 5 tras barajar y robar');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
