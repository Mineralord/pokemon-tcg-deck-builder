/* Tests de la Fase 6 (validación de construcción Estándar). node tests/reglas6.test.js */
'use strict';
const J = require('../js/juego.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

function pokeView(id, nombre, marca, fase, reglas) {
  return { id: id, nombre: nombre, supertipo: 'Pokémon', fase: fase || 'Basic', ps: '120', tipos: ['Lightning'],
    evolucionaDe: null, ataques: [{ name: 'Z', cost: ['Lightning'], damage: '30', text: '' }], habilidades: [],
    debilidades: [], resistencias: [], costoRetirada: ['Colorless'], marcaRegulacion: marca, reglas: reglas || [] };
}
function energias(n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(J.energiaBasicaView('Lightning Energy'))); return o; }
function copias(view, n) { const o = []; for (let i = 0; i < n; i++) o.push(J.cartaJuego(view)); return o; }

console.log('mazo legal — 4 Básicos (marca G) + 56 energías básicas');
let mazo = copias(pokeView('p1', 'Pichu', 'G'), 4).concat(energias(56));
let r = J.validarEstandar(mazo);
ok(r.ok, 'mazo legal: ' + JSON.stringify(r.errores));

console.log('más de 4 copias por nombre');
mazo = copias(pokeView('p1', 'Pichu', 'G'), 5).concat(energias(55));
r = J.validarEstandar(mazo);
ok(!r.ok && r.errores.join(' ').indexOf('más de 4') >= 0, 'detecta 5 copias');

console.log('energías básicas ilimitadas (sin tope de 4)');
mazo = copias(pokeView('p1', 'Pichu', 'G'), 2).concat(energias(58));
r = J.validarEstandar(mazo);
ok(r.ok, 'energías básicas no cuentan para el límite de 4');

console.log('marca de regulación no vigente');
mazo = copias(pokeView('p2', 'Viejo', 'D'), 4).concat(energias(56));
r = J.validarEstandar(mazo);
ok(!r.ok && r.errores.join(' ').indexOf('no es legal') >= 0, 'detecta marca D ilegal');
// configurable: si permitimos D, pasa
ok(J.validarEstandar(mazo, { marcasPermitidas: ['D', 'G'] }).ok, 'marcas configurables (rotación)');

console.log('no exactamente 60 cartas');
r = J.validarEstandar(copias(pokeView('p1', 'Pichu', 'G'), 4).concat(energias(50)));
ok(!r.ok && r.errores.join(' ').indexOf('60 cartas') >= 0, 'detecta 54 cartas');

console.log('1 ACE SPEC por mazo');
mazo = copias(pokeView('p1', 'Pichu', 'G'), 4)
  .concat(copias(pokeView('ace', 'Maquina ACE', 'H', 'Item', ['You may play only 1 ACE SPEC card in your deck.']), 2))
  .concat(energias(54));
r = J.validarEstandar(mazo);
ok(!r.ok && r.errores.join(' ').indexOf('ACE SPEC') >= 0, 'detecta 2 ACE SPEC');

console.log('1 Pokémon Radiante por mazo');
mazo = copias(pokeView('p1', 'Pichu', 'G'), 4)
  .concat(copias(pokeView('rad', 'Radiante X', 'G', 'Basic, Radiant'), 2))
  .concat(energias(54));
r = J.validarEstandar(mazo);
ok(!r.ok && r.errores.join(' ').indexOf('Radiante') >= 0, 'detecta 2 Radiantes');

console.log('\n' + (fail === 0 ? '✓ TODO OK' : '✗ FALLOS') + ' — pass=' + pass + ' fail=' + fail);
if (fail > 0) process.exit(1);
