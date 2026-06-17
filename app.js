// ===================== STATE =====================
let inventory = JSON.parse(localStorage.getItem('ptcg_inventory') || '[]');
let savedDecks = JSON.parse(localStorage.getItem('ptcg_decks') || '[]');

// ===================== BASE DE DATOS DE CARTAS (cartas-db.js) =====================
const CARD_DB = {};
function normName(s){ return (s || '').toLowerCase().trim().replace(/[‘’]/g, "'"); }
(function buildCardDB(){
  const data = (window.CARTAS_DB && window.CARTAS_DB.cartas) || [];
  data.forEach(c => { CARD_DB[normName(c.nombre)] = c; });
})();
function getCardData(name){ return CARD_DB[normName(name)] || null; }
function esc(s){ return (s == null ? '' : String(s)).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
// energyIcon() y los símbolos de energía están en energias.js

// ===================== REGISTRO DE CARTAS / DETALLE =====================
// Registro id -> vista de carta (para abrir el detalle desde la cuadrícula)
const cardRegistry = {};
function regCard(v){ if(v && v.id) cardRegistry[v.id] = v; return v; }
function jsq(s){ return String(s == null ? '' : s).replace(/\\/g, '\\\\').replace(/'/g, "\\'"); }

// Devuelve la "vista" (datos para mostrar) de una entrada del inventario
function viewFromEntry(e){
  let d;
  if (e.card) d = e.card;
  else { d = getCardData(e.name); if (d && !d.id) d.id = e.id; }
  if (!d) d = { id: e.id, nombre: e.name, tipos: [], ataques: [], habilidades: [] };
  return (typeof enriquecerEnergia === 'function') ? enriquecerEnergia(d) : d;
}

// Deriva el "type" interno (Pokemon-Fire, Trainer, Energy-Lightning…) desde la vista
function deriveType(v){
  const st = (v.supertipo || '').toLowerCase();
  const t = (v.tipos && v.tipos[0]) || 'Colorless';
  if (st.indexOf('trainer') >= 0 || st.indexOf('entrenad') >= 0) return 'Trainer';
  if (st.indexOf('energy') >= 0 || st.indexOf('energ') >= 0) return TYPE_COLORS['Energy-' + t] ? ('Energy-' + t) : 'Energy-Colorless';
  return TYPE_COLORS['Pokemon-' + t] ? ('Pokemon-' + t) : 'Pokemon-Colorless';
}

let currentDetailId = null;
let replaceTargetId = null;   // id de la carta de la colección que se va a reemplazar

function showCardById(id){ const v = cardRegistry[id]; if (v) renderCardDetail(v); }
function showCardDetail(name){
  const d = getCardData(name);
  if(!d){ showToast(T('no_data')+' "'+name+'". '+T('no_data2'), 'error'); return; }
  renderCardDetail(d);
}

function renderCardDetail(d){
  regCard(d);
  currentDetailId = d.id || null;
  const useEs = (lang === 'es' && d.es);
  const es = d.es || {};
  const habEs = es.habilidades || [];
  const atkEs = es.ataques || [];
  const titulo = (useEs && es.nombre) ? es.nombre : d.nombre;
  document.getElementById('modal-title').textContent = titulo;
  if(typeof enriquecerEnergia === 'function') enriquecerEnergia(d);
  const img = d.imagenGrande || d.imagenChica;
  let html = '<div class="card-detail">';
  if(img) html += `<div class="cd-img"><img src="${esc(img)}" alt="${esc(titulo)}" loading="lazy"></div>`;
  html += '<div class="cd-info">';
  const chips = [];
  if(d.ps) chips.push('❤️ '+d.ps+' '+T('d_ps'));
  if(d.supertipo) chips.push(trSuper(d.supertipo));
  if(d.fase) chips.push(trPhase(d.fase));
  if(d.rareza) chips.push('⭐ '+trRarity(d.rareza));
  let chipsHtml = chips.map(c=>`<span class="cd-chip">${esc(c)}</span>`).join('');
  if((d.tipos||[]).length){
    chipsHtml += `<span class="cd-chip">${esc(T('d_type'))}: ${d.tipos.map(t=>energyIcon(t)+' '+esc(trType(t))).join(' ')}</span>`;
  }
  if(chipsHtml) html += `<div class="cd-chips">${chipsHtml}</div>`;
  if(d.evolucionaDe) html += `<div class="cd-line">${T('d_evolves')} <b>${esc(d.evolucionaDe)}</b></div>`;
  (d.habilidades||[]).forEach((a, i) => {
    const nm = (useEs && habEs[i] && habEs[i].name) ? habEs[i].name : (a.name||'');
    const tx = (useEs && habEs[i] && habEs[i].text != null) ? habEs[i].text : (a.text||'');
    html += `<div class="cd-block"><div class="cd-h">🟣 ${T('d_ability')}: ${esc(nm)}</div><div class="cd-t">${esc(tx)}</div></div>`;
  });
  (d.ataques||[]).forEach((a, i) => {
    const cost = (a.cost||[]).map(energyIcon).join('') || '—';
    const nm = (useEs && atkEs[i] && atkEs[i].name) ? atkEs[i].name : (a.name||'');
    const tx = (useEs && atkEs[i] && atkEs[i].text != null) ? atkEs[i].text : (a.text||'');
    html += `<div class="cd-block"><div class="cd-h">${cost} <b>${esc(nm)}</b>${a.damage?(' <span class="cd-dmg">'+esc(a.damage)+'</span>'):''}</div>${tx?('<div class="cd-t">'+esc(tx)+'</div>'):''}</div>`;
  });
  const wr = [];
  (d.debilidades||[]).forEach(w => wr.push(T('d_weak')+': '+energyIcon(w.type)+' '+esc(trType(w.type))+' '+esc(w.value||'')));
  (d.resistencias||[]).forEach(w => wr.push(T('d_resist')+': '+energyIcon(w.type)+' '+esc(trType(w.type))+' '+esc(w.value||'')));
  if((d.costoRetirada||[]).length) wr.push(T('d_retreat')+': '+d.costoRetirada.map(energyIcon).join(''));
  if(wr.length) html += `<div class="cd-line">${wr.join(' · ')}</div>`;
  const reglas = (useEs && es.efecto) ? [es.efecto] : (d.reglas||[]).map(trRule);
  reglas.forEach(r => html += `<div class="cd-rule">${esc(r)}</div>`);
  if(d.descripcionPokedex) html += `<div class="cd-flavor">${esc(d.descripcionPokedex)}</div>`;
  const setn = (d.set && d.set.nombre) ? d.set.nombre : '';
  if(setn || d.numeroCarta) html += `<div class="cd-set">${esc(setn)}${d.numeroCarta?(' · '+T('d_no')+' '+esc(d.numeroCarta)):''}${d.ilustrador?(' · '+T('d_illus')+' '+esc(d.ilustrador)):''}</div>`;

  // Control para añadir / quitar de la colección (si la carta tiene id)
  if(d.id){
    const owned = inventory.find(x => x.id === d.id);
    const ownedQty = owned ? owned.qty : 0;
    const enReemplazo = !!replaceTargetId;
    html += `<div class="cd-add">
      <span class="owned-line">${T('cd_owned')}: <b id="cd-owned">${ownedQty}</b></span>
      ${enReemplazo ? '' : `<div class="qty-stepper">
        <button onclick="cdQty(-1)">−</button>
        <input id="cd-qty" type="number" min="1" value="1">
        <button onclick="cdQty(1)">+</button>
      </div>`}
      <button class="btn-add" onclick="agregarDesdeDetalle()">${enReemplazo ? T('rep_use') : T('cd_add')}</button>
      ${(ownedQty && !enReemplazo) ? `<button class="btn-rep" onclick="iniciarReemplazo('${esc(jsq(d.id))}')">${T('rep_btn')}</button>` : ''}
      ${(ownedQty && !enReemplazo) ? `<button class="btn-del" onclick="quitarDesdeDetalle()">${T('cd_remove1')}</button>` : ''}
    </div>`;
  }
  html += '</div></div>';
  document.getElementById('modal-body').innerHTML = html;
  document.getElementById('modal-overlay').classList.add('open');

  // Español en vivo (TCGdex) si falta y estamos en modo ES
  if(lang === 'es' && !d.es && d.id && typeof tcgdexEsLive === 'function'){
    tcgdexEsLive(d.id).then(es => {
      if(es){ d.es = es; if(currentDetailId === d.id) renderCardDetail(d); }
    });
  }
}

function cdQty(delta){
  const inp = document.getElementById('cd-qty'); if(!inp) return;
  inp.value = Math.max(1, (parseInt(inp.value)||1) + delta);
}
function agregarDesdeDetalle(){
  const v = cardRegistry[currentDetailId]; if(!v) return;
  if(replaceTargetId){ accionAgregar(v, 1); return; }  // en modo reemplazo, sustituye
  const qty = parseInt((document.getElementById('cd-qty')||{}).value) || 1;
  agregarPorId(v, qty);
  renderCardDetail(v); // refresca el contador "en tu colección"
}
function quitarDesdeDetalle(){
  cambiarCantidad(currentDetailId, -1);
  const v = cardRegistry[currentDetailId]; if(v) renderCardDetail(v);
}

// ===================== AÑADIR / QUITAR POR ID (versión exacta) =====================
function agregarPorId(view, qty){
  qty = parseInt(qty) || 1;
  regCard(view);
  const ex = inventory.find(x => x.id === view.id);
  if(ex){ ex.qty += qty; }
  else inventory.push({ id: view.id, name: view.nombre, type: deriveType(view), qty, card: view });
  save(); renderInventory(); renderLegal();
  showToast(`${T('to_added')}: ${view.nombre} ×${qty}`, 'success');
}
function cambiarCantidad(id, delta){
  const e = inventory.find(x => x.id === id); if(!e) return;
  const nq = e.qty + delta;
  if(nq <= 0){
    const v = regCard(viewFromEntry(e));
    if(!confirm(`${T('cf_remove')} "${v.nombre}"?`)) return;  // confirmación al eliminar
    inventory = inventory.filter(x => x.id !== id);
  } else {
    e.qty = nq;
  }
  save(); renderInventory(); renderLegal();
}
function quitarPorId(id){
  inventory = inventory.filter(x => x.id !== id);
  save(); renderInventory(); renderLegal();
  showToast(T('to_removed'), 'success');
}
// Eliminar una carta entera (con confirmación)
function pedirEliminar(id){
  const e = inventory.find(x => x.id === id); if(!e) return;
  const v = regCard(viewFromEntry(e));
  if(confirm(`${T('cf_remove')} "${v.nombre}"?`)) quitarPorId(id);
}

// ===================== REEMPLAZAR UNA CARTA POR OTRA =====================
function iniciarReemplazo(id){
  const e = inventory.find(x => x.id === id); if(!e) return;
  replaceTargetId = id;
  const v = regCard(viewFromEntry(e));
  const banner = document.getElementById('replace-banner');
  if(banner){
    banner.style.display = '';
    banner.innerHTML = `${T('rep_choosing')}: <b>${esc(v.nombre)}</b> (×${e.qty}). ${T('rep_hint')}<button onclick="cancelarReemplazo()">${T('rep_cancel')}</button>`;
  }
  document.getElementById('modal-overlay').classList.remove('open');
  showView('explorar');
}
function cancelarReemplazo(){
  replaceTargetId = null;
  const b = document.getElementById('replace-banner'); if(b) b.style.display = 'none';
  renderExploradorGrid();
}
function confirmarReemplazo(newView){
  const old = inventory.find(x => x.id === replaceTargetId);
  if(!old){ cancelarReemplazo(); return; }
  if(old.id === newView.id){ showToast(T('rep_same'), 'error'); return; }
  const oldView = regCard(viewFromEntry(old));
  if(!confirm(`${T('rep_confirm')}\n\n${oldView.nombre}  →  ${newView.nombre}\n${T('rep_keepqty')}: ×${old.qty}`)) return;
  const qty = old.qty;
  inventory = inventory.filter(x => x.id !== old.id);
  const ex = inventory.find(x => x.id === newView.id);
  if(ex) ex.qty += qty;
  else inventory.push({ id: newView.id, name: newView.nombre, type: deriveType(newView), qty, card: newView });
  regCard(newView);
  save();
  cancelarReemplazo();
  renderInventory(); renderLegal();
  showToast(`${T('rep_done')}: ${oldView.nombre} → ${newView.nombre}`, 'success');
  showView('coleccion');
}
// Punto único: en modo reemplazo sustituye; si no, añade
function accionAgregar(view, qty){
  if(!view) return;
  if(replaceTargetId) confirmarReemplazo(view);
  else agregarPorId(view, qty);
}

// Evolutionary chains for legality check
const evoChains = {
  // Gen 1
  'Ivysaur': ['Bulbasaur'], 'Venusaur': ['Bulbasaur','Ivysaur'], 'Venusaur ex': ['Bulbasaur','Ivysaur'],
  'Charmeleon': ['Charmander'], 'Charizard': ['Charmander','Charmeleon'], 'Charizard ex': ['Charmander','Charmeleon'],
  'Wartortle': ['Squirtle'], 'Blastoise': ['Squirtle','Wartortle'], 'Blastoise ex': ['Squirtle','Wartortle'],
  'Metapod': ['Caterpie'], 'Butterfree': ['Caterpie','Metapod'],
  'Kakuna': ['Weedle'], 'Beedrill': ['Weedle','Kakuna'],
  'Pidgeotto': ['Pidgey'], 'Pidgeot': ['Pidgey','Pidgeotto'],
  'Raticate': ['Rattata'],
  'Fearow': ['Spearow'],
  'Arbok': ['Ekans'], 'Arbok ex': ['Ekans'],
  'Raichu': ['Pikachu'],
  'Sandslash': ['Sandshrew'],
  'Nidorina': ['Nidoran♀'], 'Nidoqueen': ['Nidoran♀','Nidorina'],
  'Nidorino': ['Nidoran♂'], 'Nidoking': ['Nidoran♂','Nidorino'],
  'Clefable': ['Clefairy'],
  'Ninetales': ['Vulpix'], 'Ninetales ex': ['Vulpix'],
  'Wigglytuff': ['Jigglypuff'], 'Wigglytuff ex': ['Jigglypuff'],
  'Golbat': ['Zubat'],
  'Gloom': ['Oddish'], 'Vileplume': ['Oddish','Gloom'],
  'Parasect': ['Paras'],
  'Venomoth': ['Venonat'],
  'Dugtrio': ['Diglett'],
  'Persian': ['Meowth'],
  'Golduck': ['Psyduck'],
  'Primeape': ['Mankey'],
  'Arcanine': ['Growlithe'],
  'Poliwhirl': ['Poliwag'], 'Poliwrath': ['Poliwag','Poliwhirl'],
  'Kadabra': ['Abra'], 'Alakazam': ['Abra','Kadabra'], 'Alakazam ex': ['Abra','Kadabra'],
  'Machoke': ['Machop'], 'Machamp': ['Machop','Machoke'],
  'Weepinbell': ['Bellsprout'], 'Victreebel': ['Bellsprout','Weepinbell'],
  'Tentacruel': ['Tentacool'],
  'Graveler': ['Geodude'], 'Golem': ['Geodude','Graveler'], 'Golem ex': ['Geodude','Graveler'],
  'Rapidash': ['Ponyta'],
  'Slowbro': ['Slowpoke'],
  'Magneton': ['Magnemite'],
  'Dodrio': ['Doduo'],
  'Dewgong': ['Seel'],
  'Muk': ['Grimer'],
  'Cloyster': ['Shellder'],
  'Haunter': ['Gastly'], 'Gengar': ['Gastly','Haunter'],
  'Hypno': ['Drowzee'],
  'Kingler': ['Krabby'],
  'Electrode': ['Voltorb'],
  'Exeggutor': ['Exeggcute'],
  'Marowak': ['Cubone'],
  'Weezing': ['Koffing'],
  'Rhydon': ['Rhyhorn'],
  'Seadra': ['Horsea'],
  'Seaking': ['Goldeen'],
  'Starmie': ['Staryu'],
  'Vaporeon': ['Eevee'], 'Jolteon': ['Eevee'], 'Flareon': ['Eevee'],
  'Omastar': ['Omanyte'],
  'Kabutops': ['Kabuto'],
  'Dragonair': ['Dratini'], 'Dragonite': ['Dratini','Dragonair'],
  // Gen 5+
  'Flaaffy': ['Mareep'], 'Ampharos': ['Mareep','Flaaffy'],
  'Houndoom': ['Houndour'],
  'Volcarona': ['Larvesta'],
  'Crocalor': ['Fuecoco'], 'Skeledirge': ['Fuecoco','Crocalor'],
  'Armarouge': ['Charcadet'], 'Armarouge ex': ['Charcadet'],
  'Bisharp': ['Pawniard'], 'Kingambit': ['Pawniard','Bisharp'],
  'Salazzle': ['Salandit'],
  'Kilowattrel': ['Wattrel'],
};

const TYPE_COLORS = {
  'Pokemon-Fire': 'fire', 'Pokemon-Water': 'water', 'Pokemon-Lightning': 'electric',
  'Pokemon-Grass': 'grass', 'Pokemon-Psychic': 'psychic', 'Pokemon-Darkness': 'dark',
  'Pokemon-Colorless': 'colorless', 'Pokemon-Fighting': 'colorless', 'Pokemon-Metal': 'colorless',
  'Trainer': 'trainer',
  'Energy-Fire': 'fire', 'Energy-Water': 'water', 'Energy-Lightning': 'electric',
  'Energy-Grass': 'grass', 'Energy-Psychic': 'psychic', 'Energy-Darkness': 'dark',
  'Energy-Colorless': 'colorless',
};

const TYPE_LABELS = {
  'Pokemon-Fire':'🔥 Pokémon Fuego','Pokemon-Water':'💧 Pokémon Agua',
  'Pokemon-Lightning':'⚡ Pokémon Eléctrico','Pokemon-Grass':'🌿 Pokémon Planta',
  'Pokemon-Psychic':'🔮 Pokémon Psíquico','Pokemon-Darkness':'🌑 Pokémon Oscuro',
  'Pokemon-Colorless':'⭐ Pokémon Incoloro','Pokemon-Fighting':'👊 Pokémon Lucha',
  'Pokemon-Metal':'⚙️ Pokémon Metal','Trainer':'🎭 Entrenadores',
  'Energy-Fire':'🔥 Energía Fuego','Energy-Water':'💧 Energía Agua',
  'Energy-Lightning':'⚡ Energía Eléctrico','Energy-Grass':'🌿 Energía Planta',
  'Energy-Psychic':'🔮 Energía Psíquico','Energy-Darkness':'🌑 Energía Oscuro',
  'Energy-Colorless':'⭐ Energía Incoloro',
};

// ===================== SAVE / LOAD =====================
function save() {
  localStorage.setItem('ptcg_inventory', JSON.stringify(inventory));
  localStorage.setItem('ptcg_decks', JSON.stringify(savedDecks));
  updateStats();
  if (typeof syncPush === 'function') syncPush();   // sube a la nube si hay sesión
}

function updateStats() {
  const total = inventory.reduce((s, c) => s + c.qty, 0);
  document.getElementById('stat-total').textContent = total;
  document.getElementById('stat-decks').textContent = savedDecks.length;
}

// ===================== INVENTORY =====================
function addCard() {
  const name = document.getElementById('card-name').value.trim();
  const qty = parseInt(document.getElementById('card-qty').value) || 1;
  const type = document.getElementById('card-type').value;
  if (!name) { showToast(T('to_need_name'), 'error'); return; }
  const existing = inventory.find(c => c.name.toLowerCase() === name.toLowerCase());
  if (existing) {
    existing.qty += qty;
    showToast(`${T('to_updated')}: ${name} (×${existing.qty})`, 'success');
  } else {
    inventory.push({ name, qty, type });
    showToast(`${T('to_added')}: ${name} ×${qty}`, 'success');
  }
  document.getElementById('card-name').value = '';
  document.getElementById('card-qty').value = '1';
  save();
  renderInventory();
  renderLegal();
}

function removeCard(name) {
  inventory = inventory.filter(c => c.name !== name);
  save();
  renderInventory();
  renderLegal();
}

// ===================== ESTADO DE FILTROS / EXPLORADOR =====================
const FILTROS_DEF = { name:'', type:'', supertype:'', rarity:'', setId:'', subtype:'', hpMin:'', hpMax:'', regMark:'', pokedex:'', orderBy:'name' };
let colFiltros = Object.assign({}, FILTROS_DEF);
let expFiltros = Object.assign({}, FILTROS_DEF);
let _setsList = [];

// --- Localización de nombres de expansión en el desplegable (ES/EN) ---
let _setNameEs = null, _serieEsByEn = null, _locLoading = false;
function setLabel(s){
  if(lang === 'es' && _setNameEs){
    const tid = (typeof tcgdexSetIdFor === 'function') ? tcgdexSetIdFor(s.id) : s.id;
    const nombre = _setNameEs[tid] || s.name;
    const serie = (_serieEsByEn && _serieEsByEn[s.series]) || s.series;
    return nombre + (serie ? (' · ' + serie) : '');
  }
  return s.name + (s.series ? (' · ' + s.series) : '');
}
function cargarLocalizacionSets(){
  if(_setNameEs && _serieEsByEn) return;
  if(_locLoading || typeof cargarSetsTcgdex !== 'function') return;
  _locLoading = true;
  Promise.all([cargarSetsTcgdex('es'), cargarSeriesTcgdex('en'), cargarSeriesTcgdex('es')]).then(function(r){
    const esSets = r[0] || [], enSeries = r[1] || [], esSeries = r[2] || [];
    _setNameEs = {}; esSets.forEach(s => { _setNameEs[s.id] = s.name; });
    const idByEn = {}; enSeries.forEach(s => { idByEn[s.name] = s.id; });
    const esById = {}; esSeries.forEach(s => { esById[s.id] = s.name; });
    _serieEsByEn = {}; Object.keys(idByEn).forEach(en => { const sid = idByEn[en]; if(esById[sid]) _serieEsByEn[en] = esById[sid]; });
    pintarFiltros();   // repinta el desplegable con los nombres en español
  }).catch(function(){ _locLoading = false; });
}

// ¿Coincide una vista de carta con los filtros (para la colección, local)?
function matchView(v, f){
  if(f.name){ if(normName(v.nombre).indexOf(normName(f.name)) < 0) return false; }
  if(f.type && (v.tipos||[]).indexOf(f.type) < 0) return false;
  if(f.supertype && normName(v.supertipo) !== normName(f.supertype)) return false;
  if(f.rarity && (v.rareza||'') !== f.rarity) return false;
  if(f.subtype && (v.fase||'').split(', ').indexOf(f.subtype) < 0) return false;
  if(f.regMark && (v.marcaRegulacion||'') !== f.regMark) return false;
  if(f.hpMin && !(parseInt(v.ps) >= +f.hpMin)) return false;
  if(f.hpMax && !(parseInt(v.ps) <= +f.hpMax)) return false;
  if(f.pokedex && (v.pokedex||[]).indexOf(+f.pokedex) < 0) return false;
  if(f.setId && (!v.set || v.set.id !== f.setId)) return false;
  return true;
}
function ordenarVistas(arr, orderBy){
  const a = arr.slice();
  const byName = (x,y)=> (x.v.nombre||'').localeCompare(y.v.nombre||'');
  const byHp = (x,y)=> (parseInt(x.v.ps)||0) - (parseInt(y.v.ps)||0);
  const byNum = (x,y)=> (parseInt(x.v.numeroCarta)||0) - (parseInt(y.v.numeroCarta)||0);
  if(orderBy==='-name') a.sort((x,y)=>byName(y,x));
  else if(orderBy==='hp') a.sort(byHp);
  else if(orderBy==='-hp') a.sort((x,y)=>byHp(y,x));
  else if(orderBy==='number') a.sort(byNum);
  else if(orderBy==='-set.releaseDate') a.sort((x,y)=> ((y.v.set&&y.v.set.releaseDate)||'').localeCompare((x.v.set&&x.v.set.releaseDate)||''));
  else a.sort(byName);
  return a;
}

// ===================== COLECCIÓN (cuadrícula a pantalla completa) =====================
function renderInventory(){
  const el = document.getElementById('coleccion-grid');
  if(!el){ updateStats(); return; }
  let items = inventory.map(e => ({ e, v: regCard(viewFromEntry(e)) }));
  items = items.filter(o => matchView(o.v, colFiltros));
  items = ordenarVistas(items, colFiltros.orderBy);
  if(!items.length){ el.innerHTML = `<div class="empty-grid">${T('col_empty')}</div>`; updateStats(); return; }
  el.innerHTML = items.map(({e, v}) => {
    const img = v.imagenChica || v.imagenGrande;
    const ring = getLegalStatus(e) === 'illegal' ? ' ring-illegal' : '';
    const idq = esc(jsq(e.id));
    const setline = `${esc((v.set && v.set.nombre) || '')}${v.numeroCarta ? (' · ' + esc(v.numeroCarta)) : ''}`;
    return `<div class="cardtile${ring}">
      <div class="cardtile-img" onclick="showCardById('${idq}')">
        <div class="tile-actions">
          <button class="tile-act" aria-label="${esc(T('tile_replace'))}" title="${esc(T('tile_replace'))}" onclick="event.stopPropagation();iniciarReemplazo('${idq}')">⇄</button>
          <button class="tile-act del" aria-label="${esc(T('tile_delete'))}" title="${esc(T('tile_delete'))}" onclick="event.stopPropagation();pedirEliminar('${idq}')">✕</button>
        </div>
        ${img ? `<img src="${esc(img)}" alt="${esc(v.nombre)}" loading="lazy" decoding="async" onload="this.classList.add('loaded')">` : `<div class="noimg">${esc(v.nombre)}</div>`}
        <span class="qty-badge">×${e.qty}</span>
      </div>
      <div class="cardtile-bar">
        <button aria-label="-1" onclick="cambiarCantidad('${idq}',-1)">−</button>
        <span class="ct-name" title="${esc(v.nombre)}">${esc(v.nombre)}</span>
        <button aria-label="+1" onclick="cambiarCantidad('${idq}',1)">+</button>
      </div>
      <div class="cardtile-set">${setline}</div>
    </div>`;
  }).join('');
  updateStats();
}

function getLegalStatus(card) {
  if (!card.type.startsWith('Pokemon')) return 'legal';
  const needed = evoChains[card.name];
  if (!needed) return 'legal';
  const names = inventory.map(c => c.name.toLowerCase());
  const allPresent = needed.every(n => names.includes(n.toLowerCase()));
  return allPresent ? 'legal' : 'illegal';
}

// ===================== IMPORT =====================
function toggleImport() {
  document.getElementById('import-area').classList.toggle('open');
}

function importList() {
  const text = document.getElementById('import-text').value;
  if (!text.trim()) return;
  const lines = text.split('\n');
  let added = 0, skipped = 0;
  lines.forEach(raw => {
    const parsed = parseInventoryLine(raw);
    if (!parsed) { if (raw.trim()) skipped++; return; }
    mergeCard(parsed.name, parsed.type, parsed.qty);
    added++;
  });
  save();
  renderInventory();
  renderLegal();
  document.getElementById('import-text').value = '';
  document.getElementById('import-area').classList.remove('open');
  showToast(`${added} ${T('to_imported')}${skipped ? ', '+skipped+' '+T('to_lines_skipped') : ''}`, added > 0 ? 'success' : 'error');
}

function mergeCard(name, type, qty) {
  if (qty <= 0) return;
  const d = getCardData(name);
  const id = (d && d.id) ? d.id : ('legacy:' + normName(name));
  const existing = inventory.find(c => c.id === id);
  if (existing) { existing.qty += qty; }
  else { inventory.push(d ? { id, name, type, qty, card: d } : { id, name, type, qty }); }
}

// Asegura un id (real si se conoce, si no "legacy:") en una entrada antigua
function asegurarId(e){
  if (e.id) return;
  const d = getCardData(e.name);
  if (d && d.id) { e.id = d.id; if (!e.card) e.card = d; }
  else e.id = 'legacy:' + normName(e.name);
}

function cleanName(n) {
  let s = (n || '').trim();
  // Separa el "ex" pegado: Pikachuex -> Pikachu ex, Darkraiex -> Darkrai ex
  s = s.replace(/([a-z])ex$/, '$1 ex');
  return s;
}

// Detecta el tipo a partir del texto de la columna "Type" y el nombre de la carta
function mapTableType(rawType, cardName) {
  const t = (rawType || '').trim().toLowerCase();
  const name = (cardName || '').toLowerCase();
  if (['su','supporter','i','item','stadium','tool','t','su.','sup'].includes(t)) return 'Trainer';
  if (name.includes('energy') || t.endsWith(' e') || t.includes('energy')) {
    if (t.includes('fire') || name.includes('fire')) return 'Energy-Fire';
    if (t.includes('water') || name.includes('water')) return 'Energy-Water';
    if (t.includes('lightning') || name.includes('lightning')) return 'Energy-Lightning';
    if (t.includes('grass') || name.includes('grass')) return 'Energy-Grass';
    if (t.includes('psychic') || name.includes('psychic')) return 'Energy-Psychic';
    if (t.includes('dark') || name.includes('dark')) return 'Energy-Darkness';
    return 'Energy-Colorless';
  }
  if (t === 'fire') return 'Pokemon-Fire';
  if (t === 'water') return 'Pokemon-Water';
  if (t === 'lightning' || t === 'electric') return 'Pokemon-Lightning';
  if (t === 'grass') return 'Pokemon-Grass';
  if (t === 'psychic') return 'Pokemon-Psychic';
  if (t === 'darkness' || t === 'dark') return 'Pokemon-Darkness';
  if (t === 'colorless') return 'Pokemon-Colorless';
  if (t === 'fighting') return 'Pokemon-Fighting';
  if (t === 'metal') return 'Pokemon-Metal';
  return 'Pokemon-Colorless';
}

function normalizeTypeToken(tok, name) {
  const t = (tok || '').trim();
  if (/^(Pokemon|Energy)-/.test(t) || t === 'Trainer') return t;
  return mapTableType(t, name);
}

function inferType(name) {
  const n = (name || '').toLowerCase();
  if (n.includes('energy')) return 'Energy-Colorless';
  return 'Pokemon-Colorless';
}

// Parser universal: detecta formato de tabla, coma o dos puntos
function parseInventoryLine(raw) {
  let line = (raw || '').trim();
  if (!line) return null;
  const lower = line.toLowerCase();
  // Saltar encabezados y títulos de mazo
  if (lower.startsWith('no.') || lower.startsWith('no\t')) return null;
  if (/^[a-z0-9' ]+deck$/i.test(line) && !line.includes(',') && !line.includes(':')) return null;

  // 1) FORMATO TABLA (tabulado o con 2+ espacios): No | Mark | Card | Type | Qty
  let cells = line.includes('\t') ? line.split('\t') : null;
  if (!cells || cells.filter(c => c.trim()).length < 4) {
    const spaceCells = line.split(/\s{2,}/);
    if (spaceCells.length >= 4) cells = spaceCells;
  }
  if (cells) {
    cells = cells.map(c => c.trim()).filter(c => c !== '');
    if (cells.length >= 4) {
      const qtyCell = cells[cells.length - 1];
      if (qtyCell.toLowerCase() === 'quantity') return null;
      const qty = parseInt(qtyCell.replace(/[^\d]/g, ''));
      const typeCell = cells[cells.length - 2];
      const nameCell = cells[cells.length - 3];
      if (isNaN(qty) || !nameCell) return null;
      const name = cleanName(nameCell);
      return { name, type: mapTableType(typeCell, name), qty };
    }
  }

  // 2) FORMATO COMA: Nombre, Tipo, Cantidad
  if (line.includes(',')) {
    const parts = line.split(',').map(s => s.trim());
    if (parts.length >= 3) {
      const qty = parseInt(parts[2].replace(/[^\d]/g, ''));
      if (isNaN(qty)) return null;
      return { name: cleanName(parts[0]), type: normalizeTypeToken(parts[1], parts[0]), qty };
    }
  }

  // 3) FORMATO DOS PUNTOS: Nombre: Cantidad  (tipo inferido)
  if (line.includes(':')) {
    const idx = line.lastIndexOf(':');
    const name = line.slice(0, idx).trim();
    const qty = parseInt(line.slice(idx + 1).replace(/[^\d]/g, ''));
    if (!name || isNaN(qty) || qty === 0) return null;
    return { name: cleanName(name), type: inferType(name), qty };
  }

  return null;
}

// Carga el inventario confirmado de Miguel (Pikachu Deck + Darkrai Deck + cartas 151 sueltas)
// NO incluye la baraja de Armarouge (no la posee). Los duplicados se suman automáticamente.
function loadMyInventory() {
  if (inventory.length && !confirm(T('load_confirm'))) return;
  const sources = [
    // ===== PIKACHU DECK =====
    ['Pikachu ex','Pokemon-Lightning',1], ['Voltorb','Pokemon-Lightning',4],
    ['Electrode','Pokemon-Lightning',3], ['Mareep','Pokemon-Lightning',4],
    ['Flaaffy','Pokemon-Lightning',3], ['Ampharos','Pokemon-Lightning',2],
    ['Rotom','Pokemon-Lightning',1], ['Wattrel','Pokemon-Lightning',2],
    ['Kilowattrel','Pokemon-Lightning',1], ['Miraidon','Pokemon-Lightning',1],
    ['Electric Generator','Trainer',1], ['Great Ball','Trainer',4],
    ['Jacq','Trainer',1], ['Nemona','Trainer',4], ['Nest Ball','Trainer',1],
    ['Picnicker','Trainer',2], ['Potion','Trainer',2], ['Switch','Trainer',2],
    ['Youngster','Trainer',3], ['Basic Lightning Energy','Energy-Lightning',18],
    // ===== DARKRAI DECK =====
    ['Seviper','Pokemon-Darkness',2], ['Darkrai ex','Pokemon-Darkness',1],
    ['Pawniard','Pokemon-Darkness',4], ['Bisharp','Pokemon-Darkness',3],
    ['Kingambit','Pokemon-Darkness',2], ['Yveltal','Pokemon-Darkness',2],
    ['Salandit','Pokemon-Darkness',4], ['Salazzle','Pokemon-Darkness',3],
    ['Cyclizar','Pokemon-Colorless',1], ["Boss's Orders",'Trainer',1],
    ['Great Ball','Trainer',4], ['Jacq','Trainer',1], ['Nemona','Trainer',4],
    ['Nest Ball','Trainer',1], ['Picnicker','Trainer',2], ['Potion','Trainer',2],
    ['Switch','Trainer',2], ['Youngster','Trainer',3], ['Basic Darkness Energy','Energy-Darkness',18],
    // ===== CARTAS 151 SUELTAS (qty > 0) =====
    ['Venusaur ex','Pokemon-Grass',4], ['Charizard ex','Pokemon-Fire',2],
    ['Blastoise ex','Pokemon-Water',2], ['Arbok ex','Pokemon-Darkness',2],
    ['Ninetales ex','Pokemon-Fire',5], ['Wigglytuff ex','Pokemon-Colorless',1],
    ['Alakazam ex','Pokemon-Psychic',4], ['Tentacool','Pokemon-Water',6],
    ['Tentacruel','Pokemon-Water',4], ['Geodude','Pokemon-Fighting',6],
    ['Graveler','Pokemon-Fighting',5], ['Golem ex','Pokemon-Fighting',1],
    ['Ponyta','Pokemon-Fire',9], ['Rapidash','Pokemon-Fire',7],
    ['Slowpoke','Pokemon-Water',3], ['Slowbro','Pokemon-Water',6],
    ['Magnemite','Pokemon-Lightning',4], ['Magneton','Pokemon-Lightning',5],
    ["Farfetch'd",'Pokemon-Colorless',6], ['Doduo','Pokemon-Colorless',5],
    ['Dodrio','Pokemon-Colorless',8], ['Seel','Pokemon-Water',7],
    ['Dewgong','Pokemon-Water',2], ['Grimer','Pokemon-Darkness',7],
    ['Muk','Pokemon-Darkness',4], ['Shellder','Pokemon-Water',5],
    ['Cloyster','Pokemon-Water',6], ['Gastly','Pokemon-Psychic',5],
    ['Haunter','Pokemon-Psychic',8], ['Gengar','Pokemon-Psychic',6],
    ['Onix','Pokemon-Fighting',2], ['Drowzee','Pokemon-Psychic',9],
    ['Hypno','Pokemon-Psychic',4], ['Krabby','Pokemon-Water',5],
    ['Kingler','Pokemon-Water',5], ['Voltorb','Pokemon-Lightning',5],
    ['Electrode','Pokemon-Lightning',7], ['Exeggcute','Pokemon-Grass',6],
    ['Exeggutor','Pokemon-Grass',2], ['Cubone','Pokemon-Fighting',7],
    ['Marowak','Pokemon-Fighting',3], ['Hitmonlee','Pokemon-Fighting',6],
    ['Hitmonchan','Pokemon-Fighting',5], ['Lickitung','Pokemon-Colorless',6],
    ['Koffing','Pokemon-Darkness',7], ['Weezing','Pokemon-Darkness',6],
    ['Rhyhorn','Pokemon-Fighting',3], ['Rhydon','Pokemon-Fighting',5],
    ['Chansey','Pokemon-Colorless',3], ['Tangela','Pokemon-Grass',6],
    ['Kangaskhan ex','Pokemon-Colorless',3], ['Horsea','Pokemon-Water',4],
    ['Seadra','Pokemon-Water',7], ['Goldeen','Pokemon-Water',5],
    ['Seaking','Pokemon-Water',6], ['Staryu','Pokemon-Water',3],
    ['Starmie','Pokemon-Water',4], ['Mr. Mime','Pokemon-Psychic',3],
    ['Scyther','Pokemon-Grass',7], ['Jynx ex','Pokemon-Water',2],
    ['Electabuzz','Pokemon-Lightning',3], ['Magmar','Pokemon-Fire',6],
    ['Pinsir','Pokemon-Grass',5], ['Tauros','Pokemon-Colorless',5],
    ['Magikarp','Pokemon-Water',4], ['Gyarados','Pokemon-Water',4],
    ['Lapras','Pokemon-Water',3], ['Ditto','Pokemon-Colorless',6],
    ['Eevee','Pokemon-Colorless',4], ['Vaporeon','Pokemon-Water',8],
    ['Jolteon','Pokemon-Lightning',6], ['Flareon','Pokemon-Fire',4],
    ['Porygon','Pokemon-Colorless',5], ['Omanyte','Pokemon-Water',4],
    ['Zapdos ex','Pokemon-Lightning',4], ['Mew ex','Pokemon-Psychic',1],
    // ===== ENTRENADORES 151 SUELTOS =====
    ["Erika's Invitation",'Trainer',3], ["Giovanni's Charisma",'Trainer',1],
    ['Switch','Trainer',1],
  ];
  inventory = [];
  sources.forEach(([name, type, qty]) => mergeCard(name, type, qty));
  save();
  renderInventory();
  renderLegal();
  renderSaved();
  const total = inventory.reduce((s, c) => s + c.qty, 0);
  showToast(`${T('to_loaded')}: ${total} ${T('stat_cards')} (${inventory.length} ${T('to_unique')})`, 'success');
}

// ===================== LEGAL CHECK =====================
function renderLegal() {
  const panel = document.getElementById('legal-panel');
  const pokemon = inventory.filter(c => c.type.startsWith('Pokemon'));
  if (!pokemon.length) {
    panel.innerHTML = `<div class="empty-state"><div class="empty-icon">🔍</div><div class="empty-title">${T('legal_empty_title')}</div><div class="empty-desc">${T('legal_empty_desc')}</div></div>`;
    return;
  }
  const legals = pokemon.filter(c => getLegalStatus(c) === 'legal');
  const illegals = pokemon.filter(c => getLegalStatus(c) === 'illegal');
  // Datos extra de la carta (PS, imagen) desde la vista de la entrada
  const extra = c => {
    const d = regCard(viewFromEntry(c));
    return d && d.ps ? ` · <b>${esc(d.ps)} ${T('d_ps')}</b>` : '';
  };
  const thumbOf = c => {
    const d = regCard(viewFromEntry(c));
    return (d && d.imagenChica) ? `<img class="inv-thumb" src="${esc(d.imagenChica)}" alt="" loading="lazy" onclick="showCardById('${esc(jsq(c.id))}')">` : '';
  };
  const clickAttr = c => ` class="clickable" style="cursor:pointer" onclick="showCardById('${esc(jsq(c.id))}')"`;
  let html = '';
  if (legals.length) {
    html += `<div class="legal-section">
      <div class="legal-section-title" style="color:var(--legal)">✅ ${T('legal_legals')} (${legals.length})</div>
      <div class="legal-grid">`;
    legals.forEach(c => {
      const color = TYPE_COLORS[c.type] || 'colorless';
      html += `<div class="legal-item ok">
        ${thumbOf(c) || '<span class="legal-item-icon">✅</span>'}
        <div class="legal-item-info">
          <div class="legal-item-name"><span${clickAttr(c)}>${c.name}</span> <span class="inv-badge bg-${color}">×${c.qty}</span></div>
          <div class="legal-item-reason">${T('legal_reason_ok')}${extra(c)}</div>
        </div>
      </div>`;
    });
    html += '</div></div>';
  }
  if (illegals.length) {
    html += `<div class="legal-section">
      <div class="legal-section-title" style="color:var(--illegal)">❌ ${T('legal_illegals')} (${illegals.length})</div>
      <div class="legal-grid">`;
    illegals.forEach(c => {
      const needed = evoChains[c.name] || [];
      const names = inventory.map(x => x.name.toLowerCase());
      const missing = needed.filter(n => !names.includes(n.toLowerCase()));
      html += `<div class="legal-item illegal">
        ${thumbOf(c) || '<span class="legal-item-icon">❌</span>'}
        <div class="legal-item-info">
          <div class="legal-item-name"><span${clickAttr(c)}>${c.name}</span></div>
          <div class="legal-item-reason">${T('legal_missing')} ${missing.join(', ')}${extra(c)}</div>
        </div>
      </div>`;
    });
    html += '</div></div>';
  }
  panel.innerHTML = html;
}

// ===================== GENERAR PROMPT PARA LA IA =====================
function generateAIPrompt() {
  if (!inventory.length) { showToast(T('to_need_inv'), 'error'); return; }
  const deckType = document.getElementById('deck-type').value;
  const numVariants = document.getElementById('num-variants').value;
  const dt = deckTypeLabel(deckType);

  // Construir inventario agrupado y legible
  const groups = {};
  inventory.forEach(c => { (groups[c.type] = groups[c.type] || []).push(c); });
  let invText = '';
  Object.keys(groups).sort().forEach(type => {
    const label = typeGroupLabel(type).replace(/^[^\s]+\s/, '');
    invText += `\n${label}:\n`;
    groups[type].sort((a,b)=>a.name.localeCompare(b.name)).forEach(c => {
      invText += `  ${c.name} ×${c.qty}\n`;
    });
  });

  // Lista de ilegales detectadas por la app (ayuda a la IA)
  const illegals = inventory.filter(c => getLegalStatus(c) === 'illegal');
  let illegalText = '';
  const missingOf = c => {
    const needed = evoChains[c.name] || [];
    const names = inventory.map(x => x.name.toLowerCase());
    return needed.filter(n => !names.includes(n.toLowerCase()));
  };

  let prompt;
  if (lang === 'en') {
    if (illegals.length) {
      illegalText = '\nCARDS ALREADY DETECTED AS ILLEGAL (do not use them, they lack a pre-evolution):\n';
      illegals.forEach(c => { illegalText += `  ${c.name} — missing: ${missingOf(c).join(', ')}\n`; });
    }
    prompt = `Act as an expert Pokémon TCG deck builder.

This is my COMPLETE and CLOSED inventory. Treat it as the absolute totality of cards I own. If a card is not listed here, I do NOT have it and you cannot use it. Do not invent cards, quantities, abilities or attacks.
${invText}${illegalText}
MANDATORY RULES:
1. Only use cards listed above, respecting the exact quantities (max 4 copies per card in a deck, except basic energy).
2. If an evolution requires pre-evolutions I don't have, it is ILLEGAL and cannot appear in the deck.
3. Verify each Pokémon's real type (e.g. Jynx ex from set 151 is Water type, not Psychic).
4. Verify that attacks can be paid with the deck's energy.
5. Colorless Pokémon or others whose attacks are paid with colorless energy may be included if they add consistency or damage; explain why you include them.

REQUEST:
Build ${numVariants} ${dt}-type deck variants, ordered from strongest to weakest.
Preferred distribution: ~20 Pokémon, ~20 Trainers, ~20 Energy (exactly 60 cards).

For each variant show:
- Full list of 60 cards (no tables, format "quantity name")
- Number of Pokémon, Trainers and Energy
- Main strategy, strengths, weaknesses
- Difficulty of use (1-10)
- A "Considered off-type Pokémon" section

Before showing each deck, validate: all cards exist in the inventory, no quantity exceeds what's available, all evolutions are legal, the deck has exactly 60 cards.`;
  } else {
    if (illegals.length) {
      illegalText = '\nCARTAS YA DETECTADAS COMO ILEGALES (no las uses, les falta preevolución):\n';
      illegals.forEach(c => { illegalText += `  ${c.name} — falta: ${missingOf(c).join(', ')}\n`; });
    }
    prompt = `Actúa como un constructor experto de mazos de Pokémon TCG.

Este es mi inventario COMPLETO y CERRADO. Trátalo como la totalidad absoluta de cartas que poseo. Si una carta no aparece aquí, NO la tengo y no puedes usarla. No inventes cartas, cantidades, habilidades ni ataques.
${invText}${illegalText}
REGLAS OBLIGATORIAS:
1. Solo usa cartas listadas arriba, respetando las cantidades exactas (máximo 4 copias por carta en un mazo, salvo energías básicas).
2. Si una evolución requiere preevoluciones que no tengo, es ILEGAL y no puede aparecer en el mazo.
3. Verifica el tipo real de cada Pokémon (ej: Jynx ex del set 151 es tipo Agua, no Psíquico).
4. Verifica que los ataques sean pagables con las energías del mazo.
5. Pokémon incoloros o de otros tipos cuyos ataques se paguen con energías incoloras pueden incluirse si aportan consistencia o daño; explica por qué los incluyes.

PETICIÓN:
Construye ${numVariants} variantes de mazo tipo ${dt}, ordenadas de más fuerte a más débil.
Distribución preferida: ~20 Pokémon, ~20 Entrenadores, ~20 Energías (60 cartas exactas).

Para cada variante muestra:
- Lista completa de 60 cartas (sin tablas, formato "cantidad nombre")
- Cantidad de Pokémon, Entrenadores y Energías
- Estrategia principal, ventajas, debilidades
- Dificultad de uso (1-10)
- Sección "Pokémon fuera del tipo principal considerados"

Antes de mostrar cada mazo, valida: todas las cartas existen en el inventario, ninguna cantidad supera lo disponible, todas las evoluciones son legales, el mazo tiene exactamente 60 cartas.`;
  }

  // Copiar al portapapeles
  navigator.clipboard.writeText(prompt).then(
    () => showToast(T('to_prompt_copied'), 'success'),
    () => showToast(T('to_prompt_manual'), 'error')
  );

  // Mostrar el prompt con instrucciones
  const out = document.getElementById('decks-output');
  out.innerHTML = `
    <div class="prompt-result">
      <div class="prompt-steps">
        <div class="prompt-step"><span class="step-num">1</span> ${T('prompt_steps1')}</div>
        <div class="prompt-step"><span class="step-num">2</span> ${T('prompt_steps2')}</div>
        <div class="prompt-step"><span class="step-num">3</span> ${T('prompt_steps3')}</div>
      </div>
      <div class="prompt-box-header">
        <span>${T('prompt_header')} ${esc(dt)}, ${numVariants} ${T('prompt_variants_word')}</span>
        <button class="btn-copy-prompt" onclick="copyPromptAgain()">${T('btn_recopy')}</button>
      </div>
      <pre class="prompt-box" id="prompt-box">${prompt.replace(/</g,'&lt;')}</pre>
    </div>`;
}

function copyPromptAgain() {
  const text = document.getElementById('prompt-box').textContent;
  navigator.clipboard.writeText(text).then(() => showToast(T('to_prompt_recopied'), 'success'));
}

function renderDecks(decks, deckType) {
  const out = document.getElementById('decks-output');
  if (!decks || !decks.length) {
    out.innerHTML = `<div class="empty-state"><div class="empty-icon">😕</div><div class="empty-title">${T('no_decks_title')}</div><div class="empty-desc">${T('no_decks_desc')}</div></div>`;
    return;
  }
  let html = '';
  decks.forEach(d => {
    const pokemonCount = (d.pokemon||[]).reduce((s,c)=>s+c.qty,0);
    const trainerCount = (d.trainers||[]).reduce((s,c)=>s+c.qty,0);
    const energyCount = (d.energies||[]).reduce((s,c)=>s+c.qty,0);
    const total = pokemonCount + trainerCount + energyCount;
    const diff = parseInt(d.difficulty) || 5;
    const pips = Array.from({length:10}, (_,i) => `<div class="diff-pip ${i<diff?'filled':''}"></div>`).join('');

    const deckText = formatDeckText(d);

    html += `
    <div class="deck-card">
      <div class="deck-header">
        <div class="deck-rank">${d.rank}</div>
        <div class="deck-title-area">
          <div class="deck-name">${d.name}</div>
          <div class="deck-meta">
            <span class="meta-chip">${T('sec_pokemon')} <strong>${pokemonCount}</strong></span>
            <span class="meta-chip">${T('sec_trainers')} <strong>${trainerCount}</strong></span>
            <span class="meta-chip">${T('sec_energies')} <strong>${energyCount}</strong></span>
            <span class="meta-chip">${T('m_total')} <strong>${total}</strong></span>
          </div>
          <div class="ratio-bar" title="Pokémon ${pokemonCount} · ${T('sec_trainers')} ${trainerCount} · ${T('sec_energies')} ${energyCount}">
            <div class="rb-seg rb-pk" style="width:${(pokemonCount/(total||1)*100).toFixed(1)}%"></div>
            <div class="rb-seg rb-tr" style="width:${(trainerCount/(total||1)*100).toFixed(1)}%"></div>
            <div class="rb-seg rb-en" style="width:${(energyCount/(total||1)*100).toFixed(1)}%"></div>
          </div>
        </div>
        <div class="deck-actions">
          <button class="btn-copy" onclick="copyDeck(${JSON.stringify(deckText).replace(/'/g,'&apos;')})">${T('btn_copy')}</button>
          <button class="btn-save-deck" onclick='saveDeck(${JSON.stringify(d)}, "${deckType}")'>${T('btn_save')}</button>
        </div>
      </div>
      <div class="deck-body">
        <div class="deck-section">
          <div class="deck-section-title">${T('sec_pokemon')} (${pokemonCount})</div>
          <div class="deck-cards">${(d.pokemon||[]).map(c=>deckMini(c)).join('')}</div>
        </div>
        <div class="deck-section">
          <div class="deck-section-title">${T('sec_trainers')} (${trainerCount})</div>
          <div class="deck-cards">${(d.trainers||[]).map(c=>deckMini(c)).join('')}</div>
        </div>
        <div class="deck-section">
          <div class="deck-section-title">${T('sec_energies')} (${energyCount})</div>
          <div class="deck-cards">${(d.energies||[]).map(c=>deckMini(c)).join('')}</div>
        </div>
        <div class="deck-strategy">
          <div class="deck-section-title">${T('s_analysis')}</div>
          <div class="strategy-grid">
            <div class="strategy-block">
              <div class="strategy-label">${T('s_strategy')}</div>
              <div class="strategy-text">${d.strategy||'—'}</div>
            </div>
            <div class="strategy-block">
              <div class="strategy-label">${T('s_advantages')}</div>
              <div class="strategy-text">${d.advantages||'—'}</div>
            </div>
            <div class="strategy-block">
              <div class="strategy-label">${T('s_weaknesses')}</div>
              <div class="strategy-text">${d.weaknesses||'—'}</div>
            </div>
          </div>
          <div class="strategy-block" style="margin-top:12px">
            <div class="strategy-label">${T('s_difficulty')} ${diff}/10</div>
            <div class="difficulty-bar">${pips}</div>
          </div>
          ${d.colorless_notes ? `<div class="strategy-block" style="margin-top:12px"><div class="strategy-label">${T('s_offtype')}</div><div class="strategy-text">${d.colorless_notes}</div></div>` : ''}
        </div>
      </div>
    </div>`;
  });
  out.innerHTML = html;
}

// Miniatura visual de una carta dentro de un mazo (imagen + cantidad, clic al detalle)
function deckMini(c){
  const img = c.img || (getCardData(c.card) || {}).imagenChica || (typeof energyImgByName === 'function' ? energyImgByName(c.card) : null);
  const id = c.id || (getCardData(c.card) || {}).id;
  let click = '';
  if(id && cardRegistry[id]) click = `onclick="showCardById('${esc(jsq(id))}')"`;
  else if(getCardData(c.card)) click = `onclick="showCardDetail('${esc(jsq(c.card))}')"`;
  const inner = img
    ? `<img src="${esc(img)}" alt="${esc(c.card)}" loading="lazy" decoding="async" onload="this.classList.add('loaded')">`
    : `<div class="noimg">${esc(c.card)}</div>`;
  return `<div class="deck-mini${img?' has-img':''}" title="${esc(c.qty + '× ' + c.card)}" ${click}>${inner}<span class="qty-badge">×${c.qty}</span></div>`;
}

// Genera una línea de carta clicable (si hay datos) para el armador de mazos
function deckCardSpan(c) {
  const data = getCardData(c.card);
  const txt = `${c.qty}× ${c.card}`;
  if (data) {
    const jsName = String(c.card).replace(/'/g,"\\'");
    return `<span class="clickable" style="cursor:pointer" onclick="showCardDetail('${jsName}')">${esc(txt)}</span>`;
  }
  return esc(txt);
}

function formatDeckText(d) {
  const pokemon = (d.pokemon||[]).map(c=>`${c.qty} ${c.card}`).join('\n');
  const trainers = (d.trainers||[]).map(c=>`${c.qty} ${c.card}`).join('\n');
  const energies = (d.energies||[]).map(c=>`${c.qty} ${c.card}`).join('\n');
  const total = [...(d.pokemon||[]),...(d.trainers||[]),...(d.energies||[])].reduce((s,c)=>s+c.qty,0);
  return `${d.name}\n\n${T('sec_pokemon')} (${(d.pokemon||[]).reduce((s,c)=>s+c.qty,0)})\n${pokemon}\n\n${T('sec_trainers')} (${(d.trainers||[]).reduce((s,c)=>s+c.qty,0)})\n${trainers}\n\n${T('sec_energies')} (${(d.energies||[]).reduce((s,c)=>s+c.qty,0)})\n${energies}\n\n${T('m_total')}: ${total} ${T('deck_total_word')}`;
}

function copyDeck(text) {
  navigator.clipboard.writeText(text).then(() => showToast(T('to_deck_copied'), 'success'));
}

function saveDeck(deck, deckType) {
  const existing = savedDecks.findIndex(d => d.name === deck.name);
  if (existing >= 0) { savedDecks[existing] = {...deck, deckType, savedAt: Date.now()}; }
  else { savedDecks.push({...deck, deckType, savedAt: Date.now()}); }
  save();
  renderSaved();
  showToast(`"${deck.name}" ${T('to_saved')}`, 'success');
}

// ===================== SAVED DECKS =====================
function renderSaved() {
  const grid = document.getElementById('saved-grid');
  if (!savedDecks.length) {
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">💾</div><div class="empty-title">${T('saved_empty_title')}</div><div class="empty-desc">${T('saved_empty_desc')}</div></div>`;
    return;
  }
  let html = '';
  savedDecks.forEach((d, i) => {
    const preview = formatDeckText(d);
    const typeColor = {Fire:'fire',Fuego:'fire',Water:'water',Agua:'water',Lightning:'electric','Eléctrico':'electric',Grass:'grass',Planta:'grass',Psychic:'psychic','Psíquico':'psychic',Darkness:'dark',Oscuro:'dark'}[d.deckType] || 'colorless';
    html += `
    <div class="saved-deck-card">
      <div class="saved-deck-name">${d.name}</div>
      <span class="saved-deck-type bg-${typeColor}">${T('saved_type')} ${esc(deckTypeLabel(d.deckType))}</span>
      <div class="saved-deck-preview">${preview}</div>
      <div class="saved-deck-actions">
        <button class="btn-view" onclick='viewDeck(${JSON.stringify(d)})'>${T('btn_view')}</button>
        <button class="btn-delete-deck" onclick="deleteDeck(${i})">${T('btn_delete')}</button>
      </div>
    </div>`;
  });
  grid.innerHTML = html;
}

function viewDeck(d) {
  document.getElementById('modal-title').textContent = d.name;
  document.getElementById('modal-body').textContent = formatDeckText(d);
  document.getElementById('modal-overlay').classList.add('open');
}

function deleteDeck(i) {
  savedDecks.splice(i, 1);
  save();
  renderSaved();
  showToast(T('to_deck_deleted'), 'success');
}

function closeModal(e) {
  if (e.target === document.getElementById('modal-overlay'))
    document.getElementById('modal-overlay').classList.remove('open');
}

// ===================== NAVEGACIÓN DE VISTAS =====================
function showView(name){
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  const sec = document.getElementById('view-'+name); if(sec) sec.classList.add('active');
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  const nb = document.getElementById('nav-'+name); if(nb) nb.classList.add('active');
  if(name==='coleccion') renderInventory();
  if(name==='legalidad') renderLegal();
  if(name==='mazos') renderSaved();
  if(name==='explorar' && !expCards.length) doExplorar(true);
  if(name==='proxies'){ if(typeof renderProxies==='function') renderProxies(); if(typeof proxLlenarFiltros==='function') proxLlenarFiltros(); }
}

// ===================== BARRA DE FILTROS (compartida) =====================
function pintarFiltros(){
  [['col','filtros-col'], ['exp','filtros-exp']].forEach(([sc, id]) => {
    const cont = document.getElementById(id);
    if(cont) cont.innerHTML = `<button class="f-toggle" aria-label="${esc(T('f_toggle'))}" onclick="toggleFiltros('${sc}')">🎛️ ${esc(T('f_toggle'))}</button>` + buildFilterBar(sc);
  });
  if(lang === 'es') cargarLocalizacionSets();   // traduce los nombres de expansión al español
}
function toggleFiltros(scope){
  const cont = document.getElementById('filtros-' + scope);
  if(cont) cont.classList.toggle('open');
}
function buildFilterBar(scope){
  const f = scope==='col' ? colFiltros : expFiltros;
  const sel = (field, opts) => `<select onchange="setFiltro('${scope}','${field}',this.value)">`
    + opts.map(o => `<option value="${esc(o.v)}"${f[field]===o.v?' selected':''}>${esc(o.label)}</option>`).join('') + '</select>';
  const any = {v:'', label:T('f_any')};
  const tipos  = [any].concat(['Fire','Water','Lightning','Grass','Psychic','Fighting','Darkness','Metal','Fairy','Dragon','Colorless'].map(t=>({v:t,label:trType(t)})));
  const supers = [any].concat(['Pokémon','Trainer','Energy'].map(s=>({v:s,label:trSuper(s)})));
  const rarezas= [any].concat(['Common','Uncommon','Rare','Rare Holo','Double Rare','Ultra Rare','Illustration Rare','Special Illustration Rare','Hyper Rare','Promo','ACE SPEC Rare'].map(r=>({v:r,label:trRarity(r)})));
  const subs   = [any].concat(['Basic','Stage 1','Stage 2','ex','Supporter','Item','Stadium','Pokémon Tool','Special'].map(s=>({v:s,label:trPhase(s)})));
  const marcas = [any].concat(['D','E','F','G','H'].map(m=>({v:m,label:m})));
  const sets   = [any].concat(_setsList.map(s=>({v:s.id, label: setLabel(s)})));
  const ordenes= [
    {v:'name',label:T('ord_name')},{v:'-name',label:T('ord_name_desc')},
    {v:'hp',label:T('ord_hp')},{v:'-hp',label:T('ord_hp_desc')},
    {v:'number',label:T('ord_number')},{v:'-set.releaseDate',label:T('ord_recent')}
  ];
  return `<div class="filtros-inner">
    <div class="f-field" style="flex:1"><label>${T('f_search')}</label><input class="f-text" value="${esc(f.name)}" placeholder="${esc(T('f_search'))}" oninput="setFiltro('${scope}','name',this.value)"></div>
    <div class="f-field"><label>${T('f_type')}</label>${sel('type',tipos)}</div>
    <div class="f-field"><label>${T('f_supertype')}</label>${sel('supertype',supers)}</div>
    <div class="f-field"><label>${T('f_rarity')}</label>${sel('rarity',rarezas)}</div>
    <div class="f-field"><label>${T('f_subtype')}</label>${sel('subtype',subs)}</div>
    <div class="f-field"><label>${T('f_set')}</label>${sel('setId',sets)}</div>
    <div class="f-field"><label>${T('f_hp_min')}</label><input class="f-hp" type="number" value="${esc(f.hpMin)}" oninput="setFiltro('${scope}','hpMin',this.value)"></div>
    <div class="f-field"><label>${T('f_hp_max')}</label><input class="f-hp" type="number" value="${esc(f.hpMax)}" oninput="setFiltro('${scope}','hpMax',this.value)"></div>
    <div class="f-field"><label>${T('f_regmark')}</label>${sel('regMark',marcas)}</div>
    <div class="f-field"><label>${T('f_pokedex')}</label><input class="f-hp" type="number" value="${esc(f.pokedex)}" oninput="setFiltro('${scope}','pokedex',this.value)"></div>
    <div class="f-field"><label>${T('f_order')}</label>${sel('orderBy',ordenes)}</div>
    <button class="f-reset" onclick="resetFiltros('${scope}')">${T('f_reset')}</button>
  </div>`;
}
let _expTimer = null;
function setFiltro(scope, field, value){
  const f = scope==='col' ? colFiltros : expFiltros;
  f[field] = value;
  if(scope==='col') renderInventory();
  else { clearTimeout(_expTimer); _expTimer = setTimeout(() => doExplorar(true), 350); }
}
function resetFiltros(scope){
  if(scope==='col') colFiltros = Object.assign({}, FILTROS_DEF);
  else expFiltros = Object.assign({}, FILTROS_DEF);
  pintarFiltros();
  if(scope==='col') renderInventory(); else doExplorar(true);
}

// ===================== EXPLORADOR (todas las cartas, en vivo) =====================
let expPage = 1, expCards = [], expTotal = 0, expLoading = false, expReq = 0;
async function doExplorar(reset){
  if(typeof apiBuscar !== 'function') return;
  if(reset){ expPage = 1; }
  const myReq = ++expReq;   // ficha de esta petición (para ignorar respuestas viejas)
  expLoading = true;
  const status = document.getElementById('exp-status');
  // Pintado instantáneo desde caché mientras llega la red
  if(reset && typeof apiCacheGet === 'function'){
    const c = apiCacheGet(expFiltros, expPage);
    if(c && c.cards.length){ c.cards.forEach(regCard); expCards = c.cards.slice(); expTotal = c.totalCount; renderExploradorGrid(); }
  }
  if(status) status.textContent = T('exp_loading');
  try{
    const r = await apiBuscar(expFiltros, expPage);
    if(myReq !== expReq) return;   // llegó una búsqueda más nueva: descartamos esta respuesta
    expTotal = r.totalCount;
    r.cards.forEach(regCard);
    expCards = reset ? r.cards.slice() : expCards.concat(r.cards);
    renderExploradorGrid();
    let suf = '';
    if(r.fromCache) suf = ' · ' + T('exp_cache');
    else if(r.fuente === 'tcgdex') suf = ' · ' + T('exp_backup');
    if(status) status.textContent = `${expTotal} ${T('exp_results')}${suf}`;
    const more = document.getElementById('exp-more');
    if(more) more.style.display = (expCards.length < expTotal) ? '' : 'none';
  }catch(e){
    if(myReq === expReq && status) status.textContent = T('exp_error');
  }finally{
    if(myReq === expReq) expLoading = false;
  }
}
function cargarMas(){ expPage++; doExplorar(false); }
function renderExploradorGrid(){
  const grid = document.getElementById('explorador-grid');
  if(!grid) return;
  if(!expCards.length){ grid.innerHTML = `<div class="empty-grid">${T('exp_empty')}</div>`; return; }
  grid.innerHTML = expCards.map(v => {
    const img = v.imagenChica || v.imagenGrande;
    const owned = inventory.find(x => x.id === v.id);
    const idq = esc(jsq(v.id));
    return `<div class="cardtile">
      <div class="cardtile-img" onclick="showCardById('${idq}')">
        ${img ? `<img src="${esc(img)}" alt="${esc(v.nombre)}" loading="lazy" decoding="async" onload="this.classList.add('loaded')">` : `<div class="noimg">${esc(v.nombre)}</div>`}
        ${owned ? `<span class="qty-badge owned">×${owned.qty}</span>` : ''}
      </div>
      <div class="cardtile-bar">
        <span class="ct-name" title="${esc(v.nombre)}">${esc(v.nombre)}</span>
        <button class="ct-add" aria-label="${esc(T('cd_add'))}" title="${esc(T('cd_add'))}" onclick="accionAgregar(cardRegistry['${idq}'],1)">＋</button>
      </div>
      <div class="cardtile-set">${esc((v.set&&v.set.nombre)||'')}${v.rareza?(' · '+esc(trRarity(v.rareza))):''}</div>
    </div>`;
  }).join('');
}

// ===================== EXPORTAR / IMPORTAR COLECCIÓN =====================
function vaciarColeccion(){
  if(!inventory.length){ showToast(T('to_cleared'), 'success'); return; }
  if(!confirm(T('cf_clear'))) return;        // 1ª confirmación
  if(!confirm(T('cf_clear2').replace('{n}', inventory.length))) return;  // 2ª confirmación
  inventory = [];
  save(); renderInventory(); renderLegal();
  showToast(T('to_cleared'), 'success');
}

function exportarColeccion(){
  const blob = new Blob([JSON.stringify({version:1, inventory, savedDecks}, null, 2)], {type:'application/json'});
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob); a.download = 'mi-coleccion-pokemon.json';
  document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(a.href);
}
function importarColeccionFile(input){
  const file = input.files && input.files[0]; if(!file) return;
  const r = new FileReader();
  r.onload = () => {
    try{
      const d = JSON.parse(r.result);
      if(Array.isArray(d.inventory)){
        if(inventory.length && !confirm(T('cf_import'))){ input.value = ''; return; }
        inventory = d.inventory;
        inventory.forEach(asegurarId);
        if(Array.isArray(d.savedDecks)) savedDecks = d.savedDecks;
        save(); renderInventory(); renderLegal(); renderSaved();
        showToast(T('to_import_ok'), 'success');
      } else showToast(T('to_import_err'), 'error');
    }catch(e){ showToast(T('to_import_err'), 'error'); }
    input.value = '';
  };
  r.readAsText(file);
}

// ===================== TOAST =====================
function showToast(msg, type='success') {
  const t = document.getElementById('toast');
  const icon = type === 'success' ? '✅' : '❌';
  t.innerHTML = `${icon} ${msg}`;
  t.className = `toast ${type} show`;
  setTimeout(() => t.classList.remove('show'), 3000);
}

// ===================== INIT =====================
// Migrar inventario antiguo: asegurar un id (real si se conoce) en cada entrada
inventory.forEach(asegurarId);
applyLang();          // traduce la interfaz, pinta filtros y renderiza
showView('coleccion');
updateStats();
// Cargar la lista de sets en segundo plano para el desplegable de filtros
if(typeof cargarSets === 'function'){
  cargarSets().then(s => { _setsList = s; pintarFiltros(); }).catch(() => {});
}
