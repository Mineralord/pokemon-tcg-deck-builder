/* patch-es-imagenes.js — Añade al DB local (data/cartas-db.js) las IMÁGENES en
   español (es.imagenChica / es.imagenGrande / es.setNombre) y completa nombre/
   ataques/habilidades/efecto en español desde TCGdex. Idempotente: solo agrega
   campos que falten. Ejecutar: node tools/patch-es-imagenes.js
   Las imágenes de las cartas en inglés venían de no tener la URL española. */
'use strict';
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, '..', 'data', 'cartas-db.js');
const TCGDEX = 'https://api.tcgdex.net/v2/es/cards';

const TCGDEX_SETS = {
  sv1:'sv01', sv2:'sv02', sv3:'sv03', sv3pt5:'sv03.5', sv4:'sv04', sv4pt5:'sv04.5',
  sv5:'sv05', sv6:'sv06', sv6pt5:'sv06.5', sv7:'sv07', sv8:'sv08', sv8pt5:'sv08.5',
  sv9:'sv09', sv10:'sv10', svp:'svp', me1:'me01', me2:'me02', me2pt5:'me02.5', me3:'me03', me4:'me04'
};
function tcgdexId(pid){
  if (!pid || pid.indexOf('-') < 0) return null;
  const i = pid.lastIndexOf('-');
  const sid = pid.slice(0, i), num = pid.slice(i + 1);
  let tset = TCGDEX_SETS[sid];
  if (!tset) { const m = /^sv(\d+)(pt5)?$/.exec(sid); if (m) tset = 'sv' + String(+m[1]).padStart(2, '0') + (m[2] ? '.5' : ''); }
  if (tset) { const n = parseInt(num, 10); return isNaN(n) ? (tset + '-' + num) : (tset + '-' + String(n).padStart(3, '0')); }
  return pid;
}
const ENERGIA_ES = { Grass:'Energía Planta', Fire:'Energía Fuego', Water:'Energía Agua', Lightning:'Energía Rayo',
  Psychic:'Energía Psíquica', Fighting:'Energía Lucha', Darkness:'Energía Oscura', Metal:'Energía Metal',
  Fairy:'Energía Hada', Dragon:'Energía Dragón', Colorless:'Energía Incolora' };

function cargarDB(){
  const win = {}; const sandbox = { window: win };
  const code = fs.readFileSync(DB_PATH, 'utf8');
  // El archivo hace `window.CARTAS_DB = {...};`
  new Function('window', code)(win);
  return win.CARTAS_DB;
}

async function fetchEs(tid){
  try {
    const r = await fetch(TCGDEX + '/' + tid, { headers: { 'User-Agent': 'patch-es' } });
    if (!r.ok) return null;
    return await r.json();
  } catch (e) { return null; }
}

async function main(){
  const db = cargarDB();
  const cartas = db.cartas || [];
  let imgAdd = 0, txtAdd = 0, fail = 0, energia = 0;

  // Energías básicas: nombre ES garantizado (no hay imagen en TCGdex por id-energy).
  cartas.forEach(c => {
    if (/^energy-basic-/.test(c.id || '') || (/energ/i.test(c.supertipo||'') && /basic/i.test(c.fase||''))){
      const t = (c.tipos && c.tipos[0]) || 'Colorless';
      c.es = c.es || {};
      if (!c.es.nombre) { c.es.nombre = ENERGIA_ES[t] || c.nombre; energia++; }
    }
  });

  const objetivo = cartas.filter(c => !(c.es && c.es.imagenChica));
  let i = 0;
  const N = 8;
  async function worker(){
    while (i < objetivo.length){
      const c = objetivo[i++];
      const tid = tcgdexId(c.id);
      if (!tid) { continue; }
      const d = await fetchEs(tid);
      if (!d) { fail++; continue; }
      c.es = c.es || {};
      if (d.image){ c.es.imagenChica = d.image + '/low.webp'; c.es.imagenGrande = d.image + '/high.webp'; imgAdd++; }
      if (!c.es.nombre && d.name) c.es.nombre = d.name;
      if (d.set && d.set.name) c.es.setNombre = d.set.name;
      // completar textos si faltan
      if (Array.isArray(d.attacks)){
        c.es.ataques = c.es.ataques && c.es.ataques.length ? c.es.ataques : [];
        d.attacks.forEach((a, k) => {
          c.es.ataques[k] = c.es.ataques[k] || { name: a.name, text: a.effect || null };
          if (c.es.ataques[k].text == null && a.effect) { c.es.ataques[k].text = a.effect; txtAdd++; }
          if (!c.es.ataques[k].name && a.name) c.es.ataques[k].name = a.name;
        });
      }
      if (Array.isArray(d.abilities) && d.abilities.length){
        c.es.habilidades = c.es.habilidades && c.es.habilidades.length ? c.es.habilidades : d.abilities.map(a => ({ name: a.name, text: a.effect || null }));
      }
      if (d.effect && !c.es.efecto) c.es.efecto = d.effect;
    }
  }
  await Promise.all(Array.from({ length: N }, worker));

  db.generadoEl = db.generadoEl + ' (+es-img ' + new Date().toISOString().slice(0,10) + ')';
  fs.writeFileSync(DB_PATH, 'window.CARTAS_DB = ' + JSON.stringify(db, null, 2) + ';\n', 'utf8');
  console.log('Imágenes ES añadidas:', imgAdd, '| textos ES añadidos:', txtAdd, '| energías:', energia, '| fallos:', fail, '| total cartas:', cartas.length);
  const conImg = cartas.filter(c => c.es && c.es.imagenChica).length;
  console.log('Cartas con imagen ES ahora:', conImg, '/', cartas.length);
}
main();
