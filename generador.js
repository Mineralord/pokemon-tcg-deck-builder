// =============================================================
//  GENERADOR DE MAZOS (por reglas, en el navegador)
//  Arma mazos válidos de 60 cartas con la colección del usuario:
//   - solo cartas que posee, respetando cantidades (máx 4, energía básica libre)
//   - líneas evolutivas legales (incluye las preevoluciones que tienes)
//   - energía básica del tipo elegido para completar
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

// Preevoluciones necesarias para una carta (por nombre)
function _requeridas(v){
  if (typeof evoChains !== 'undefined' && evoChains[v.nombre]) return evoChains[v.nombre].slice();
  return v.evolucionaDe ? [v.evolucionaDe] : [];
}

// Debilidad/ventaja aproximada por tipo (para el texto de análisis)
const _DEBIL = { Fire:'Water', Water:'Lightning', Grass:'Fire', Lightning:'Fighting',
  Psychic:'Darkness', Darkness:'Fighting', Fighting:'Psychic', Colorless:'Fighting' };

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

  // Candidatos: Pokémon del tipo, legales, con ataque; ordenados por fuerza
  let candidatos = owned.filter(o => _esPokemon(o.v) && encaja(o.v) && legal(o.v));
  candidatos.forEach(o => { o.score = _maxDmg(o.v) + (parseInt(o.v.ps)||0)/12 + (_esEx(o.v)?45:0); });
  candidatos.sort((a,b)=>b.score-a.score);

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

  // Construye una variante con objetivos {P, T, E} (Pokémon/Entrenadores/Energía ≈ 20)
  function construir(tg){
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
    const extra = owned.filter(o => _esPokemon(o.v) && legal(o.v)).sort((a,b)=> _maxDmg(b.v) - _maxDmg(a.v));
    const llenarPokemon = objetivo => {
      for(const c of candidatos){ if(totalPk() >= objetivo) break; meter(c, 4); }
      for(const c of extra){ if(totalPk() >= objetivo) break; if(counts[c.v.nombre]) continue; meter(c, 4); }
    };

    // Reparto buscando ~20/20/20 con la colección disponible
    let nTr = Math.min(tg.T, trainerUnits.length);          // entrenadores (idealmente 20)
    llenarPokemon(Math.min(34, Math.max(14, 60 - nTr - tg.E))); // Pokémon para dejar ~E energías
    let nPk = totalPk();
    let energy = 60 - nTr - nPk;
    // Si sobra energía, usa más entrenadores que tengas (hasta agotar) y luego más Pokémon
    if(energy > tg.E && trainerUnits.length > nTr){
      const add = Math.min(trainerUnits.length - nTr, energy - tg.E);
      nTr += add; energy -= add;
    }
    if(energy > tg.E){
      llenarPokemon(Math.min(34, nPk + (energy - tg.E)));
      nPk = totalPk(); energy = 60 - nTr - nPk;
    }
    // Construir listas con imágenes
    let pokemon = Object.keys(counts).map(nm => ({ qty: counts[nm].qty, card: nm, id: counts[nm].v.id, img: counts[nm].v.imagenChica }));
    // Recorta si nos pasamos de 60 (las líneas pueden sobrepasar el objetivo)
    if(energy < 0){
      pokemon.sort((a,b)=>a.qty-b.qty);
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

  // Genera N variantes alrededor de 20/20/20 (con ligeras variaciones de énfasis)
  const objetivos = [
    {P:20,T:20,E:20}, {P:18,T:22,E:20}, {P:22,T:18,E:20}, {P:20,T:18,E:22}, {P:18,T:20,E:22}, {P:22,T:20,E:18}
  ].slice(0, nVar);
  let variantes = objetivos.map(construir).filter(d => d.nPk > 0);
  // dedup por composición y orden por score
  variantes.sort((a,b)=>b.score-a.score);

  const tipoNom = trType(energyType);
  const debil = trType(_DEBIL[tipo] || 'Fighting');
  const esLang = (lang === 'es');
  const decks = variantes.map((d, i) => {
    const rank = i + 1;
    const dificultad = Math.min(10, Math.max(2, Math.round(4 + d.nPk/5 + (rank-1))));
    return {
      rank: String(rank),
      name: (esLang ? `Mazo ${tipoNom} #${rank}` : `${tipoNom} Deck #${rank}`),
      pokemon: d.pokemon.sort((a,b)=>b.qty-a.qty),
      trainers: d.trainers.sort((a,b)=>b.qty-a.qty),
      energies: d.energies,
      difficulty: dificultad,
      strategy: esLang
        ? `Mazo de tipo ${tipoNom} centrado en tus mejores atacantes. Evoluciona y ataca de forma constante; usa los entrenadores para buscar piezas y energía.`
        : `${tipoNom}-type deck built around your strongest attackers. Evolve and attack consistently; use trainers to find pieces and energy.`,
      advantages: esLang
        ? `Usa solo cartas que tienes (${d.nPk} Pokémon, ${d.nTr} entrenadores, ${d.energies.reduce((s,c)=>s+c.qty,0)} energías). Líneas evolutivas completas.`
        : `Uses only cards you own (${d.nPk} Pokémon, ${d.nTr} trainers, ${d.energies.reduce((s,c)=>s+c.qty,0)} energy). Complete evolution lines.`,
      weaknesses: esLang ? `Cuidado con mazos de tipo ${debil}.` : `Watch out for ${debil}-type decks.`,
      colorless_notes: (d.offtype && d.offtype.length)
        ? (esLang ? `Pokémon de otros tipos incluidos para dar consistencia: ${d.offtype.join(', ')}.`
                  : `Off-type Pokémon included for consistency: ${d.offtype.join(', ')}.`)
        : ''
    };
  });

  renderDecks(decks, sel);
  showToast(esLang ? `${decks.length} mazos generados` : `${decks.length} decks generated`, 'success');
}
