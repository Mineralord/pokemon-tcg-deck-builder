/* cobertura-efectos.js — Reporte de cobertura de efectos sobre la colección.
   Clasifica ataques/habilidades/entrenadores en: autorado (DSL), solo-daño
   (sin texto, lo cubre el daño base), o pendiente (tiene texto y no está autorado).
   Ejecutar: node tools/cobertura-efectos.js  */
'use strict';
global.EFECTOS_DSL = require('../js/efectos-dsl.js');
global.window = {}; require('../data/cartas-db.js');
const DB = require('../data/efectos-db.js');
const cartas = global.window.CARTAS_DB.cartas;

function tieneAtaque(id, nombre) { const e = DB[id]; return !!(e && e.ataques && e.ataques[nombre]); }
function tieneHab(id, nombre) { const e = DB[id]; return !!(e && (e.habilidades && e.habilidades[nombre] || (e.pasivos && e.pasivos.length))); }
function tieneJugar(id) { const e = DB[id]; return !!(e && (e.jugar || (e.pasivos && e.pasivos.length))); }

let atk = { solo: 0, autorado: 0, pendiente: 0 }, hab = { autorado: 0, pendiente: 0 }, tr = { autorado: 0, pendiente: 0 };
const pendientesAtk = {}, pendientesTr = {};

cartas.forEach(c => {
  const sup = (c.supertipo || '').toLowerCase();
  if (sup.indexOf('pok') >= 0) {
    (c.ataques || []).forEach(a => {
      const txt = (a.text || '').trim();
      if (!txt) atk.solo++;
      else if (tieneAtaque(c.id, a.name)) atk.autorado++;
      else { atk.pendiente++; const k = (c.nombre + ' · ' + a.name); pendientesAtk[k] = (pendientesAtk[k] || 0) + 1; }
    });
    (c.habilidades || []).forEach(h => {
      if (tieneHab(c.id, h.name)) hab.autorado++; else hab.pendiente++;
    });
  } else if (sup.indexOf('train') >= 0) {
    if (tieneJugar(c.id)) tr.autorado++; else { tr.pendiente++; pendientesTr[c.nombre] = (pendientesTr[c.nombre] || 0) + 1; }
  }
});

function pct(n, t) { return t ? Math.round(n * 100 / t) + '%' : '—'; }
const atkTot = atk.solo + atk.autorado + atk.pendiente;
console.log('=== COBERTURA DE EFECTOS (colección: ' + cartas.length + ' cartas) ===\n');
console.log('ATAQUES (' + atkTot + '):');
console.log('  solo daño (sin texto, cubierto) : ' + atk.solo + '  ' + pct(atk.solo, atkTot));
console.log('  autorados (DSL)                 : ' + atk.autorado + '  ' + pct(atk.autorado, atkTot));
console.log('  pendientes (con texto)          : ' + atk.pendiente + '  ' + pct(atk.pendiente, atkTot));
console.log('  => cubiertos sin manual         : ' + pct(atk.solo + atk.autorado, atkTot));
console.log('\nHABILIDADES (' + (hab.autorado + hab.pendiente) + '): autoradas ' + hab.autorado + ' | pendientes ' + hab.pendiente);
console.log('ENTRENADORES (' + (tr.autorado + tr.pendiente) + '): autorados ' + tr.autorado + ' | pendientes ' + tr.pendiente);

const topTr = Object.keys(pendientesTr).sort().slice(0, 20);
console.log('\nEntrenadores pendientes (' + Object.keys(pendientesTr).length + '): ' + topTr.join(', '));
