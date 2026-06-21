/* Tests de ataques ex autorados (Fase 7). node tests/ataquesex7.test.js */
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

const rel = { id: 'rel', nombre: 'Reli', supertipo: 'Pokémon', fase: 'Basic', ps: '400', tipos: ['Colorless'], ataques: [{ name: 'Tap', cost: [], damage: '10', text: '' }], habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] };
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
function partida() {
  const est = J.crearPartida({ ladoA: { cartas: cartas(rel, 30) }, ladoB: { cartas: cartas(rel, 30) }, seed: 13 });
  J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
  est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
  return est;
}
function atkView(id, atk) { return { id: id, nombre: id, supertipo: 'Pokémon', fase: 'Basic, ex', ps: '330', tipos: ['Colorless'], evolucionaDe: null, ataques: [atk], habilidades: [], debilidades: [], resistencias: [], reglas: [], costoRetirada: [] }; }
function ponerAtacante(est, id, atk, danio) {
  const c = J.cartaJuego(atkView(id, atk)); c.iid = 'AT'; c.danio = danio || 0; c.energias = []; c.condiciones = []; c.enJuegoDesde = 0;
  est.lados.A.activo = c; return c;
}

console.log('Charizard ex — Brave Wing: 60 (+100 si tiene daño)');
let est = partida(); ponerAtacante(est, 'sv3pt5-6', { name: 'Brave Wing', cost: [], damage: '60', text: '' }, 20);
J.atacar(est, 'A', 0);
eq(est.lados.B.activo.danio, 160, '60 + 100 = 160');
est = partida(); ponerAtacante(est, 'sv3pt5-6', { name: 'Brave Wing', cost: [], damage: '60', text: '' }, 0);
J.atacar(est, 'A', 0);
eq(est.lados.B.activo.danio, 60, 'sin daño propio: solo 60');

console.log('Ninetales ex — Heat Wave: rival Quemado');
est = partida(); ponerAtacante(est, 'sv3pt5-38', { name: 'Heat Wave', cost: [], damage: '30', text: '' });
const defN = est.lados.B.activo;
J.atacar(est, 'A', 0);
eq(defN.danio, 50, 'rival Quemado: 30 ataque + 20 de quemadura en el chequeo');

console.log('Alakazam ex — Mind Jack: +30 por cada banca rival');
est = partida(); // autoSetup deja banca rival con varios básicos
const nb = est.lados.B.banca.length;
ponerAtacante(est, 'sv3pt5-65', { name: 'Mind Jack', cost: [], damage: '90', text: '' });
J.atacar(est, 'A', 0);
eq(est.lados.B.activo.danio, 90 + 30 * nb, '90 + 30×' + nb);

console.log('Zapdos ex — Multishot: 120 al Activo + 90 a un banco elegido');
est = partida();
const banco = est.lados.B.banca[0]; const bIid = banco.iid;
ponerAtacante(est, 'sv3pt5-145', { name: 'Multishot Lightning', cost: [], damage: '120', text: '' });
J.atacar(est, 'A', 0);
ok(!!est.pendiente && est.pendiente.tipo === 'elegirObjetivo', 'pausa para elegir banco');
J.resolverEleccion(est, 'A', [bIid]);
eq(banco.danio, 90, 'banco elegido recibió 90');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
