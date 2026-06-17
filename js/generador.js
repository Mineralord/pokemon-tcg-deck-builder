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

function generarMazos(){
  if(!inventory.length){ showToast(T('to_need_inv'), 'error'); return; }
  const sel = document.getElementById('deck-type').value;
  const nVar = parseInt(document.getElementById('num-variants').value) || 3;

  // Inventario con datos de carta
  const owned = inventory.map(e => ({ name: e.name, qty: e.qty, v: regCard(viewFromEntry(e)) }));
  const ownedMap = {}; const ownedNames = new Set();
  owned.forEach(o => { ownedMap[normName(o.name)] = o; ownedNames.add(normName(o.name)); });

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

  // Entrenadores disponibles (orden por utilidad aproximada)
  const PRIOR = ['professor','research','iono','arven','boss','orders','nemona','jacq','youngster','nest ball','ultra ball','great ball','switch','potion','picnicker','generator','invitation','charisma'];
  const prioVal = nm => { const n = normName(nm); let p = PRIOR.findIndex(k => n.indexOf(k) >= 0); return p < 0 ? 99 : p; };
  const trainersDisp = owned.filter(o => _esTrainer(o.v)).sort((a,b)=>prioVal(a.name)-prioVal(b.name));

  // Unidades de entrenador disponibles (máx 4 por carta), aplanadas
  const trainerUnits = [];
  trainersDisp.forEach(t => { const q = Math.min(t.qty, 4); for(let k=0;k<q;k++) trainerUnits.push({ card:t.name, id:t.v.id, img:t.v.imagenChica }); });

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
    const sumar = (nm, q) => {
      const o = ownedMap[normName(nm)]; if(!o) return;
      q = Math.min(q, o.qty, 4);
      if(q <= 0) return;
      if(!counts[nm]) counts[nm] = { qty:0, v:o.v };
      counts[nm].qty = Math.max(counts[nm].qty, q);
    };
    const totalPk = () => Object.values(counts).reduce((s,o)=>s+o.qty,0);
    const meter = (c, base) => {
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

  // Firma de la composición de Pokémon (nombre × cantidad) para evitar mazos repetidos
  const firmaPk = d => d.pokemon.map(p => p.card + 'x' + p.qty).sort().join('|');

  // Genera hasta nVar variantes DISTINTAS y lógicas: combina cada arquetipo con
  // rotación del atacante núcleo en varias pasadas y descarta composiciones repetidas.
  // Si la colección no da para tantos mazos diferentes, devuelve los que sí son únicos.
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
      vistas.add(sig);
      usedCores.add(normName(core.v.nombre));
      d.arq = arq; d.core = core.v;
      variantes.push(d);
    }
  }
  if(!variantes.length){ showToast(T('gen_none'), 'error'); renderDecks([], sel); return; }

  const esLang = (lang === 'es');
  const decks = variantes.map((d, i) => {
    const arq = d.arq, core = d.core;
    const rank = i + 1;
    const arqNom = esLang ? arq.es : arq.en;
    const energyTot = d.energies.reduce((s,c)=>s+c.qty,0);
    const ba = core ? _bestAttack(core) : null;
    const desc = _descAtaque(ba, esLang);
    const coreNom = core ? core.nombre : '';
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
  showToast(esLang ? `${decks.length} mazos generados` : `${decks.length} decks generated`, 'success');
}
