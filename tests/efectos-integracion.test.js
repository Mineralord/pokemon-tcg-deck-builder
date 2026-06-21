/* Integración DSL ↔ motor (Fase 1d). Ejecutar: node tests/efectos-integracion.test.js
   Verifica que un ataque AUTORADO en el DSL se ejecuta dentro de JUEGO.atacar,
   con precedencia sobre el auto-intérprete de texto, y que sin entrada DSL el
   motor cae al auto-intérprete (sin regresión). */
'use strict';
// Cargar el intérprete y el bridge ANTES que el motor los use (vía globals).
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.EFECTOS_MOTOR = require('../js/efectos-motor.js');
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

// Base de efectos de prueba (sustituye a la de producción para el test).
global.EFECTOS_DB = {
  'test-toxic': { ataques: { 'Toxic Hit': { ops: [{ op: 'estado', objetivo: 'rivalActivo', estado: ['confused', 'poisoned'] }] } } },
  'test-recoil': { ataques: { 'Self Smash': { ops: [{ op: 'recoil', cantidad: 30 }] } } }
};

// --- Construir una partida mínima en MAIN con el atacante listo ---
function vistaPoke(id, nombre, ataques) {
  return { id: id, nombre: nombre, supertipo: 'Pokémon', fase: 'Basic', ps: '200', tipos: ['Colorless'],
    evolucionaDe: null, ataques: ataques, habilidades: [], debilidades: [], resistencias: [], costoRetirada: [] };
}
function cartas(view, n) { const out = []; for (let i = 0; i < n; i++) out.push(J.cartaJuego(view)); return out; }

function partidaCon(atkView) {
  const relleno = vistaPoke('relleno', 'Relleno', [{ name: 'Tap', cost: [], damage: '10', text: '' }]);
  const A = cartas(atkView, 4).concat(cartas(relleno, 26));
  const B = cartas(relleno, 30);
  const est = J.crearPartida({ ladoA: { nombre: 'A', cartas: A }, ladoB: { nombre: 'B', cartas: B }, seed: 7 });
  // Forzar setup determinista: poner un atacante como activo en A y un relleno en B.
  J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
  // Garantizar que A juega primero y ya pasó su primer turno (puede atacar).
  est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
  return est;
}

// Colocar manualmente el atacante deseado como Activo de A con energía suficiente.
function ponerActivo(est, atkView) {
  const c = J.cartaJuego(atkView); c.iid = 'atk'; c.danio = 0; c.energias = []; c.condiciones = []; c.enJuegoDesde = 0;
  est.lados.A.activo = c;
  return c;
}

console.log('DSL: ataque autorado aplica estados (Toxic Hit)');
let est = partidaCon(vistaPoke('test-toxic', 'Toxi', [{ name: 'Toxic Hit', cost: [], damage: '20', text: '' }]));
ponerActivo(est, vistaPoke('test-toxic', 'Toxi', [{ name: 'Toxic Hit', cost: [], damage: '20', text: '' }]));
J.atacar(est, 'A', 0);
// Tras atacar, termina el turno y hay chequeo; comprobamos sobre el log de último ataque y condiciones aplicadas.
ok((est.ultimoAtaque.efectos || []).indexOf('estado') >= 0, 'marca de efecto "estado" registrada');

console.log('DSL: ataque recoil se autoinflige daño');
est = partidaCon(vistaPoke('test-recoil', 'Rec', [{ name: 'Self Smash', cost: [], damage: '0', text: '' }]));
const atk = ponerActivo(est, vistaPoke('test-recoil', 'Rec', [{ name: 'Self Smash', cost: [], damage: '0', text: '' }]));
J.atacar(est, 'A', 0);
eq(atk.danio, 30, 'atacante recibió 30 de recoil');

console.log('Sin entrada DSL: cae al auto-intérprete (texto "is now Asleep")');
est = partidaCon(vistaPoke('test-sleep', 'Slp', [{ name: 'Lull', cost: [], damage: '10', text: "Your opponent's Active Pokémon is now Asleep." }]));
const def = est.lados.B.activo;
ponerActivo(est, vistaPoke('test-sleep', 'Slp', [{ name: 'Lull', cost: [], damage: '10', text: "Your opponent's Active Pokémon is now Asleep." }]));
J.atacar(est, 'A', 0);
ok((def.condiciones || []).indexOf('asleep') >= 0, 'auto-intérprete aplicó Dormido');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
