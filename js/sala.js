// =============================================================
//  SALA / VERSUS — capa multijugador en tiempo real (sin servidor)
//  Documento compartido `sala/main` (lo leen/escriben los 2 autorizados).
//  SOLO datos NO sensibles: presencia, reglas acordadas, estado, stats del
//  mazo (números, nunca cartas), marcador, moneda, timer y chat.
//  La privacidad de los mazos la siguen garantizando las Reglas de Firestore.
//
//  Enganche: js/sync.js expone window.PTCG = { db, auth } y llama
//  window.salaLogin(uid, esDueno, user) / window.salaLogout().
// =============================================================
(function(){
  let uid = null, esDueno = false, miNombre = '', miFoto = '';
  let unsub = null, hbTimer = null, reglasTimer = null;
  let _sala = {};                 // último snapshot
  let _prevOnline = false, _prevEstado = '';   // para toasts del otro jugador
  let _esqueletoListo = false, _lastReglasVer = -1;

  const ONLINE_MS = 45000, HEARTBEAT_MS = 25000;
  const tx = (k, alt) => (typeof T === 'function' ? T(k) : (alt || k));

  function ref(){ return window.PTCG && window.PTCG.db ? window.PTCG.db.collection('sala').doc('main') : null; }
  function vistaActual(){ const v = document.querySelector('.view.active'); return v ? v.id.replace('view-','') : ''; }
  function onlineDe(j){ return !!j && (Date.now() - (j.online||0) < ONLINE_MS); }
  function jugadores(){ return (_sala && _sala.jugadores) || {}; }
  function yo(){ return jugadores()[uid] || null; }
  function otroUid(){ return Object.keys(jugadores()).find(k => k !== uid) || null; }
  function otro(){ const o = otroUid(); return o ? jugadores()[o] : null; }
  function nombreDe(j){ return (j && j.nombre) || tx('vs_player','Jugador'); }

  function escapa(s){ return (typeof esc === 'function') ? esc(s) : String(s==null?'':s); }

  // ---------- Escrituras ----------
  function actualizarJugador(campos){
    const r = ref(); if(!r || !uid) return;
    const datos = {}; datos[uid] = Object.assign({}, campos);
    r.set({ jugadores: datos }, { merge: true }).catch(e => console.warn('[sala] jugador', e));
  }
  function latido(){
    actualizarJugador({ nombre: miNombre, foto: miFoto, rol: esDueno?'owner':'guest', online: Date.now(), vista: vistaActual() });
  }
  function setReglas(reglas){
    const r = ref(); if(!r) return;
    r.set({ reglas: reglas, reglasVer: (_sala.reglasVer||0) + 1 }, { merge: true }).catch(e => console.warn('[sala] reglas', e));
  }
  function marcarListo(){
    const idx = parseInt((document.getElementById('vs-mydeck')||{}).value);
    const deck = (typeof savedDecks!=='undefined' && savedDecks[idx]) ? savedDecks[idx] : null;
    if(!deck){ if(typeof showToast==='function') showToast(tx('vs_pick_deck','Elige un mazo guardado primero'), 'error'); return; }
    const m = (typeof calcMetricasDeck==='function') ? calcMetricasDeck(deck) : null;
    actualizarJugador({ estado:'ready', stats:m, mazoEtiqueta: tx('vs_deck_of','Mazo de')+' '+miNombre });
  }
  function marcarNoListo(){ actualizarJugador({ estado:'building', stats:null }); }
  function addPunto(quien){
    const r = ref(); if(!r) return;
    const m = Object.assign({}, _sala.marcador || {});
    m[quien] = (m[quien]||0) + 1;
    r.set({ marcador: m }, { merge:true }).catch(()=>{});
  }
  function resetMarcador(){ const r = ref(); if(!r) return; const m={}; Object.keys(jugadores()).forEach(k=>m[k]=0); r.set({ marcador:m, moneda:null }, {merge:true}).catch(()=>{}); }
  function lanzarMoneda(){
    const ks = Object.keys(jugadores()); if(ks.length<2){ if(typeof showToast==='function') showToast(tx('vs_need_two','Falta el otro jugador'),'error'); return; }
    const quien = ks[Math.floor(Math.random()*ks.length)];
    const r = ref(); if(r) r.set({ moneda:{ quien:quien, t:Date.now() } }, {merge:true}).catch(()=>{});
  }
  function iniciarTimer(){
    const min = parseInt((document.getElementById('vs-timer-min')||{}).value) || 5;
    const r = ref(); if(r) r.set({ timer:{ fin: Date.now()+min*60000 } }, {merge:true}).catch(()=>{});
  }
  function pararTimer(){ const r = ref(); if(r) r.set({ timer:null }, {merge:true}).catch(()=>{}); }
  function enviarChat(txt){
    txt = (txt||'').toString().trim().slice(0,200); if(!txt) return;
    const r = ref(); if(!r) return;
    const arr = ((_sala.chat)||[]).slice(-39);
    arr.push({ uid:uid, nombre:miNombre, txt:txt, t:Date.now() });
    r.set({ chat: arr }, {merge:true}).catch(()=>{});
  }

  // ---------- Lectura de reglas desde los controles de la Sala ----------
  function reglasDesdeDOM(){
    const val = id => { const e=document.getElementById(id); return e?e.value:''; };
    const num = id => { const e=document.getElementById(id); return e&&e.value?(parseInt(e.value)||0):0; };
    const chk = id => { const e=document.getElementById(id); return !!(e&&e.checked); };
    const lim = val('vr-speclimit');
    return {
      deckType: val('vr-deckType'), set: val('vr-set'), serie: val('vr-serie'), marca: val('vr-marca'),
      formato: val('vr-format'), profundidad: val('vr-depth'),
      limiteEspeciales: (lim===''?null:(parseInt(lim)||0)),
      singlePrize: chk('vr-singleprize'), hpMin: num('vr-hpmin'),
      mecanicas: Array.from(document.querySelectorAll('.vr-mec:checked')).map(c=>c.value),
      bestOf: parseInt(val('vr-bestof'))||3
    };
  }
  function reglasADOM(r){
    if(!r) return;
    const set = (id,v)=>{ const e=document.getElementById(id); if(e && document.activeElement!==e) e.value = (v==null?'':v); };
    set('vr-deckType', r.deckType); set('vr-set', r.set); set('vr-serie', r.serie); set('vr-marca', r.marca);
    set('vr-format', r.formato); set('vr-depth', r.profundidad);
    set('vr-speclimit', (r.limiteEspeciales==null?'':String(r.limiteEspeciales)));
    set('vr-hpmin', r.hpMin?String(r.hpMin):''); set('vr-bestof', String(r.bestOf||3));
    const sp=document.getElementById('vr-singleprize'); if(sp && document.activeElement!==sp) sp.checked=!!r.singlePrize;
    const mecs=r.mecanicas||[];
    document.querySelectorAll('.vr-mec').forEach(c=>{ if(document.activeElement!==c) c.checked = mecs.indexOf(c.value)>=0; });
  }
  let _reglasPush = null;
  function onReglaChange(){
    clearTimeout(_reglasPush);
    _reglasPush = setTimeout(()=>{ setReglas(reglasDesdeDOM()); }, 400);
  }

  // ---------- Esqueleto de la vista Versus (se construye una vez) ----------
  function cloneOpts(srcId){ const e=document.getElementById(srcId); return e?e.innerHTML:'<option value=""></option>'; }
  function setOptsFromInv(){
    if(typeof poblarFiltrosGen==='function') poblarFiltrosGen();
    ['set','serie','marca'].forEach(k=>{
      const dst=document.getElementById('vr-'+k); const src=document.getElementById('f-'+k);
      if(dst && src) dst.innerHTML = src.innerHTML;
    });
  }
  function mecChecks(){
    const lista = (typeof MECANICAS!=='undefined') ? MECANICAS : ['EX','V','VMAX','VSTAR','GX','MEGA'];
    return lista.map(m=>`<label><input type="checkbox" class="vr-mec" value="${m}">${m==='EX'?'ex':m}</label>`).join('');
  }
  function construirEsqueleto(){
    const root = document.getElementById('versus-root'); if(!root || _esqueletoListo) return;
    root.innerHTML = `
      <div id="vs-players" class="vs-players"></div>
      <div id="vs-stats" class="vs-stats"></div>

      <div class="vs-block">
        <div class="vs-h">${tx('vs_rules','Reglas de la partida')}</div>
        <div class="vs-rules-grid">
          <label class="vs-f"><span>${tx('c_deck_type','Tipo')}</span><select id="vr-deckType" class="control-select">${cloneOpts('deck-type')}</select></label>
          <label class="vs-f"><span>${tx('c_set','Expansión')}</span><select id="vr-set" class="control-select"></select></label>
          <label class="vs-f"><span>${tx('c_serie','Serie')}</span><select id="vr-serie" class="control-select"></select></label>
          <label class="vs-f"><span>${tx('c_marca','Marca')}</span><select id="vr-marca" class="control-select"></select></label>
          <label class="vs-f"><span>${tx('c_format','Formato')}</span><select id="vr-format" class="control-select">${cloneOpts('f-format')}</select></label>
          <label class="vs-f"><span>${tx('c_depth','Profundidad')}</span><select id="vr-depth" class="control-select">${cloneOpts('f-depth')}</select></label>
          <label class="vs-f"><span>${tx('c_speclimit','Máx especiales')}</span><select id="vr-speclimit" class="control-select">${cloneOpts('f-speclimit')}</select></label>
          <label class="vs-f"><span>${tx('c_hpmin','PS mínimo')}</span><input id="vr-hpmin" class="control-select" type="number" min="0" step="10"></label>
          <label class="vs-f"><span>${tx('vs_bestof','Best of')}</span><select id="vr-bestof" class="control-select"><option value="1">1</option><option value="3" selected>3</option><option value="5">5</option></select></label>
          <label class="vs-f vs-f-wide gm-check"><input type="checkbox" id="vr-singleprize"> <span>${tx('c_singleprize','Solo single-prize')}</span></label>
          <div class="vs-f vs-f-wide"><span>${tx('c_mech','Mecánicas permitidas')}</span><div class="gm-mecs" id="vr-mecs">${mecChecks()}</div></div>
        </div>
        <button class="btn-build btn-secondary" id="vs-use-rules" type="button">${tx('vs_use_rules','Usar estas reglas para generar')}</button>
      </div>

      <div class="vs-block">
        <div class="vs-h">${tx('vs_my_deck','Mi mazo para la partida')}</div>
        <div class="vs-deck-row">
          <select id="vs-mydeck" class="control-select"></select>
          <button class="btn-build" id="vs-ready" type="button">${tx('vs_ready','Estoy listo')}</button>
          <button class="btn-build btn-secondary" id="vs-unready" type="button" style="display:none">${tx('vs_unready','No listo')}</button>
        </div>
        <div class="vs-note">${tx('vs_ready_note','Solo se comparten las estadísticas (estrellas), nunca tus cartas ni el tipo.')}</div>
      </div>

      <div class="vs-block">
        <div class="vs-h">${tx('vs_score','Marcador')}</div>
        <div id="vs-score" class="vs-score"></div>
        <div id="vs-coin" class="vs-coin"></div>
        <div class="vs-extras">
          <button class="btn-build btn-secondary" id="vs-coin-btn" type="button">${tx('vs_flip','Lanzar moneda')}</button>
          <button class="btn-build btn-secondary" id="vs-reset-btn" type="button">${tx('vs_reset','Reiniciar marcador')}</button>
          <span class="vs-timer-wrap"><input id="vs-timer-min" class="control-select" type="number" min="1" value="5" style="width:64px"> min
            <button class="btn-build btn-secondary" id="vs-timer-btn" type="button">${tx('vs_timer','Temporizador')}</button>
            <span id="vs-timer-disp" class="vs-timer-disp"></span></span>
        </div>
      </div>

      <div class="vs-block">
        <div class="vs-h">${tx('vs_chat','Chat')}</div>
        <div id="vs-chat-list" class="vs-chat-list"></div>
        <div class="vs-chat-emojis">${['👍','🔥','😅','😏','🎉','💜'].map(e=>`<button type="button" class="vs-emoji">${e}</button>`).join('')}</div>
        <div class="vs-chat-input"><input id="vs-chat-text" type="text" maxlength="200" placeholder="${tx('vs_chat_ph','Escribe un mensaje…')}"><button class="btn-build" id="vs-chat-send" type="button">${tx('vs_send','Enviar')}</button></div>
      </div>`;

    setOptsFromInv();
    // listeners
    root.querySelectorAll('#vr-deckType,#vr-set,#vr-serie,#vr-marca,#vr-format,#vr-depth,#vr-speclimit,#vr-hpmin,#vr-bestof,#vr-singleprize,.vr-mec')
      .forEach(el => el.addEventListener('change', onReglaChange));
    document.getElementById('vr-hpmin').addEventListener('input', onReglaChange);
    document.getElementById('vs-use-rules').onclick = () => {
      if(typeof aplicarReglasAFiltros==='function') aplicarReglasAFiltros(reglasDesdeDOM());
      if(typeof showView==='function') showView('construir');
    };
    document.getElementById('vs-ready').onclick = marcarListo;
    document.getElementById('vs-unready').onclick = marcarNoListo;
    document.getElementById('vs-coin-btn').onclick = lanzarMoneda;
    document.getElementById('vs-reset-btn').onclick = resetMarcador;
    document.getElementById('vs-timer-btn').onclick = iniciarTimer;
    root.querySelectorAll('.vs-emoji').forEach(b => b.onclick = () => enviarChat(b.textContent));
    document.getElementById('vs-chat-send').onclick = () => { const i=document.getElementById('vs-chat-text'); enviarChat(i.value); i.value=''; };
    document.getElementById('vs-chat-text').addEventListener('keydown', e => { if(e.key==='Enter'){ const i=e.target; enviarChat(i.value); i.value=''; } });

    _esqueletoListo = true;
  }

  // ---------- Render dinámico ----------
  let _mazosSig = null;
  function llenarMisMazos(){
    const sel = document.getElementById('vs-mydeck'); if(!sel) return;
    const lista = (typeof savedDecks!=='undefined') ? savedDecks : [];
    const sig = lista.map(d=>d.name).join('|');
    if(sig === _mazosSig) return;     // sin cambios: no tocar (no cierra el desplegable)
    _mazosSig = sig;
    const prev = sel.value;
    sel.innerHTML = lista.length
      ? lista.map((d,i)=>`<option value="${i}">${escapa(d.name||('Mazo '+(i+1)))}</option>`).join('')
      : `<option value="">${escapa(tx('vs_no_decks','No tienes mazos guardados'))}</option>`;
    if(prev && sel.querySelector(`option[value="${prev}"]`)) sel.value = prev;
  }

  function tarjetaJugador(j, esYo){
    const on = onlineDe(j);
    const ini = (nombreDe(j)[0]||'?').toUpperCase();
    const av = (j && j.foto) ? `<img src="${escapa(j.foto)}" alt="">` : `<span class="vs-ini">${escapa(ini)}</span>`;
    const estado = j && j.estado==='ready' ? tx('vs_st_ready','Listo')
                  : j && j.estado==='building' ? tx('vs_st_building','Armando')
                  : tx('vs_st_idle','Inactivo');
    const rol = j && j.rol==='owner' ? tx('vs_owner','Anfitrión') : tx('vs_guest','Invitada');
    return `<div class="vs-player ${on?'on':'off'} ${j&&j.estado==='ready'?'ready':''}">
        <div class="vs-avatar">${av}<span class="vs-dot"></span></div>
        <div class="vs-pinfo">
          <div class="vs-pname">${escapa(nombreDe(j))}${esYo?' ('+tx('vs_you','tú')+')':''}</div>
          <div class="vs-pmeta">${rol} · ${on?tx('vs_online','en línea'):tx('vs_offline','desconectado')}</div>
          <div class="vs-pstate">${estado}</div>
        </div>
      </div>`;
  }

  function renderStats(){
    const cont = document.getElementById('vs-stats'); if(!cont) return;
    const a = yo(), b = otro();
    const ambosListos = a && b && a.estado==='ready' && b.estado==='ready' && a.stats && b.stats;
    if(!ambosListos){
      cont.innerHTML = (a && a.estado==='ready')
        ? `<div class="vs-waiting">${tx('vs_waiting','Esperando a que el otro jugador esté listo…')}</div>` : '';
      return;
    }
    const star = (typeof estrellas==='function') ? estrellas : (n=>('★'.repeat(n)+'☆'.repeat(5-n)));
    const filas = [
      ['cmp_consistency', s=>star(s.consistencia)],
      ['cmp_speed', s=>star(s.velocidad)],
      ['cmp_damage', s=>star(s.dano)],
      ['cmp_ease', s=>star(s.facilidad)],
      ['cmp_potential', s=>s.potencial+'/10'],
      ['m_total', s=>s.nCartas]
    ];
    const cols = [a,b];
    const head = `<th>${tx('cmp_feature','Característica')}</th>` + cols.map(j=>`<th>${escapa(nombreDe(j))}</th>`).join('');
    const body = filas.map(([k,fn])=>`<tr><td class="cmp-feat">${tx(k,k)}</td>`+cols.map(j=>`<td>${fn(j.stats)}</td>`).join('')+'</tr>').join('');
    cont.innerHTML = `<div class="cmp-wrap"><div class="deck-section-title">${tx('vs_stats_title','Comparativa de fuerzas (sin revelar cartas)')}</div>`
      + `<div class="cmp-scroll"><table class="cmp-table"><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div></div>`;
  }

  function renderScore(){
    const cont = document.getElementById('vs-score'); if(!cont) return;
    const ks = Object.keys(jugadores());
    if(ks.length < 1){ cont.innerHTML = tx('vs_need_two','Falta el otro jugador'); return; }
    const m = _sala.marcador || {};
    cont.innerHTML = ks.map(k=>{
      const j = jugadores()[k];
      return `<div class="vs-score-cell"><div class="vs-score-name">${escapa(nombreDe(j))}</div>`
        + `<div class="vs-score-num">${m[k]||0}</div>`
        + `<button class="btn-build btn-secondary vs-score-add" data-uid="${escapa(k)}" type="button">+1</button></div>`;
    }).join('<span class="vs-score-sep">—</span>');
    cont.querySelectorAll('.vs-score-add').forEach(b=> b.onclick = ()=> addPunto(b.getAttribute('data-uid')));
    const coin = document.getElementById('vs-coin');
    if(coin){
      if(_sala.moneda && _sala.moneda.quien && jugadores()[_sala.moneda.quien])
        coin.innerHTML = `🪙 ${tx('vs_starts','Empieza')}: <strong>${escapa(nombreDe(jugadores()[_sala.moneda.quien]))}</strong>`;
      else coin.innerHTML = '';
    }
  }

  let _timerInt = null;
  function renderTimer(){
    const disp = document.getElementById('vs-timer-disp'); if(!disp) return;
    clearInterval(_timerInt); _timerInt = null;
    const fin = _sala.timer && _sala.timer.fin;
    if(!fin){ disp.textContent=''; return; }
    const pintar = ()=>{
      const ms = fin - Date.now();
      if(ms <= 0){ disp.textContent = '00:00'; clearInterval(_timerInt); _timerInt=null; return; }
      const s = Math.floor(ms/1000); disp.textContent = String(Math.floor(s/60)).padStart(2,'0')+':'+String(s%60).padStart(2,'0');
    };
    pintar(); _timerInt = setInterval(pintar, 500);
  }

  function renderChat(){
    const list = document.getElementById('vs-chat-list'); if(!list) return;
    const arr = (_sala.chat)||[];
    list.innerHTML = arr.map(m=>`<div class="vs-msg ${m.uid===uid?'mine':''}"><span class="vs-msg-name">${escapa(m.nombre||'')}</span> ${escapa(m.txt||'')}</div>`).join('');
    list.scrollTop = list.scrollHeight;
  }

  function renderBadge(){
    const badge = document.getElementById('mp-badge'); if(!badge) return;
    if(!uid){ badge.innerHTML=''; badge.style.display='none'; return; }
    badge.style.display='';
    const ks = Object.keys(jugadores());
    const orden = ks.sort((a,b)=> (a===uid?-1:1));
    badge.innerHTML = orden.map(k=>{
      const j = jugadores()[k]; const on = onlineDe(j);
      const ini = (nombreDe(j)[0]||'?').toUpperCase();
      const av = (j&&j.foto)?`<img src="${escapa(j.foto)}" alt="">`:`<span class="vs-ini">${escapa(ini)}</span>`;
      const st = j&&j.estado==='ready'?tx('vs_st_ready','Listo'):j&&j.estado==='building'?tx('vs_st_building','Armando'):'';
      return `<span class="mp-chip ${on?'on':'off'}" title="${escapa(nombreDe(j))}${st?(' · '+st):''}">${av}<span class="mp-dot"></span></span>`;
    }).join('');
    badge.onclick = ()=> { if(typeof showView==='function') showView('versus'); };
  }

  function renderBanner(){
    const b = document.getElementById('role-banner'); if(!b) return;
    if(esDueno || !uid){ b.style.display='none'; return; }
    const otroDueno = Object.values(jugadores()).find(j=>j.rol==='owner');
    const sinColeccion = (typeof inventory==='undefined' || !inventory.length);
    b.style.display='';
    b.textContent = sinColeccion
      ? tx('vs_owner_noshare','El anfitrión aún no ha compartido su colección.')
      : tx('vs_guest_banner','Modo invitada — ves la colección en solo lectura y armas tu mazo.');
  }

  function toastsTransicion(){
    const o = otro();
    const onNow = onlineDe(o), estNow = (o&&o.estado)||'';
    if(o && onNow && !_prevOnline && typeof showToast==='function') showToast(nombreDe(o)+' '+tx('vs_connected','se conectó'),'success');
    if(o && estNow==='ready' && _prevEstado!=='ready' && typeof showToast==='function') showToast(nombreDe(o)+' '+tx('vs_is_ready','está listo'),'success');
    _prevOnline = onNow; _prevEstado = estNow;
  }

  function renderTodo(){
    renderBadge(); renderBanner();
    if(!_esqueletoListo) return;
    const pl = document.getElementById('vs-players');
    if(pl){
      const a = yo(), b = otro();
      pl.innerHTML = tarjetaJugador(a, true) + (b ? tarjetaJugador(b, false) : `<div class="vs-player off"><div class="vs-pinfo"><div class="vs-pname">${tx('vs_waiting_player','Esperando jugador…')}</div></div></div>`);
    }
    // estado de mis botones listo/no-listo
    const a = yo();
    const rdy = a && a.estado==='ready';
    const bR=document.getElementById('vs-ready'), bU=document.getElementById('vs-unready');
    if(bR) bR.style.display = rdy?'none':'';
    if(bU) bU.style.display = rdy?'':'none';
    llenarMisMazos();
    if(_sala.reglas && (_sala.reglasVer||0) !== _lastReglasVer){ reglasADOM(_sala.reglas); _lastReglasVer = _sala.reglasVer||0; }
    renderStats(); renderScore(); renderTimer(); renderChat();
  }

  // Punto de entrada para showView('versus')
  window.salaRenderVersus = function(){ construirEsqueleto(); setOptsFromInv(); renderTodo(); };

  // ---------- Ciclo de vida ----------
  window.salaLogin = function(u, dueno, user){
    uid = u; esDueno = !!dueno;
    miNombre = (user && (user.displayName || user.email)) || (esDueno?'Anfitrión':'Invitada');
    miFoto = (user && user.photoURL) || '';
    construirEsqueleto();
    latido();
    if(hbTimer) clearInterval(hbTimer);
    hbTimer = setInterval(latido, HEARTBEAT_MS);
    if(unsub) unsub();
    const r = ref(); if(!r) return;
    unsub = r.onSnapshot(s => { _sala = s.data() || {}; toastsTransicion(); renderTodo(); },
                         e => console.warn('[sala] snapshot', e));
  };
  window.salaLogout = function(){
    if(hbTimer){ clearInterval(hbTimer); hbTimer=null; }
    if(_timerInt){ clearInterval(_timerInt); _timerInt=null; }
    try{ if(uid) actualizarJugador({ online:0, estado:'idle' }); }catch(e){}
    if(unsub){ unsub(); unsub=null; }
    uid=null; esDueno=false; _sala={}; _prevOnline=false; _prevEstado='';
    renderBadge(); renderBanner();
  };

  // Presencia: marcar offline al ocultar; latir al volver.
  document.addEventListener('visibilitychange', function(){
    if(!uid) return;
    if(document.visibilityState==='hidden') actualizarJugador({ online:0 });
    else latido();
  });
  window.addEventListener('pagehide', function(){ if(uid) try{ actualizarJugador({ online:0 }); }catch(e){} });

  // Repinta los mazos guardados cuando cambian (la nube/local llaman renderSaved).
  window.salaRefrescarMazos = function(){ if(_esqueletoListo) llenarMisMazos(); };
})();
