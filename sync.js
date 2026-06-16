// =============================================================
//  SINCRONIZACIÓN EN LA NUBE (Firebase Auth con Google + Firestore)
//  - Inicio de sesión con Google.
//  - La colección y los mazos se guardan en Firestore y se
//    sincronizan en vivo entre todos tus dispositivos.
//  - Si Firebase no está configurado, la app sigue funcionando
//    igual (solo guarda en este dispositivo, como antes).
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
  try { db.enablePersistence({ synchronizeTabs: true }).catch(()=>{}); } catch(e){}

  let uid = null, unsub = null, aplicandoNube = false, pushTimer = null;

  window.loginGoogle = function(){
    auth.signInWithPopup(new firebase.auth.GoogleAuthProvider())
      .catch(e => { console.error(e); if(typeof showToast==='function') showToast((typeof T==='function'?T('sync_err'):'Error'), 'error'); });
  };
  window.logoutSync = function(){ auth.signOut(); };

  function pintarBoton(){
    const b = document.getElementById('sync-btn'); if(!b) return;
    if(uid){
      const u = auth.currentUser;
      b.textContent = '👤 ' + ((u && (u.displayName || u.email)) || '') + ' · ' + (typeof T==='function'?T('sync_out'):'Salir');
      b.onclick = window.logoutSync;
    } else {
      b.textContent = '☁️ ' + (typeof T==='function'?T('sync_in'):'Sincronizar');
      b.onclick = window.loginGoogle;
    }
  }
  // Repinta el botón al cambiar de idioma
  window.syncRepaint = pintarBoton;

  // Sube la colección actual a la nube (con pequeño retardo para agrupar cambios)
  window.syncPush = function(){
    if(!uid || aplicandoNube) return;
    clearTimeout(pushTimer);
    pushTimer = setTimeout(function(){
      db.collection('colecciones').doc(uid).set({
        inventory: inventory, savedDecks: savedDecks, updatedAt: Date.now()
      }).catch(e => console.error('[sync] error al guardar', e));
    }, 600);
  };

  auth.onAuthStateChanged(function(u){
    if(unsub){ unsub(); unsub = null; }
    uid = u ? u.uid : null;
    pintarBoton();
    if(!u) return;
    const ref = db.collection('colecciones').doc(uid);
    ref.get().then(function(snap){
      // Primera vez: si la nube está vacía, sube lo que tengas en este dispositivo
      if((!snap.exists || !snap.data()) && (inventory.length || savedDecks.length)){ window.syncPush(); }
      // Escucha cambios en vivo (otros dispositivos)
      unsub = ref.onSnapshot(function(s){
        const d = s.data(); if(!d) return;
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
      });
    });
    if(typeof showToast === 'function') showToast((typeof T==='function'?T('sync_ok'):'Sincronización activada'), 'success');
  });
})();
