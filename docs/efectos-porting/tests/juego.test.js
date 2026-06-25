/* Tests del motor del juego (sin DOM). Ejecutar: node tests/juego.test.js */
'use strict';
const J = require('../js/juego.js');

let pass = 0, fail = 0;
function ok(cond, msg) { if (cond) { pass++; } else { fail++; console.error('  ✗ ' + msg); } }
function eq(a, b, msg) { ok(a === b, msg + ' (esperaba ' + JSON.stringify(b) + ', fue ' + JSON.stringify(a) + ')'); }

// --- Vistas de ejemplo (forma de data/cartas-db.js) ---
const pikachuEx = {
  id: 'svp-106', nombre: 'Pikachu ex', supertipo: 'Pokémon', fase: 'Basic, ex', ps: '200',
  tipos: ['Lightning'], evolucionaDe: null,
  ataques: [{ name: 'Thunderbolt', cost: ['Lightning', 'Lightning', 'Colorless'], convertedEnergyCost: 3, damage: '120', text: '' }],
  habilidades: [],
  debilidades: [{ type: 'Fighting', value: '×2' }],
  resistencias: [],
  costoRetirada: ['Colorless', 'Colorless'],
  reglas: ['Pokémon ex rule: When your Pokémon ex is Knocked Out, your opponent takes 2 Prize cards.'],
  imagenChica: 'x'
};
const raichu = {
  id: 'sv1-100', nombre: 'Raichu', supertipo: 'Pokémon', fase: 'Stage 1', ps: '120',
  tipos: ['Lightning'], evolucionaDe: 'Pikachu',
  ataques: [{ name: 'Zap', cost: ['Lightning'], damage: '30+', text: 'more' }],
  debilidades: [{ type: 'Fighting', value: '×2' }],
  resistencias: [{ type: 'Metal', value: '-30' }],
  costoRetirada: ['Colorless']
};
const lightningEnergy = { id: 'sve-4', nombre: 'Lightning Energy', supertipo: 'Energy', tipos: ['Lightning'] };

console.log('cartaJuego — Pokémon ex');
let p = J.cartaJuego(pikachuEx);
eq(p.supertipo, 'Pokemon', 'supertipo Pokemon');
eq(p.hp, 200, 'hp 200');
eq(p.esBasico, true, 'es básico');
eq(p.stage, 0, 'stage 0');
eq(p.ataques[0].costeN, 3, 'coste 3');
eq(p.ataques[0].danio, 120, 'daño 120');
eq(p.debilidad.tipo, 'Fighting', 'debilidad tipo');
eq(p.debilidad.mult, 2, 'debilidad ×2');
eq(p.retirada, 2, 'retirada 2');
eq(p.premiosKO, 2, 'premios 2 (ex desde reglas)');

console.log('cartaJuego — Stage 1 + resistencia + daño "30+"');
let r = J.cartaJuego(raichu);
eq(r.stage, 1, 'stage 1');
eq(r.esBasico, false, 'no básico (evoluciona)');
eq(r.evolucionaDe, 'Pikachu', 'evoluciona de Pikachu');
eq(r.resistencia.resta, 30, 'resistencia -30');
eq(r.ataques[0].danio, 30, 'daño base 30');
eq(r.ataques[0].mas, true, 'flag "+" en 30+');
eq(r.premiosKO, 1, 'premios 1 (normal)');

console.log('cartaJuego — Energía');
let e = J.cartaJuego(lightningEnergy);
eq(e.supertipo, 'Energy', 'energía');
eq(e.energiaTipo, 'Lightning', 'tipo de energía');

console.log('energía básica sintetizada');
eq(J.tipoEnergia('Fire Energy'), 'Fire', 'Fire');
eq(J.tipoEnergia('Energía Rayo'), 'Lightning', 'Rayo->Lightning');
eq(J.cartaJuego(J.energiaBasicaView('Water Energy')).energiaTipo, 'Water', 'Water básica');

console.log('expandirMazo + validarMazoJugable (60 cartas)');
const deck = {
  pokemon: [{ card: 'Pikachu ex', id: 'svp-106', qty: 4 }, { card: 'Raichu', id: 'sv1-100', qty: 4 }],
  trainers: [{ card: 'X', id: 't1', qty: 10 }],
  energies: [{ card: 'Lightning Energy', qty: 42 }]
};
const byId = { 'svp-106': pikachuEx, 'sv1-100': raichu, 't1': { id: 't1', nombre: 'X', supertipo: 'Trainer' } };
const resolver = function (c) { return byId[c.id] || null; };
const cartas = J.expandirMazo(deck, resolver);
eq(cartas.length, 60, 'expande a 60 cartas');
const val = J.validarMazoJugable(deck, resolver);
eq(val.total, 60, 'validación total 60');
ok(val.basicos >= 4, 'tiene básicos (' + val.basicos + ')');
eq(val.faltan.length, 0, 'sin Pokémon faltos de datos');
eq(val.ok, true, 'mazo jugable');

// ---------- Fase 3: preparación de partida ----------
function mazo60(resolver) {
  // 12 Pokémon básicos + resto entrenadores/energía hasta 60 (garantiza básicos en mano).
  return {
    pokemon: [{ card: 'Pikachu ex', id: 'svp-106', qty: 12 }],
    trainers: [{ card: 'X', id: 't1', qty: 18 }],
    energies: [{ card: 'Lightning Energy', qty: 30 }]
  };
}
const cartas60 = J.expandirMazo(mazo60(), resolver);
eq(cartas60.length, 60, 'mazo de prueba = 60');

console.log('crearPartida — reparto y determinismo');
const p1 = J.crearPartida({ ladoA: { nombre: 'Tú', cartas: cartas60 }, ladoB: { nombre: 'Rival', cartas: cartas60 }, seed: 42 });
const p2 = J.crearPartida({ ladoA: { nombre: 'Tú', cartas: cartas60 }, ladoB: { nombre: 'Rival', cartas: cartas60 }, seed: 42 });
eq(p1.lados.A.mano.length >= 7, true, 'mano inicial >= 7');
eq(p1.lados.A.mano[0].iid, p2.lados.A.mano[0].iid, 'misma semilla -> mismo reparto (determinista)');
eq(J.tieneBasico(p1.lados.A.mano), true, 'mano A tiene básico');
eq(J.totalCartasLado(p1.lados.A), 60, 'conservación: 60 cartas lado A');

console.log('colocar Activo / Banca + confirmar setup');
const bA = p1.lados.A.mano.filter(function (c) { return c.supertipo === 'Pokemon' && c.esBasico; });
J.colocarActivo(p1, 'A', bA[0].iid);
eq(p1.lados.A.activo && p1.lados.A.activo.iid, bA[0].iid, 'activo colocado');
if (bA[1]) J.colocarBanca(p1, 'A', bA[1].iid);
const manoAntes = p1.lados.A.mano.length;
J.quitarColocado(p1, 'A', bA[0].iid);
eq(p1.lados.A.activo, null, 'quitar activo lo devuelve a la mano');
eq(p1.lados.A.mano.length, manoAntes + 1, 'la mano recupera la carta');
J.colocarActivo(p1, 'A', bA[0].iid);
J.confirmarSetup(p1, 'A');
eq(p1.lados.A.premios.length, 6, 'A coloca 6 premios al confirmar');
eq(p1.lados.A.estado, 'ready', 'A queda listo');
eq(J.totalCartasLado(p1.lados.A), 60, 'conservación tras premios');

console.log('autoSetup del rival arranca la partida');
J.autoSetup(p1, 'B');
eq(p1.lados.B.estado, 'ready', 'B listo (auto)');
eq(p1.lados.B.premios.length, 6, 'B con 6 premios');
eq(p1.fase, J.FASE.MAIN, 'ambos listos -> arranca en MAIN (ya robó)');
ok(p1.turnoDe === 'A' || p1.turnoDe === 'B', 'turnoDe definido tras setup');
eq(p1.turno, 1, 'turno 1');

// ---------- Fase 4: estructura de turno ----------
console.log('estructura de turno');
const ini = p1.inicia, otro = ini === 'A' ? 'B' : 'A';
eq(p1.turnoDe, ini, 'empieza el ganador de la moneda');
eq(J.puedeAtacar(p1), false, 'el que empieza NO ataca en su primer turno');
const manoIniAntes = p1.lados[otro].mano.length;
J.terminarTurno(p1);
eq(p1.turnoDe, otro, 'terminarTurno pasa al rival');
eq(p1.turno, 2, 'turno 2');
eq(p1.fase, J.FASE.MAIN, 'el rival arranca en MAIN');
eq(p1.lados[otro].mano.length, manoIniAntes + 1, 'el rival robó 1 al empezar su turno');
eq(J.puedeAtacar(p1), true, 'el segundo jugador SÍ puede atacar en su turno 1');
J.terminarTurno(p1);
eq(p1.turnoDe, ini, 'vuelve al inicial');
eq(J.puedeAtacar(p1), true, 'el inicial ya puede atacar en su 2º turno');

console.log('derrota por mazo vacío (deck-out)');
const p3 = J.crearPartida({ ladoA: { nombre: 'Tú', cartas: cartas60 }, ladoB: { nombre: 'Rival', cartas: cartas60 }, seed: 7 });
J.autoSetup(p3, 'A'); J.autoSetup(p3, 'B');
const enTurno = p3.turnoDe, rival = enTurno === 'A' ? 'B' : 'A';
p3.lados[rival].mazo = []; // el rival se quedará sin cartas al empezar su turno
J.terminarTurno(p3);
eq(p3.ganador, enTurno, 'gana quien forzó el deck-out del rival');
eq(p3.fase, J.FASE.END, 'fase END al terminar');

// ---------- Fase 5: acciones principales ----------
console.log('acciones del turno: banca, energía, evolución, retirada');
const pikachu = { id: 'x-1', nombre: 'Pikachu', supertipo: 'Pokémon', fase: 'Basic', ps: '60', tipos: ['Lightning'], ataques: [{ name: 'a', cost: ['Lightning'], damage: '10' }], costoRetirada: ['Colorless'] };
let _id = 0;
function inst(view, iid) { const c = J.cartaJuego(view); c.iid = iid || ('i' + (_id++)); c.energias = []; c.danio = 0; c.condiciones = []; return c; }
function escena() {
  const e = J.crearPartida({ ladoA: { cartas: cartas60 }, ladoB: { cartas: cartas60 }, seed: 1 });
  J.autoSetup(e, 'A'); J.autoSetup(e, 'B');
  e.turnoDe = 'A'; e.fase = J.FASE.MAIN; e.turno = 3; e.ganador = null;
  e.lados.A.turnosJugados = 1; e.lados.A.energiaUsada = false; e.lados.A.retiroUsado = false;
  const act = inst(pikachu, 'act'); act.enJuegoDesde = 0;
  e.lados.A.activo = act; e.lados.A.banca = [];
  e.lados.A.mano = [inst(lightningEnergy, 'e1'), inst(raichu, 'rc1'), inst(pikachu, 'pk2'), inst(lightningEnergy, 'e2')];
  return e;
}

let e1 = escena();
J.ponerEnBanca(e1, 'A', 'pk2');
eq(e1.lados.A.banca.length, 1, 'poner básico en banca');
eq(e1.lados.A.mano.length, 3, 'la mano baja a 3');

let e2 = escena();
J.adjuntarEnergia(e2, 'A', 'e1', 'act');
eq(e2.lados.A.activo.energias.length, 1, 'energía adjuntada al activo');
eq(e2.lados.A.energiaUsada, true, 'energiaUsada=true');
J.adjuntarEnergia(e2, 'A', 'e2', 'act');
eq(e2.lados.A.activo.energias.length, 1, 'segunda energía bloqueada (1 por turno)');

let e3 = escena();
J.adjuntarEnergia(e3, 'A', 'e1', 'act');     // 1 energía en el activo Pikachu
J.evolucionar(e3, 'A', 'rc1', 'act');
eq(e3.lados.A.activo.nombre, 'Raichu', 'el activo evoluciona a Raichu');
eq(e3.lados.A.activo.energias.length, 1, 'la evolución conserva la energía');
eq((e3.lados.A.activo.debajo || []).length >= 1, true, 'queda Pikachu debajo');

let e4 = escena();
e4.lados.A.turnosJugados = 0;                // primer turno: no se evoluciona
J.evolucionar(e4, 'A', 'rc1', 'act');
eq(e4.lados.A.activo.nombre, 'Pikachu', 'no evoluciona en el primer turno');

let e5 = escena();
J.adjuntarEnergia(e5, 'A', 'e1', 'act');     // Pikachu retirada=1 -> necesita 1 energía
e5.lados.A.banca = [inst(pikachu, 'b1')];
J.retirar(e5, 'A', 'b1');
eq(e5.lados.A.activo.iid, 'b1', 'el de banca pasa a Activo al retirar');
eq(e5.lados.A.retiroUsado, true, 'retiroUsado=true');
eq(e5.lados.A.descarte.length, 1, 'se descarta 1 energía por el coste de retirada');

// ---------- Fase 6: ataque ----------
console.log('ataque: coste, debilidad/resistencia, KO, premios, victoria');
function combate(defView, bancaDef) {
  const e = escena();
  const atk = inst(pikachu, 'atk'); atk.enJuegoDesde = 0; atk.energias = [inst(lightningEnergy, 'le')];
  e.lados.A.activo = atk; e.lados.A.turnosJugados = 1;
  const def = inst(defView, 'def'); e.lados.B.activo = def;
  e.lados.B.banca = bancaDef || [];
  return e;
}
// puedePagar
ok(J.puedePagar({ energias: [{ energiaTipo: 'Lightning' }] }, ['Lightning']), 'paga coste tipado');
ok(!J.puedePagar({ energias: [] }, ['Lightning']), 'sin energía no paga');
ok(J.puedePagar({ energias: [{ energiaTipo: 'Water' }] }, ['Colorless']), 'incoloro lo paga cualquiera');

// debilidad ×2 -> 10 daño *2 = 20, KO sobre 20 HP
let c1 = combate(Object.assign({}, pikachu, { ps: '20', debilidades: [{ type: 'Lightning', value: '×2' }] }), [inst(pikachu, 'bb')]);
J.atacar(c1, 'A', 0);
eq(c1.lados.B.activo.iid, 'bb', 'tras KO, sube el de banca a Activo');
eq(c1.lados.A.premios.length, 5, 'el atacante toma 1 premio (6->5)');
eq(c1.lados.B.descarte.length >= 1, true, 'el KO va al descarte');
eq(c1.turnoDe, 'B', 'atacar termina el turno');

// sin debilidad: 10 daño, sin KO sobre 100 HP
let c2 = combate(Object.assign({}, pikachu, { ps: '100' }), [inst(pikachu, 'bb')]);
J.atacar(c2, 'A', 0);
eq(c2.lados.B.activo.danio, 10, 'daño base 10 aplicado');
eq(c2.lados.B.activo.iid, 'def', 'sin KO sigue el mismo Activo');

// resistencia -20 con un ataque de 30
const golpe30 = { id: 'g', nombre: 'Golpeador', supertipo: 'Pokémon', fase: 'Basic', ps: '90', tipos: ['Lightning'], ataques: [{ name: 'big', cost: ['Lightning'], damage: '30' }] };
let c3 = escena();
const a3 = inst(golpe30, 'a3'); a3.enJuegoDesde = 0; a3.energias = [inst(lightningEnergy, 'le3')];
c3.lados.A.activo = a3; c3.lados.A.turnosJugados = 1;
c3.lados.B.activo = inst(Object.assign({}, pikachu, { ps: '100', resistencias: [{ type: 'Lightning', value: '-20' }] }), 'd3');
c3.lados.B.banca = [inst(pikachu, 'bb3')];
J.atacar(c3, 'A', 0);
eq(c3.lados.B.activo.danio, 10, 'resistencia -20 reduce 30 a 10');

// energía insuficiente -> no hace nada
let c4 = combate(Object.assign({}, pikachu, { ps: '100' }), []);
c4.lados.A.activo.energias = []; // quitamos energía
J.atacar(c4, 'A', 0);
eq(c4.lados.B.activo.danio || 0, 0, 'sin energía no ataca');
eq(c4.turnoDe, 'A', 'no se gasta el turno si el ataque es ilegal');

// victoria por premios: 1 premio restante, KO da el último
let c5 = combate(Object.assign({}, pikachu, { ps: '20', debilidades: [{ type: 'Lightning', value: '×2' }] }), [inst(pikachu, 'bb5')]);
c5.lados.A.premios = c5.lados.A.premios.slice(0, 1);
J.atacar(c5, 'A', 0);
eq(c5.ganador, 'A', 'gana al tomar el último premio');
eq(c5.fase, J.FASE.END, 'fase END');

// victoria por dejar al rival sin Pokémon
let c6 = combate(Object.assign({}, pikachu, { ps: '20', debilidades: [{ type: 'Lightning', value: '×2' }] }), []);
J.atacar(c6, 'A', 0);
eq(c6.ganador, 'A', 'gana si el rival se queda sin Pokémon');
eq(c6.motivoFin, 'sinpokemon', 'motivo sinpokemon');

// ---------- Fase 7: condiciones especiales ----------
console.log('condiciones especiales: chequeo, bloqueos, confusión');
const heads = function () { return true; };
const tails = function () { return false; };

// Veneno: +10 en el chequeo
let s1 = escena(); s1.lados.A.activo.hp = 100;
J.aplicarCondicion(s1, 'A', 'poisoned');
J.chequeo(s1, 'A', heads);
eq(s1.lados.A.activo.danio, 10, 'veneno suma 10 en el chequeo');

// Quemado: +20 siempre; cara -> se cura
let s2 = escena(); s2.lados.A.activo.hp = 100;
J.aplicarCondicion(s2, 'A', 'burned');
J.chequeo(s2, 'A', heads);
eq(s2.lados.A.activo.danio, 20, 'quemado suma 20');
eq(s2.lados.A.activo.condiciones.indexOf('burned'), -1, 'cara cura el quemado');
let s2b = escena(); s2b.lados.A.activo.hp = 100;
J.aplicarCondicion(s2b, 'A', 'burned');
J.chequeo(s2b, 'A', tails);
eq(s2b.lados.A.activo.condiciones.indexOf('burned') >= 0, true, 'cruz mantiene el quemado');

// Dormido bloquea ataque; cara despierta en el chequeo
let s3 = escena(); s3.lados.A.activo.energias = [inst(lightningEnergy, 'le')];
J.aplicarCondicion(s3, 'A', 'asleep');
eq(J.puedeAtacar(s3), false, 'dormido no puede atacar');
J.chequeo(s3, 'A', heads);
eq(s3.lados.A.activo.condiciones.indexOf('asleep'), -1, 'cara despierta');

// Paralizado: no ataca y se cura tras el turno del dueño
let s4 = escena(); s4.lados.A.activo.energias = [inst(lightningEnergy, 'le')];
J.aplicarCondicion(s4, 'A', 'paralyzed');
eq(J.puedeAtacar(s4), false, 'paralizado no puede atacar');
J.chequeo(s4, 'A', heads);
eq(s4.lados.A.activo.condiciones.indexOf('paralyzed'), -1, 'se cura tras su turno');

// Rotativas: se reemplazan entre sí
let s5 = escena();
J.aplicarCondicion(s5, 'A', 'asleep'); J.aplicarCondicion(s5, 'A', 'paralyzed');
eq(s5.lados.A.activo.condiciones.indexOf('asleep'), -1, 'paralizar reemplaza dormir');
eq(s5.lados.A.activo.condiciones.indexOf('paralyzed') >= 0, true, 'queda paralizado');

// Confusión: cruz -> ataque falla y +30 a sí mismo
let s6 = combate(Object.assign({}, pikachu, { ps: '100' }), [inst(pikachu, 'bb')]);
J.aplicarCondicion(s6, 'A', 'confused');
J.atacar(s6, 'A', 0, tails);
eq(s6.lados.A.activo.danio, 30, 'confusión (cruz): +30 a sí mismo');
eq(s6.lados.B.activo.danio || 0, 0, 'cruz: el rival no recibe daño');
eq(s6.turnoDe, 'B', 'el ataque confuso termina el turno');

// ---------- Fase 8a: motor de efectos (auto-intérprete + manual) ----------
console.log('efectos: auto-intérprete y respaldo manual');
let h1 = escena(); h1.lados.B.activo = inst(pikachu, 'd');
J.efectoAuto(h1, 'A', { texto: "Your opponent's Active Pokémon is now Asleep." });
eq(h1.lados.B.activo.condiciones.indexOf('asleep') >= 0, true, 'auto: aplica Dormido al rival');

let h2 = escena(); h2.lados.A.activo.danio = 30;
J.efectoAuto(h2, 'A', { texto: 'Heal 30 damage from this Pokémon.' });
eq(h2.lados.A.activo.danio, 0, 'auto: cura 30');

let h3 = escena(); const m0 = h3.lados.A.mano.length;
J.efectoAuto(h3, 'A', { texto: 'Draw 2 cards.' });
eq(h3.lados.A.mano.length, m0 + 2, 'auto: roba 2');

let h4 = escena(); h4.lados.B.activo = inst(Object.assign({}, pikachu, { ps: '100' }), 'd4');
J.manualDanioRival(h4, 'A', 30);
eq(h4.lados.B.activo.danio, 30, 'manual: pone 30 de daño al rival');
J.manualCondicionRival(h4, 'A', 'poisoned');
eq(h4.lados.B.activo.condiciones.indexOf('poisoned') >= 0, true, 'manual: envenena al rival');
h4.lados.A.activo.danio = 50; J.manualCurar(h4, 'A', 20);
eq(h4.lados.A.activo.danio, 30, 'manual: cura 20 propio');

// ---------- Fase 8b: jugar Entrenadores ----------
console.log('entrenadores: subtipo, límites y efecto');
const nemona = { id: 'tr-n', nombre: 'Nemona', supertipo: 'Trainer', reglas: ['Draw 3 cards.', 'You may play only 1 Supporter card during your turn.'] };
const greatball = { id: 'tr-g', nombre: 'Great Ball', supertipo: 'Trainer', reglas: ['Look at the top 7 cards of your deck...', 'You may play any number of Item cards during your turn.'] };
const stad = { id: 'tr-s', nombre: 'Arena X', supertipo: 'Trainer', reglas: ['Some effect.', 'You may play only 1 Stadium card during your turn.'] };
eq(J.cartaJuego(nemona).subTrainer, 'supporter', 'Nemona = Supporter');
eq(J.cartaJuego(greatball).subTrainer, 'item', 'Great Ball = Item');
eq(J.cartaJuego(stad).subTrainer, 'stadium', 'Arena = Stadium');
ok(J.cartaJuego(nemona).texto.indexOf('Draw 3') >= 0, 'texto del entrenador capturado');

let t1 = escena();
t1.lados.A.mano = [inst(nemona, 'n1'), inst(nemona, 'n2'), inst(greatball, 'g1')];
const m1 = t1.lados.A.mano.length;
J.jugarEntrenador(t1, 'A', 'n1');
eq(t1.lados.A.supporterUsado, true, 'supporterUsado tras jugar Nemona');
eq(t1.lados.A.descarte.indexOf(t1.lados.A.descarte.find(function (c) { return c.id === 'tr-n'; })) >= 0, true, 'Nemona al descarte');
// jugó 1 (-1) y robó 3 (+3) => mano +2
eq(t1.lados.A.mano.length, m1 - 1 + 3, 'Nemona roba 3');
const m2 = t1.lados.A.mano.length;
J.jugarEntrenador(t1, 'A', 'n2');
eq(t1.lados.A.mano.length, m2, 'segundo Supporter bloqueado (1/turno)');

// Supporter bloqueado en el primer turno del que empieza
let t2 = escena(); t2.inicia = 'A'; t2.lados.A.turnosJugados = 0;
t2.lados.A.mano = [inst(nemona, 'n3')];
J.jugarEntrenador(t2, 'A', 'n3');
eq(t2.lados.A.supporterUsado, false, 'el que empieza no juega Supporter en su 1er turno');

// Estadio reemplaza
let t3 = escena(); t3.lados.A.mano = [inst(stad, 's1')];
J.jugarEntrenador(t3, 'A', 's1');
eq(t3.lados.A.estadio && t3.lados.A.estadio.id, 'tr-s', 'Estadio queda en juego');

// ---------- Fase 8d: auto-intérprete ampliado ----------
console.log('auto-intérprete ampliado: monedas, daño variable, retroceso, descarte');
function escd() { const e = escena(); e.lados.B.activo = inst(Object.assign({}, pikachu, { ps: '500' }), 'dd'); e.lados.B.banca = [inst(pikachu, 'b1'), inst(pikachu, 'b2')]; return e; }
const alwaysH = function () { return true; };
const seq = function (arr) { let i = 0; return function () { return arr[i++]; }; };

let d1 = escd(); J.efectoAuto(d1, 'A', { texto: 'Flip a coin. If heads, this attack does 20 more damage.' }, alwaysH);
eq(d1.lados.B.activo.danio, 20, 'moneda cara: +20 daño');

let d2 = escd(); J.efectoAuto(d2, 'A', { texto: 'Flip a coin until you get tails. This attack does 30 damage for each heads.' }, seq([true, true, false]));
eq(d2.lados.B.activo.danio, 60, 'racha 2 caras x30 = 60');

let d3 = escd(); J.efectoAuto(d3, 'A', { texto: "This attack does 30 more damage for each of your opponent's Benched Pokémon." });
eq(d3.lados.B.activo.danio, 60, '30 x 2 en banca = 60');

let d4 = escd(); d4.lados.A.activo.danio = 30;
J.efectoAuto(d4, 'A', { texto: 'This attack does 30 more damage for each damage counter on this Pokémon.' });
eq(d4.lados.B.activo.danio, 90, '30 x 3 contadores = 90');

let d5 = escd(); J.efectoAuto(d5, 'A', { texto: 'This Pokémon also does 30 damage to itself.' });
eq(d5.lados.A.activo.danio, 30, 'retroceso 30 a sí mismo');

let d6 = escd(); d6.lados.A.activo.energias = [inst(lightningEnergy, 'x1'), inst(lightningEnergy, 'x2')];
J.efectoAuto(d6, 'A', { texto: 'Discard an Energy from this Pokémon.' });
eq(d6.lados.A.activo.energias.length, 1, 'descarta 1 energía propia');
eq(d6.lados.A.descarte.length, 1, 'energía al descarte');

let d7 = escd(); J.efectoAuto(d7, 'A', { texto: 'Flip a coin. If heads, your opponent\'s Active Pokémon is now Paralyzed.' }, alwaysH);
eq(d7.lados.B.activo.condiciones.indexOf('paralyzed') >= 0, true, 'moneda cara: paraliza (sin doble aplicación)');

// ---------- Fase 12: IA ----------
console.log('IA: juega su turno y ataca');
const IA = require('../js/juego-ia.js');
let ia1 = J.crearPartida({ ladoA: { cartas: cartas60 }, ladoB: { cartas: cartas60 }, seed: 5 });
J.autoSetup(ia1, 'A'); J.autoSetup(ia1, 'B');
ia1.turnoDe = 'B'; ia1.fase = J.FASE.MAIN; ia1.turno = 4; ia1.ganador = null;
ia1.lados.B.turnosJugados = 1; ia1.lados.B.energiaUsada = false;
const bat = inst(pikachu, 'bteam'); bat.enJuegoDesde = 0; bat.energias = [inst(lightningEnergy, 'le')];
ia1.lados.B.activo = bat;
ia1.lados.A.activo = inst(Object.assign({}, pikachu, { ps: '100' }), 'ahead');
ia1.lados.A.banca = [inst(pikachu, 'abk')];
IA.configurar({ dificultad: 'medio', modo: 'reglas' });
IA.jugarTurno(ia1);
eq(ia1.turnoDe, 'A', 'la IA terminó su turno (pasa a A)');
ok((ia1.lados.A.activo.danio || 0) > 0 || ia1.lados.A.activo.iid !== 'ahead', 'la IA atacó al jugador');

// IA sin energía: no puede atacar pero igual termina el turno
let ia2 = J.crearPartida({ ladoA: { cartas: cartas60 }, ladoB: { cartas: cartas60 }, seed: 6 });
J.autoSetup(ia2, 'A'); J.autoSetup(ia2, 'B');
ia2.turnoDe = 'B'; ia2.fase = J.FASE.MAIN; ia2.turno = 4; ia2.lados.B.turnosJugados = 1;
ia2.lados.B.activo = inst(pikachu, 'b2'); ia2.lados.B.activo.energias = []; ia2.lados.B.mano = [];
ia2.lados.A.activo = inst(pikachu, 'a2'); ia2.lados.A.banca = [inst(pikachu, 'a2b')];
IA.jugarTurno(ia2);
eq(ia2.turnoDe, 'A', 'la IA termina turno aunque no pueda atacar');

// ---------- Fase 13: rendirse ----------
console.log('rendirse');
let rr = escena();
J.rendirse(rr, 'A');
eq(rr.ganador, 'B', 'al rendirse A, gana B');
eq(rr.motivoFin, 'rendicion', 'motivo rendicion');

console.log('\n' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
