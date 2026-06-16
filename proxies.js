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
    const s = document.createElement('script');
    s.src = 'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.2/jspdf.umd.min.js';
    s.onload = () => (window.jspdf && window.jspdf.jsPDF) ? resolve(window.jspdf.jsPDF) : reject(new Error('jspdf'));
    s.onerror = () => reject(new Error('jspdf'));
    document.head.appendChild(s);
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
    img.src = src;
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
  let fallos = 0;
  for(let i = 0; i < lista.length; i++){
    const idx = i % L.perPage;
    if(i > 0 && idx === 0) doc.addPage();
    const col = idx % L.cols, row = Math.floor(idx / L.cols);
    const x = L.marginX + col * L.cardW, y = L.marginY + row * L.cardH;
    let data;
    try { data = await proxNormalize(lista[i]); } catch(e){ fallos++; continue; }
    try { doc.addImage(data, 'JPEG', x, y, L.cardW, L.cardH); } catch(e){ fallos++; continue; }
    if(cut) proxCutMarks(doc, x, y, L.cardW, L.cardH);
  }
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
