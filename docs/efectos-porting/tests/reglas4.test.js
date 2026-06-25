/* Tests de la Fase 4 (KO de banca + restricciones de habilidad). node tests/reglas4.test.js */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.EFECTOS_MOTOR = require('../js/efectos-motor.js');
global.EFECTOS_PASIVOS = require('../js/efectos-pasivos.js');
const J = require('../js/juego.js');
global.JUEGO_EFECTOS = require('../js/juego-efectos.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

global.EFECTOS_DB = {
  'snp': { ataques: { 'Snipe': { ops: [{ op: 'danio', objetivo: 'rivalBanca', cantidad: 200 }] } } },
  'abil': { habilidades: { 'Draw Engine': { unaVezPorTurno: true, ops: [{ op: 'robar', cantidad: 1 }] } } },
  'abil2': { habilidades: { 'Front Only': { soloActivo: true, ops: [{ op: 'robar', cantidad: 1 }] } } },
  'glue': { pasivos: [{ mod: 'noRetira', a: 'esteP' }] }
};

function vista(id, nombre, ataques, habilidades) {
  return { id: id, nombre: nombre, supertipo: 'Pokémon', fase: 'Basic', ps: '200', tipos: ['Colorless'],
    evolucionaDe: null, ataques: ataques || [{ name: 'Tap', cost: [], damage: '10', text: '' }],
    habilidades: habilidades || [], debilidades: [], resistencias: [], costoRetirada: [] };
}
function cartas(v, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(v)); return o; }
function partida() {
  const rel = vista('relleno', 'Relleno');
  const est = J.crearPartida({ ladoA: { nombre: 'A', cartas: cartas(rel, 30) }, ladoB: { nombre: 'B', cartas: cartas(rel, 30) }, seed: 5 });
  J.autoSetup(est, 'A'); J.autoSetup(est, 'B');
  est.inicia = 'A'; est.turnoDe = 'A'; est.fase = J.FASE.MAIN; est.lados.A.turnosJugados = 1;
  return est;
}
function ponerActivo(est, lado, v) {
  const c = J.cartaJuego(v); c.iid = lado + '-act'; c.danio = 0; c.energias = []; c.condiciones = []; c.enJuegoDesde = 0;
  est.lados[lado].activo = c; return c;
}

console.log('snipe — KO en banca otorga premio al atacante');
let est = partida();
ponerActivo(est, 'A', vista('snp', 'Sniper', [{ name: 'Snipe', cost: [], damage: '0', text: '' }]));
// Banca rival con un Pokémon de 60 PS.
const benchB = J.cartaJuego(vista('weak', 'Weakling', [{ name: 't', cost: [], damage: '0', text: '' }]));
benchB.id = 'weak'; benchB.hp = 60; benchB.iid = 'B-bench'; benchB.danio = 0; benchB.energias = []; benchB.condiciones = [];
est.lados.B.banca = [benchB];
const premiosAntes = est.lados.A.premios.length;
J.atacar(est, 'A', 0);
eq(est.lados.B.banca.length, 0, 'la banca rival quedó KO y vacía');
eq(est.lados.A.premios.length, premiosAntes - 1, 'el atacante tomó 1 premio por el KO de banca');

console.log('habilidad unaVezPorTurno — solo una vez por turno, se reinicia al siguiente');
est = partida();
const ab = ponerActivo(est, 'A', vista('abil', 'Engine', [{ name: 'Tap', cost: [], damage: '10', text: '' }], [{ name: 'Draw Engine', text: '' }]));
let mano0 = est.lados.A.mano.length;
J.usarHabilidad(est, 'A', ab.iid, 0);
eq(est.lados.A.mano.length, mano0 + 1, 'primera vez: roba 1');
J.usarHabilidad(est, 'A', ab.iid, 0);
eq(est.lados.A.mano.length, mano0 + 1, 'segunda vez: bloqueada (sin robar)');
// Avanzar a un nuevo turno propio (A -> B -> A) y volver a usar.
J.terminarTurno(est); // pasa a B
est.fase = J.FASE.MAIN; est.turnoDe = 'B'; J.terminarTurno(est); // vuelve a A, reinicia habUsadas
ok(!est.ganador, 'sin ganador');
mano0 = est.lados.A.mano.length;
est.fase = J.FASE.MAIN; est.turnoDe = 'A';
J.usarHabilidad(est, 'A', ab.iid, 0);
eq(est.lados.A.mano.length, mano0 + 1, 'nuevo turno: vuelve a poder usarse');

console.log('habilidad soloActivo — bloqueada desde la banca');
est = partida();
const front = J.cartaJuego(vista('abil2', 'Front', [{ name: 'Tap', cost: [], damage: '10', text: '' }], [{ name: 'Front Only', text: '' }]));
front.iid = 'A-front'; front.danio = 0; front.energias = []; front.condiciones = [];
est.lados.A.banca = [front];
const m0 = est.lados.A.mano.length;
J.usarHabilidad(est, 'A', front.iid, 0);
eq(est.lados.A.mano.length, m0, 'en banca: no se activa');

console.log('KO simultáneo del último Pokémon de ambos lados → empate');
function lethal() { const c = J.cartaJuego(vista('z', 'Z')); c.hp = 100; c.danio = 120; c.energias = []; c.condiciones = []; c.premiosKO = 1; return c; }
function premios(n) { const a = []; for (let i = 0; i < n; i++) a.push({}); return a; }
let estE = { seed: 1, rngN: 0, turno: 3, turnoDe: 'A', inicia: 'A', ganador: null, motivoFin: null, fase: J.FASE.MAIN,
  lados: { A: { activo: lethal(), banca: [], descarte: [], premios: premios(6), lost: [], mazo: [{}], mano: [], turnosJugados: 1, estadio: null },
           B: { activo: lethal(), banca: [], descarte: [], premios: premios(6), lost: [], mazo: [{}], mano: [], turnosJugados: 1, estadio: null } } };
J.terminarTurno(estE);
eq(estE.ganador, 'empate', 'resultado empate');
eq(estE.motivoFin, 'empate', 'motivo empate');

console.log('noRetira — un Pokémon con el efecto no puede retirarse');
const glue = J.cartaJuego(vista('glue', 'Glue')); glue.iid = 'A-glue'; glue.retirada = 1;
glue.energias = [{ supertipo: 'Energy', energiaTipo: 'Colorless' }]; glue.condiciones = [];
const bn = J.cartaJuego(vista('b', 'Banco')); bn.iid = 'A-bn';
let estR = { fase: J.FASE.MAIN, turnoDe: 'A', ganador: null,
  lados: { A: { activo: glue, banca: [bn], descarte: [], estadio: null, retiroUsado: false }, B: { activo: J.cartaJuego(vista('o', 'O')), banca: [], descarte: [], estadio: null } } };
J.retirar(estR, 'A', bn.iid);
eq(estR.lados.A.activo.iid, 'A-glue', 'el activo NO cambió (no puede retirarse)');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
