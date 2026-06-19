// =============================================================
//  SERVICE WORKER — caché de la app + imágenes de cartas
//  - App (HTML/CSS/JS): network-first (siempre lo último online,
//    y funciona offline desde caché).
//  - Imágenes de cartas: cache-first con tope (rápidas y offline).
//  - APIs (pokemontcg / tcgdex / Firebase / Google): SIN cachear
//    (pasan directas a la red para no servir datos viejos).
// =============================================================
// Sube VER en cada deploy que cambie ficheros del shell para forzar limpieza de caché.
const VER   = '31';
const SHELL = 'ptcg-shell-v' + VER;
const IMGS  = 'ptcg-img-v' + VER;
const MAX_IMG = 600;

const SHELL_FILES = [
  './index.html',
  './css/estilos.css', './css/juego.css',
  './data/cartas-db.js',
  './js/cartas-api.js', './js/energias.js', './js/idiomas.js',
  './js/app.js', './js/generador.js', './js/proxies.js', './js/sala.js',
  './js/sonido.js', './js/juego.js', './js/juego-efectos.js', './js/juego-ia.js', './js/juego-ui.js',
  './js/firebase-config.js', './js/sync.js',
  './manifest.json', './assets/icon.svg',
  './assets/icon-192.png', './assets/icon-512.png'
];

const IMG_HOSTS = ['images.pokemontcg.io', 'images.scrydex.com', 'assets.tcgdex.net'];
const CDN_HOSTS = ['fonts.googleapis.com', 'fonts.gstatic.com', 'www.gstatic.com'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(SHELL).then(c => c.addAll(SHELL_FILES)).then(() => self.skipWaiting()).catch(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.filter(k => k !== SHELL && k !== IMGS).map(k => caches.delete(k))
    )).then(() => self.clients.claim())
  );
});

async function trim(cache, max){
  const keys = await cache.keys();
  if(keys.length > max){ for(let i = 0; i < keys.length - max; i++) await cache.delete(keys[i]); }
}
async function cacheFirst(req, cacheName, max){
  const cache = await caches.open(cacheName);
  const hit = await cache.match(req);
  if(hit) return hit;
  try{
    const res = await fetch(req);
    if(res && (res.ok || res.type === 'opaque')){ cache.put(req, res.clone()); if(max) trim(cache, max); }
    return res;
  }catch(e){ return hit || Response.error(); }
}
async function networkFirst(req, cacheName){
  const cache = await caches.open(cacheName);
  try{
    const res = await fetch(req);
    if(res && res.ok) cache.put(req, res.clone());
    return res;
  }catch(e){
    const hit = await cache.match(req);
    return hit || (await cache.match('./index.html')) || Response.error();
  }
}

self.addEventListener('fetch', e => {
  const req = e.request;
  if(req.method !== 'GET') return;                 // solo GET
  let url; try { url = new URL(req.url); } catch(_){ return; }
  const host = url.hostname;

  if(IMG_HOSTS.includes(host)){ e.respondWith(cacheFirst(req, IMGS, MAX_IMG)); return; }   // imágenes
  if(url.origin === self.location.origin){ e.respondWith(networkFirst(req, SHELL)); return; } // app
  if(CDN_HOSTS.includes(host)){ e.respondWith(cacheFirst(req, SHELL)); return; }            // fuentes / SDK Firebase
  // resto (api.pokemontcg.io, *.googleapis.com, firebaseapp.com, oauth...) -> red directa
});
