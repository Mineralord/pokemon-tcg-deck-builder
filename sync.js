// =============================================================
//  SINCRONIZACIÓN EN LA NUBE (Firebase Auth con Google + Firestore)
//  - Inicio de sesión con Google por REDIRECT (fiable en móvil).
//  - Sesión persistente: una vez dentro, queda permanente y todo
//    se sincroniza solo entre dispositivos (sin botón de sincronizar).
//  - Guardado inmediato + el listener en vivo NO pisa tus cambios.
//  - Si Firebase no está configurado, la app funciona solo en local.
// =============================================================
(function(){
  const noConfig = (typeof firebase === 'undefined')
    || (typeof firebaseConfig === 'undefined')
    || !firebaseConfig.apiKey
    || /PEGA_/.test(firebaseConfig.apiKey);
  if(noConfig){
    console.warn('[sync] Firebase no configurado: la app guarda solo en este dispositivo.');
    const b = document.getElementById('sync-btn'); if(b) b.style.display = 'none';
    return;
  }

  firebase.initializeApp(firebaseConfig);
  const auth = firebase.auth();
  const db = firebase.firestore();
  try { auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL).catch(()=>{}); } catch(e){}
  try { db.enablePersistence({ synchronizeTabs: true }).catch(()=>{}); } catch(e){}
  // Procesa la vuelta del login por redirect (los errores se muestran)
  auth.getRedirectResult().catch(e => { console.error('[sync] redirect', e); if(typeof showToast==='function') showToast((typeof T==='function'?T('sync_err'):'Error'), 'error'); });

  let uid = null, unsub = null, aplicandoNube = false, pushTimer = null, pushPendiente = false;

  function texto(k, alt){ return (typeof T === 'function') ? T(k) : alt; }

  window.loginGoogle = function(){
    const prov = new firebase.auth.GoogleAuthProvider();
    auth.signInWithRedirect(prov).catch(e => { console.error(e); if(typeof showToast==='function') showToast(texto('sync_err','Error'), 'error'); });
  };
  window.logoutSync = function(){ auth.signOut(); };

  function pintarBoton(){
    const b = document.getElementById('sync-btn'); if(!b) return;
    if(uid){
      const u = auth.currentUser;
      b.textContent = '👤 ' + ((u && (u.displayName || u.email)) || '');
      b.title = texto('sync_out', 'Salir');
      b.onclick = function(){ if(confirm(texto('sync_out','Salir') + '?')) window.logoutSync(); };
    } else {
      b.textContent = texto('sync_login', 'Entrar con Google');
      b.title = '';
      b.onclick = window.loginGoogle;
    }
  }
  window.syncRepaint = pintarBoton;   // repinta al cambiar de idioma

  // Sube la colección a la nube (agrupando ráfagas de cambios, pero sin perderlos)
  function flushPush(){
    clearTimeout(pushTimer); pushTimer = null;
    if(!uid || !pushPendiente) return;
    pushPendiente = false;
    db.collection('colecciones').doc(uid).set({
      inventory: inventory, savedDecks: savedDecks, updatedAt: Date.now()
    }).catch(e => console.error('[sync] error al guardar', e));
  }
  window.syncPush = function(){
    if(!uid || aplicandoNube) return;
    pushPendiente = true;
    clearTimeout(pushTimer);
    pushTimer = setTimeout(flushPush, 300);
  };
  // No perder cambios al cerrar/cambiar de app o pestaña
  document.addEventListener('visibilitychange', function(){ if(document.visibilityState === 'hidden') flushPush(); });
  window.addEventListener('pagehide', flushPush);
  window.addEventListener('beforeunload', flushPush);

  function aplicarDoc(d){
    if(!d) return;
    aplicandoNube = true;
    if(Array.isArray(d.inventory)){
      inventory = d.inventory;
      if(typeof asegurarId === 'function') inventory.forEach(asegurarId);
    }
    if(Array.isArray(d.savedDecks)) savedDecks = d.savedDecks;
    try {
      localStorage.setItem('ptcg_inventory', JSON.stringify(inventory));
      localStorage.setItem('ptcg_decks', JSON.stringify(savedDecks));
    } catch(e){}
    if(typeof renderInventory === 'function') renderInventory();
    if(typeof renderLegal === 'function') renderLegal();
    if(typeof renderSaved === 'function') renderSaved();
    if(typeof updateStats === 'function') updateStats();
    aplicandoNube = false;
  }

  auth.onAuthStateChanged(function(u){
    if(unsub){ unsub(); unsub = null; }
    uid = u ? u.uid : null;
    pintarBoton();
    if(typeof aplicarPermisosDueno === 'function') aplicarPermisosDueno(!!(u && u.email === 'pmmt99@gmail.com'));
    if(!u) return;
    const ref = db.collection('colecciones').doc(uid);
    ref.get().then(function(snap){
      const d = snap.exists ? snap.data() : null;
      const nubeTiene = d && ((Array.isArray(d.inventory) && d.inventory.length) || (Array.isArray(d.savedDecks) && d.savedDecks.length));
      if(!nubeTiene){
        // Cuenta nueva / nube vacía: no heredar lo de este dispositivo sin preguntar
        const localTiene = inventory.length || savedDecks.length;
        let conservar = false;
        if(localTiene && typeof confirm === 'function'){
          conservar = confirm(texto('sync_keep', '¿Conservar las {n} cartas de este dispositivo en tu cuenta? (Cancelar = empezar vacío)').replace('{n}', inventory.length));
        }
        if(conservar){
          pushPendiente = true; flushPush();           // subir lo local como su colección
        } else {
          aplicandoNube = true; inventory = []; savedDecks = [];
          try { localStorage.setItem('ptcg_inventory','[]'); localStorage.setItem('ptcg_decks','[]'); } catch(e){}
          if(typeof renderInventory === 'function') renderInventory();
          if(typeof renderLegal === 'function') renderLegal();
          if(typeof renderSaved === 'function') renderSaved();
          if(typeof updateStats === 'function') updateStats();
          aplicandoNube = false;
          pushPendiente = true; flushPush();           // guardar vacío en su cuenta
        }
      }
      // Cambios en vivo: aplicar SOLO lo confirmado por el servidor (ignorar ecos de nuestras escrituras)
      unsub = ref.onSnapshot(function(s){
        if(s.metadata && s.metadata.hasPendingWrites) return;
        aplicarDoc(s.data());
      });
    }).catch(e => console.error('[sync] get', e));
  });
})();
