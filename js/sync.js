// =============================================================
//  SINCRONIZACIÓN EN LA NUBE (Firebase Auth con Google + Firestore)
//  - Inicio de sesión con Google (popup, con respaldo por redirect).
//  - Sesión persistente: una vez dentro, todo se sincroniza solo.
//  - Guardado inmediato + el listener en vivo NO pisa tus cambios.
//  - Si Firebase no está configurado, la app queda en el gate de login.
//
//  ROLES (modo dos jugadores con privacidad):
//   - DUEÑO (pmmt99): edita su colección. La publica en `compartido/coleccion`
//     (solo el inventario) para que P2 pueda verla en solo lectura. Sus mazos
//     guardados viven en `colecciones/{uid}` (privados).
//   - INVITADA (P2): ve la colección del dueño desde `compartido/coleccion`
//     (solo lectura) y guarda SUS mazos en `colecciones/{suUid}` (privados).
//     Ni el dueño ve los mazos de P2 ni P2 los del dueño (lo garantizan las
//     Reglas de Firestore: cada quien solo lee/escribe su propio documento).
// =============================================================
(function(){
  const noConfig = (typeof firebase === 'undefined')
    || (typeof firebaseConfig === 'undefined')
    || !firebaseConfig.apiKey
    || /PEGA_/.test(firebaseConfig.apiKey);
  // Estado visual del gate de login: 'checking' | 'out' | 'in'
  function setAuthState(s){
    document.body.classList.remove('auth-checking','auth-out','auth-in');
    document.body.classList.add('auth-'+s);
  }
  function gateMsg(txt){
    const m = document.getElementById('login-gate-msg'); if(m) m.textContent = txt || '';
  }

  if(noConfig){
    // Con login obligatorio no podemos revelar la app sin Firebase: quedamos en el gate.
    console.warn('[sync] Firebase no configurado: no se puede iniciar sesión.');
    setAuthState('out');
    gateMsg('Configuración de acceso no disponible.');
    return;
  }

  firebase.initializeApp(firebaseConfig);
  const auth = firebase.auth();
  const db = firebase.firestore();
  // Compartimos el handle de Firestore y Auth para el módulo de Sala/Versus (js/sala.js).
  window.PTCG = { db: db, auth: auth };
  try { auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL).catch(()=>{}); } catch(e){}
  try { db.enablePersistence({ synchronizeTabs: true }).catch(()=>{}); } catch(e){}
  // Procesa la vuelta del login por redirect (los errores se muestran)
  auth.getRedirectResult().catch(e => { console.error('[sync] redirect', e); if(typeof showToast==='function') showToast((typeof T==='function'?T('sync_err'):'Error'), 'error'); });

  let uid = null, esDueno = false, unsub = null, unsubInv = null, aplicandoNube = false, pushTimer = null, pushPendiente = false;

  // Dueño de la colección y lista blanca de acceso (debe coincidir con firestore.rules).
  const OWNER = 'pmmt99@gmail.com';
  const ALLOWED = ['pmmt99@gmail.com', 'elizabethrgj378@gmail.com'];
  function permitido(u){ return !!u && ALLOWED.includes((u.email || '').toLowerCase()); }

  function texto(k, alt){ return (typeof T === 'function') ? T(k) : alt; }

  // Claves de localStorage aisladas por usuario
  function invKey(id){ return 'inv_' + id; }
  function decksKey(id){ return 'decks_' + id; }

  window.loginGoogle = function(){
    const prov = new firebase.auth.GoogleAuthProvider();
    prov.setCustomParameters({ prompt: 'select_account' });
    // Popup primero: funciona en dominios cruzados (GitHub Pages + *.firebaseapp.com)
    // sin depender de cookies de terceros (que rompen signInWithRedirect en móvil/Safari/Chrome).
    auth.signInWithPopup(prov).catch(function(e){
      const code = e && e.code;
      // Si el navegador bloquea o no soporta el popup, caemos a redirect.
      if(code === 'auth/popup-blocked' || code === 'auth/operation-not-supported-in-this-environment' || code === 'auth/cancelled-popup-request' || code === 'auth/popup-closed-by-user'){
        if(code === 'auth/popup-closed-by-user') return; // el usuario cerró el popup, no es error
        auth.signInWithRedirect(prov).catch(err => { console.error('[sync] redirect', err); if(typeof showToast==='function') showToast(texto('sync_err','Error'), 'error'); });
        return;
      }
      console.error('[sync] popup', e);
      if(typeof showToast==='function') showToast(texto('sync_err','Error'), 'error');
    });
  };
  window.logoutSync = function(){ auth.signOut(); };

  function pintarBoton(){
    const b = document.getElementById('sync-btn'); if(!b) return;
    if(uid){
      const u = auth.currentUser;
      b.textContent = ((u && (u.displayName || u.email)) || '');
      b.title = texto('sync_out', 'Salir');
      b.onclick = function(){ if(confirm(texto('sync_out','Salir') + '?')) window.logoutSync(); };
    } else {
      b.textContent = texto('sync_login', 'Entrar con Google');
      b.title = '';
      b.onclick = window.loginGoogle;
    }
  }
  // ---------- Indicador de estado de sincronización ----------
  let _estado = '', _estadoTimer = null;
  function setSyncStatus(estado){
    _estado = estado;
    const el = document.getElementById('sync-status'); if(!el) return;
    clearTimeout(_estadoTimer);
    const map = { saving:['⏳','sync_saving','Guardando…'], saved:['✅','sync_saved','Guardado'], offline:['⚠️','sync_offline','Sin conexión'] };
    const m = map[estado];
    if(!m){ el.style.display = 'none'; el.textContent = ''; return; }
    el.className = 'stat-pill sync-status ' + estado;
    el.textContent = m[0] + ' ' + texto(m[1], m[2]);
    el.style.display = '';
    if(estado === 'saved'){ _estadoTimer = setTimeout(function(){ const e2=document.getElementById('sync-status'); if(e2) e2.style.display='none'; }, 2000); }
  }

  function pintarTodo(){ pintarBoton(); if(uid && _estado) setSyncStatus(_estado); }
  window.syncRepaint = pintarTodo;   // repinta botón + estado al cambiar de idioma

  // Publica el inventario del dueño en el documento compartido (solo lectura para P2).
  function publicarCompartido(){
    if(!esDueno || !uid) return;
    const invSlim = (typeof slimInventory === 'function') ? slimInventory() : inventory;
    db.collection('compartido').doc('coleccion').set({ inventory: invSlim, updatedAt: Date.now() })
      .catch(function(e){ console.error('[sync] compartido', e); });
  }

  // Sube los datos (agrupando ráfagas de cambios, pero sin perderlos)
  function flushPush(){
    clearTimeout(pushTimer); pushTimer = null;
    if(!uid || !pushPendiente) return;
    pushPendiente = false;
    setSyncStatus('saving');
    if(esDueno){
      const invSlim = (typeof slimInventory === 'function') ? slimInventory() : inventory;
      Promise.all([
        db.collection('colecciones').doc(uid).set({ inventory: invSlim, savedDecks: savedDecks, updatedAt: Date.now() }),
        db.collection('compartido').doc('coleccion').set({ inventory: invSlim, updatedAt: Date.now() })
      ]).then(function(){ setSyncStatus('saved'); })
        .catch(function(e){ console.error('[sync] error al guardar', e); setSyncStatus(navigator.onLine ? 'saved' : 'offline'); });
    } else {
      // P2: solo sus mazos privados; nunca el inventario (es del dueño, solo lectura).
      db.collection('colecciones').doc(uid).set({ inventory: [], savedDecks: savedDecks, updatedAt: Date.now() })
        .then(function(){ setSyncStatus('saved'); })
        .catch(function(e){ console.error('[sync] error al guardar', e); setSyncStatus(navigator.onLine ? 'saved' : 'offline'); });
    }
  }
  window.syncPush = function(){
    if(!uid || aplicandoNube) return;
    pushPendiente = true;
    if(!navigator.onLine){ setSyncStatus('offline'); }
    clearTimeout(pushTimer);
    pushTimer = setTimeout(flushPush, 300);
  };
  // No perder cambios al cerrar/cambiar de app o pestaña
  document.addEventListener('visibilitychange', function(){ if(document.visibilityState === 'hidden') flushPush(); });
  window.addEventListener('pagehide', flushPush);
  window.addEventListener('beforeunload', flushPush);
  // Estado de conexión: al volver online, reintentar; al perderla, avisar
  window.addEventListener('offline', function(){ if(uid) setSyncStatus('offline'); });
  window.addEventListener('online', function(){ if(uid){ pushPendiente = true; flushPush(); } });

  function cacheLocal(){
    try {
      if(uid){
        const invSlim = (typeof slimInventory === 'function') ? slimInventory() : inventory;
        localStorage.setItem(invKey(uid), JSON.stringify(invSlim));
        localStorage.setItem(decksKey(uid), JSON.stringify(savedDecks));
      }
    } catch(e){}
  }

  // Aplica un documento de colección completo (inventario + mazos): para el DUEÑO.
  function aplicarDoc(d){
    if(!d) return;
    aplicandoNube = true;
    if(Array.isArray(d.inventory)){
      inventory = d.inventory;
      if(typeof asegurarId === 'function') inventory.forEach(asegurarId);
    }
    if(Array.isArray(d.savedDecks)) savedDecks = d.savedDecks;
    cacheLocal();
    renderAll();
    aplicandoNube = false;
  }

  // Solo los mazos guardados (para P2: su propio documento privado).
  function aplicarMazos(d){
    aplicandoNube = true;
    if(d && Array.isArray(d.savedDecks)) savedDecks = d.savedDecks;
    else if(!d) savedDecks = savedDecks || [];
    cacheLocal();
    if(typeof renderSaved === 'function') renderSaved();
    if(typeof updateStats === 'function') updateStats();
    aplicandoNube = false;
  }

  // Solo el inventario compartido del dueño (para P2: solo lectura).
  function aplicarInventario(d){
    aplicandoNube = true;
    inventory = (d && Array.isArray(d.inventory)) ? d.inventory : [];
    if(typeof asegurarId === 'function') inventory.forEach(asegurarId);
    cacheLocal();
    if(typeof renderInventory === 'function') renderInventory();
    if(typeof renderLegal === 'function') renderLegal();
    if(typeof updateStats === 'function') updateStats();
    aplicandoNube = false;
  }

  function renderAll(){
    if(typeof renderInventory === 'function') renderInventory();
    if(typeof renderLegal === 'function') renderLegal();
    if(typeof renderSaved === 'function') renderSaved();
    if(typeof updateStats === 'function') updateStats();
  }

  auth.onAuthStateChanged(function(u){
    if(unsub){ unsub(); unsub = null; }
    if(unsubInv){ unsubInv(); unsubInv = null; }

    // Sin sesión: mostrar el gate y vaciar la pantalla (la caché por-uid se conserva).
    if(!u){
      uid = null; esDueno = false; window.__ptcgUid = null; window.__esDueno = false;
      inventory = []; savedDecks = [];
      pintarBoton();
      setSyncStatus('');
      if(typeof aplicarPermisosDueno === 'function') aplicarPermisosDueno(false);
      if(typeof window.salaLogout === 'function') window.salaLogout();
      renderAll();
      setAuthState('out');
      return;
    }

    // Cuenta NO autorizada: denegar acceso y cerrar sesión.
    if(!permitido(u)){
      uid = null; esDueno = false; window.__ptcgUid = null; window.__esDueno = false;
      inventory = []; savedDecks = [];
      if(typeof window.salaLogout === 'function') window.salaLogout();
      renderAll();
      gateMsg(texto('login_denied', 'Esa cuenta no tiene acceso.'));
      setAuthState('out');
      auth.signOut();
      return;
    }

    // Cuenta autorizada: conceder acceso.
    uid = u.uid; window.__ptcgUid = uid;
    esDueno = ((u.email || '').toLowerCase() === OWNER); window.__esDueno = esDueno;
    gateMsg('');
    setAuthState('in');
    pintarBoton();
    if(typeof aplicarPermisosDueno === 'function') aplicarPermisosDueno(esDueno);
    if(typeof window.salaLogin === 'function') window.salaLogin(uid, esDueno, u);

    // Carga instantánea desde la caché local por-uid (offline / mientras llega la nube)
    aplicandoNube = true;
    try {
      const ci = localStorage.getItem(invKey(uid));
      const cd = localStorage.getItem(decksKey(uid));
      inventory = ci ? JSON.parse(ci) : [];
      savedDecks = cd ? JSON.parse(cd) : [];
    } catch(e){ inventory = []; savedDecks = []; }
    if(typeof asegurarId === 'function') inventory.forEach(asegurarId);
    renderAll();
    aplicandoNube = false;

    if(esDueno){
      // DUEÑO: su documento completo (inventario + mazos) y publica el compartido.
      const ref = db.collection('colecciones').doc(uid);
      ref.get().then(function(snap){
        const d = snap.exists ? snap.data() : null;
        const nubeTiene = d && ((Array.isArray(d.inventory) && d.inventory.length) || (Array.isArray(d.savedDecks) && d.savedDecks.length));
        if(nubeTiene){
          aplicarDoc(d);
        } else if(inventory.length || savedDecks.length){
          pushPendiente = true; flushPush();
        }
        publicarCompartido();   // asegura que P2 vea el inventario aunque el dueño no edite nada
        unsub = ref.onSnapshot(function(s){
          if(s.metadata && s.metadata.hasPendingWrites) return;
          aplicarDoc(s.data());
        });
      }).catch(e => console.error('[sync] get', e));
    } else {
      // INVITADA (P2): sus mazos desde su doc; el inventario, del compartido (solo lectura).
      const ref = db.collection('colecciones').doc(uid);
      ref.get().then(function(snap){
        if(snap.exists) aplicarMazos(snap.data());
        unsub = ref.onSnapshot(function(s){
          if(s.metadata && s.metadata.hasPendingWrites) return;
          aplicarMazos(s.data());
        });
      }).catch(e => console.error('[sync] get', e));
      const refInv = db.collection('compartido').doc('coleccion');
      unsubInv = refInv.onSnapshot(function(s){ aplicarInventario(s.data()); },
        function(e){ console.error('[sync] compartido onSnapshot', e); });
    }
  });
})();
