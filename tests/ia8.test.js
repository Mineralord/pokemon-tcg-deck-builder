/* Tests de la IA consciente del DSL (Fase 8). node tests/ia8.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.EFECTOS_MOTOR = require('../js/efectos-motor.js');
global.EFECTOS_PASIVOS = require('../js/efectos-pasivos.js');
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');
global.window = {}; require('../data/cartas-db.js');
global.EFECTOS_DB = require('../data/efectos-db.js');
const IA = require('../js/juego-ia.js');

let pass = 0, fail = 0;
function ok(c, m) { if (c) pass++; else { fail++; console.error('  ✗ ' + m); } }
function eq(a, b, m) { ok(a === b, m + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

const rel = { id: 'rel', nombre: 'Reli', supertipo: 'Pokémon', fase: 'Basic', ps: '300', tipos: ['Colorless'], ataques: [{ name: 'Tap', cost: [], damage: '10', text: '' }], habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] };
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
function atkView(id, atk) { return { id: id, nombre: id, supertipo: 'Pokémon', fase: 'Basic, ex', ps: '330', tipos: ['Colorless'], evolucionaDe: null, ataques: [atk], habilidades: [], debilidades: [], resistencias: [], reglas: [], costoRetirada: [] }; }

console.log('IA resuelve la elección de su propio ataque (snipe de Zapdos)');
let est = J.crearPartida({ ladoA: { cartas: cartas(rel, 30) }, ladoB: { cartas: cartas(rel, 30) }, seed: 21 });
J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
est.inicia = 'A'; est.turnoDe = 'B'; est.fase = J.FASE.MAIN; est.lados.B.turnosJugados = 1;
const z = J.cartaJuego(atkView('sv3pt5-145', { name: 'Multishot Lightning', cost: [], damage: '120', text: '' }));
z.iid = 'ZAP'; z.energias = []; z.condiciones = []; z.danio = 0; z.enJuegoDesde = 0;
est.lados.B.activo = z;
est.lados.B.mano = []; // sin distracciones
const aBench = est.lados.A.banca[0];
IA.configurar({ dificultad: 'medio', modo: 'reglas' });
IA.jugarTurno(est);
ok(!est.pendiente, 'sin elección pendiente (la IA la resolvió)');
eq(aBench.danio, 90, 'el banco de A recibió 90 por el snipe');
eq(est.turnoDe, 'A', 'el turno volvió a A');

console.log('IA no se cuelga si su ataque pausa y resuelve en bucle acotado');
est = J.crearPartida({ ladoA: { cartas: cartas(rel, 30) }, ladoB: { cartas: cartas(rel, 30) }, seed: 22 });
J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
est.inicia = 'A'; est.turnoDe = 'B'; est.fase = J.FASE.MAIN; est.lados.B.turnosJugados = 1;
IA.jugarTurno(est);
ok(!est.pendiente, 'sin pendiente tras el turno de la IA');
ok(est.turnoDe === 'A' || est.ganador, 'el turno terminó');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
