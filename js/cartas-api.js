// =============================================================
//  CAPA DE DATOS EN VIVO
//  - api.pokemontcg.io  -> TODAS las cartas del juego (inglés)
//  - api.tcgdex.net     -> textos en español (ataques/habilidades)
//  Se consulta en el navegador (CORS abierto). Requiere internet.
// =============================================================
const API = 'https://api.pokemontcg.io/v2';
const API_KEY = ''; // sin clave: la API funciona igual (límite más bajo, suficiente para uso personal)
const PAGE_SIZE = 24;
const SELECT = [
  'id','name','supertype','subtypes','hp','types','evolvesFrom','abilities','attacks',
  'weaknesses','resistances','retreatCost','convertedRetreatCost','set','number','artist',
  'rarity','flavorText','nationalPokedexNumbers','images','legalities','rules','regulationMark'
].join(',');

function apiHeaders(){ return API_KEY ? { 'X-Api-Key': API_KEY } : {}; }

// Construye la query Lucene de la API a partir del estado de filtros
function construirQuery(f){
  const p = [];
  if (f.name && f.name.trim()) {
    // Comodín de PREFIJO (term*) — rápido en la API (el comodín inicial *term* es muy lento)
    f.name.trim().split(/\s+/).forEach(t => {
      const limpio = t.replace(/[:"()\[\]*]/g, '');
      if (limpio) p.push('name:' + limpio + '*');
    });
  }
  if (f.type)       p.push('types:' + f.type);
  if (f.supertype)  p.push('supertype:"' + f.supertype + '"');
  if (f.rarity)     p.push('rarity:"' + f.rarity + '"');
  if (f.setId)      p.push('set.id:' + f.setId);
  if (f.subtype)    p.push('subtypes:"' + f.subtype + '"');
  if (f.hpMin || f.hpMax) p.push('hp:[' + (f.hpMin || '0') + ' TO ' + (f.hpMax || '1000') + ']');
  if (f.regMark)    p.push('regulationMark:' + f.regMark);
  if (f.pokedex)    p.push('nationalPokedexNumbers:' + f.pokedex);
  return p.join(' ');
}

// Normaliza una carta de la API al formato que usa la app (igual que cartas-db.js)
function apiCardToView(c){
  const img = c.images || {};
  const set = c.set || {};
  return {
    id: c.id, nombre: c.name, supertipo: c.supertype, fase: (c.subtypes || []).join(', '),
    evolucionaDe: c.evolvesFrom || null, ps: c.hp || null, tipos: c.types || [],
    habilidades: (c.abilities || []).map(a => ({ name: a.name, text: a.text, type: a.type })),
    ataques: (c.attacks || []).map(a => ({ name: a.name, cost: a.cost || [], damage: a.damage, text: a.text })),
    debilidades: c.weaknesses || [], resistencias: c.resistances || [], costoRetirada: c.retreatCost || [],
    rareza: c.rarity || null, ilustrador: c.artist || null, numeroCarta: c.number || null,
    descripcionPokedex: c.flavorText || null, marcaRegulacion: c.regulationMark || null,
    reglas: c.rules || [], imagenChica: img.small || null, imagenGrande: img.large || null,
    set: { id: set.id, nombre: set.name, serie: set.series, releaseDate: set.releaseDate },
    pokedex: c.nationalPokedexNumbers || [], legalidad: c.legalities || {}
  };
}

// ---------- Caché de búsquedas (localStorage, máx 50 consultas) ----------
const QCACHE_KEY = 'ptcg_qcache';
let _qcache = {};
try { _qcache = JSON.parse(localStorage.getItem(QCACHE_KEY) || '{}'); } catch(e){ _qcache = {}; }
function _qkey(f, page){ return JSON.stringify({ q: construirQuery(f), p: page || 1, o: f.orderBy || 'name' }); }
function apiCacheGet(f, page){
  const e = _qcache[_qkey(f, page)];
  return e ? { cards: e.cards, totalCount: e.totalCount, page: e.page, pageSize: e.pageSize, fromCache: true } : null;
}
function _qset(f, page, val){
  _qcache[_qkey(f, page)] = { cards: val.cards, totalCount: val.totalCount, page: val.page, pageSize: val.pageSize, ts: Date.now() };
  const ks = Object.keys(_qcache);
  if (ks.length > 50){ ks.sort((a,b)=>(_qcache[a].ts||0)-(_qcache[b].ts||0)).slice(0, ks.length-50).forEach(k=>delete _qcache[k]); }
  try { localStorage.setItem(QCACHE_KEY, JSON.stringify(_qcache)); }
  catch(e){ _qcache = {}; _qcache[_qkey(f,page)] = { cards: val.cards, totalCount: val.totalCount, page: val.page, pageSize: val.pageSize, ts: Date.now() }; try { localStorage.setItem(QCACHE_KEY, JSON.stringify(_qcache)); } catch(_){} }
}

// ---------- Respaldo: TCGdex (si pokemontcg falla) ----------
// Nombres de tipo que usa TCGdex en español (para filtrar por tipo en modo ES)
const TCGDEX_TIPO_ES = {
  Fire:'Fuego', Water:'Agua', Lightning:'Rayo', Grass:'Planta', Psychic:'Psíquico',
  Fighting:'Lucha', Darkness:'Oscura', Metal:'Metálica', Fairy:'Hada', Dragon:'Dragón', Colorless:'Incolora'
};
function tcgdexToView(c){
  return {
    id: c.id, nombre: c.name, supertipo: '', fase: '', tipos: [], ps: null,
    habilidades: [], ataques: [], debilidades: [], resistencias: [], costoRetirada: [],
    rareza: null, ilustrador: null, numeroCarta: (c.localId != null ? String(c.localId) : null),
    descripcionPokedex: null, marcaRegulacion: null, reglas: [],
    imagenChica: c.image ? c.image + '/low.webp' : null,
    imagenGrande: c.image ? c.image + '/high.webp' : null,
    set: { id: null, nombre: '', serie: null }, pokedex: [], legalidad: {}, fuente: 'tcgdex'
  };
}
async function tcgdexBuscar(f, page, lang){
  page = page || 1; lang = lang || 'en';
  const params = new URLSearchParams();
  if (f.name && f.name.trim()) params.set('name', f.name.trim());
  if (f.type){ params.set('types', (lang === 'es' && TCGDEX_TIPO_ES[f.type]) ? TCGDEX_TIPO_ES[f.type] : f.type); }
  if (f.setId) params.set('set', f.setId);
  params.set('pagination:page', String(page));
  params.set('pagination:itemsPerPage', String(PAGE_SIZE));
  const res = await fetch('https://api.tcgdex.net/v2/' + lang + '/cards?' + params.toString());
  if (!res.ok) throw new Error('TCGdex ' + res.status);
  const arr = await res.json();
  const total = (arr || []).length >= PAGE_SIZE ? (page * PAGE_SIZE + 1) : ((page - 1) * PAGE_SIZE + (arr || []).length);
  const cards = (arr || []).filter(c => c && c.image).map(tcgdexToView);  // solo con imagen (imprimible)
  return { cards, totalCount: total, page, pageSize: PAGE_SIZE, fuente: 'tcgdex' };
}

// Busca cartas: red (pokemontcg) → si falla, caché → si no, respaldo TCGdex
async function apiBuscar(f, page){
  const q = construirQuery(f);
  const params = new URLSearchParams();
  if (q) params.set('q', q);
  params.set('page', page || 1);
  params.set('pageSize', PAGE_SIZE);
  params.set('orderBy', f.orderBy || 'name');
  params.set('select', SELECT);
  try {
    const res = await fetch(API + '/cards?' + params.toString(), { headers: apiHeaders() });
    if (!res.ok) throw new Error('API ' + res.status);
    const data = await res.json();
    const out = {
      cards: (data.data || []).map(apiCardToView),
      totalCount: data.totalCount || 0,
      page: data.page || 1,
      pageSize: data.pageSize || PAGE_SIZE
    };
    _qset(f, page, out);
    return out;
  } catch(e){
    const cached = apiCacheGet(f, page);
    if (cached) return cached;                       // sin conexión: servir de caché
    try { return await tcgdexBuscar(f, page); }      // respaldo: TCGdex
    catch(e2){ throw e; }
  }
}

// ---------- Caché de cartas por id ----------
// Permite rehidratar el inventario sin guardar el objeto completo de cada carta
// (en Firestore/localStorage solo se guarda {id,name,type,qty}).
const CARDCACHE_KEY = 'ptcg_cardcache';
let _cardcache = {};
try { _cardcache = JSON.parse(localStorage.getItem(CARDCACHE_KEY) || '{}'); } catch(e){ _cardcache = {}; }
function cardCacheGet(id){ return (id && _cardcache[id]) ? _cardcache[id] : null; }
function _cardCacheSet(id, view){
  if(!id || !view) return;
  _cardcache[id] = view;
  const ks = Object.keys(_cardcache);
  if(ks.length > 800){ ks.slice(0, ks.length - 800).forEach(k => delete _cardcache[k]); }
  try { localStorage.setItem(CARDCACHE_KEY, JSON.stringify(_cardcache)); }
  catch(e){ _cardcache = {}; _cardcache[id] = view; try { localStorage.setItem(CARDCACHE_KEY, JSON.stringify(_cardcache)); } catch(_){} }
}

// Rehidrata UNA carta por id: caché -> pokemontcg -> null. Cachea el resultado.
const _cardInflight = {};
async function apiCardById(id){
  if(!id) return null;
  const hit = cardCacheGet(id);
  if(hit) return hit;
  if(_cardInflight[id]) return _cardInflight[id];
  const params = new URLSearchParams();
  params.set('q', 'id:"' + id + '"');
  params.set('select', SELECT);
  const p = (async () => {
    try {
      const res = await fetch(API + '/cards?' + params.toString(), { headers: apiHeaders() });
      if(!res.ok) throw new Error('API ' + res.status);
      const data = await res.json();
      const c = (data.data || [])[0];
      const view = c ? apiCardToView(c) : null;
      if(view) _cardCacheSet(id, view);
      return view;
    } catch(e){ return null; }
    finally { delete _cardInflight[id]; }
  })();
  _cardInflight[id] = p;
  return p;
}

// Lista de todos los sets (cacheada) para el desplegable de filtros
let _setsCache = null;
async function cargarSets(){
  if (_setsCache) return _setsCache;
  const res = await fetch(API + '/sets?select=id,name,series,releaseDate', { headers: apiHeaders() });
  const data = await res.json();
  _setsCache = (data.data || []).sort((a, b) => (b.releaseDate || '').localeCompare(a.releaseDate || ''));
  return _setsCache;
}

// Sets de TCGdex (ids que coinciden con su búsqueda), para el filtro de set en modo ES
let _tcgdexSetsCache = {};
async function cargarSetsTcgdex(lang){
  lang = lang || 'es';
  if (_tcgdexSetsCache[lang]) return _tcgdexSetsCache[lang];
  const res = await fetch('https://api.tcgdex.net/v2/' + lang + '/sets');
  const arr = await res.json();
  _tcgdexSetsCache[lang] = (arr || []).map(s => ({ id: s.id, name: s.name })).reverse();
  return _tcgdexSetsCache[lang];
}

// Series de TCGdex (id -> nombre localizado) para traducir la serie de cada set
let _tcgdexSeriesCache = {};
async function cargarSeriesTcgdex(lang){
  lang = lang || 'es';
  if (_tcgdexSeriesCache[lang]) return _tcgdexSeriesCache[lang];
  const res = await fetch('https://api.tcgdex.net/v2/' + lang + '/series');
  const arr = await res.json();
  _tcgdexSeriesCache[lang] = (arr || []).map(s => ({ id: s.id, name: s.name }));
  return _tcgdexSeriesCache[lang];
}

// Id de set de pokemontcg -> id de set de TCGdex (sv3pt5->sv03.5, me4->me04, o el mismo en sets compartidos)
function tcgdexSetIdFor(pokeId){
  if (!pokeId) return pokeId;
  if (TCGDEX_SETS[pokeId]) return TCGDEX_SETS[pokeId];
  const m = /^sv(\d+)(pt5)?$/.exec(pokeId);
  if (m) return 'sv' + String(+m[1]).padStart(2, '0') + (m[2] ? '.5' : '');
  return pokeId;
}

// Lista de sets de TCGdex CON su serie (recorriendo cada serie). Para el desplegable de proxies en ES.
let _tcgdexSetsSerieCache = {};
async function cargarSetsSerieTcgdex(lang){
  lang = lang || 'es';
  if (_tcgdexSetsSerieCache[lang]) return _tcgdexSetsSerieCache[lang];
  const series = await cargarSeriesTcgdex(lang);   // [{id, name}]
  const detalles = await Promise.all(series.map(s =>
    fetch('https://api.tcgdex.net/v2/' + lang + '/series/' + s.id).then(r => r.json()).catch(() => null)
  ));
  const out = [];
  detalles.forEach((d, i) => {
    if (!d) return;
    const serieName = d.name || series[i].name;
    (d.sets || []).forEach(st => out.push({ id: st.id, name: st.name, serie: serieName }));
  });
  out.reverse();   // recientes primero
  _tcgdexSetsSerieCache[lang] = out;
  return out;
}

// ---------- TCGdex: textos en español, en vivo ----------
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
  if (!tset) {
    const m = /^sv(\d+)(pt5)?$/.exec(sid);
    if (m) tset = 'sv' + String(+m[1]).padStart(2, '0') + (m[2] ? '.5' : '');
  }
  if (!tset) return null;
  const n = parseInt(num, 10);
  return isNaN(n) ? (tset + '-' + num) : (tset + '-' + String(n).padStart(3, '0'));
}

// Caché v2: incluye también imagen española y nombre de set (las entradas v1 no los traían).
const ES_CACHE_KEY = 'ptcg_es_cache2';
let _esCache = {};
try { _esCache = JSON.parse(localStorage.getItem(ES_CACHE_KEY) || '{}'); } catch (e) { _esCache = {}; }

async function tcgdexEsLive(id){
  if (!id) return null;
  if (_esCache[id] !== undefined) return _esCache[id];
  const tid = tcgdexId(id);
  if (!tid) { _esCache[id] = null; return null; }
  try {
    const res = await fetch(TCGDEX + '/' + tid);
    if (!res.ok) { _esCache[id] = null; return null; }
    const d = await res.json();
    const es = {
      nombre: d.name,
      habilidades: (d.abilities || []).map(a => ({ name: a.name, text: a.effect })),
      ataques: (d.attacks || []).map(a => ({ name: a.name, text: a.effect })),
      efecto: d.effect || null,
      imagenChica: d.image ? (d.image + '/low.webp') : null,
      imagenGrande: d.image ? (d.image + '/high.webp') : null,
      setNombre: (d.set && d.set.name) || null
    };
    _esCache[id] = es;
    try { localStorage.setItem(ES_CACHE_KEY, JSON.stringify(_esCache)); } catch (e) {}
    return es;
  } catch (e) { return null; }
}

// Asegura el español (nombre + imagen + set) de una lista de vistas y, cuando termina de
// traer las que faltaban, llama onReady() UNA vez. Concurrencia limitada. No-op fuera de ES.
function localizarVistasEs(views, onReady){
  if (typeof lang !== 'undefined' && lang !== 'es') return;
  const faltan = (views || []).filter(v => v && v.id && !v.es);
  if (!faltan.length) return;
  let i = 0, activos = 0, hechos = 0, disparado = false;
  const total = faltan.length;
  const fin = () => { if (!disparado && hechos >= total){ disparado = true; if (typeof onReady === 'function') onReady(); } };
  const lanzar = () => {
    while (activos < 8 && i < total){
      const v = faltan[i++]; activos++;
      tcgdexEsLive(v.id).then(es => { if (es) v.es = es; })
        .catch(()=>{})
        .then(() => { activos--; hechos++; lanzar(); fin(); });
    }
  };
  lanzar();
}
