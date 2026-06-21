// ===================== IDIOMA / LANGUAGE (i18n) =====================
// Idioma único: español, base universal. (Inglés eliminado por completo.)
const lang = 'es';
try { localStorage.removeItem('ptcg_lang'); } catch (e) {}

const I18N = {
  es: {
    app_sub:'Gestor de cartas Pokémon', stat_inv:'Inventario', stat_cards:'cartas', stat_decks:'Mazos guardados', stat_decks_short:'guardados',
    inventory:'Inventario', search_ph:'Buscar carta...', f_name:'Nombre de la carta', f_name_ph:'Ej: Rapidash',
    f_qty:'Cantidad', f_type:'Tipo', btn_add:'+ Agregar', btn_quick_load:'⚡ Cargar mi inventario confirmado',
    btn_import_toggle:'Importar lista de cartas', import_do:'Importar', import_ph:'Pega aquí tu tabla o lista de cartas...',
    import_hint:'Pega tu lista en <strong>cualquiera</strong> de estos formatos:<br>• <strong>Tabla:</strong> <code>106&nbsp;&nbsp;H&nbsp;&nbsp;Pikachu ex&nbsp;&nbsp;Lightning&nbsp;&nbsp;1×</code><br>• <strong>Coma:</strong> <code>Rapidash, Pokemon-Fire, 7</code><br>• <strong>Dos puntos:</strong> <code>Rapidash: 7</code>',
    empty_inv:'Agrega cartas manualmente<br>o importa una lista para comenzar',
    tab_build:'⚔️ Construir Mazo', tab_legal:'✅ Verificar Legalidad', tab_saved:'💾 Mazos Guardados',
    controls_title:'Generar prompt para la IA', c_deck_type:'Tipo de mazo', c_variants:'Variantes', btn_build:'📋 Generar prompt para la IA',
    loading:'Analizando inventario', build_empty_title:'Genera tu prompt',
    build_empty_desc:'Elige el tipo de mazo y presiona "Generar prompt para la IA". Se copiará un texto listo para pegar en una conversación con Claude.',
    legal_empty_title:'Inventario vacío', legal_empty_desc:'Agrega cartas para ver el análisis de legalidad de líneas evolutivas.',
    saved_empty_title:'Sin mazos guardados', saved_empty_desc:'Construye mazos con la IA y guárdalos aquí para consultarlos después.',
    t_pk_fire:'Pokémon — Fuego', t_pk_water:'Pokémon — Agua', t_pk_lightning:'Pokémon — Eléctrico', t_pk_grass:'Pokémon — Planta',
    t_pk_psychic:'Pokémon — Psíquico', t_pk_dark:'Pokémon — Oscuro', t_pk_colorless:'Pokémon — Incoloro', t_pk_fighting:'Pokémon — Lucha',
    t_pk_metal:'Pokémon — Metal', t_trainer:'Entrenador', t_en_fire:'Energía — Fuego', t_en_water:'Energía — Agua',
    t_en_lightning:'Energía — Eléctrico', t_en_grass:'Energía — Planta', t_en_psychic:'Energía — Psíquico', t_en_dark:'Energía — Oscuro', t_en_colorless:'Energía — Incoloro',
    d_fire:'🔥 Fuego', d_water:'💧 Agua', d_lightning:'⚡ Eléctrico', d_grass:'🌿 Planta', d_psychic:'🔮 Psíquico', d_dark:'🌑 Oscuro', d_fighting:'👊 Lucha', d_colorless:'⭐ Incoloro', d_best:'🏆 El mejor posible',
    v3:'3 variantes', v4:'4 variantes', v5:'5 variantes', v6:'6 variantes', v7:'7 variantes', v8:'8 variantes', v9:'9 variantes', v10:'10 variantes',
    grp_pokemon:'Pokémon', grp_energy:'Energía', grp_trainer:'Entrenadores',
    d_ps:'PS', d_ability:'Habilidad', d_weak:'Debilidad', d_resist:'Resistencia', d_retreat:'Retirada', d_evolves:'Evoluciona de', d_type:'Tipo', d_no:'Nº', d_illus:'Ilus.',
    legal_legals:'Pokémon Legales', legal_illegals:'Pokémon Ilegales', legal_reason_ok:'Cadena evolutiva completa', legal_missing:'Falta:',
    no_results:'Sin resultados para', empty_default:'Agrega cartas manualmente<br>o importa una lista',
    sec_pokemon:'Pokémon', sec_trainers:'Entrenadores', sec_energies:'Energías', m_total:'Total',
    s_analysis:'Análisis', s_strategy:'Estrategia', s_advantages:'Ventajas', s_weaknesses:'Debilidades', s_difficulty:'Dificultad', s_offtype:'Pokémon fuera del tipo principal',
    btn_copy:'📋 Copiar', btn_save:'💾 Guardar', btn_view:'Ver', btn_delete:'Eliminar', btn_recopy:'📋 Copiar de nuevo',
    no_decks_title:'Sin resultados', no_decks_desc:'La IA no pudo construir mazos con el inventario actual.',
    saved_type:'Tipo', deck_total_word:'cartas',
    to_need_name:'Ingresa el nombre de la carta', to_updated:'Actualizado', to_added:'Agregado', to_loaded:'Inventario cargado',
    to_unique:'únicas', to_imported:'cartas importadas', to_lines_skipped:'líneas omitidas', to_need_inv:'Carga tu inventario primero',
    to_prompt_copied:'Prompt copiado — pégalo en una conversación con Claude', to_prompt_manual:'Genera el prompt y cópialo manualmente abajo',
    to_prompt_recopied:'Prompt copiado de nuevo', to_deck_copied:'Mazo copiado al portapapeles', to_saved:'guardado', to_deck_deleted:'Mazo eliminado',
    load_confirm:'Esto reemplazará tu inventario actual con tu inventario confirmado (Pikachu Deck + Darkrai Deck + cartas 151). ¿Continuar?',
    no_data:'No hay datos de', no_data2:'Ejecuta descargar-cartas.py para incluirla.',
    prompt_steps1:'El prompt ya se copió a tu portapapeles ✅', prompt_steps2:'Abre una conversación con Claude y <strong>pégalo</strong> (Ctrl/Cmd + V)', prompt_steps3:'Claude te dará los mazos listos para copiar',
    prompt_header:'Prompt generado — tipo', prompt_variants_word:'variantes',
    nav_col:'Colección', nav_exp:'Explorar cartas', nav_build:'Construir mazo', nav_legal:'Legalidad', nav_saved:'Mazos', nav_proxies:'Proxies',
    px_upload:'Subir imágenes', px_from_col:'Añadir desde mi colección', px_size:'Hoja', px_cut:'Líneas de corte', px_gen:'Generar PDF',
    px_drop:'Arrastra aquí tus imágenes (o usa "Subir imágenes")', px_note:'Imprime al 100% / tamaño real (NO "ajustar a página"). Cada carta 6,3 × 8,8 cm · 9 por hoja. Total: ',
    px_empty_grid:'Aún no has añadido imágenes.', px_empty:'Añade imágenes primero.', px_no_col:'Tu colección está vacía.', px_added:'añadidas', px_generating:'Generando PDF…', px_done:'PDF generado', px_failed:'fallaron', px_lib_err:'No se pudo cargar el generador de PDF (¿sin conexión?).',
    px_search_ph:'Buscar cualquier carta del juego…', px_searching:'Buscando…', px_no_results:'Sin resultados.', px_add:'Añadir al PDF', px_more:'Cargar más', px_search_results:'resultados', px_none:'No se pudo añadir ninguna imagen al PDF.',
    btn_export:'Exportar', btn_import_file:'Importar archivo',
    f_search:'Buscar por nombre…', f_any:'(cualquiera)', f_type:'Tipo', f_supertype:'Categoría', f_rarity:'Rareza', f_set:'Set / Expansión',
    f_subtype:'Subtipo', f_hp_min:'PS mín', f_hp_max:'PS máx', f_pokedex:'Nº Pokédex', f_order:'Ordenar', f_reset:'Limpiar', f_regmark:'Marca', f_toggle:'Filtros',
    aria_close:'Cerrar',
    sync_in:'Sincronizar', sync_out:'Salir', sync_ok:'Sincronización activada', sync_err:'No se pudo iniciar sesión', sync_login:'Entrar con Google',
    sync_keep:'Tienes {n} cartas en este dispositivo. ¿Conservarlas en tu cuenta?\n\nAceptar = conservarlas · Cancelar = empezar con colección vacía',
    login_title:'Pokémon TCG Deck Builder', login_sub:'Inicia sesión para acceder a tu colección.', login_checking:'Comprobando sesión…', login_btn:'Entrar con Google', login_denied:'Esa cuenta no tiene acceso.',
    sync_saving:'Guardando…', sync_saved:'Guardado', sync_offline:'Sin conexión',
    btn_clear:'Vaciar colección', cf_clear:'¿Vaciar TODA tu colección? Esto no se puede deshacer.', cf_clear2:'Última confirmación: se eliminarán {n} cartas y NO se puede deshacer. ¿Seguro?', to_cleared:'Colección vaciada',
    ord_name:'Nombre (A-Z)', ord_name_desc:'Nombre (Z-A)', ord_hp:'PS ↑', ord_hp_desc:'PS ↓', ord_number:'Nº de carta', ord_recent:'Más recientes',
    col_empty:'Tu colección está vacía o ningún resultado coincide con los filtros.',
    exp_empty:'Sin resultados. Prueba otros filtros.', exp_loading:'Buscando…', exp_results:'resultados', exp_error:'Error al consultar (¿sin conexión?).',
    exp_more:'Cargar más', exp_hint:'Busca y filtra entre TODAS las cartas del juego. Pulsa una carta para ver su versión y añadirla.',
    exp_cache:'caché', exp_backup:'respaldo TCGdex',
    cd_owned:'En tu colección', cd_add:'Añadir a mi colección', cd_remove1:'Quitar 1',
    to_import_ok:'Colección importada', to_import_err:'Archivo no válido', to_removed:'Quitado de la colección',
    rep_btn:'🔄 Reemplazar', rep_use:'Usar como reemplazo', rep_choosing:'Reemplazando', rep_cancel:'✕ Cancelar reemplazo',
    rep_confirm:'¿Reemplazar esta carta por la nueva?', rep_keepqty:'Se mantiene la cantidad', rep_done:'Carta reemplazada',
    rep_same:'Es la misma carta', rep_hint:'Elige abajo la carta exacta que quieres en su lugar.',
    cf_remove:'¿Quitar de la colección', cf_import:'Esto reemplazará TODA tu colección actual. ¿Continuar?',
    tile_replace:'Reemplazar esta carta', tile_delete:'Eliminar de la colección',
    btn_generate:'Generar mazos', btn_prompt:'Prompt para IA', gen_none:'No hay atacantes de ese tipo en tu colección.',
    gen_filter_none:'Ninguna carta cumple los filtros elegidos.',
    // Modal de modo de inventario
    gm_title:'¿Cómo deben manejarse los inventarios?', gm_indep_t:'Inventarios independientes',
    gm_indep_d:'Cada mazo puede usar cualquier carta disponible; las cartas pueden repetirse entre mazos.',
    gm_shared_t:'Inventario compartido', gm_shared_d:'Una sola colección física: una carta usada por un mazo no puede repetirse en otro de esta generación.',
    gm_cancel:'Cancelar',
    // Filtros del generador
    c_filters:'Filtros del mazo', c_set:'Expansión', c_serie:'Serie', c_marca:'Marca', c_format:'Formato',
    c_mech:'Mecánicas permitidas', c_speclimit:'Máx. Pokémon especiales', c_depth:'Profundidad evolutiva',
    c_singleprize:'Solo single-prize (sin ex/V/GX)', c_hpmin:'PS mínimo', c_exclude:'Excluir cartas',
    opt_nolimit:'Sin límite', fmt_std:'Standard', fmt_exp:'Expanded', fmt_unl:'Unlimited',
    depth_any:'Cualquiera', depth_basic:'Solo básicos', depth_s1:'Hasta Stage 1', depth_s2:'Hasta Stage 2',
    ph_exclude:'Ej: Charizard ex, Pikachu', ph_hpmin:'0',
    // Tabla comparativa
    cmp_title:'Comparativa de mazos', cmp_feature:'Característica', cmp_deck:'Mazo',
    cmp_consistency:'Consistencia', cmp_speed:'Velocidad', cmp_damage:'Daño', cmp_ease:'Facilidad',
    cmp_learn:'Ideal para aprender', cmp_potential:'Potencial con la colección',
    // Versus / Sala
    nav_versus:'Versus',
    vs_mode_fisico:'Cartas físicas', vs_mode_virtual:'Juego virtual',
    jv_wip_title:'Juego virtual en construcción',
    jv_wip_body:'El tablero, las animaciones y el motor de reglas llegarán por fases. El modo de cartas físicas sigue disponible aquí al lado.',
    jv_preview:'Vista previa', jv_exit:'Salir', jv_your_turn:'Tu turno', jv_their_turn:'Turno del rival',
    jv_z_prizes:'Premios', jv_z_deck:'Mazo', jv_z_discard:'Descarte', jv_z_stadium:'Estadio', jv_z_lost:'Zona perdida',
    jv_rival:'Rival', jv_start_title:'Juego virtual',
    jv_your_deck:'Tu mazo', jv_rival_deck:'Mazo del rival (práctica)',
    jv_no_decks:'Primero crea o guarda un mazo en "Construir mazo" o "Mazos".',
    jv_start_note:'Práctica local: tú colocas tus Pokémon; el rival se prepara solo. Los turnos llegarán en la siguiente fase.',
    jv_start_btn:'Repartir y empezar',
    jv_setup_active:'Toca un Pokémon Básico de tu mano para ponerlo como Activo',
    jv_setup_bench:'Añade Básicos a tu Banca (opcional) y pulsa Confirmar',
    jv_auto:'Automático', jv_confirm:'Confirmar',
    jv_turn:'Turno', jv_end_turn:'Terminar turno', jv_rival_thinking:'El rival juega…',
    jv_actions_soon:'Toca una carta de la mano para jugarla, tu Activo para retirarte, y usa los ataques de abajo.',
    jv_pick_energy_t:'Elige a quién adjuntar la energía', jv_pick_evo_t:'Elige el Pokémon a evolucionar',
    jv_pick_retreat_t:'Elige el Pokémon de banca que pasa a Activo', jv_cancel:'Cancelar',
    jv_attack:'Atacar', jv_by_prizes:'por tomar todos los premios', jv_by_nopokemon:'el rival se quedó sin Pokémon',
    jv_manual:'Acciones manuales (efectos)', jv_m_dmg:'Daño rival', jv_m_heal:'Curar', jv_m_draw:'Robar 1',
    jv_abilities:'Habilidades', jv_use:'Usar', jv_sound:'Sonido',
    jv_ai_diff:'Dificultad de la IA', jv_easy:'Fácil', jv_med:'Media', jv_hard:'Difícil',
    jv_ai_mode:'Comportamiento de la IA', jv_rules_b:'Sigue reglas', jv_random_b:'Aleatorio',
    jv_or:'o', jv_online:'Jugar online (2 jugadores)',
    jv_online_note:'Online: los dos jugadores deben tener Versus abierto. Juegas con el mazo de arriba.',
    jv_waiting_online:'Esperando al otro jugador… (debe elegir su mazo y pulsar "Jugar online").',
    jv_login_first:'Inicia sesión y abre Versus primero',
    jv_pase:'Pase y juega (mismo dispositivo)', jv_pass_device:'Pasa el dispositivo', jv_pass_to:'Le toca a',
    jv_im_ready:'Estoy listo', jv_p1:'Jugador 1', jv_p2:'Jugador 2', jv_player:'Jugador',
    jv_concede:'Rendirse', jv_concede_q:'¿Seguro que quieres rendirte?', jv_by_concede:'por rendición',
    jv_undo:'Deshacer', jv_resume:'Reanudar partida', jv_close:'Cerrar',
    jv_hint_play:'Toca cartas de tu mano para jugarlas, tu Activo para retirarte, y abajo para atacar.',
    jv_win:'¡Ganaste!', jv_lose:'Perdiste', jv_by_deckout:'por agotar el mazo del rival', jv_new_game:'Nueva partida',
    vs_player:'Jugador', vs_you:'tú', vs_owner:'Anfitrión', vs_guest:'Invitada',
    vs_online:'en línea', vs_offline:'desconectado',
    vs_st_idle:'Inactivo', vs_st_building:'Armando', vs_st_ready:'Listo',
    vs_rules:'Reglas de la partida', vs_bestof:'Best of', vs_use_rules:'Usar estas reglas para generar',
    vs_my_deck:'Mi mazo para la partida', vs_ready:'Estoy listo', vs_unready:'No listo',
    vs_ready_note:'Solo se comparten las estadísticas (estrellas), nunca tus cartas ni el tipo.',
    vs_pick_deck:'Elige un mazo guardado primero', vs_no_decks:'No tienes mazos guardados',
    vs_deck_of:'Mazo de', vs_waiting:'Esperando a que el otro jugador esté listo…',
    vs_stats_title:'Comparativa de fuerzas (sin revelar cartas)', vs_waiting_player:'Esperando jugador…',
    vs_score:'Marcador', vs_flip:'Lanzar moneda', vs_reset:'Reiniciar marcador', vs_starts:'Empieza',
    vs_timer:'Temporizador', vs_need_two:'Falta el otro jugador',
    vs_chat:'Chat', vs_chat_ph:'Escribe un mensaje…', vs_send:'Enviar',
    vs_connected:'se conectó', vs_is_ready:'está listo',
    vs_guest_banner:'Modo invitada — ves la colección en solo lectura y armas tu mazo.',
    vs_owner_noshare:'El anfitrión aún no ha compartido su colección.',
    ro_blocked:'Solo lectura: no puedes editar la colección del anfitrión.',
    vs_preview:'Ver mi mazo', vs_new_deck:'Generar mazo nuevo',
    vs_ready_note2:'Tu mazo es privado: el otro jugador solo verá tus estadísticas y las cartas en común a repartir.',
    vs_prizes:'Premios', vs_new_match:'Nueva partida', vs_you_win:'¡Ganaste la partida!',
    vs_both_ready:'¡Listos!', vs_conflicts_title:'Cartas en común a repartir',
    vs_conflicts_note:'Comparten estas cartas y no alcanzan las copias físicas. Decidan quién las usa.',
    vs_no_conflicts:'Sin choques: pueden montar ambos mazos a la vez.',
    vs_have:'Tienes', vs_split:'Faltan',
    vs_only_compliant:'Solo los que cumplen las reglas', vs_compliant:'Cumple las reglas',
    vs_none_compliant:'Ninguno de tus mazos cumple las reglas pactadas.',
    vs_deck_nocumple:'Ojo: este mazo no cumple las reglas pactadas.', vs_notif_enable:'Activar avisos',
  },
};
function T(k){ return (I18N.es[k] != null) ? I18N.es[k] : k; }

// --- Traducción de vocabulario fijo de las cartas (inglés de origen -> español) ---
const TYPE_WORD = {
  Fire:'Fuego', Water:'Agua', Lightning:'Eléctrico', Grass:'Planta', Psychic:'Psíquico',
  Darkness:'Oscuro', Fighting:'Lucha', Metal:'Metal', Fairy:'Hada', Dragon:'Dragón', Colorless:'Incoloro'
};
function trType(t){ return TYPE_WORD[t] || t; }
const SUPER_WORD = { 'Pokémon':'Pokémon', 'Pokemon':'Pokémon', 'Trainer':'Entrenador', 'Energy':'Energía' };
function trSuper(s){ return SUPER_WORD[s] || s; }
const PHASE_WORD = {
  'Basic':'Básico', 'Stage 1':'Fase 1', 'Stage 2':'Fase 2', 'ex':'ex', 'Supporter':'Partidario',
  'Item':'Objeto', 'Stadium':'Estadio', 'Pokémon Tool':'Herramienta Pokémon',
  'Basic Energy':'Energía Básica', 'Special Energy':'Energía Especial', 'Special':'Especial', 'VSTAR':'VSTAR', 'V':'V'
};
function trPhase(fase){ if(!fase) return ''; return fase.split(',').map(s=>s.trim()).map(t=>PHASE_WORD[t]||t).join(', '); }
const RARITY_WORD = {
  'Common':'Común', 'Uncommon':'Infrecuente', 'Rare':'Rara', 'Rare Holo':'Rara Holo', 'Double Rare':'Doble Rara',
  'Ultra Rare':'Ultra Rara', 'Illustration Rare':'Rara Ilustración', 'Special Illustration Rare':'Rara Ilustración Especial',
  'Hyper Rare':'Híper Rara', 'Promo':'Promo', 'ACE SPEC Rare':'ACE SPEC Rara'
};
function trRarity(r){ return RARITY_WORD[r] || r; }
const RULE_TEXT = {
  'Pokémon ex rule: When your Pokémon ex is Knocked Out, your opponent takes 2 Prize cards.':
    'Regla de Pokémon ex: cuando tu Pokémon ex queda Fuera de Combate, tu rival roba 2 cartas de Premio.'
};
function trRule(r){ return RULE_TEXT[r] || r; }

// --- Localización de carta: ESPAÑOL GARANTIZADO. Nunca se devuelve texto de
//     efecto en inglés: si no hay traducción, se omite (cadena vacía). ---
// Energías básicas: siempre en español (no pasan por TCGdex).
const ENERGIA_ES = {
  Grass:'Energía Planta', Fire:'Energía Fuego', Water:'Energía Agua', Lightning:'Energía Rayo',
  Psychic:'Energía Psíquica', Fighting:'Energía Lucha', Darkness:'Energía Oscura', Metal:'Energía Metal',
  Fairy:'Energía Hada', Dragon:'Energía Dragón', Colorless:'Energía Incolora'
};
const _ETIPO_ID = { grass:'Grass', fire:'Fire', water:'Water', lightning:'Lightning', psychic:'Psychic',
  fighting:'Fighting', darkness:'Darkness', metal:'Metal', fairy:'Fairy', dragon:'Dragon', colorless:'Colorless' };
// Respaldo curado nombre EN -> ES para huecos puntuales (se completa con el DB).
const ES_FALLBACK_NOM = {};
function _tipoEnergiaVista(v){
  if(v.tipos && v.tipos[0]) return v.tipos[0];
  const id=(v.id||'').toLowerCase(), nm=(v.nombre||'').toLowerCase();
  for(const k in _ETIPO_ID){ if(id.indexOf(k)>=0 || nm.indexOf(k)>=0) return _ETIPO_ID[k]; }
  return null;
}
function esEnergiaBasicaVista(v){
  if(!v) return false;
  const sup=(v.supertipo||'').toLowerCase(), fase=(v.fase||'').toLowerCase(), id=(v.id||'');
  return id.indexOf('energy-basic-')===0 || (sup.indexOf('energ')>=0 && fase.indexOf('basic')>=0 && fase.indexOf('special')<0);
}
function nombreLocal(v){
  if(!v) return '';
  if(v.es && v.es.nombre) return v.es.nombre;
  if(esEnergiaBasicaVista(v)){ const t=_tipoEnergiaVista(v); if(t && ENERGIA_ES[t]) return ENERGIA_ES[t]; }
  return ES_FALLBACK_NOM[v.id] || ES_FALLBACK_NOM[v.nombre] || v.nombre || '';
}
// Deriva la URL de la imagen ESPAÑOLA (TCGdex) directamente desde el id de la carta,
// sin depender del DB ni de peticiones async. Patrón: assets.tcgdex.net/es/<serie>/<set>/<num3>/low|high.webp
const _TCGDEX_SETS_IMG = {
  sv1:'sv01', sv2:'sv02', sv3:'sv03', sv3pt5:'sv03.5', sv4:'sv04', sv4pt5:'sv04.5',
  sv5:'sv05', sv6:'sv06', sv6pt5:'sv06.5', sv7:'sv07', sv8:'sv08', sv8pt5:'sv08.5',
  sv9:'sv09', sv10:'sv10', svp:'svp', me1:'me01', me2:'me02', me2pt5:'me02.5', me3:'me03', me4:'me04'
};
function _tcgdexSetImg(sid){
  if(_TCGDEX_SETS_IMG[sid]) return _TCGDEX_SETS_IMG[sid];
  const m = /^sv(\d+)(pt5)?$/.exec(sid); if(m) return 'sv'+String(+m[1]).padStart(2,'0')+(m[2]?'.5':'');
  return null;
}
function _imgEsUrl(id, grande){
  if(!id || id.indexOf('-')<0) return null;
  const i = id.lastIndexOf('-'); const sid = id.slice(0,i), num = id.slice(i+1);
  const tset = _tcgdexSetImg(sid); if(!tset) return null;
  const serie = /^sv/.test(tset) ? 'sv' : (/^me/.test(tset) ? 'me' : null); if(!serie) return null;
  const n = parseInt(num,10); if(isNaN(n)) return null;
  return 'https://assets.tcgdex.net/es/'+serie+'/'+tset+'/'+String(n).padStart(3,'0')+'/'+(grande?'high':'low')+'.webp';
}
function imagenLocal(v, grande){
  if(!v) return null;
  if(v.es){
    const im = grande ? v.es.imagenGrande : v.es.imagenChica;
    if(im) return im;
  }
  const der = _imgEsUrl(v.id, grande);           // imagen ES derivada del id (garantiza español)
  if(der) return der;
  return grande ? (v.imagenGrande || v.imagenChica) : (v.imagenChica || v.imagenGrande);
}
function setNombreLocal(v){
  if(v && v.es && v.es.setNombre) return v.es.setNombre;
  return (v && v.set && v.set.nombre) || '';
}
// Ataque/habilidad i en español. texto '' si no hay traducción (NUNCA inglés).
function atkInfoEs(v, i){
  const o = (v && v.es && v.es.ataques && v.es.ataques[i]) || {};
  const orig = (v && v.ataques && v.ataques[i]) || {};
  return { nombre: o.name || ES_FALLBACK_NOM[orig.name] || orig.name || '', texto: (o.text != null && o.text !== '') ? o.text : '' };
}
function habInfoEs(v, i){
  const o = (v && v.es && v.es.habilidades && v.es.habilidades[i]) || {};
  const orig = (v && v.habilidades && v.habilidades[i]) || {};
  return { nombre: o.name || ES_FALLBACK_NOM[orig.name] || orig.name || '', texto: (o.text != null && o.text !== '') ? o.text : '' };
}
// Reglas/efecto de carta en español (efecto ES de TCGdex; o reglas conocidas; nunca inglés crudo).
function reglasEs(v){
  if(v && v.es && v.es.efecto) return [v.es.efecto];
  return ((v && v.reglas) || []).map(trRule).filter(function(r){ return r && !/[a-z]{4,}.*\b(your|the|you|this|when|may|of)\b/i.test(r); });
}

const TYPE_EMOJI = {
  'Pokemon-Fire':'🔥','Pokemon-Water':'💧','Pokemon-Lightning':'⚡','Pokemon-Grass':'🌿','Pokemon-Psychic':'🔮',
  'Pokemon-Darkness':'🌑','Pokemon-Colorless':'⭐','Pokemon-Fighting':'👊','Pokemon-Metal':'⚙️','Trainer':'🎭',
  'Energy-Fire':'🔥','Energy-Water':'💧','Energy-Lightning':'⚡','Energy-Grass':'🌿','Energy-Psychic':'🔮','Energy-Darkness':'🌑','Energy-Colorless':'⭐'
};
function typeGroupLabel(type){
  const emoji = TYPE_EMOJI[type] || '';
  if(type === 'Trainer') return (emoji+' '+T('grp_trainer')).trim();
  const parts = type.split('-');
  const kind = parts[0], t = parts[1] || '';
  const word = kind === 'Energy' ? T('grp_energy') : T('grp_pokemon');
  return `${emoji} ${word} ${trType(t)}`.trim();
}
// El tipo de mazo ya está en español (base universal); se muestra tal cual.
function deckTypeLabel(v){ return v; }

function applyLang(){
  document.documentElement.lang = lang;
  document.querySelectorAll('[data-i18n]').forEach(el => { el.textContent = T(el.getAttribute('data-i18n')); });
  document.querySelectorAll('[data-i18n-html]').forEach(el => { el.innerHTML = T(el.getAttribute('data-i18n-html')); });
  document.querySelectorAll('[data-i18n-ph]').forEach(el => { el.placeholder = T(el.getAttribute('data-i18n-ph')); });
  document.querySelectorAll('[data-i18n-aria]').forEach(el => { el.setAttribute('aria-label', T(el.getAttribute('data-i18n-aria'))); });
  if(typeof pintarFiltros === 'function') pintarFiltros();
  if(typeof syncRepaint === 'function') syncRepaint();
  renderInventory(); renderLegal(); renderSaved();
  if(typeof renderExploradorGrid === 'function') renderExploradorGrid();
}
