// =============================================================
//  SÍMBOLOS DE ENERGÍA (SVG propios, sin emojis) + imagen de las
//  cartas de energía básica (set g1 de la API, cubre los 9 tipos).
// =============================================================

const ENERGY_COLOR = {
  Grass:'#5FB36B', Fire:'#E8533F', Water:'#4FA4DE', Lightning:'#F2C32B',
  Psychic:'#9B5BA5', Fighting:'#C75B36', Darkness:'#3C3A56', Metal:'#8C97A6',
  Dragon:'#C9A227', Fairy:'#E06FA9', Colorless:'#C9C7BE'
};
const ENERGY_GLYPH = {
  Grass:'<path d="M7 17C6 10 11 6 18 6c1 7-4 11-11 11Z" fill="#fff"/><path d="M9.5 15 16 8.5" stroke="#5FB36B" stroke-width="1.3" fill="none"/>',
  Fire:'<path d="M12 4c2 3.5 4 5 4 8a4 4 0 0 1-8 0c0-2 1-3 2-4 0 1 .6 1.6 1.2 1.8C11 8 11.5 6 12 4Z" fill="#fff"/>',
  Water:'<path d="M12 5s5 5.5 5 8.5a5 5 0 0 1-10 0C7 10.5 12 5 12 5Z" fill="#fff"/>',
  Lightning:'<polygon points="14,3 7.5,13 11,13 10,21 16.5,10 12.5,10" fill="#fff"/>',
  Psychic:'<circle cx="12" cy="12" r="4.3" fill="#fff"/><circle cx="12" cy="12" r="1.7" fill="#9B5BA5"/>',
  Fighting:'<polygon points="12,6 18,12 12,18 6,12" fill="#fff"/>',
  Darkness:'<path d="M15 6a6 6 0 1 0 0 12 5 5 0 0 1 0-12Z" fill="#fff"/>',
  Metal:'<polygon points="12,5 18,8.5 18,15.5 12,19 6,15.5 6,8.5" fill="#fff"/>',
  Dragon:'<polygon points="12,6 18,17 6,17" fill="#fff"/>',
  Fairy:'<path d="M12 5l1.8 4.9 5.2.2-4.1 3.2 1.5 5-4.4-3-4.4 3 1.5-5L5 10.3l5.2-.2Z" fill="#fff"/>',
  Colorless:'<circle cx="12" cy="12" r="4.6" fill="#fff"/>'
};
function _energySvg(t){
  const color = ENERGY_COLOR[t] || '#7b8498';
  const glyph = ENERGY_GLYPH[t] || '<text x="12" y="16" text-anchor="middle" font-size="11" fill="#fff">?</text>';
  return '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">'
    + `<circle cx="12" cy="12" r="11" fill="${color}"/>`
    + '<circle cx="12" cy="9" r="7.5" fill="#fff" opacity="0.13"/>'
    + glyph
    + '<circle cx="12" cy="12" r="11" fill="none" stroke="#000" stroke-opacity="0.18" stroke-width="1"/>'
    + '</svg>';
}
const ENERGY_SYMBOL = {};
Object.keys(ENERGY_COLOR).forEach(t => { ENERGY_SYMBOL[t] = _energySvg(t); });

// Archivo de imagen por tipo (tus PNG en assets/energias/). Si un tipo no está
// aquí, se usa "<tipo>.svg"; y si tampoco existe, el SVG en línea de respaldo.
const ENERGY_FILE = {
  Grass:'GRASS ENERGY.png', Fire:'FIRE ENERGY.png', Water:'WATER ENERGY.png',
  Lightning:'ELECTRIC ENERGY.png', Psychic:'PSYCHIC ENERGY.png', Fighting:'STRENGHT SNERGY.png',
  Darkness:'DARK ENERGY.png', Metal:'METAL ENERGY.png', Colorless:'COLORLESS ENERGY.png'
  // Dragon y Fairy: no diste PNG -> usan el vector de respaldo (.svg)
};

// Símbolo de energía. Usa tu archivo de assets/energias/ si existe; si falta,
// recurre al SVG en línea (respaldo). Cero riesgo de que se rompa.
function energyIcon(t){
  const label = (typeof trType === 'function') ? trType(t) : t;
  const file = ENERGY_FILE[t] || (String(t || '').toLowerCase() + '.svg');
  return `<span class="ener-sym" title="${label}" aria-label="${label}">`
    + `<img src="assets/energias/${encodeURI(file)}" alt="${label}" `
    + `onerror="this.parentNode.innerHTML=(ENERGY_SYMBOL['${t}']||ENERGY_SYMBOL.Colorless||'')">`
    + `</span>`;
}

// Imagen real de la carta de energía básica (set g1)
const ENERGY_CARD_IMG = {
  Grass:'75', Fire:'76', Water:'77', Lightning:'78', Psychic:'79',
  Fighting:'80', Darkness:'81', Metal:'82', Fairy:'83'
};
(function(){
  Object.keys(ENERGY_CARD_IMG).forEach(t => {
    const n = ENERGY_CARD_IMG[t];
    ENERGY_CARD_IMG[t] = {
      small: 'https://images.pokemontcg.io/g1/' + n + '.png',
      large: 'https://images.pokemontcg.io/g1/' + n + '_hires.png'
    };
  });
})();

function _esEnergiaSup(v){ return v && /energy|energ/i.test(v.supertipo || ''); }

// Deduce el tipo de una energía por sus tipos o por el nombre "Basic X Energy"
function energyTypeOf(v){
  if (v && v.tipos && v.tipos[0]) return v.tipos[0];
  const m = /basic\s+(\w+)\s+energy/i.exec((v && v.nombre) || '');
  if (m){ return m[1].charAt(0).toUpperCase() + m[1].slice(1).toLowerCase(); }
  return null;
}

// Imagen de carta de energía a partir del nombre (para mazos antiguos)
function energyImgByName(name){
  const t = energyTypeOf({ nombre: name });
  return (t && ENERGY_CARD_IMG[t]) ? ENERGY_CARD_IMG[t].small : null;
}

// Si la vista es una energía sin imagen, le pone la imagen real de carta g1
function enriquecerEnergia(v){
  if (!v || v.imagenChica || !_esEnergiaSup(v)) return v;
  const t = energyTypeOf(v);
  const img = t && ENERGY_CARD_IMG[t];
  if (img){ v.imagenChica = img.small; v.imagenGrande = img.large; }
  return v;
}
