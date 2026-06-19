// ===================== IDIOMA / LANGUAGE (i18n) =====================
let lang = localStorage.getItem('ptcg_lang') || 'es';

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
    lang_btn:'EN',
    nav_col:'Colección', nav_exp:'Explorar cartas', nav_build:'Construir mazo', nav_legal:'Legalidad', nav_saved:'Mazos', nav_proxies:'Proxies',
    px_upload:'Subir imágenes', px_from_col:'Añadir desde mi colección', px_size:'Hoja', px_cut:'Líneas de corte', px_gen:'Generar PDF',
    px_drop:'Arrastra aquí tus imágenes (o usa "Subir imágenes")', px_note:'Imprime al 100% / tamaño real (NO "ajustar a página"). Cada carta 6,3 × 8,8 cm · 9 por hoja. Total: ',
    px_empty_grid:'Aún no has añadido imágenes.', px_empty:'Añade imágenes primero.', px_no_col:'Tu colección está vacía.', px_added:'añadidas', px_generating:'Generando PDF…', px_done:'PDF generado', px_failed:'fallaron', px_lib_err:'No se pudo cargar el generador de PDF (¿sin conexión?).',
    px_search_ph:'Buscar cualquier carta del juego…', px_searching:'Buscando…', px_no_results:'Sin resultados.', px_add:'Añadir al PDF', px_more:'Cargar más', px_search_results:'resultados', px_none:'No se pudo añadir ninguna imagen al PDF.',
    btn_export:'Exportar', btn_import_file:'Importar archivo',
    f_search:'Buscar por nombre…', f_any:'(cualquiera)', f_type:'Tipo', f_supertype:'Categoría', f_rarity:'Rareza', f_set:'Set / Expansión',
    f_subtype:'Subtipo', f_hp_min:'PS mín', f_hp_max:'PS máx', f_pokedex:'Nº Pokédex', f_order:'Ordenar', f_reset:'Limpiar', f_regmark:'Marca', f_toggle:'Filtros',
    aria_lang:'Cambiar idioma', aria_close:'Cerrar',
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
  en: {
    app_sub:'Pokémon Card Manager', stat_inv:'Inventory', stat_cards:'cards', stat_decks:'Saved decks', stat_decks_short:'saved',
    inventory:'Inventory', search_ph:'Search card...', f_name:'Card name', f_name_ph:'E.g. Rapidash',
    f_qty:'Quantity', f_type:'Type', btn_add:'+ Add', btn_quick_load:'⚡ Load my confirmed inventory',
    btn_import_toggle:'Import card list', import_do:'Import', import_ph:'Paste your table or card list here...',
    import_hint:'Paste your list in <strong>any</strong> of these formats:<br>• <strong>Table:</strong> <code>106&nbsp;&nbsp;H&nbsp;&nbsp;Pikachu ex&nbsp;&nbsp;Lightning&nbsp;&nbsp;1×</code><br>• <strong>Comma:</strong> <code>Rapidash, Pokemon-Fire, 7</code><br>• <strong>Colon:</strong> <code>Rapidash: 7</code>',
    empty_inv:'Add cards manually<br>or import a list to begin',
    tab_build:'⚔️ Build Deck', tab_legal:'✅ Check Legality', tab_saved:'💾 Saved Decks',
    controls_title:'Generate AI prompt', c_deck_type:'Deck type', c_variants:'Variants', btn_build:'📋 Generate AI prompt',
    loading:'Analyzing inventory', build_empty_title:'Generate your prompt',
    build_empty_desc:'Choose the deck type and press "Generate AI prompt". A ready-to-paste text will be copied for a conversation with Claude.',
    legal_empty_title:'Empty inventory', legal_empty_desc:'Add cards to see the evolution-line legality analysis.',
    saved_empty_title:'No saved decks', saved_empty_desc:'Build decks with the AI and save them here to review later.',
    t_pk_fire:'Pokémon — Fire', t_pk_water:'Pokémon — Water', t_pk_lightning:'Pokémon — Lightning', t_pk_grass:'Pokémon — Grass',
    t_pk_psychic:'Pokémon — Psychic', t_pk_dark:'Pokémon — Darkness', t_pk_colorless:'Pokémon — Colorless', t_pk_fighting:'Pokémon — Fighting',
    t_pk_metal:'Pokémon — Metal', t_trainer:'Trainer', t_en_fire:'Energy — Fire', t_en_water:'Energy — Water',
    t_en_lightning:'Energy — Lightning', t_en_grass:'Energy — Grass', t_en_psychic:'Energy — Psychic', t_en_dark:'Energy — Darkness', t_en_colorless:'Energy — Colorless',
    d_fire:'🔥 Fire', d_water:'💧 Water', d_lightning:'⚡ Lightning', d_grass:'🌿 Grass', d_psychic:'🔮 Psychic', d_dark:'🌑 Darkness', d_fighting:'👊 Fighting', d_colorless:'⭐ Colorless', d_best:'🏆 The best possible',
    v3:'3 variants', v4:'4 variants', v5:'5 variants', v6:'6 variants', v7:'7 variants', v8:'8 variants', v9:'9 variants', v10:'10 variants',
    grp_pokemon:'Pokémon', grp_energy:'Energy', grp_trainer:'Trainers',
    d_ps:'HP', d_ability:'Ability', d_weak:'Weakness', d_resist:'Resistance', d_retreat:'Retreat', d_evolves:'Evolves from', d_type:'Type', d_no:'No.', d_illus:'Illus.',
    legal_legals:'Legal Pokémon', legal_illegals:'Illegal Pokémon', legal_reason_ok:'Complete evolution line', legal_missing:'Missing:',
    no_results:'No results for', empty_default:'Add cards manually<br>or import a list',
    sec_pokemon:'Pokémon', sec_trainers:'Trainers', sec_energies:'Energy', m_total:'Total',
    s_analysis:'Analysis', s_strategy:'Strategy', s_advantages:'Strengths', s_weaknesses:'Weaknesses', s_difficulty:'Difficulty', s_offtype:'Off-type Pokémon',
    btn_copy:'📋 Copy', btn_save:'💾 Save', btn_view:'View', btn_delete:'Delete', btn_recopy:'📋 Copy again',
    no_decks_title:'No results', no_decks_desc:'The AI could not build decks with the current inventory.',
    saved_type:'Type', deck_total_word:'cards',
    to_need_name:'Enter the card name', to_updated:'Updated', to_added:'Added', to_loaded:'Inventory loaded',
    to_unique:'unique', to_imported:'cards imported', to_lines_skipped:'lines skipped', to_need_inv:'Load your inventory first',
    to_prompt_copied:'Prompt copied — paste it into a conversation with Claude', to_prompt_manual:'Generate the prompt and copy it manually below',
    to_prompt_recopied:'Prompt copied again', to_deck_copied:'Deck copied to clipboard', to_saved:'saved', to_deck_deleted:'Deck deleted',
    load_confirm:'This will replace your current inventory with your confirmed inventory (Pikachu Deck + Darkrai Deck + 151 cards). Continue?',
    no_data:'No data for', no_data2:'Run descargar-cartas.py to include it.',
    prompt_steps1:'The prompt is already copied to your clipboard ✅', prompt_steps2:'Open a conversation with Claude and <strong>paste it</strong> (Ctrl/Cmd + V)', prompt_steps3:'Claude will give you ready-to-copy decks',
    prompt_header:'Prompt generated — type', prompt_variants_word:'variants',
    lang_btn:'ES',
    nav_col:'Collection', nav_exp:'Browse cards', nav_build:'Build deck', nav_legal:'Legality', nav_saved:'Decks', nav_proxies:'Proxies',
    px_upload:'Upload images', px_from_col:'Add from my collection', px_size:'Sheet', px_cut:'Cut lines', px_gen:'Generate PDF',
    px_drop:'Drag your images here (or use "Upload images")', px_note:'Print at 100% / actual size (NOT "fit to page"). Each card 6.3 × 8.8 cm · 9 per sheet. Total: ',
    px_empty_grid:'No images added yet.', px_empty:'Add images first.', px_no_col:'Your collection is empty.', px_added:'added', px_generating:'Generating PDF…', px_done:'PDF generated', px_failed:'failed', px_lib_err:'Could not load the PDF generator (offline?).',
    px_search_ph:'Search any card in the game…', px_searching:'Searching…', px_no_results:'No results.', px_add:'Add to PDF', px_more:'Load more', px_search_results:'results', px_none:'Could not add any image to the PDF.',
    btn_export:'Export', btn_import_file:'Import file',
    f_search:'Search by name…', f_any:'(any)', f_type:'Type', f_supertype:'Category', f_rarity:'Rarity', f_set:'Set / Expansion',
    f_subtype:'Subtype', f_hp_min:'HP min', f_hp_max:'HP max', f_pokedex:'Pokédex No.', f_order:'Sort', f_reset:'Clear', f_regmark:'Reg. mark', f_toggle:'Filters',
    aria_lang:'Toggle language', aria_close:'Close',
    sync_in:'Sync', sync_out:'Sign out', sync_ok:'Sync enabled', sync_err:'Could not sign in', sync_login:'Sign in with Google',
    sync_keep:'You have {n} cards on this device. Keep them in your account?\n\nOK = keep them · Cancel = start with an empty collection',
    login_title:'Pokémon TCG Deck Builder', login_sub:'Sign in to access your collection.', login_checking:'Checking session…', login_btn:'Sign in with Google', login_denied:'That account does not have access.',
    sync_saving:'Saving…', sync_saved:'Saved', sync_offline:'Offline',
    btn_clear:'Clear collection', cf_clear:'Clear your ENTIRE collection? This cannot be undone.', cf_clear2:'Final confirmation: {n} cards will be deleted and this CANNOT be undone. Are you sure?', to_cleared:'Collection cleared',
    ord_name:'Name (A-Z)', ord_name_desc:'Name (Z-A)', ord_hp:'HP ↑', ord_hp_desc:'HP ↓', ord_number:'Card No.', ord_recent:'Newest',
    col_empty:'Your collection is empty or no card matches the filters.',
    exp_empty:'No results. Try other filters.', exp_loading:'Searching…', exp_results:'results', exp_error:'Query error (offline?).',
    exp_more:'Load more', exp_hint:'Search and filter ALL cards in the game. Tap a card to see its printing and add it.',
    exp_cache:'cache', exp_backup:'TCGdex backup',
    cd_owned:'In your collection', cd_add:'Add to my collection', cd_remove1:'Remove 1',
    to_import_ok:'Collection imported', to_import_err:'Invalid file', to_removed:'Removed from collection',
    rep_btn:'🔄 Replace', rep_use:'Use as replacement', rep_choosing:'Replacing', rep_cancel:'✕ Cancel replacement',
    rep_confirm:'Replace this card with the new one?', rep_keepqty:'Quantity kept', rep_done:'Card replaced',
    rep_same:'Same card', rep_hint:'Pick below the exact card you want instead.',
    cf_remove:'Remove from collection', cf_import:'This will replace your ENTIRE current collection. Continue?',
    tile_replace:'Replace this card', tile_delete:'Remove from collection',
    btn_generate:'Generate decks', btn_prompt:'AI prompt', gen_none:'No attackers of that type in your collection.',
    gen_filter_none:'No card matches the selected filters.',
    // Inventory mode modal
    gm_title:'How should inventories be handled?', gm_indep_t:'Independent inventories',
    gm_indep_d:'Each deck may use any available card; cards can repeat across decks.',
    gm_shared_t:'Shared inventory', gm_shared_d:'A single physical collection: a card used by one deck cannot be reused in another deck of this run.',
    gm_cancel:'Cancel',
    // Generator filters
    c_filters:'Deck filters', c_set:'Expansion', c_serie:'Series', c_marca:'Reg. mark', c_format:'Format',
    c_mech:'Allowed mechanics', c_speclimit:'Max special Pokémon', c_depth:'Evolution depth',
    c_singleprize:'Single-prize only (no ex/V/GX)', c_hpmin:'Min HP', c_exclude:'Exclude cards',
    opt_nolimit:'No limit', fmt_std:'Standard', fmt_exp:'Expanded', fmt_unl:'Unlimited',
    depth_any:'Any', depth_basic:'Basics only', depth_s1:'Up to Stage 1', depth_s2:'Up to Stage 2',
    ph_exclude:'E.g. Charizard ex, Pikachu', ph_hpmin:'0',
    // Comparison table
    cmp_title:'Deck comparison', cmp_feature:'Feature', cmp_deck:'Deck',
    cmp_consistency:'Consistency', cmp_speed:'Speed', cmp_damage:'Damage', cmp_ease:'Ease',
    cmp_learn:'Ideal to learn', cmp_potential:'Potential with collection',
    // Versus / Room
    nav_versus:'Versus',
    vs_player:'Player', vs_you:'you', vs_owner:'Host', vs_guest:'Guest',
    vs_online:'online', vs_offline:'offline',
    vs_st_idle:'Idle', vs_st_building:'Building', vs_st_ready:'Ready',
    vs_rules:'Match rules', vs_bestof:'Best of', vs_use_rules:'Use these rules to generate',
    vs_my_deck:'My deck for the match', vs_ready:"I'm ready", vs_unready:'Not ready',
    vs_ready_note:'Only the stats (stars) are shared, never your cards or deck type.',
    vs_pick_deck:'Pick a saved deck first', vs_no_decks:'You have no saved decks',
    vs_deck_of:'Deck of', vs_waiting:'Waiting for the other player to be ready…',
    vs_stats_title:'Strength comparison (cards stay hidden)', vs_waiting_player:'Waiting for player…',
    vs_score:'Scoreboard', vs_flip:'Flip a coin', vs_reset:'Reset scoreboard', vs_starts:'Starts',
    vs_timer:'Timer', vs_need_two:'The other player is missing',
    vs_chat:'Chat', vs_chat_ph:'Type a message…', vs_send:'Send',
    vs_connected:'connected', vs_is_ready:'is ready',
    vs_guest_banner:'Guest mode — you view the collection read-only and build your deck.',
    vs_owner_noshare:'The host has not shared their collection yet.',
    ro_blocked:'Read-only: you cannot edit the host collection.',
    vs_preview:'View my deck', vs_new_deck:'Generate new deck',
    vs_ready_note2:'Your deck stays private: the other player only sees your stats and the shared cards to split.',
    vs_prizes:'Prizes', vs_new_match:'New match', vs_you_win:'You won the match!',
    vs_both_ready:'Ready!', vs_conflicts_title:'Shared cards to split',
    vs_conflicts_note:'You both use these cards and there aren’t enough physical copies. Decide who uses them.',
    vs_no_conflicts:'No clashes: you can build both decks at once.',
    vs_have:'You have', vs_split:'Short',
    vs_only_compliant:'Only rule-compliant decks', vs_compliant:'Meets the rules',
    vs_none_compliant:'None of your decks meet the agreed rules.',
    vs_deck_nocumple:'Heads up: this deck does not meet the agreed rules.', vs_notif_enable:'Enable alerts',
  }
};
function T(k){ return (I18N[lang] && I18N[lang][k] != null) ? I18N[lang][k] : (I18N.es[k] != null ? I18N.es[k] : k); }

// --- Traducción de vocabulario fijo de las cartas (inglés -> ES/EN) ---
const TYPE_WORD = {
  Fire:{es:'Fuego',en:'Fire'}, Water:{es:'Agua',en:'Water'}, Lightning:{es:'Eléctrico',en:'Lightning'},
  Grass:{es:'Planta',en:'Grass'}, Psychic:{es:'Psíquico',en:'Psychic'}, Darkness:{es:'Oscuro',en:'Darkness'},
  Fighting:{es:'Lucha',en:'Fighting'}, Metal:{es:'Metal',en:'Metal'}, Fairy:{es:'Hada',en:'Fairy'},
  Dragon:{es:'Dragón',en:'Dragon'}, Colorless:{es:'Incoloro',en:'Colorless'}
};
function trType(t){ return (TYPE_WORD[t] && TYPE_WORD[t][lang]) || t; }
const SUPER_WORD = { 'Pokémon':{es:'Pokémon',en:'Pokémon'}, 'Pokemon':{es:'Pokémon',en:'Pokémon'}, 'Trainer':{es:'Entrenador',en:'Trainer'}, 'Energy':{es:'Energía',en:'Energy'} };
function trSuper(s){ return (SUPER_WORD[s] && SUPER_WORD[s][lang]) || s; }
const PHASE_WORD = {
  'Basic':{es:'Básico',en:'Basic'}, 'Stage 1':{es:'Fase 1',en:'Stage 1'}, 'Stage 2':{es:'Fase 2',en:'Stage 2'},
  'ex':{es:'ex',en:'ex'}, 'Supporter':{es:'Partidario',en:'Supporter'}, 'Item':{es:'Objeto',en:'Item'},
  'Stadium':{es:'Estadio',en:'Stadium'}, 'Pokémon Tool':{es:'Herramienta Pokémon',en:'Pokémon Tool'},
  'Basic Energy':{es:'Energía Básica',en:'Basic Energy'}, 'Special Energy':{es:'Energía Especial',en:'Special Energy'},
  'Special':{es:'Especial',en:'Special'}, 'VSTAR':{es:'VSTAR',en:'VSTAR'}, 'V':{es:'V',en:'V'}
};
function trPhase(fase){ if(!fase) return ''; return fase.split(',').map(s=>s.trim()).map(t=>(PHASE_WORD[t]&&PHASE_WORD[t][lang])||t).join(', '); }
const RARITY_WORD = {
  'Common':{es:'Común',en:'Common'}, 'Uncommon':{es:'Infrecuente',en:'Uncommon'}, 'Rare':{es:'Rara',en:'Rare'},
  'Rare Holo':{es:'Rara Holo',en:'Rare Holo'}, 'Double Rare':{es:'Doble Rara',en:'Double Rare'},
  'Ultra Rare':{es:'Ultra Rara',en:'Ultra Rare'}, 'Illustration Rare':{es:'Rara Ilustración',en:'Illustration Rare'},
  'Special Illustration Rare':{es:'Rara Ilustración Especial',en:'Special Illustration Rare'},
  'Hyper Rare':{es:'Híper Rara',en:'Hyper Rare'}, 'Promo':{es:'Promo',en:'Promo'}, 'ACE SPEC Rare':{es:'ACE SPEC Rara',en:'ACE SPEC Rare'}
};
function trRarity(r){ return (RARITY_WORD[r] && RARITY_WORD[r][lang]) || r; }
const RULE_TEXT = {
  'Pokémon ex rule: When your Pokémon ex is Knocked Out, your opponent takes 2 Prize cards.':
    {es:'Regla de Pokémon ex: cuando tu Pokémon ex queda Fuera de Combate, tu rival roba 2 cartas de Premio.',
     en:'Pokémon ex rule: When your Pokémon ex is Knocked Out, your opponent takes 2 Prize cards.'}
};
function trRule(r){ return (RULE_TEXT[r] && RULE_TEXT[r][lang]) || r; }

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
const DECK_TYPE_WORD = {
  'Fuego':'Fire','Agua':'Water','Eléctrico':'Lightning','Planta':'Grass','Psíquico':'Psychic',
  'Oscuro':'Darkness','Lucha':'Fighting','Incoloro':'Colorless','el mejor posible':'the best possible'
};
function deckTypeLabel(v){ return lang === 'en' ? (DECK_TYPE_WORD[v] || v) : v; }

function applyLang(){
  document.documentElement.lang = lang;
  document.querySelectorAll('[data-i18n]').forEach(el => { el.textContent = T(el.getAttribute('data-i18n')); });
  document.querySelectorAll('[data-i18n-html]').forEach(el => { el.innerHTML = T(el.getAttribute('data-i18n-html')); });
  document.querySelectorAll('[data-i18n-ph]').forEach(el => { el.placeholder = T(el.getAttribute('data-i18n-ph')); });
  document.querySelectorAll('[data-i18n-aria]').forEach(el => { el.setAttribute('aria-label', T(el.getAttribute('data-i18n-aria'))); });
  const lb = document.getElementById('lang-toggle'); if(lb) lb.textContent = T('lang_btn');
  if(typeof pintarFiltros === 'function') pintarFiltros();
  if(typeof syncRepaint === 'function') syncRepaint();
  renderInventory(); renderLegal(); renderSaved();
  if(typeof renderExploradorGrid === 'function') renderExploradorGrid();
  localStorage.setItem('ptcg_lang', lang);
}
function toggleLang(){ lang = (lang === 'es') ? 'en' : 'es'; applyLang(); }
