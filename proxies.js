// =============================================================
//  GENERADOR DE PDF DE PROXIES  (solo para el dueño)
//  - Subir/arrastrar imágenes y/o añadir desde la colección.
//  - PDF a tamaño real: 63 × 88 mm por carta, 9 por hoja (3×3),
//    en Carta o A4, con líneas de corte opcionales.
//  Todo en el navegador (jsPDF, carga diferida).
// =============================================================
const OWNER_EMAIL = 'pmmt99@gmail.com';
let proxImgs = [];   // [{ src, copies }]

// ---- Permisos: mostrar la herramienta solo al dueño ----
function aplicarPermisosDueno(esDueno){
  const nav = document.getElementById('nav-proxies');
  if(nav) nav.style.display = esDueno ? '' : 'none';
  if(!esDueno){
    const v = document.getElementById('view-proxies');
    if(v && v.classList.contains('active') && typeof showView === 'function') showView('coleccion');
  }
}

// ---- Añadir imágenes ----
function proxAddFiles(files){
  const arr = Array.from(files || []);
  arr.forEach(f => {
    if(!/^image\//.test(f.type)) return;
    const r = new FileReader();
    r.onload = () => { proxImgs.push({ src: r.result, copies: 1 }); renderProxies(); };
    r.readAsDataURL(f);
  });
}
function proxAddFromCollection(){
  if(typeof inventory === 'undefined' || !inventory.length){ showToast(T('px_no_col'), 'error'); return; }
  let n = 0;
  inventory.forEach(e => {
    const v = (typeof viewFromEntry === 'function') ? viewFromEntry(e) : null;
    const img = v && (v.imagenGrande || v.imagenChica);
    if(img){ proxImgs.push({ src: img, copies: (e.qty && e.qty > 0 ? Math.min(e.qty, 9) : 1) }); n++; }
  });
  renderProxies();
  showToast(n + ' ' + T('px_added'), 'success');
}
function proxCopies(i, d){ if(proxImgs[i]){ proxImgs[i].copies = Math.max(1, (proxImgs[i].copies || 1) + d); renderProxies(); } }
function proxRemove(i){ proxImgs.splice(i, 1); renderProxies(); }
function proxVaciar(){ proxImgs = []; renderProxies(); }

// ---- Buscar cualquier carta (en vivo) y añadirla ----
let proxFiltros = { name: '', type: '', setId: '', orderBy: 'name' };
let proxLang = 'es';   // idioma de las imágenes a imprimir: 'es' (TCGdex) | 'en' (pokemontcg)
let proxPage = 1, proxResultados = {}, proxReq = 0, _proxTimer = null;

function proxSetLang(l){
  proxLang = l;
  document.querySelectorAll('#prox-lang button').forEach(b => b.classList.toggle('active', b.getAttribute('data-l') === l));
  proxFiltros.setId = '';        // los ids de set difieren entre ES (TCGdex) y EN (pokemontcg)
  if(typeof proxLlenarSets === 'function') proxLlenarSets();
  proxBuscar(true);
}

function proxOnSearchInput(val){
  proxFiltros.name = val;
  clearTimeout(_proxTimer);
  _proxTimer = setTimeout(() => proxBuscar(true), 350);
}
function proxSetFiltro(campo, val){ proxFiltros[campo] = val; proxBuscar(true); }
// Rellena el desplegable de tipo (fijo) y el de set (según idioma)
function proxLlenarFiltros(){
  const tSel = document.getElementById('prox-f-type');
  if(tSel && !tSel.dataset.filled){
    const tipos = ['Fire','Water','Lightning','Grass','Psychic','Fighting','Darkness','Metal','Fairy','Dragon','Colorless'];
    const trT = (typeof trType === 'function') ? trType : (x => x);
    tSel.innerHTML = `<option value="">${esc(T('f_type'))}</option>` + tipos.map(t => `<option value="${t}">${esc(trT(t))}</option>`).join('');
    tSel.dataset.filled = '1';
  }
  proxLlenarSets();
}
// El set depende del idioma: ES usa los sets de TCGdex (ids que coinciden con su búsqueda); EN usa los de pokemontcg
function proxLlenarSets(){
  const sSel = document.getElementById('prox-f-set'); if(!sSel) return;
  const opt = s => `<option value="${esc(s.id)}">${esc(s.name + (s.serie ? (' · ' + s.serie) : (s.series ? (' · ' + s.series) : '')))}</option>`;
  const pintar = (sets) => {
    sSel.innerHTML = `<option value="">${esc(T('f_set'))}</option>` + (sets || []).map(opt).join('');
    sSel.value = proxFiltros.setId || '';
  };
  if(proxLang === 'es'){
    pintar([]);
    if(typeof cargarSetsSerieTcgdex === 'function') cargarSetsSerieTcgdex('es').then(pintar).catch(() => {});
  } else {
    const sets = (typeof _setsList !== 'undefined' && _setsList) ? _setsList : [];
    pintar(sets);
    if(!sets.length && typeof cargarSets === 'function'){ cargarSets().then(() => proxLlenarSets()).catch(() => {}); }
  }
}
async function proxBuscar(reset){
  if(typeof apiBuscar !== 'function') return;
  if(reset){ proxPage = 1; proxResultados = {}; }
  const grid = document.getElementById('prox-search-grid');
  const status = document.getElementById('prox-search-status');
  const more = document.getElementById('prox-search-more');
  if(!proxFiltros.name || !proxFiltros.name.trim()){
    if(grid) grid.innerHTML = ''; if(status) status.textContent = ''; if(more) more.style.display = 'none';
    return;
  }
  const myReq = ++proxReq;
  if(status) status.textContent = T('px_searching');
  try{
    const r = (proxLang === 'es' && typeof tcgdexBuscar === 'function')
      ? await tcgdexBuscar(proxFiltros, proxPage, 'es')
      : await apiBuscar(proxFiltros, proxPage);
    if(myReq !== proxReq) return;
    r.cards.forEach(c => { if(c && c.id) proxResultados[c.id] = c; });
    renderProxSearch();
    if(status) status.textContent = r.totalCount ? (r.totalCount + ' ' + T('px_search_results')) : T('px_no_results');
    if(more) more.style.display = (Object.keys(proxResultados).length < r.totalCount) ? '' : 'none';
  }catch(e){ if(myReq === proxReq && status) status.textContent = T('exp_error'); }
}
function proxMasResultados(){ proxPage++; proxBuscar(false); }
function renderProxSearch(){
  const grid = document.getElementById('prox-search-grid'); if(!grid) return;
  const list = Object.keys(proxResultados).map(k => proxResultados[k]);
  grid.innerHTML = list.map(v => {
    const img = v.imagenChica || v.imagenGrande;
    const idq = esc(String(v.id).replace(/'/g, "\\'"));
    return `<div class="prox-res">
      <div class="prox-res-img">
        ${img ? `<img src="${esc(img)}" alt="${esc(v.nombre)}" loading="lazy">` : `<div class="noimg">${esc(v.nombre)}</div>`}
        <button class="prox-res-add" aria-label="${esc(T('px_add'))}" title="${esc(T('px_add'))}" onclick="proxAddResult('${idq}')">＋</button>
      </div>
      <div class="prox-res-name" title="${esc(v.nombre)}">${esc(v.nombre)}</div>
      <div class="prox-res-set">${esc((v.set && v.set.nombre) || '')}${v.numeroCarta ? (' · ' + esc(v.numeroCarta)) : ''}</div>
    </div>`;
  }).join('');
}
function proxAddResult(id){
  const v = proxResultados[id]; if(!v) return;
  const img = v.imagenGrande || v.imagenChica; if(!img){ showToast(T('px_empty'), 'error'); return; }
  proxImgs.push({ src: img, copies: 1 });
  renderProxies();
  showToast((v.nombre || '') + ' +1', 'success');
}

function renderProxies(){
  const grid = document.getElementById('prox-grid'); if(!grid) return;
  const tot = document.getElementById('prox-total');
  const total = proxImgs.reduce((s, e) => s + (e.copies || 1), 0);
  if(tot) tot.textContent = total;
  if(!proxImgs.length){ grid.innerHTML = `<div class="empty-grid">${T('px_empty_grid')}</div>`; return; }
  grid.innerHTML = proxImgs.map((e, i) => `
    <div class="prox-tile">
      <img src="${esc(e.src)}" alt="" loading="lazy">
      <div class="prox-tile-bar">
        <button aria-label="-1" onclick="proxCopies(${i},-1)">−</button>
        <span class="px-copies">×${e.copies}</span>
        <button aria-label="+1" onclick="proxCopies(${i},1)">+</button>
        <button class="prox-del" aria-label="quitar" onclick="proxRemove(${i})">✕</button>
      </div>
    </div>`).join('');
}

// ---- Maquetación (mm) ----
function proxLayout(size){
  const pageW = size === 'a4' ? 210 : 215.9;
  const pageH = size === 'a4' ? 297 : 279.4;
  const cardW = 63, cardH = 88, cols = 3, rows = 3;
  return { pageW, pageH, cardW, cardH, cols, rows, perPage: cols * rows,
    marginX: (pageW - cols * cardW) / 2, marginY: (pageH - rows * cardH) / 2 };
}
function proxCutMarks(doc, x, y, w, h){
  doc.setDrawColor(150); doc.setLineWidth(0.1);
  const m = 3;
  const esq = [[x, y, -m, -m], [x + w, y, m, -m], [x, y + h, -m, m], [x + w, y + h, m, m]];
  esq.forEach(([px, py, sx, sy]) => { doc.line(px, py, px + sx, py); doc.line(px, py, px, py + sy); });
}

// ---- Cargar jsPDF bajo demanda ----
let _jspdfPromise = null;
function cargarJsPDF(){
  if(window.jspdf && window.jspdf.jsPDF) return Promise.resolve(window.jspdf.jsPDF);
  if(_jspdfPromise) return _jspdfPromise;
  _jspdfPromise = new Promise((resolve, reject) => {
    const urls = [
      'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js',
      'https://unpkg.com/jspdf@2.5.1/dist/jspdf.umd.min.js'
    ];
    let i = 0;
    function intentar(){
      const s = document.createElement('script');
      s.src = urls[i];
      s.onload = () => (window.jspdf && window.jspdf.jsPDF) ? resolve(window.jspdf.jsPDF) : siguiente();
      s.onerror = siguiente;
      document.head.appendChild(s);
    }
    function siguiente(){ i++; if(i < urls.length) intentar(); else reject(new Error('jspdf')); }
    intentar();
  });
  return _jspdfPromise;
}

// Normaliza cualquier imagen a JPEG dataURL (uniforma formatos y controla peso)
function proxNormalize(src){
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      const maxH = 1000;
      let w = img.naturalWidth, h = img.naturalHeight;
      if(!w || !h){ reject(new Error('size')); return; }
      if(h > maxH){ w = Math.round(w * maxH / h); h = maxH; }
      const cv = document.createElement('canvas'); cv.width = w; cv.height = h;
      cv.getContext('2d').drawImage(img, 0, 0, w, h);
      try { resolve(cv.toDataURL('image/jpeg', 0.92)); } catch(e){ reject(e); }
    };
    img.onerror = () => reject(new Error('img'));
    // En URLs remotas añadimos un parámetro para forzar una petición CORS limpia
    // (evita reutilizar una respuesta "opaca" cacheada por el Service Worker -> canvas "tainted").
    const esRemota = /^https?:/i.test(src);
    img.src = esRemota ? (src + (src.indexOf('?') >= 0 ? '&' : '?') + 'proxycb=1') : src;
  });
}

async function proxGenerarPDF(){
  if(!proxImgs.length){ showToast(T('px_empty'), 'error'); return; }
  let JsPDF;
  try { JsPDF = await cargarJsPDF(); } catch(e){ showToast(T('px_lib_err'), 'error'); return; }
  const size = (document.getElementById('prox-size') || {}).value || 'letter';
  const cut = (document.getElementById('prox-cut') || {}).checked;
  const L = proxLayout(size);
  showToast(T('px_generating'), 'success');
  const doc = new JsPDF({ unit: 'mm', format: size, orientation: 'portrait' });
  const lista = [];
  proxImgs.forEach(e => { for(let k = 0; k < (e.copies || 1); k++) lista.push(e.src); });
  let colocadas = 0, fallos = 0;
  for(let i = 0; i < lista.length; i++){
    let data;
    try { data = await proxNormalize(lista[i]); } catch(e){ fallos++; continue; }
    const idx = colocadas % L.perPage;
    if(colocadas > 0 && idx === 0) doc.addPage();
    const col = idx % L.cols, row = Math.floor(idx / L.cols);
    const x = L.marginX + col * L.cardW, y = L.marginY + row * L.cardH;
    try { doc.addImage(data, 'JPEG', x, y, L.cardW, L.cardH); } catch(e){ fallos++; continue; }
    if(cut) proxCutMarks(doc, x, y, L.cardW, L.cardH);
    colocadas++;
  }
  if(!colocadas){ showToast(T('px_none'), 'error'); return; }
  doc.save('proxies.pdf');
  showToast(T('px_done') + (fallos ? (' (' + fallos + ' ' + T('px_failed') + ')') : ''), 'success');
}

// ---- Zona de arrastre ----
(function initProxDrop(){
  const dz = document.getElementById('prox-drop');
  if(!dz) return;
  ['dragenter', 'dragover'].forEach(ev => dz.addEventListener(ev, e => { e.preventDefault(); dz.classList.add('drag'); }));
  ['dragleave', 'drop'].forEach(ev => dz.addEventListener(ev, e => { e.preventDefault(); dz.classList.remove('drag'); }));
  dz.addEventListener('drop', e => { if(e.dataTransfer && e.dataTransfer.files) proxAddFiles(e.dataTransfer.files); });
})();
