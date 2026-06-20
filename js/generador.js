// =============================================================
//  GENERADOR DE MAZOS (por reglas, en el navegador)
//  Arma mazos válidos de 60 cartas con la colección del usuario:
//   - solo cartas que posee, respetando cantidades (máx 4, energía básica libre)
//   - líneas evolutivas legales (incluye las preevoluciones que tienes)
//   - energía básica del tipo elegido para completar
//  Cada variante es un ARQUETIPO distinto (agresivo, pegadores, habilidades,
//  equilibrado, resistente) y ROTA el atacante núcleo para dar variedad.
//  Usa los datos ricos de cada carta: ataques (daño/coste), habilidades,
//  coste de retirada, debilidad/resistencia.
//  Reutiliza renderDecks()/saveDeck() de app.js para mostrar y guardar.
//
//  AMPLIACIONES:
//   - Modo de inventario INDEPENDIENTE (cada mazo se valida contra el inventario
//     completo) o COMPARTIDO (una sola colección física que se agota entre los
//     mazos de la misma ejecución).
//   - Filtros por mazo: expansión (set), serie, marca de regulación, mecánicas
//     (ex/V/VMAX/VSTAR/GX/Mega/Tera…), límite de Pokémon especiales (0-4),
//     formato legal, profundidad evolutiva, single-prize, HP mínimo y excluir.
//   - Validación con reconstrucción automática: nunca se muestra un mazo inválido.
//   - Tabla comparativa de fortalezas calculada con métricas reales.
// =============================================================

const TIPO_ES_A_EN = {
  'Fuego':'Fire','Agua':'Water','Eléctrico':'Lightning','Planta':'Grass',
  'Psíquico':'Psychic','Oscuro':'Darkness','Lucha':'Fighting','Incoloro':'Colorless'
};

function _dmg(a){ const m = /(\d+)/.exec(a && a.damage || ''); return m ? +m[1] : 0; }
function _maxDmg(v){ return (v.ataques||[]).reduce((s,a)=>Math.max(s,_dmg(a)),0); }
function _esEx(v){ return /(\bex\b|\bV\b|VSTAR|VMAX|GX)/i.test(v.fase||''); }
function _esPokemon(v){ return /pok/i.test(v.supertipo||''); }
function _esTrainer(v){ return /trainer|entrenad/i.test(v.supertipo||''); }
function _esEnergiaBasica(v){ return /energy|energ/i.test(v.supertipo||'') && /basic|básic/i.test(v.fase||''); }
function _esBasico(v){ return /basic|básic/i.test(v.fase||''); }

// ---- Métricas derivadas de los datos de la carta (ataques, habilidades, retirada) ----
function _coste(a){ return Math.max(1, (a && a.cost || []).length); }           // coste de energía de un ataque
function _atkEff(v){ return (v.ataques||[]).reduce((m,a)=>Math.max(m, _dmg(a)/_coste(a)), 0); } // daño por energía
function _cheapDmg(v){ return (v.ataques||[]).filter(a=>(a.cost||[]).length<=2).reduce((m,a)=>Math.max(m,_dmg(a)),0); }
function _avgCost(v){
  const ds = (v.ataques||[]).filter(a=>_dmg(a)>0);
  if(!ds.length) return 2;
  return ds.reduce((s,a)=>s+_coste(a),0) / ds.length;
}
function _hasAbility(v){ return (v.habilidades||[]).length > 0; }
function _retreat(v){ return (v.costoRetirada||[]).length; }
function _hp(v){ return parseInt(v.ps)||0; }
function _tieneResistencia(v){ return (v.resistencias||[]).length > 0; }
function _bestAttack(v){
  let best=null, bd=-1;
  (v.ataques||[]).forEach(a=>{ const d=_dmg(a); if(d>bd){ bd=d; best=a; } });
  return best;
}

// ---- Metadatos para filtros (set / serie / marca / formato / mecánicas) ----
function _setName(v){ return (v.set && v.set.nombre) || ''; }
function _serie(v){ return (v.set && v.set.serie) || ''; }
function _marca(v){ return v.marcaRegulacion || ''; }
function _legalEn(v, fmt){ return ((v.legalidad||{})[fmt] === 'Legal'); }
// Profundidad evolutiva: 0 básico, 1 stage 1, 2 stage 2
function _stage(v){
  const f = (v.fase||'').toLowerCase();
  if(/stage\s*2|fase\s*2|nivel\s*2/.test(f)) return 2;
  if(/stage\s*1|fase\s*1|nivel\s*1/.test(f)) return 1;
  return 0;
}
// Mecánicas oficiales detectadas en fase + nombre + texto de reglas
function _mecanicas(v){
  if(!_esPokemon(v)) return [];
  const hay = ((v.fase||'') + ' ' + (v.nombre||'') + ' ' + ((v.reglas||[]).join(' '))).toString();
  const tags = [];
  if(/\bVMAX\b/i.test(hay)) tags.push('VMAX');
  if(/\bVSTAR\b/i.test(hay)) tags.push('VSTAR');
  if(/\bV[-\s]?UNION\b/i.test(hay)) tags.push('VUNION');
  if(/\bGX\b/i.test(hay)) tags.push('GX');
  if(/\btera\b|teracristal|tera type/i.test(hay)) tags.push('TERA');
  if(/\bradiant\b|radiante/i.test(hay)) tags.push('RADIANT');
  if(/\bBREAK\b/i.test(hay)) tags.push('BREAK');
  if(/\bLV\.?\s*X\b/i.test(hay)) tags.push('LVX');
  if(/\bmega\b|\bM\s+.+\bEX\b/i.test(hay)) tags.push('MEGA');
  if(/\bex\b/i.test(hay)) tags.push('EX');
  // "V" a secas (no VMAX/VSTAR/VUNION)
  if(/\bV\b/.test((v.fase||'') + ' ' + (v.nombre||'')) && !/VMAX|VSTAR|VUNION/i.test(hay)) tags.push('V');
  return tags;
}
function _esEspecial(v){ return _mecanicas(v).length > 0; }
// Premios que entrega al ser noqueado (para el estilo single-prize)
function _premios(v){
  if(!_esPokemon(v)) return 1;
  const m = _mecanicas(v);
  if(m.indexOf('VMAX')>=0 || m.indexOf('VUNION')>=0) return 3;
  if(m.length) return 2;
  return 1;
}

// Lista de todas las mecánicas que el filtro puede ofrecer (orden de presentación)
const MECANICAS = ['EX','V','VMAX','VSTAR','VUNION','GX','MEGA','TERA','RADIANT','BREAK','LVX'];

// Entrenadores de robo/búsqueda por utilidad aproximada (selección y métrica de consistencia)
const PRIOR = ['professor','research','iono','arven','boss','orders','nemona','jacq','youngster','nest ball','ultra ball','great ball','switch','potion','picnicker','generator','invitation','charisma'];
function _prioVal(nm){ const n = normName(nm); const p = PRIOR.findIndex(k => n.indexOf(k) >= 0); return p < 0 ? 99 : p; }

// Texto corto del mejor ataque del núcleo, p.ej. «Burning Darkness» 180 de daño por 2 energías
function _descAtaque(a, esLang){
  if(!a) return '';
  const n = (a.cost||[]).length || 0;
  const dmg = _dmg(a);
  if(esLang){
    const en = n + ' energía' + (n===1?'':'s');
    return dmg ? `«${a.name}» ${dmg} de daño por ${en}` : `«${a.name}» (${en})`;
  }
  const en = n + ' energ' + (n===1?'y':'ies');
  return dmg ? `"${a.name}" ${dmg} damage for ${en}` : `"${a.name}" (${en})`;
}

// Preevoluciones necesarias para una carta (por nombre)
function _requeridas(v){
  if (typeof evoChains !== 'undefined' && evoChains[v.nombre]) return evoChains[v.nombre].slice();
  return v.evolucionaDe ? [v.evolucionaDe] : [];
}

// Debilidad/ventaja aproximada por tipo (respaldo para el texto de análisis)
const _DEBIL = { Fire:'Water', Water:'Lightning', Grass:'Fire', Lightning:'Fighting',
  Psychic:'Darkness', Darkness:'Fighting', Fighting:'Psychic', Colorless:'Fighting' };

// ---- Arquetipos: cada uno puntúa a los Pokémon de forma distinta ----
// id, nombre ES/EN, y score(v) sobre la vista de carta.
const ARQUETIPOS = [
  { id:'aggro',    es:'Agresivo',    en:'Aggro',
    score:v => _cheapDmg(v)*2 + _atkEff(v)*8 - _retreat(v)*5 + (_esBasico(v)?15:0) },
  { id:'power',    es:'Pegadores',   en:'Heavy hitters',
    score:v => _maxDmg(v) + _hp(v)/8 + (_esEx(v)?60:0) },
  { id:'ability',  es:'Habilidades', en:'Ability engine',
    score:v => (_hasAbility(v)?80:0) + _maxDmg(v)*0.5 + _atkEff(v)*2 },
  { id:'balanced', es:'Equilibrado', en:'Balanced',
    score:v => _maxDmg(v) + _hp(v)/12 + _atkEff(v)*3 + (_esEx(v)?30:0) - _retreat(v)*2 },
  { id:'tank',     es:'Resistente',  en:'Tanky',
    score:v => _hp(v)/4 + (_tieneResistencia(v)?15:0) - _retreat(v)*6 + _maxDmg(v)*0.4 },
];

// =============================================================
//  FILTROS DEL GENERADOR
// =============================================================

// Lee los filtros del panel de controles de la vista «Construir».
function leerFiltros(){
  const val = id => { const e = document.getElementById(id); return e ? e.value : ''; };
  const chk = id => { const e = document.getElementById(id); return !!(e && e.checked); };
  const num = id => { const e = document.getElementById(id); return e && e.value ? (parseInt(e.value)||0) : 0; };
  const mecs = Array.from(document.querySelectorAll('.gm-mec:checked')).map(c => c.value);
  const lim = val('f-speclimit');
  const excl = (val('f-exclude') || '').split(',').map(s => s.trim()).filter(Boolean);
  return {
    set: val('f-set'), serie: val('f-serie'), marca: val('f-marca'),
    formato: val('f-format'),
    mecanicas: mecs,
    limiteEspeciales: (lim === '' ? null : (parseInt(lim) || 0)),
    profundidad: val('f-depth'),
    singlePrize: chk('f-singleprize'),
    hpMin: num('f-hpmin'),
    excluir: excl
  };
}

// Aplica los filtros al inventario "owned" (entradas {name, qty, v}).
function filtrarInventario(owned, f){
  if(!f) return owned;
  const exSet = new Set((f.excluir || []).map(normName));
  return owned.filter(o => {
    const v = o.v;
    // set / serie / marca / formato y exclusión: a TODAS las cartas
    if(f.set    && _setName(v) !== f.set)   return false;
    if(f.serie  && _serie(v)   !== f.serie) return false;
    if(f.marca  && _marca(v)   !== f.marca) return false;
    if(f.formato && !_legalEn(v, f.formato)) return false;
    if(exSet.has(normName(o.name))) return false;
    // filtros específicos de Pokémon (no afectan entrenadores ni energías)
    if(_esPokemon(v)){
      if(f.profundidad === 'basic' && _stage(v) > 0) return false;
      if(f.profundidad === 's1'    && _stage(v) > 1) return false;
      if(f.singlePrize && _premios(v) >= 2) return false;
      if(f.hpMin && _hp(v) < f.hpMin) return false;
      const mec = _mecanicas(v);
      if(mec.length){                              // es un Pokémon especial
        if(f.limiteEspeciales === 0) return false; // sin especiales en el mazo
        if(f.mecanicas && f.mecanicas.length && !mec.some(t => f.mecanicas.indexOf(t) >= 0)) return false;
      }
    }
    return true;
  });
}

// =============================================================
//  FLUJO: pregunta de modo de inventario y arranque
// =============================================================

function generarMazos(){
  if(!inventory.length){ showToast(T('to_need_inv'), 'error'); return; }
  abrirModalModo();
}

function abrirModalModo(){
  const o = document.getElementById('gen-mode-overlay');
  if(o){ o.classList.add('open'); }
  else { _generarMazos('indep', leerFiltros()); }   // respaldo si no existe el modal
}
function cerrarModalModo(){
  const o = document.getElementById('gen-mode-overlay');
  if(o) o.classList.remove('open');
}
// Lanzado por los botones del modal: 'indep' | 'comp'
function generarConModo(modo){
  cerrarModalModo();
  _generarMazos(modo, leerFiltros());
}

// =============================================================
//  GENERACIÓN
// =============================================================

function _generarMazos(modo, filtros){
  const compartido = (modo === 'comp');
  const sel = document.getElementById('deck-type').value;
  const nVar = parseInt(document.getElementById('num-variants').value) || 3;
  const f = filtros || {};

  // Inventario con datos de carta, ya filtrado por los criterios elegidos
  let owned = inventory.map(e => ({ name: e.name, qty: e.qty, v: regCard(viewFromEntry(e)) }));
  owned = filtrarInventario(owned, f);
  if(!owned.length){ showToast(T('gen_filter_none'), 'error'); renderDecks([], sel); return; }

  const ownedMap = {}; const ownedNames = new Set();
  owned.forEach(o => { ownedMap[normName(o.name)] = o; ownedNames.add(normName(o.name)); });

  // Pool físico: en modo compartido las cartas se agotan entre mazos.
  const remaining = {};
  owned.forEach(o => { remaining[normName(o.name)] = o.qty; });
  const disp = nm => {
    const k = normName(nm); const o = ownedMap[k]; if(!o) return 0;
    return compartido ? (remaining[k] != null ? remaining[k] : 0) : o.qty;
  };
  const consumir = d => {
    if(!compartido) return;
    const dec = (nm, q) => { const k = normName(nm); if(remaining[k] != null) remaining[k] = Math.max(0, remaining[k] - q); };
    (d.pokemon||[]).forEach(c => dec(c.card, c.qty));
    (d.trainers||[]).forEach(c => dec(c.card, c.qty));
    (d.energies||[]).forEach(c => dec(c.card, c.qty));   // si no está listada, es no-op
  };

  // Tipo objetivo (inglés)
  let tipo = TIPO_ES_A_EN[sel];
  if(!tipo){ // "el mejor posible": tipo con más atacantes
    const cont = {};
    owned.filter(o=>_esPokemon(o.v)).forEach(o => (o.v.tipos||[]).forEach(t => cont[t]=(cont[t]||0)+1));
    tipo = Object.keys(cont).sort((a,b)=>cont[b]-cont[a])[0] || 'Colorless';
  }

  const encaja = v => (v.tipos||[]).indexOf(tipo) >= 0 || (v.tipos||[]).indexOf('Colorless') >= 0 || tipo === 'Colorless';
  const legal = v => _requeridas(v).every(n => ownedNames.has(normName(n)));

  // Candidatos: Pokémon del tipo, legales, con algún ataque
  let candidatos = owned.filter(o => _esPokemon(o.v) && encaja(o.v) && legal(o.v) && (o.v.ataques||[]).length);
  if(!candidatos.length){ showToast(T('gen_none'), 'error'); renderDecks([], sel); return; }

  // Energía del mazo
  let energyType = tipo;
  if(energyType === 'Colorless'){
    const cont = {};
    candidatos.forEach(o => (o.v.ataques||[]).forEach(a => (a.cost||[]).forEach(c => { if(c!=='Colorless') cont[c]=(cont[c]||0)+1; })));
    energyType = Object.keys(cont).sort((a,b)=>cont[b]-cont[a])[0] || 'Lightning';
  }
  const energyName = 'Basic ' + energyType + ' Energy';
  const energyListed = !!ownedMap[normName(energyName)];

  // Entrenadores disponibles (orden por utilidad aproximada)
  const trainersDisp = owned.filter(o => _esTrainer(o.v)).sort((a,b)=>_prioVal(a.name)-_prioVal(b.name));

  // Energía básica del mazo (para mostrar)
  const energyCard = (typeof getCardData==='function') ? getCardData(energyName) : null;
  const energyId = energyCard ? energyCard.id : ('energy-' + normName(energyName).replace(/ /g,'-'));
  const energyImg = (typeof ENERGY_CARD_IMG !== 'undefined' && ENERGY_CARD_IMG[energyType])
    ? ENERGY_CARD_IMG[energyType].small
    : (energyCard ? energyCard.imagenChica : null);

  // Energía objetivo según el coste medio de los ataques del núcleo (atacantes baratos
  // necesitan menos energía; pegadores caros, más).
  function energiaObjetivo(coreV){
    const c = coreV ? _avgCost(coreV) : (candidatos.reduce((s,o)=>s+_avgCost(o.v),0)/candidatos.length);
    if(c <= 1.6) return 16;
    if(c <= 2.4) return 19;
    return 21;
  }

  // Construye un mazo de 60 priorizando el arquetipo y arrancando por el núcleo.
  function construir(arq, core){
    const orden = (a,b) => arq.score(b.v) - arq.score(a.v);
    const cand = candidatos.slice().sort(orden);
    const counts = {}; // name -> {qty, v}

    // Unidades de entrenador según la disponibilidad ACTUAL (respeta el pool compartido)
    const trainerUnits = [];
    trainersDisp.forEach(t => { const q = Math.min(disp(t.name), 4); for(let k=0;k<q;k++) trainerUnits.push({ card:t.name, id:t.v.id, img:t.v.imagenChica }); });

    const sumar = (nm, q) => {
      const o = ownedMap[normName(nm)]; if(!o) return;
      q = Math.min(q, disp(nm), 4);
      if(q <= 0) return;
      if(!counts[nm]) counts[nm] = { qty:0, v:o.v };
      counts[nm].qty = Math.max(counts[nm].qty, q);
    };
    const totalPk = () => Object.values(counts).reduce((s,o)=>s+o.qty,0);
    // copias de Pokémon especiales ya incluidas (para el tope 0-4)
    const especialCopias = () => Object.keys(counts).reduce((s,nm)=> s + (_esEspecial(counts[nm].v) ? counts[nm].qty : 0), 0);
    const meter = (c, base) => {
      // Respeta el tope de Pokémon especiales (máximo total combinado)
      if(_esEspecial(c.v) && f.limiteEspeciales != null){
        const room = f.limiteEspeciales - especialCopias();
        if(room <= 0) return;
        base = Math.min(base, room);
      }
      const linea = _requeridas(c.v).concat([c.v.nombre]);
      linea.forEach((nm, i) => sumar(nm, i === linea.length-1 ? base : (i===0 ? base : base-1)));
    };
    const extra = owned.filter(o => _esPokemon(o.v) && legal(o.v)).sort(orden);
    const llenarPokemon = objetivo => {
      for(const c of cand){ if(totalPk() >= objetivo) break; meter(c, 4); }
      for(const c of extra){ if(totalPk() >= objetivo) break; if(counts[c.v.nombre]) continue; meter(c, 4); }
    };

    const E = energiaObjetivo(core && core.v);
    if(core) meter(core, 4);                                  // el núcleo primero

    let nTr = Math.min(trainerUnits.length, 22);              // entrenadores (idealmente ~20)
    llenarPokemon(Math.min(34, Math.max(12, 60 - nTr - E)));
    let nPk = totalPk();
    let energy = 60 - nTr - nPk;
    // Si sobra energía, usa más entrenadores que tengas y luego más Pokémon
    if(energy > E && trainerUnits.length > nTr){
      const add = Math.min(trainerUnits.length - nTr, energy - E);
      nTr += add; energy -= add;
    }
    if(energy > E){
      llenarPokemon(Math.min(34, nPk + (energy - E)));
      nPk = totalPk(); energy = 60 - nTr - nPk;
    }
    // Construir listas con imágenes
    let pokemon = Object.keys(counts).map(nm => ({ qty: counts[nm].qty, card: nm, id: counts[nm].v.id, img: counts[nm].v.imagenChica }));
    // Recorta si nos pasamos de 60 (las líneas pueden sobrepasar). Recorta el núcleo el último.
    if(energy < 0){
      const coreNm = core ? core.v.nombre : null;
      pokemon.sort((a,b)=> (a.card===coreNm?1:0)-(b.card===coreNm?1:0) || a.qty-b.qty);
      let falta = -energy;
      for(const l of pokemon){ if(falta<=0) break; const cut = Math.min(l.qty, falta); l.qty -= cut; falta -= cut; }
      pokemon = pokemon.filter(l => l.qty > 0);
      nPk = pokemon.reduce((s,c)=>s+c.qty,0);
      energy = 60 - nTr - nPk;
      if(energy < 0){ nTr += energy; energy = 0; } // último ajuste por entrenadores
    }
    // En modo compartido la energía básica LISTADA también se agota
    if(compartido && energyListed && energy > disp(energyName)){
      energy = Math.max(0, disp(energyName));
    }
    // Agrupar entrenadores elegidos
    const tmap = {};
    trainerUnits.slice(0, nTr).forEach(u => { (tmap[u.card] = tmap[u.card] || { qty:0, card:u.card, id:u.id, img:u.img }).qty++; });
    const trainers = Object.values(tmap);
    const energies = energy > 0 ? [{ qty: energy, card: energyName, id: energyId, img: energyImg }] : [];

    const offtype = pokemon.filter(c => { const v = counts[c.card] && counts[c.card].v; return v && (v.tipos||[]).indexOf(tipo) < 0 && (v.tipos||[]).indexOf('Colorless') < 0; }).map(c => c.card);
    const score = pokemon.reduce((s,c)=> s + _maxDmg(counts[c.card].v) * c.qty, 0);
    return { pokemon, trainers, energies, offtype,
      nPk: pokemon.reduce((s,c)=>s+c.qty,0), nTr: trainers.reduce((s,c)=>s+c.qty,0), score };
  }

  // ---- Validación: nunca mostrar un mazo inválido ----
  function validarMazo(d, coreV){
    const err = [];
    const all = [...(d.pokemon||[]), ...(d.trainers||[]), ...(d.energies||[])];
    const total = all.reduce((s,c)=>s+c.qty,0);
    if(total !== 60) err.push('total='+total);
    all.forEach(c => {
      const o = ownedMap[normName(c.card)];
      const v = o && o.v;
      const enBasica = v ? _esEnergiaBasica(v) : /basic .* energy/i.test(c.card);
      if(!enBasica && c.qty > 4) err.push('>4 '+c.card);
      if(o){ if(c.qty > disp(c.card)) err.push('qty '+c.card); }
      else if(!enBasica) err.push('noinv '+c.card);
    });
    // Evoluciones legales: las preevoluciones deben estar en el mazo
    (d.pokemon||[]).forEach(c => {
      const o = ownedMap[normName(c.card)]; if(!o) return;
      _requeridas(o.v).forEach(req => {
        if(!d.pokemon.some(p => normName(p.card) === normName(req))) err.push('evo '+c.card);
      });
    });
    // Tope de Pokémon especiales
    if(f.limiteEspeciales != null){
      const esp = (d.pokemon||[]).reduce((s,c)=>{ const o=ownedMap[normName(c.card)]; return s + (o && _esEspecial(o.v) ? c.qty : 0); },0);
      if(esp > f.limiteEspeciales) err.push('esp='+esp);
    }
    // Ataque del núcleo pagable con las energías presentes
    const tiposEnergia = new Set((d.energies||[]).map(c => { const m=/Basic (\w+) Energy/i.exec(c.card); return m?m[1]:null; }).filter(Boolean));
    if(coreV){
      const ba = _bestAttack(coreV);
      if(ba){ const need = (ba.cost||[]).filter(x=>x!=='Colorless'); if(need.some(t=>!tiposEnergia.has(t))) err.push('core-pago'); }
    }
    return { ok: err.length === 0, errores: err };
  }

  // ---- Métricas reales para la tabla comparativa ----
  function calcMetricas(d, dificultad){
    const pk = (d.pokemon||[]).map(c => ({ qty:c.qty, v:(ownedMap[normName(c.card)]||{}).v })).filter(x=>x.v);
    const nPk = pk.reduce((s,x)=>s+x.qty,0) || 1;
    const basics = pk.filter(x=>_esBasico(x.v)).reduce((s,x)=>s+x.qty,0);
    const lineas = new Set(pk.map(x=>x.v.nombre)).size;
    const draw = (d.trainers||[]).reduce((s,c)=> s + (_prioVal(c.card) <= 8 ? c.qty : 0), 0);
    const avgCost = pk.reduce((s,x)=>s+_avgCost(x.v)*x.qty,0)/nPk;
    const maxDmg = pk.reduce((m,x)=>Math.max(m,_maxDmg(x.v)),0);
    const cheap = pk.reduce((m,x)=>Math.max(m,_cheapDmg(x.v)),0);
    const especiales = pk.filter(x=>_esEspecial(x.v)).reduce((s,x)=>s+x.qty,0);

    const clamp = n => Math.max(1, Math.min(5, Math.round(n)));
    const consistencia = clamp(1 + draw/4 + (basics/nPk)*3 + (lineas<=3?1 : lineas<=5?0 : -1));
    const velocidad = clamp(1 + (avgCost<=1.6?3 : avgCost<=2.2?2 : avgCost<=2.8?1 : 0) + (cheap>=60?1:0) + (basics/nPk)*1.5);
    const dano = clamp(1 + maxDmg/70);
    const facilidad = clamp(6 - (dificultad||5)/2);
    const aprender = (facilidad>=4 && consistencia>=3 && especiales<=2);
    const sum = consistencia + velocidad + dano + facilidad;        // 4..20
    const potencial = Math.round((sum/20)*10*2)/2;                  // 0..10 en medios puntos
    return { consistencia, velocidad, dano, facilidad, aprender, potencial };
  }

  // Firma de la composición de Pokémon (nombre × cantidad) para evitar mazos repetidos
  const firmaPk = d => d.pokemon.map(p => p.card + 'x' + p.qty).sort().join('|');

  // Genera hasta nVar variantes DISTINTAS y VÁLIDAS: combina cada arquetipo con
  // rotación del atacante núcleo en varias pasadas y descarta composiciones repetidas
  // o inválidas (el propio bucle actúa como reconstrucción automática).
  const usedCores = new Set();
  const vistas = new Set();
  const variantes = [];
  const maxPasadas = Math.min(candidatos.length + 1, 12);
  for(let pasada = 0; pasada < maxPasadas && variantes.length < nVar; pasada++){
    for(let ai = 0; ai < ARQUETIPOS.length && variantes.length < nVar; ai++){
      const arq = ARQUETIPOS[ai];
      const ordenados = candidatos.slice().sort((a,b)=> arq.score(b.v) - arq.score(a.v));
      // Núcleo: el mejor de este arquetipo aún sin usar; si ya se usaron todos, rota igual.
      let core = ordenados.find(o => !usedCores.has(normName(o.v.nombre)));
      if(!core) core = ordenados[pasada % ordenados.length] || ordenados[0];
      if(!core) continue;
      const d = construir(arq, core);
      if(d.nPk <= 0) continue;
      const sig = firmaPk(d);
      if(vistas.has(sig)) continue;            // ya existe un mazo con esa misma composición
      const val = validarMazo(d, core.v);
      if(!val.ok) continue;                     // inválido: se descarta y el bucle prueba otro
      vistas.add(sig);
      usedCores.add(normName(core.v.nombre));
      d.arq = arq; d.core = core.v;
      variantes.push(d);
      consumir(d);                              // agota el pool en modo compartido
    }
  }
  if(!variantes.length){ showToast(T('gen_none'), 'error'); renderDecks([], sel); return; }

  const esLang = true; // español, base universal
  const decks = variantes.map((d, i) => {
    const arq = d.arq, core = d.core;
    const rank = i + 1;
    const arqNom = esLang ? arq.es : arq.en;
    const energyTot = d.energies.reduce((s,c)=>s+c.qty,0);
    const ba = core ? _bestAttack(core) : null;
    const desc = _descAtaque(ba, esLang);
    const coreNom = core ? ((typeof nombreLocal==='function') ? nombreLocal(core) : core.nombre) : '';
    const ret = core ? _retreat(core) : 0;
    const abil = core && (core.habilidades||[])[0] ? core.habilidades[0].name : '';
    const debil = (core && (core.debilidades||[])[0] && core.debilidades[0].type)
      ? trType(core.debilidades[0].type)
      : trType(_DEBIL[tipo] || 'Fighting');

    const dificultad = Math.min(10, Math.max(2,
      Math.round(4 + d.nPk/6 + (arq.id==='power'?2:0) + (arq.id==='ability'?1:0))));

    // Estrategia por arquetipo, citando el atacante núcleo y su mejor ataque
    let strategy;
    if(esLang){
      const por = coreNom ? ` con ${coreNom}${desc?` (${desc})`:''}` : '';
      strategy = {
        aggro:    `Presión temprana${por}. Golpea desde los primeros turnos con ataques baratos y retira barato para no perder ritmo.`,
        power:    `Gira en torno a un pegador principal${por}. Acumula energía y aguanta hasta dar golpes decisivos.`,
        ability:  `Motor de habilidades${abil?` («${abil}»)`:''}${por}. Acelera tu juego y ataca de forma constante.`,
        balanced: `Mazo consistente y equilibrado${por}. Buena curva entre ataque, PS y coste de retirada.`,
        tank:     `Estilo resistente${por}. Mucho PS y poco coste de retirada para desgastar al rival mientras devuelves daño.`
      }[arq.id];
    } else {
      const wi = coreNom ? ` around ${coreNom}${desc?` (${desc})`:''}` : '';
      strategy = {
        aggro:    `Early pressure${wi}. Attack from the first turns with cheap attacks and low retreat to keep tempo.`,
        power:    `Built${wi} as the main hitter. Load energy and survive until you land decisive hits.`,
        ability:  `Ability engine${abil?` ("${abil}")`:''}${wi}. Speed up your game and attack consistently.`,
        balanced: `Consistent, well-rounded deck${wi}. Good curve across damage, HP and retreat cost.`,
        tank:     `Tanky style${wi}. High HP and low retreat to grind the opponent down while trading damage.`
      }[arq.id];
    }

    const advantages = esLang
      ? `Solo cartas que tienes (${d.nPk} Pokémon, ${d.nTr} entrenadores, ${energyTot} energías). Líneas evolutivas completas.`
      : `Uses only cards you own (${d.nPk} Pokémon, ${d.nTr} trainers, ${energyTot} energy). Complete evolution lines.`;

    let weaknesses = esLang ? `Cuidado con mazos de tipo ${debil}.` : `Watch out for ${debil}-type decks.`;
    if((arq.id==='power' || arq.id==='tank') && ret >= 3){
      weaknesses += esLang
        ? ` Coste de retirada alto (${ret}) en el atacante: lleva cambios/saltos.`
        : ` High retreat cost (${ret}) on the attacker: include switch cards.`;
    }

    return {
      rank: String(rank),
      name: `${trType(energyType)} · ${arqNom}${coreNom ? ` · ${coreNom}` : ''} #${rank}`,
      pokemon: d.pokemon.sort((a,b)=>b.qty-a.qty),
      trainers: d.trainers.sort((a,b)=>b.qty-a.qty),
      energies: d.energies,
      difficulty: dificultad,
      metrics: calcMetricas(d, dificultad),
      strategy,
      advantages,
      weaknesses,
      colorless_notes: (d.offtype && d.offtype.length)
        ? (esLang ? `Pokémon de otros tipos incluidos para dar consistencia: ${d.offtype.join(', ')}.`
                  : `Off-type Pokémon included for consistency: ${d.offtype.join(', ')}.`)
        : ''
    };
  });

  renderDecks(decks, sel);
  let msg = esLang ? `${decks.length} mazos generados` : `${decks.length} decks generated`;
  if(compartido && decks.length < nVar){
    msg = esLang
      ? `${decks.length} mazos: el inventario compartido no alcanzó para más.`
      : `${decks.length} decks: shared inventory ran out before more.`;
  }
  showToast(msg, 'success');
}

// =============================================================
//  POBLAR LOS SELECTORES DE FILTRO DESDE EL INVENTARIO
// =============================================================
function poblarFiltrosGen(){
  if(typeof inventory === 'undefined') return;
  const sets = new Set(), series = new Set(), marcas = new Set();
  inventory.forEach(e => {
    const v = (typeof viewFromEntry === 'function') ? viewFromEntry(e) : null; if(!v) return;
    if(_setName(v)) sets.add(_setName(v));
    if(_serie(v)) series.add(_serie(v));
    if(_marca(v)) marcas.add(_marca(v));
  });
  const llenar = (id, valores) => {
    const selEl = document.getElementById(id); if(!selEl) return;
    const actual = selEl.value;
    const any = (typeof T==='function') ? T('f_any') : '(cualquiera)';
    selEl.innerHTML = `<option value="">${any}</option>` +
      Array.from(valores).sort().map(x => `<option value="${esc(x)}">${esc(x)}</option>`).join('');
    if(actual) selEl.value = actual;
  };
  llenar('f-set', sets);
  llenar('f-serie', series);
  llenar('f-marca', marcas);
}

// =============================================================
//  MÉTRICAS DE UN MAZO YA ARMADO (para el modo Versus / Sala)
//  Mismas fórmulas que calcMetricas() pero sobre un mazo guardado:
//  resuelve las vistas de carta por id (cardRegistry) o por nombre (getCardData).
// =============================================================
function calcMetricasDeck(deck){
  if(!deck) return null;
  if(deck.metrics && deck.metrics.potencial != null){
    const m = deck.metrics;
    const n = [...(deck.pokemon||[]),...(deck.trainers||[]),...(deck.energies||[])].reduce((s,c)=>s+c.qty,0);
    return { consistencia:m.consistencia, velocidad:m.velocidad, dano:m.dano, facilidad:m.facilidad, potencial:m.potencial, nCartas:n };
  }
  const vista = c => ((typeof cardRegistry!=='undefined' && c.id && cardRegistry[c.id]) ||
                      (typeof getCardData==='function' && getCardData(c.card)) || null);
  const pk = (deck.pokemon||[]).map(c=>({ qty:c.qty, v:vista(c) })).filter(x=>x.v);
  const nPk = pk.reduce((s,x)=>s+x.qty,0) || 1;
  const basics = pk.filter(x=>_esBasico(x.v)).reduce((s,x)=>s+x.qty,0);
  const lineas = new Set(pk.map(x=>x.v.nombre)).size;
  const draw = (deck.trainers||[]).reduce((s,c)=> s + (_prioVal(c.card) <= 8 ? c.qty : 0), 0);
  const avgCost = pk.length ? pk.reduce((s,x)=>s+_avgCost(x.v)*x.qty,0)/nPk : 2;
  const maxDmg = pk.reduce((m,x)=>Math.max(m,_maxDmg(x.v)),0);
  const cheap = pk.reduce((m,x)=>Math.max(m,_cheapDmg(x.v)),0);
  const nCartas = [...(deck.pokemon||[]),...(deck.trainers||[]),...(deck.energies||[])].reduce((s,c)=>s+c.qty,0);
  const dificultad = deck.difficulty || 5;
  const clamp = n => Math.max(1, Math.min(5, Math.round(n)));
  const consistencia = clamp(1 + draw/4 + (basics/nPk)*3 + (lineas<=3?1 : lineas<=5?0 : -1));
  const velocidad = clamp(1 + (avgCost<=1.6?3 : avgCost<=2.2?2 : avgCost<=2.8?1 : 0) + (cheap>=60?1:0) + (basics/nPk)*1.5);
  const dano = clamp(1 + maxDmg/70);
  const facilidad = clamp(6 - dificultad/2);
  const potencial = Math.round(((consistencia+velocidad+dano+facilidad)/20)*10*2)/2;
  return { consistencia, velocidad, dano, facilidad, potencial, nCartas };
}

// Vuelca un objeto de "reglas" (acordadas en la Sala) a los controles del generador.
function aplicarReglasAFiltros(r){
  if(!r) return;
  const set = (id,val)=>{ const e=document.getElementById(id); if(e) e.value = (val==null?'':val); };
  set('deck-type', r.deckType);
  if(typeof poblarFiltrosGen==='function') poblarFiltrosGen();   // opciones de set/serie/marca
  set('f-set', r.set); set('f-serie', r.serie); set('f-marca', r.marca);
  set('f-format', r.formato); set('f-depth', r.profundidad);
  set('f-speclimit', (r.limiteEspeciales==null?'':String(r.limiteEspeciales)));
  set('f-hpmin', r.hpMin? String(r.hpMin):'');
  const sp=document.getElementById('f-singleprize'); if(sp) sp.checked = !!r.singlePrize;
  const mecs = r.mecanicas||[];
  document.querySelectorAll('.gm-mec').forEach(c => { c.checked = mecs.indexOf(c.value) >= 0; });
  const det=document.getElementById('gen-filters'); if(det) det.open = true;
}

// =============================================================
//  CUMPLIMIENTO DE UN MAZO GUARDADO RESPECTO A LAS REGLAS PACTADAS
// =============================================================
function _vistaDeckCard(c){
  return ((typeof cardRegistry!=='undefined' && c.id && cardRegistry[c.id]) ||
          (typeof getCardData==='function' && getCardData(c.card)) || null);
}
// ¿Hay alguna regla significativa puesta?
function reglasActivas(r){
  if(!r) return false;
  if(r.deckType && r.deckType !== 'el mejor posible') return true;
  if(r.set || r.serie || r.marca || r.formato || r.profundidad) return true;
  if(r.singlePrize) return true;
  if(r.limiteEspeciales != null) return true;
  if(r.hpMin) return true;
  if(r.mecanicas && r.mecanicas.length) return true;
  return false;
}
// ¿El mazo guardado cumple las reglas pactadas? (energía básica exenta de set/serie/marca/formato)
function mazoCumpleReglas(deck, r){
  if(!deck || !reglasActivas(r)) return true;
  if(r.deckType && r.deckType !== 'el mejor posible' && deck.deckType && deck.deckType !== r.deckType) return false;
  const pks = (deck.pokemon||[]).map(c=>({ c, v:_vistaDeckCard(c) })).filter(x=>x.v);
  const trs = (deck.trainers||[]).map(c=>({ c, v:_vistaDeckCard(c) })).filter(x=>x.v);
  for(const {v} of pks.concat(trs)){
    if(r.set    && _setName(v) && _setName(v) !== r.set)   return false;
    if(r.serie  && _serie(v)   && _serie(v)   !== r.serie) return false;
    if(r.marca  && _marca(v)   && _marca(v)   !== r.marca) return false;
    if(r.formato && !_legalEn(v, r.formato)) return false;
  }
  for(const {v} of pks){
    if(r.profundidad === 'basic' && _stage(v) > 0) return false;
    if(r.profundidad === 's1'    && _stage(v) > 1) return false;
    if(r.singlePrize && _premios(v) >= 2) return false;
    if(r.hpMin && _hp(v) < r.hpMin) return false;
    if(_esEspecial(v) && r.mecanicas && r.mecanicas.length){
      const mec = _mecanicas(v);
      if(!mec.some(t => r.mecanicas.indexOf(t) >= 0)) return false;
    }
  }
  if(r.limiteEspeciales != null){
    const esp = pks.reduce((s,x)=> s + (_esEspecial(x.v) ? x.c.qty : 0), 0);
    if(esp > r.limiteEspeciales) return false;
  }
  return true;
}
