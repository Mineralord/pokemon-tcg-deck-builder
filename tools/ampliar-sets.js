/* ampliar-sets.js — Descarga sets COMPLETOS (pokemontcg.io) con su español
   (TCGdex: nombre, textos e IMÁGENES) y los fusiona en data/cartas-db.js sin
   duplicar (por id) y sin tocar las cartas ya existentes.
   Uso: node tools/ampliar-sets.js sv3pt5 sv8 sv5
   Sin argumentos usa la lista SETS_DEFECTO. Datos factuales de juego vía APIs
   públicas, para uso offline de la app.  */
'use strict';
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, '..', 'data', 'cartas-db.js');
const PTCG = 'https://api.pokemontcg.io/v2/cards';
const TCGDEX = 'https://api.tcgdex.net/v2/es/cards';
const SETS_DEFECTO = ['sv3pt5']; // 151 (se amplía por argv)

const TCGDEX_SETS = {
  sv1:'sv01', sv2:'sv02', sv3:'sv03', sv3pt5:'sv03.5', sv4:'sv04', sv4pt5:'sv04.5',
  sv5:'sv05', sv6:'sv06', sv6pt5:'sv06.5', sv7:'sv07', sv8:'sv08', sv8pt5:'sv08.5',
  sv9:'sv09', sv10:'sv10', svp:'svp', me1:'me01', me2:'me02', me2pt5:'me02.5', me3:'me03', me4:'me04'
};
function tcgdexId(pid){
  if (!pid || pid.indexOf('-') < 0) return null;
  const i = pid.lastIndexOf('-'); const sid = pid.slice(0, i), num = pid.slice(i + 1);
  let tset = TCGDEX_SETS[sid];
  if (!tset){ const m=/^sv(\d+)(pt5)?$/.exec(sid); if(m) tset='sv'+String(+m[1]).padStart(2,'0')+(m[2]?'.5':''); }
  if (tset){ const n=parseInt(num,10); return isNaN(n)?(tset+'-'+num):(tset+'-'+String(n).padStart(3,'0')); }
  return pid;
}
function cantidadDe(c){
  const f=((c.subtypes||[]).join(' ')).toLowerCase();
  if(/\b(ex|gx|vstar|vmax|v-union|radiant|ace spec)\b/.test(f) || /\bv\b/.test(f)) return 2;
  return 4;
}
function apiToDB(c){
  const img=c.images||{}, set=c.set||{}, setImg=set.images||{};
  return {
    id:c.id, nombre:c.name, cantidad:cantidadDe(c), supertipo:c.supertype,
    fase:(c.subtypes||[]).join(', '), evolucionaDe:c.evolvesFrom||null, ps:c.hp||null,
    tipos:c.types||[],
    habilidades:(c.abilities||[]).map(a=>({name:a.name,text:a.text,type:a.type})),
    ataques:(c.attacks||[]).map(a=>({name:a.name,cost:a.cost||[],convertedEnergyCost:a.convertedEnergyCost,damage:a.damage,text:a.text})),
    debilidades:c.weaknesses||[], resistencias:c.resistances||[], costoRetirada:c.retreatCost||[],
    retiradaConvertida:(c.convertedRetreatCost!=null)?c.convertedRetreatCost:(c.retreatCost||[]).length,
    descripcionPokedex:c.flavorText||null, numeroPokedex:c.nationalPokedexNumbers||[],
    numeroCarta:c.number||null, rareza:c.rarity||null, ilustrador:c.artist||null,
    marcaRegulacion:c.regulationMark||null, reglas:c.rules||[],
    imagenChica:img.small||null, imagenGrande:img.large||null,
    set:{ nombre:set.name, serie:set.series, simbolo:setImg.symbol, logo:setImg.logo },
    legalidad:c.legalities||{}, es:null
  };
}
function cargarDB(){
  const win={}; new Function('window', fs.readFileSync(DB_PATH,'utf8'))(win); return win.CARTAS_DB;
}
async function jget(url){
  for(let i=0;i<3;i++){ try{ const r=await fetch(url,{headers:{'User-Agent':'ampliar-sets'}}); if(r.ok) return await r.json(); if(r.status===404) return null; }catch(e){} await new Promise(s=>setTimeout(s,1500)); }
  return null;
}
async function descargarSet(sid){
  let page=1, out=[];
  while(true){
    const j=await jget(PTCG+'?q=set.id:'+sid+'&pageSize=250&page='+page);
    if(!j || !j.data || !j.data.length) break;
    out=out.concat(j.data);
    if(j.data.length<250) break; page++;
  }
  return out;
}
async function esDe(id){
  const tid=tcgdexId(id); if(!tid) return null;
  const d=await jget(TCGDEX+'/'+tid); if(!d) return null;
  return {
    nombre:d.name,
    habilidades:(d.abilities||[]).map(a=>({name:a.name,text:a.effect||null})),
    ataques:(d.attacks||[]).map(a=>({name:a.name,text:a.effect||null})),
    efecto:d.effect||null,
    imagenChica:d.image?(d.image+'/low.webp'):null,
    imagenGrande:d.image?(d.image+'/high.webp'):null,
    setNombre:(d.set&&d.set.name)||null
  };
}
async function main(){
  const sets=process.argv.slice(2).length?process.argv.slice(2):SETS_DEFECTO;
  const db=cargarDB();
  const existentes=new Set(db.cartas.map(c=>c.id));
  console.log('Sets a añadir:', sets.join(', '));
  let nuevas=[];
  for(const sid of sets){
    const cartas=await descargarSet(sid);
    const add=cartas.filter(c=>!existentes.has(c.id)).map(apiToDB);
    console.log('  '+sid+': '+cartas.length+' en API, '+add.length+' nuevas');
    nuevas=nuevas.concat(add);
  }
  // Español (con imágenes) en paralelo.
  let i=0; const N=8;
  await Promise.all(Array.from({length:N},async()=>{ while(i<nuevas.length){ const c=nuevas[i++]; c.es=await esDe(c.id); } }));
  db.cartas=db.cartas.concat(nuevas);
  db.totalCartas=db.cartas.length;
  db.generadoEl=(db.generadoEl||'')+' (+sets '+sets.join('/')+' '+new Date().toISOString().slice(0,10)+')';
  fs.writeFileSync(DB_PATH,'window.CARTAS_DB = '+JSON.stringify(db,null,2)+';\n','utf8');
  const conImg=nuevas.filter(c=>c.es&&c.es.imagenChica).length;
  console.log('Añadidas '+nuevas.length+' cartas ('+conImg+' con imagen ES). Total DB: '+db.totalCartas);
}
main();
