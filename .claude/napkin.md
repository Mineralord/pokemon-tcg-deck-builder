# Napkin — Pokémon TCG (Colección y Generador de Mazos)

Runbook curado. Solo guía recurrente de alto valor.

## ⏸️ RETOMAR AQUÍ (23 Ago 2026) — COSMETIC ASSET LIBRARY, siguiente sprint
Sesión pausada con árbol de git LIMPIO (todo committeado, último = `0f20558` Kenney Emotes IMAGE). Rama
`feature/studio-match-mode`, sin push (local). **Infra de cosméticos data-driven COMPLETA** (ver bloque
"COSMETIC ASSET LIBRARY" más abajo): S1 registro · S2 licencias/ledger · S3 VFX+categorías · S4 perfil+colección
+favoritos · S5 parcial Cosmetics Lab (Studio) · **S6 ✅ 1er pack IMAGE = Kenney Emotes CC0** (commit `0f20558`).
**➡️ SIGUIENTE (elegir con el usuario):** (a) hook Museo/Legado para ARCHIVED/LEGACY · (b) audio SFX cosmético (lazy).
**S6 (23 Ago, Demo):** primer `assetType=IMAGE` real. 6 emotes vector del **Kenney Emotes Pack (CC0=GREEN)** en
`app/src/main/assets/cosmetics/kenney-emotes/` (+`License.txt`), pack data-driven `kenney-emotes.json`. `CosmeticPreview`
(app/Store.kt) gana rama IMAGE → carga con **Coil** desde `file:///android_asset/{assetRef}`, con prioridad sobre el render
procedural. Binarios SOLO en `:app` (no en Studio → su Lab muestra fallback). **Patrón futuros packs GREEN: extraer
binario→assets/→pack JSON→index.json.**
Plan aprobado en `~/.claude/plans/merry-brewing-crescent.md`. Entregables: `entregables/TCG-Juego-debug.apk` +
`TCG-Studio-debug.apk`. **Límite firme mantenido:** NO empaquetar assets Pokémon/PTCGO/KARDS (solo GREEN).

## 📜 CANON EN EL REPO + AUDITORÍAS POR FASE (22 Ago 2026) — rama feature/studio-match-mode
**El Canon vive AHORA en `docs/canon/`** (`fase-00`…`fase-11`, con `README.md` índice) — antes estaba
fuera del repo. Auditorías de conformidad en `docs/audits/` (`AAAA-MM-DD-faseN-audit.md`).
**Flujo de trabajo por fase (repetible):** auditar código vs esa fase del Canon → documentar hallazgos
(severidad + decisión canónica + archivo) → arreglar → build verde → commit. En conflicto **manda la Fase 0**.
**➡️ SIGUIENTE: continuar con la FASE 4 (Obtención de Cartas).** Ya auditadas: Fases 0, 1, 2 y 3.
**Fase 3 — TIENDA Y PRECIOS (auditada 22 Ago): Sobres CONFORME + Tienda de COSMÉTICOS implementada
(Sprint "Tienda AAA v1", Demo).** Auditoría: `docs/audits/2026-08-22-fase3-audit.md`. **F3-A/B/C RESUELTOS.**
La Tienda de Sobres ya cumplía el Canon (reglas de la Fase 2: 10 cartas, prob. oficiales, 2/24h acum. a 6,
compra Cristales tope 10/día, precio uniforme, sin descuentos, omitir animación). **Tienda de Cosméticos
(sumidero de Monedas):**
- Dominio `data:profile/Cosmetic.kt`: `CosmeticCategory`(Avatar/Marco/Fondo/Tapete/Funda/Caja) · `CosmeticRarity`
  (Común…Mítico §9.3) · `Cosmetic` + `CosmeticCatalog` (estático, aditivo, arte procedural propio, sin copyright).
  Precio por rareza en `EconomyRules.cosmeticPrice(rarity, prestige)` (prestigio ×3, §14.3/§14.5).
- Persistencia: `PlayerProfile.ownedCosmetics:Set` + `equippedCosmetics:Map<CosmeticCategory,id>`;
  `ProfileRepository.buyCosmetic` (gasto ATÓMICO de Monedas, no re-compra) + `equipCosmetic`. Nube:
  `ProfileSnapshotDto` **v2** (retro-compat v1, defaults vacíos) + **fusión aditiva** de `ownedCosmetics` en
  `MergeStrategy` (nunca se pierde un cosmético). `equippedCosmetics` sigue al lado más reciente.
- UI AAA `app/Store.kt`: piano-glass (Marvel Snap); `StoreHubScreen` (hub Sobres|Cosméticos, §2.2) +
  `CosmeticStoreScreen` (pestañas por categoría, marcos por rareza con glow + **shimmer** en Legendario/Mítico,
  preview procedural, ficha compra/equipar). Avatar equipado se refleja en `HomeScreen` (`AvatarHex`).
- Concesión inicial `STARTER_MONEDAS=1500` (gate propio `MONEDAS_SEEDED`, aplica también a installs ya sembrados);
  `seedBalancesOnce()` ahora se llama también en `AppShell` (LaunchedEffect). "TIENDA" → `Screen.STORE` (hub).
- F3-C: `PacksUiState.maxPerDay` → `walletMax` (era el tope del monedero, no un límite diario).
- **Pendiente F3 (futuro, no bloquea):** colecciones cosméticas (Cap. 10) + recompensas por colección (Cap. 11);
  vías de obtención logros/eventos/temporadas/regalos (§12, Fases 6/10); categorías Efectos/Sonidos/Museo.
- **`data:cloud` NO tiene test source set** (0 tests); cambios de merge validados por build verde de `:app`.
**Sprint "Cosméticos en partida: Tapetes + Fundas AAA" (22 Ago, Demo):** los cosméticos equipados YA se ven en
combate. Temas de PRESENTACIÓN en `feature:combat/CosmeticThemes.kt` (mapeados por String id, NO dependen de
data:profile): `MatTheme`+`matThemeFor(id)` (tapetes: arena/liga/campeon/maestro) revisten base+zonas+enrejado
del `CombatMat` **manteniendo el LENTE GRIS central y los rieles idénticos** (todo por fracciones, proporciones
intactas). `SleeveTheme`+`sleeveFor(id)`+`SleeveArt` = dorsos procedurales AAA (funda-lisa/ola/prisma/eclipse),
provistos por `LocalCardSleeve` (CompositionLocal) en la raíz de `CombatScreen` → los dos `CardBack`
(CombatComponents + board/BattleBoard) lo leen; DEFAULT sigue usando `card_back_default`. `CombatScreen` gana
params `matThemeId`/`sleeveId`; `MainActivity` los pasa desde `profile.equippedIn(TAPETE/FUNDA)`. La TIENDA
muestra la previsualización REAL (WYSIWYG) vía `MatThemePreview`/`SleevePreview` en `app/Store.kt`.
**Cableado de visualización de cosméticos hecho:** avatar(Home) ✅ · tapete(partida) ✅ · funda(partida) ✅.
**Pendiente:** marco/fondo(Home/Perfil) · caja de mazo(Barajas) · cosméticos en OnlineGameScreen (solo PvE por ahora).
**COSMETIC ASSET LIBRARY (22 Ago) — infra data-driven (plan aprobado `merry-brewing-crescent`):**
- **Persistencia real:** perfil (owned/equipped/balances) = DataStore local + **Google Drive** (`data:cloud`,
  `ProfileSnapshotDto` v2, fusión aditiva). **Firestore (`data:netfirestore`) = SOLO netplay online**, NO perfil.
  Cosméticos NO usan Firestore.
- **S1 (commit `0e359bc`):** módulo **`data:cosmetics`** (Kotlin puro, espejo de `data:cards`). Catálogo
  data-driven en `resources/cosmetics/index.json`+`core.json` (§17, ya NO hardcodeado). `CosmeticDto`→`Cosmetic`
  (metadata rica: status A-G, license GREEN/YELLOW/RED/ARCHIVE_ONLY, assetType, renderer/assetRef, source/author/
  originalUrl, assetVersion/metadataVersion). `CosmeticRepository.load()` + `Cosmetics.repo` (cache lazy).
  `CosmeticPricing` (precio por rareza, movido aquí para evitar dep circular). **IDs ESTABLES** conservados
  (avatar-brasa, tapete-arena, funda-eclipse…) → saves/nube válidos. `data:profile` depende (api) de `data:cosmetics`.
  Tienda usa `Cosmetics.repo.shopByCategory` (filtra ACTIVE+distribuible). Test `:data:cosmetics` verde.
- **S2 (docs+catalog):** `docs/cosmetics/ASSET-LEDGER.md` (licencias: **Kenney=CC0 GREEN**; **KARDS=ARCHIVE-ONLY**
  © 1939 Games; **Spirit-PTCGO** código GPL / assets **RED** IP Pokémon) + `GUIA-AÑADIR-COSMETICO.md` (manual).
  `restricted.json` cataloga RESTRICTED (KARDS/PTCGO/Pokémon) como **metadata sin binario** (trazabilidad §8/§32);
  la tienda los filtra. **Política firme: solo se empaqueta GREEN (procedural+CC0); NUNCA IP ajena aunque sea privado.**
- **S3 (commit `acc352b`):** efectos de fin de partida como cosméticos (VICTORIA/DERROTA) → `CosmeticCelebrationFx`
  procedural en `feature:combat/CosmeticThemes.kt` (`victoryEffectFor`/`defeatEffectFor`, mapeado por String id),
  dibujado tras `GameOverPanel`; `CombatScreen` gana `victoryEffectId`/`defeatEffectId` (desde MainActivity). Reusa
  Compose animation (NO nueva arq). Catálogo ampliado: VICTORIA/DERROTA/MONEDA/BADGE/EMOTE con previews en tienda.
- **S4 (commits `e76dd19` perfil + `3ddae64` colección):** `ProfileScreen` muestra vitrina de equipados
  (avatar+marco+fondo+insignia) procedural. **Colección de cosméticos** (`CosmeticCollectionScreen` en `app/Store.kt`):
  todo el catálogo por categoría con estado (equipado/obtenido/bloqueado), rareza, procedencia, contador y
  filtros (obtenidos/favoritos, orden rareza/nombre); rejilla perezosa. **Favoritos persistentes**:
  `PlayerProfile.favoriteCosmetics` + `ProfileRepository.toggleCosmeticFavorite` + snapshot nube + fusión aditiva.
  Hub de Tienda gana sección **Colección** (`Screen.COSMETIC_COLLECTION`).
- **S5 parcial — Cosmetics Lab (Studio):** `app-studio/CosmeticsLab.kt` (`CosmeticsLabContent` registrado en
  `StudioLabs.kt`, id `cosmetics`, pantalla completa vía `onEnterFullscreen`). Inspecciona TODO `Cosmetics.repo.all`
  (incl. RESTRICTED etiquetados): rejilla adaptable por categoría, filtro Todos/Procedural/Con asset, **etiqueta
  PROC/IMG/… indicando procedural vs asset**, nombre + **id monoespaciado**, rareza; ficha de metadata completa
  (categoría/rareza/precio/renderer/assetRef/fuente/autor/repo/versión/estado/licencia). Thumbs: tapete/funda/
  victoria/derrota delegan a previews de `feature:combat`; resto procedural local (self-contained, dup menor tolerada
  en Studio). APK Studio: `entregables/TCG-Studio-debug.apk`.
- **Pendiente:** hook Museo/Legado (ARCHIVED/LEGACY) · audio SFX lazy · más assets CC0 (Kenney). Añadir cosmético = JSON.
**Fase 0 (fixes de preservación, ya commiteados):** fusión de nube ADITIVA (unión de colección por máx.
copias, no last-write-wins) en `data/cloud/MergeStrategy.kt`; duplicados sobrantes preservados en
`pending_shards` (no se descartan); **Energías Básicas ILIMITADAS y sin Fichas** (`capFor`→`UNLIMITED`).
**Fase 2 — ECONOMÍA (substrato + primeras fuentes/consumos, ya commiteado):**
- 4 recursos oficiales = `CurrencyKind{MONEDAS,CRISTALES,FICHAS,CREDITOS}` (en `data:profile`).
  `PlayerProfile.balances: Map<CurrencyKind,Int>`; persistido en `ProfileRepository` (`credit`/`spend`
  ATÓMICOS) y sincronizado en `ProfileSnapshotDto`. Parámetros en `data/profile/EconomyRules.kt`.
- **Fuente PvE (23 Ago):** terminar partida SIEMPRE premia (`GameViewModel.onGameFinished`): victoria
  25 Cristales+10 Monedas · derrota 10+5 (participación, §4.3.1). El panel de fin (`GameOverPanel` en
  `feature:combat/CombatScreen.kt`) MUESTRA la recompensa (Cristales cian/Monedas oro); cantidades vía
  params de `CombatScreen` desde `MainActivity`/`EconomyRules` (combat NO depende de data:profile).
- **Fuente PvP CASUAL (23 Ago) — DECISIÓN CANÓNICA (dueño):** el PvP online actual es CASUAL/no
  clasificatorio (sin Rating ni temporada) → fuera de la regla de la Fase 10 Cap.VII (que reserva las
  recompensas por TEMPORADA al **Ranked**). Como amistoso, premia por partida (menor que PvE): victoria
  15 Cristales+8 Monedas · derrota 6+3 (`PVP_*` en `EconomyRules`, `OnlineGameController.onGameFinished`
  acredita atómico; host y guest ven su lado como PLAYER). `OnlineGameScreen` reusa `CombatScreen` → mismo
  panel con recompensa. **El Ranked por temporada (Fase 10) sigue SIN construir; cuando exista, convive.**
- Concesión inicial de Cristales (`seedBalancesOnce`, estado inicial, NO login-reward).
- **Consumo:** compra de sobres con Cristales (`PacksViewModel.buyPack`, gasto atómico + tope 10/día);
  `DailyPackLimiter`/`DailyPackState` REPURPOSADOS = "sobres comprados hoy" (ya no es código muerto).
  Botón "Comprar sobre" en `PackStage` (`PacksScreen.kt`).
- **UI:** 4 monedas en `HomeScreen` con identidad canónica; **tap → ficha informativa** (qué es/para qué
  sirve/cómo se obtiene). Saldos reales (0 por defecto) leídos del perfil vía `MainActivity`.
- Pendiente de economía (fases futuras): crafting/Fichas y reciclaje (Fase 5), mercado/Créditos (Fase 11),
  más fuentes (PvP/logros/temporadas). `pending_shards` aún NO se sincroniza a la nube (falta conversión F5).
**APK demo:** `app/build/outputs/apk/debug/app-debug.apk` (`./gradlew :app:assembleDebug`).

## 🏛 GOBERNANZA POR AGENTES (22 Jul 2026) — DC-5, canon operativo → `docs/GOBERNANZA-AGENTES.md`
**El proyecto trabaja como un equipo AAA de agentes especializados, invocados AUTOMÁTICAMENTE
según la tarea (no hay que pedirlo).** Antes de responder: identificar qué agentes participan;
si varios aplican, TODOS intervienen y el resultado INTEGRA sus conclusiones.
**Orden de autoridad (inferior nunca contradice a superior):** 1) 📚 Canon Auditor · 2) 🏛 Software
Architect · 3) 🎮 Game Systems Architect · 4) 🌎 Tech Research · 5) 🔬 OSINT · 6) Especialistas
(UI/UX · Animation · Network · Persistence · Security · Performance · Test · 📈 Technical Debt Auditor) · 7) ✍ Code Writer · 8) 🔍 Code Reviewer.
**📈 Technical Debt Auditor (nivel 6, asesor):** salud GLOBAL del proyecto (deuda, módulos/clases grandes,
acoplamiento, código muerto, violaciones de arquitectura, degradación) — distinto del Code Reviewer (cambios
concretos). Solo audita/recomienda, nunca modifica. Interviene SOLO en auditorías completas, revisión de
arquitectura, refactors importantes o análisis de evolución (no en trabajo normal → no suma subagentes habituales).
**Política permanente Architectural Health Review (AHR):** revisión global en grandes hitos / antes de versión
importante, con Canon + Architect + Tech Debt + Performance + Test + Tech Research + OSINT. Solo produce INFORME
(`docs/ahr/AHR-<fecha>.md`); no cambia arquitectura — toda mejora pasa luego por el gate del Canon Auditor.
**Gate de código:** antes de que Code Writer implemente, conformidad de Architect + Game Systems (si toca juego)
+ Code Reviewer + Performance (si toca rendimiento) + Canon Auditor. **Regla de investigación:** ante frameworks/
libs/arquitectura/rendimiento/Android/Kotlin/Compose/Firebase/Room/SQL/red/seguridad/testing/CI/shaders/anim/
benchmarks → Tech Research + OSINT intervienen con fuentes externas reales + Skills del proyecto (no solo memoria).
**Fronteras:** Game Systems nunca toca Android/UI; Animation nunca toca el Engine; UI nunca toca reglas.
**Mecánica:** por defecto = roles como LENTES de razonamiento (inmediato); agentes/Skills REALES solo cuando
hay investigación externa genuina, código a escribir/revisar o análisis multi-archivo (no lanzar todos siempre).
**Marco de canon vigente = Fases 0–11.9 + DMI (DC-4). Fases 12–13 = ASPIRACIONALES, aún NO escritas.**
**Invariantes que el Canon Auditor protege:** determinismo del Engine · Engine puro · proveedores tras interfaz
(patrón `MatchTransport`) · versionado multi-eje (ningún blob sin `version`) · direccionalidad `feature→data→engine`.

## 🎴 BOOSTER OPENING LAB / APERTURA DE SOBRES (12 Ago 2026) — feature:packs, rama feature/studio-match-mode
**Es una SOLA feature en `feature:packs`; el "Booster Opening Lab" = el simulador que hospeda el Studio
(`app-studio` → Lab "Simulador de Sobres" → `PackOpeningSimulator`, pantalla completa, sin monedero).
NO crear otro Lab/simulador/timeline: evolucionar el existente.** RNG/colección/economía NO se tocan:
la presentación RECIBE un resultado ya determinado (`RevealedCard[]`).
**Archivos:** `PackOpening.kt` (overlay/revelado por beats), `BoosterPresentation.kt` (enum `Beat`,
`SfxCue`/`HapticCue`+`BoosterFeedback`, `RarityPresentation`/`presentationFor` data-driven por rareza),
`PacksScreen.kt` (selección expansión + `PackStage` rasgado + simulador con controles de Lab),
`BoosterPack.kt` (`TearablePack` troceado + costura `seamYs`/`seamRegionPath`), `PackAudio.kt` (SFX sintetizados).
**Estado ACTUAL (lo que el usuario aprobó):**
- Revelado LIMPIO: sin sobre en escena. La carta sale **boca abajo** y luego gira (fix: ángulo inicial
  del giro = `revealTurns*360` SIN `+180`, si no las comunes salían de frente). **Fondo CLARO iridiscente
  SIEMPRE** (no oscuro); textos oscuros. `iridescent=true` fijo en `CardScene` para colores.
- El **rasgado** vive en su pantalla (`PackStage`): gesto CONTINUO por delta (no posición absoluta,
  umbral 300dp), propagación LOCAL (tear-front x=ancho·tear; la parte no rasgada sigue cerrada), luz solo
  en el tear-front. Sobre grande con "pack approach".
- **Dorso oficial** = Bulbapedia File:Cardback.jpg (745×1040) → `card_back_default.webp` 620×866, con
  esquinas redondeadas transparentes al **~7.5% del ancho** (el marco azul es grueso; radios menores se
  ven "puntiagudos"). En el revelado el dorso se pinta SIN `.clip()` dp (manda la esquina horneada).
  NOTA: reinstalar/DESINSTALAR el APK para evitar caché del dorso viejo.
- **REVERTIDO por preferencia del usuario:** el "sobre abierto persistente en el overlay" + extracción
  por swipe (V3/V5). Código en historial: `ef18d7b` (V3), `d274db3` (V5), revert `154841f`.
- Controles del Lab (en el simulador): velocidad 0.5/1/2×, Play(auto), bucle, forzar rareza (SOLO
  presentación), beat actual, "Recibir todo". HUD se oculta durante el revelado (Presentation Mode).
- Entregables: `entregables/TCG-Studio-SimuladorSobres-debug.apk` y `TCG-Juego-debug.apk` (git-ignored).
- Pendiente menor: igualar el radio del dorso al de los frentes (hoy va algo más redondeado).
- **Fixes 12 Ago (revelado):** el DORSO ahora se recorta con `clip(RoundedCornerShape(10.dp))` + `ContentScale.Crop`
  (igual que el frente) → adiós picos negros en esquinas (el arte "redondeado" horneado los dejaba). Eliminados
  también: la elipse de **sombra de contacto negra** bajo la carta y el `BackStack` (pila de dorsos abanicada a la
  izquierda) + su composable. Todo en `PackOpening.kt`. Compila verde.

## 🗃️ COLECCIÓN "MIS CARTAS" — ✅ CANON EN EL JUEGO (14 Ago 2026) — rama feature/studio-match-mode
**HECHA CANON:** el Lab se PORTÓ al juego reemplazando TOTALMENTE la colección vieja. `app/CollectionScreen.kt`
es ahora una copia del Lab (package `com.mineralord.tcg.app`, entry público `CollectionScreen(onExit, modifier)`,
a PANTALLA COMPLETA sin `SubScreen`). `app/CollectionViewModel.kt` (SlotUi/SetTab/CollectionUiState/binder viejo)
**BORRADO**. `MainActivity` `Screen.COLLECTION` → `CollectionScreen(onExit={HOME})`. Drawables copiados a `app`:
`set_*_logo.png` (5 logos HD ES) + `rarity_*.png` (8 símbolos oficiales). Iconos de energía se leen de `core:designsystem`.
APK `TCG-Juego-debug.apk` (raíz, git-ignored) compila+ensambla verde. **DEUDA:** el Lab (`app-studio/CollectionLab.kt`)
y `app/CollectionScreen.kt` son ahora CÓDIGO DUPLICADO; si se itera más, unificar en un módulo compartido (`feature:collection`).
**Ítems 1–5 del backlog COMPLETADOS antes de canon** (Buscar ampliado · logos+sub-variantes · Ataques+energías · rarezas oficiales).
**Detalle de carta (14 Ago):** el visor es ahora `InteractiveHoloCard` al 96% del ancho (holo que sigue al DEDO, sin diálogo
aparte). Tabla en español: `seriesEs()` traduce la Serie (dato viene en inglés) + fila "Expansión" = `set.name.es`. Ataques
usan el ES impreso del dataset (`atk.text.es`). **DEUDA DE DATOS:** solo el set 151 trae texto ES de ataques/habilidades
(160/207); sv1–sv4/promos (barajas de inicio) NO están scrapeadas en ES → caen a inglés. Para español impreso en TODAS:
scrapear `es.ataques[i].text`/`es.habilidades[i].text` de esas expansiones y añadirlo al dataset. (Archivo 151.json es UTF-8 válido, sin mojibake.)

## 🗃️ COLECCIÓN "MIS CARTAS" — Lab del Studio (13 Ago 2026) — rama feature/studio-match-mode
**Objetivo del usuario:** la colección del juego debe ser **idéntica a la de TCG Live** ("Mis cartas"). Se
construye PRIMERO como Lab del Studio ("Colección de cartas") para iterar/validar; luego se hace **canon** en
el juego (`app/CollectionScreen.kt` + `CollectionViewModel.kt`, que hoy son un binder estilo TCG Live oscuro).
**Referencia AUTORITATIVA (vídeo):** `C:\DOCUMENTOS\POKÉMON TCG\TCG LIVE VS MI APP CLON\POKEMON TCG LIVE\COLECCION TCG POCKET.mp4`
(1080×2400, ~5:12). OJO: el nombre dice "POCKET" pero MUESTRA la colección de **TCG Live**. Fotogramas ya extraídos
en `.vidref/coleccion/f_*.jpg` (regenerar: `ffmpeg -i "<video>" -vf "fps=1/4,scale=540:-1" .vidref/coleccion/f_%03d.jpg`).
**Archivos:** `app-studio/.../studio/CollectionLab.kt` (TODO el Lab, autocontenido) + registro en `StudioLabs.kt`
(Lab id `collection`, ahora recibe `onEnterFullscreen/onExitFullscreen`). `build.gradle.kts` ya tiene `data:profile`+`coil.compose`.
**Datos:** `rememberDexSets()` = `CardRepository.load()` agrupado por prefijo de set × `ProfileRepository(context).profile`
(`seedOnce(starters)` idempotente). El Studio tiene su PROPIO DataStore → colección del Lab = starters + lo abierto en el Simulador.
**Estética = NEUMÓRFICA CLARA de TCG Live** (fondo azul-gris, paneles blancos, línea arcoíris, cian `#35C4E8`/verde `#19D08B`).
**⚠️ La referencia real es TCG Live, NO Pocket** (el vídeo `COLECCION TCG POCKET.mp4` engaña con el nombre; frames en `.vidref/coleccion/`).
**13 Ago se exploró el TELÉFONO EN VIVO por ADB** (más fiable que el vídeo) — el usuario navega y dice "mira ahora".
**📱 ADB (para re-explorar el TCG Live real):** `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`; device `3bf89e4f`; pantalla
`1080×2400`. Captura fiable: `adb shell screencap -p /sdcard/s.png; adb pull …` (NO `screencap -p >` en PowerShell: corrompe a UTF-16).
Interacción: `adb shell input tap X Y` / `input swipe x1 y1 x2 y2 ms`. Recortar iconos con `System.Drawing` en PS.

**✅ HECHO 13 Ago (compila + APK `TCG-Studio-Coleccion-debug.apk` en raíz, git-ignored):**
- **Pantalla completa:** el Lab entra por `onEnterFullscreen{ CollectionScreen(onExit) }` (patrón Match/Pack). Botón `⤢` en cabecera para salir.
- **Expansiones NUEVAS arriba** → `ORDERED_SETS = sv4·sv3pt5(151)·sv3·sv2·sv1·svp·energy`. En agrupado la **flecha ↑/↓ de "Por expansión" invierte** nuevas↔viejas (`shownSets.reversed()` si `!asc`).
- **Vista agrupada (verde): rejilla 5 columnas** (era 3); huecos numerados **gateados por toggle "Mostrar todo"** (arriba), que **se oculta al deslizar abajo y reaparece al deslizar arriba** (`NestedScrollConnection.onPreScroll`).
- **Vista rejilla (blanco): SIN huecos** — solo obtenidas (`.filter{it.owned}`).
- **Contador** se oculta al desplazar y reaparece al soltar (`state.isScrollInProgress`+`AnimatedVisibility`).
- **Icono álbum+pokébola** recreado como **VECTOR** (`AlbumPokeballIcon`, Canvas) junto al interruptor y en el botón flotante (crop 66px se veía borroso).
- **Botón flotante inferior-derecho = ORDENAR** (era "subir arriba", incorrecto). **Contextual:** blanco=8 opciones (añadidas *Por fecha de obtención de cartas* y *…de efectos visuales*, aprox. por nº); verde=SOLO *Por expansión*+dirección.
- **Lupa = Buscar** (`SearchSheet`, **idéntico en ambas vistas**): texto, Conjuntos de filtros (visual), Favoritas/No favoritas, Lista de deseadas, **Rareza** (chips+Marcar todo), **Pokémon·Tipo** (10 tipos+Marcar todo), Otros (Con/Sin habilidad, ex), **Expansiones** (→`ExpansionSelectorSheet`), barra Buscar/✕/Restablecer. `applyFilter()` aplica query/fav/wish/rareza/tipo/habilidad/ex/sets a AMBAS vistas.
- **Selector de Expansiones** = "Serie mostrada actualmente" + desplegable de **Serie** (agrupa por `set.series`) → expansiones seleccionables (multi-select, ✓).

**🔎 Hallazgos del TCG Live real (13 Ago, en vivo):** la **lupa/Buscar es la MISMA en ambas vistas**; lo que cambia por interruptor
es el **Ordenar** (flotante). Buscar real tiene ADEMÁS: **PS** mín/máx, **Daño de ataque** mín/máx, **Carta de Entrenador**
(Objeto/Herramienta/Objeto Fósil/Partidario/Estadio), **Idioma** (9), **Conjuntos de filtros** (guardar). El selector de Expansiones
real usa **"Serie A/Serie B"** (interno) y cada expansión tiene **sub-variantes** (artes de sobre, p.ej. GENES FORMIDABLES
Charizard/Mewtwo/Pikachu) con "Marcar todo"; seleccionar resalta + serie muestra "Seleccionada" + botón → "Selección efectuada".

**🔜 SIGUIENTE:** (1) añadir a Buscar: **PS mín/máx**, **Daño de ataque mín/máx**, **Carta de Entrenador**, **Idioma**, guardar
**Conjuntos de filtros**. (2) **Sub-variantes por expansión** en el selector. (3) **Logos de expansión** reales con relleno arcoíris.
(4) **Sección "Ataques"** en el detalle + **iconos de energía** reales. (5) Símbolos de rareza reales (◆/corona). (6) Validar el APK
en el dispositivo y hacer canon cuando el usuario apruebe.

## 🎬 FRAMEWORK DE ANIMACIONES (21 Jul 2026) — pipeline propio, Kotlin puro + Compose
**Workstream SEPARADO del motor de efectos 151.** Framework de animación propio, diseñado por fases
(primero arquitectura, luego implementación) con investigación previa en GitHub. **Regla del proyecto
ahora en la skill `revision-critica`:** antes de cualquier subsistema complejo (render/shaders/audio/red/
IA/persistencia/sync/PvP/animación) → investigar 3–5 implementaciones reales + comparación ✅/❌/⚠️ ANTES de codear.

**DOS módulos:**
- **`core:animation`** — Kotlin PURO JVM (solo coroutines + kotlin.reflect). SIN Android/Compose/engine.
  **ESTABLE, NO MODIFICAR.** Pipeline: `AnimationDirector(Facade) → Scheduler → Queue → Runner → Player → Steps`.
- **`core:animation-compose`** — Android/Compose. Implementa los *seams* que dejó abiertos el puro. Depende
  de `core:animation` (dependencia SOLO en ese sentido). namespace `com.mineralord.tcg.core.animationcompose`.

**Piezas `core:animation` (fases 1–6, todas con tests verdes):**
- `AnimationRequest` (sealed, entrada; NO "Event" para no chocar con EngineEvent) · `AnimationDefinition`
  (receta) + `AnimationStep` (árbol Composite: Sequence/Parallel + hojas) + `AnimationPolicy` (data class con
  3 ejes ortogonales: Concurrency/Interruptibility/Conflict) · `AnimationHandle` (+`PausableHandle` ISP) +
  `AnimationResult`.
- `AnimationPlayer` = intérprete del árbol (compuestos por recursión, hojas → `AnimationStepExecutorRegistry`
  con OCP; NADA de `when` gigante). `AnimationScheduler` = coordinador (arbiter PURO `DefaultPolicyArbiter` +
  `RunningAnimations` + drena `AnimationQueue`; aplica política: Exclusive bloquea/Parallel solapa/Replace
  cancela/Ignore descarta). `DefaultAnimationQueue` = **buckets FIFO por prioridad** (NO PriorityQueue: estable
  + O(1); `EnumMap`+`ReentrantLock`). `AnimationDirector` = **Facade** = única API pública (`submit`+`StateFlow
  <AnimationState>`); `DefaultAnimationDirector.create(runner, scope, …)` = composition root; `StateReportingRunner`
  decora el runner para reportar estado SIN tocar el scheduler.
- OJO: `AnimationState.pending` queda en 0 (profundidad de cola no observable sin tocar scheduler). `AnimationPriority.Low`
  no lo produce ninguna request hoy.

**Piezas `core:animation-compose` (fases 1–2 hechas, tests verdes):**
- **RenderLayer** — enum con `zOrder` EXPLÍCITO (0/100/200/300/400/500, huecos), NO ordinal. `orderedByZ`.
  Capas: Board→Cards→Flight(overlay anti-clipping)→Particles(Canvas)→Effects→Overlay. **Cero zIndex arbitrarios.**
- **CoordinateRegistry** (frontera lógico↔pantalla): `SlotId`→`SlotBounds` (Offset+IntSize), `SnapshotStateMap`.
  Modifier **`trackBounds`** = SEAM: hoy `onGloballyPositioned`; migrar a `onLayoutRectChanged` cuando se suba el
  BOM a 2025.04.01 (Compose 1.8). **BOM actual = 2024.12.01, mantener por ahora** (decidido). Único punto a cambiar.
- **AnimationRenderState** (renombrado de "Scene": es DTO reactivo de dibujo, no scene-graph). `RenderNode` =
  interfaz ABIERTA (no sealed) → cada feature aporta sus nodos. `AnimationStage` = esqueleto que dibuja capas por
  `orderedByZ` y provee registry+renderState por CompositionLocal (aún NO dibuja nodos).
- **AnimationDefinitionRegistry** = DISTRIBUIDO: `AnimationDefinitionContributor` por módulo, `from(contributors)`
  fusiona con **fail-fast** ante colisión, clave por tipo sealed. **ComposeAnimationRunner** = cierra
  `Request→Definition→Player→Handle` (request sin receta → handle inerte completado).
- Se añadió `compose-foundation` al catálogo (aditivo).

**✅ v1.0 STABLE (21 Jul) — pipeline validado extremo-a-extremo. NO seguir construyendo infraestructura.**
Prueba vertical `Move` HECHA + prueba de INTEGRACIÓN por la fachada HECHA (tests verdes). Piezas nuevas en
`core:animation-compose`: `AnimationStep.Move(fromSlotId,toSlotId,duration)` (reformado en el puro; era el ejemplo
sin usar), **`MoveRenderNode`** (1er RenderNode real: id+origin+destination+progress, capa Flight fija, SIN campos
"por si acaso"), **`MoveExecutor`** (`AnimationStepExecutor<Move>`: resuelve rects vía CoordinateRegistry → crea/
actualiza/elimina nodo con `withFrameNanos`; limpieza en `finally` incl. cancelación), **`MoveNodeRenderer`**
(+`interpolatedBounds()` = `lerp(Rect,Rect,t)` NATIVO de Compose; posición vía `graphicsLayer` translationX/Y, NO
`offset` = fase draw sin relayout), y `FlightLayer` en `AnimationStage` (única capa real conectada; resto vacías).
Tests dirigen el reloj con `BroadcastFrameClock`+`Dispatchers.Unconfined` (no hay coroutines-test). `PipelineIntegration
Test` entra por `AnimationDirector.submit(PokemonPlayed)` y recorre Scheduler→Queue→Runner→Registry→Definition→
Player→MoveExecutor→RenderState (Default policy = Exclusive). **Regla:** lo nuevo se construye SOBRE el framework
(nuevo RenderNode+Executor+Definition+NodeRenderer+capa), NO se rediseña salvo defecto arquitectónico real.
**Pendiente NO hecho (a propósito):** composition root real en `feature:game` (montar AnimationStage +
DefaultAnimationDirector.create + `trackBounds` en las ranuras). El dibujo Compose vivo NO está instrumentado
(no hay compose-ui-test/robolectric); se valida el RenderState que el Stage consume + la matemática del renderer.

## 🎞️ BACKLOG DE ANIMACIONES (21 Jul) — 4 pilares listos, ahora se ALIMENTA el motor
Ya NO se habla de "fases del framework" sino de **Backlog de Animaciones**. Cuatro pilares cubren el ciclo de vida:
✅ **Animation Framework v1.0** (motor desacoplado, probado E2E) · ✅ skill **`animation-benchmark`** (define el
ESTÁNDAR VISUAL; no busca código) · ✅ skill **`implementation-research`** (encuentra la mejor implementación open
source compatible; fichas + A/B/C/D/E) · ✅ skill **`revision-critica`** (audita antes de consolidar).
**FLUJO CANÓNICO de toda animación nueva (repetible, no degradar):**
```
Nueva animación → animation-benchmark → implementation-research → Integración sobre Framework v1.0 → revision-critica → Canónica
```
`animation-benchmark` encadena AUTO con `implementation-research` al terminar el benchmark. **Regla de oro:** primero
entender (mejor referencia visual) → luego investigar (mejor implementación) → integrar/adaptar → desarrollar desde
cero SOLO lo estrictamente exclusivo.
**ORDEN del backlog (decisión del usuario, como Blizzard/Riot/Second Dinner — microanimaciones ANTES que FX):**
**N1 Gameplay fundamental** (robar carta[s], jugar carta, mover entre zonas, evolucionar, adjuntar Energía, barajar,
buscar en mazo, revelar, descartar) → **N2 Combate** (ataque, daño, marcadores, KO, cambio de Activo, retirada,
moneda, dados) → **N3 Efectos** (partículas, glow, holo, shaders, explosiones, rayos, FX por tipo) → **N4 Cinemáticas**
(victoria, derrota, inicio de combate, apertura de sobres, recompensas). **Empezar por N1 "robar carta".** OJO: mover
carta entre zonas YA está validado E2E por el slice (`PokemonPlayed`→`Move`); el pipeline le añade la PIEL de calidad
(timing/easing/overshoot/peso/arco/sonido/haptic), no parte de cero.
**✅ ANIM #001 "Robar carta" CANÓNICA (21 Jul) — 1er ítem del backlog, pipeline completo ejecutado.**
Estándar visual = **"Snap sobrio"** (Marvel Snap tier S calibrado a claridad TCG Live): ~320ms, arco leve
(0.18·dist), escala 0.82→1, rotación −6°→0, overshoot ~8%, SIN partículas. **Triad canónico** (patrón a repetir
para toda animación nueva): hoja pura `AnimationStep.DrawCard(fromSlotId,toSlotId,duration)` → `DrawCardRenderNode`
(2º RenderNode real: +arcHeightPx, capa Flight) → `DrawCardExecutor` (clona el frame-loop de MoveExecutor pero
aplica `DrawCardEasing`=`CubicBezierEasing(0.22,1,0.36,1.08)` overshoot nativo) → `DrawCardNodeRenderer` (Bézier
cuadrática `center()` + `scale()`/`rotationDeg()` acotados vía `graphicsLayer`). Request dominio = `CardDrawn`
(ya existía). Contributor `DrawCardAnimations` mapea CardDrawn→Definition[DrawCard]; convención de slots
`deck:$id`/`hand:$id` (helpers `deckSlotId/handSlotId`; la UI debe publicarlas con `trackBounds`). **0 dependencias
nuevas** (reutiliza Easing+graphicsLayer+lerp de Compose). Framework v1.0 INTACTO (solo +1 subclase al sealed, OCP).
Tests: `DrawCardRenderNodeTest` (puros) + `DrawCardPipelineTest` (E2E por `submit(CardDrawn)`), verdes.
**Deuda anotada (no bloquea):** (1) extraer un `frameProgressLoop` compartido cuando aparezca el 3er executor
(regla de tres; hoy Move+DrawCard duplican el loop); (2) **handoff de estado**: al integrar en composition root,
encadenar la aparición de la carta REAL de mano al fin de la animación (hoy el nodo de vuelo se elimina en `finally`,
pero la carta de reposo la mostrará el feature); (3) object churn (1 node/frame) → pooling solo si el profiling
lo exige. **Método del backlog (repetir):** `animation-benchmark`→`implementation-research`→triad sobre v1.0→
`revision-critica`→Canónica. **Siguiente candidato N1:** jugar carta desde la mano (o robo múltiple, ya diferido).
**Catálogo Maestro de candidatas técnicas (de implementation-research, 21 Jul):** Partículas → **ParticleEmitter**
(`io.github.piotrprus`, Apache-2.0, física, dual-render) clase B, o **Konfetti** (ISC) B para celebraciones. Holo →
**propio AGSL** (API 33+ con FALLBACK gradiente para minSdk 26–32; GPL de simeydotme/pokemon-cards-css BLOQUEA copiar
código = solo inspiración visual; patrón de cableado de ShaderX). Vuelo de carta → **ya lo tenemos** (SharedTransition
solo estudio). Fan-out/drag → Card-Game-Animation (Apache) inspiración. Regla: toda lib externa vive DETRÁS de un
NodeRenderer/Executor en `core:animation-compose`, sustituible; NUNCA en el puro.

## 🏗️ FASE DE IMPLEMENTACIÓN CONTINUA (23 Jul 2026) — skill `implementacion-continua`
Arquitectura del Studio CONGELADA. Se construye **Sprint a Sprint**, política permanente en la skill
**`implementacion-continua`**: flujo `Sprint→Implementación→Build verde→Validación→Commit→Cierre→Siguiente`.
**Prioridad: software funcional > arquitectura.** Cambios de arquitectura SOLO ante bloqueo técnico real o
violación del Canon (si aparece: detenerse, evidenciar, pedir decisión). DoD por Sprint: build verde + tests
afectados verdes + commit quirúrgico + **reproducible desde clon limpio** (verificar con `git worktree` si toca
build/módulos/deps) + sin deuda nueva + sin romper Dual/Regla de Oro/Canon. Prohibido: rediseñar, módulos
innecesarios, abstracciones "por si acaso" (YAGNI), componentes/tokens nuevos en el DS, ciclo plan→revisión→plan.
Sprint más pequeño posible con mejora VISIBLE y funcional; nada de roadmaps. **Studio Bootstrap ✅ cerrado**
(app-studio arranca el Shell 11.1 vacío; commits 2cf57e7/44728c4/6ea9825/6e605bc).
**POLÍTICA PERMANENTE DE ENTREGABLES (en la skill):** cada Sprint se clasifica AUTO como **Interno**
(por defecto, refactor/tests/limpieza/deuda/infra/arquitectura → **NO** genera APK) o **Demo** (mejora
visual, interacción nueva, funcionalidad validable en dispositivo, o petición expresa → **genera APK**
instalable `assembleDebug` como entregable). Ante duda "¿querría verlo/tocarlo en el móvil?" → Demo.
**✅ Sprint "Shell Navigation v1: Rail de Labs conmutable" (23 Jul, DEMO):** el Shell dejó de estar
vacío → hospeda Labs y CONMUTA entre ellos. Responsabilidad ÚNICA del Shell: hospedar+conmutar (NO
lógica de ningún Lab). `Lab(id,title,content:@Composable)` = unidad hospedable (studio:shell/Lab.kt);
`StudioShell(labs: List<Lab>)` guarda `activeLabId` (state), Rail (R2) lista Labs seleccionables con
`SidebarStyle`+barra guía, Host (R3) dibuja `activeLab.content()` o el home vacío, ruta (R1) y Status
(R4) reflejan el Lab activo. La **Galería de Animaciones** = 1er Lab registrado, pero su registro vive
en **app-studio/StudioLabs.kt** (`studioLabs()`, placeholder sin contenido 11.2) → Shell desacoplado.
0 tokens/componentes nuevos (usa SidebarStyle/ButtonStyle/EmptyStateStyle). APK: `app-studio/build/
outputs/apk/debug/app-studio-debug.apk`. Sin tests (módulos UI sin infra de test, como 11.1).
**✅ Sprint "Animation Gallery Lab v1" (23 Jul, DEMO):** primer Lab con CONTENIDO REAL → demuestra que
la infra de Labs soporta contenido, no solo placeholders. La Galería es un Lab NORMAL: el placeholder
de `studioLabs()` se cambió por `AnimationGalleryLabContent()` (app-studio/AnimationGalleryLab.kt); el
Shell NO se tocó (mismo mecanismo registrar→hospedar→conmutar). Contenido: rejilla mínima de 2 col con
las animaciones canónicas del pipeline v1.0 (catálogo estático `CanonicalAnimations`: "Robar carta"
ANIM #001 + "Mover carta" slice), seleccionables (state `selectedId`, default 1ª) con `ListRowStyle`
+ barra guía, y panel de info básica (`PanelStyle`: nombre/categoría/duración/descripción). Todo dentro
del Host (R3). 0 tokens/componentes nuevos, sin dependencias nuevas (catálogo estático, NO se acopló a
core:animation). FUERA de alcance (diferido): preview, timeline, inspector, comparador, edición,
pipeline, importación. APK ensamblado. Sin tests (UI sin infra de test).
**✅ Sprint "Animation Gallery Lab v2: Preview real" (23 Jul, DEMO):** el Studio HOSPEDA el motor de
animaciones REAL y reproduce la animación seleccionada con el MISMO pipeline que usará el juego (cero
código paralelo/mocks). **Composition root ÚNICO compartido:** `core:animation-compose/
CanonicalAnimationHost.kt` → `rememberCanonicalAnimationDirector(coordinates, renderState)` +
`CanonicalAnimationContributors` (lista única de recetas) + `canonicalStepExecutors` (registro único de
executors); tanto el juego como el Studio obtendrán su `AnimationDirector` desde aquí. **Move ascendido
a producción:** `MoveAnimations` (PokemonPlayed→Move) sale del test y pasa a contribuidor real (+helper
`activeSlotId`), junto a `DrawCardAnimations` (CardDrawn→DrawCard). **Preview:** `AnimationGalleryLab.kt`
monta el `AnimationStage` real con 3 ranuras `trackBounds` (mazo/mano/activo, playerId "studio"); botón
"Reproducir" hace `director.submit(anim.request)` (CardDrawn / PokemonPlayed) y el pipeline dibuja el
vuelo. `app-studio` gana dep a `core:animation-compose` (núcleo compartido, OK Dual/Regla de Oro).
**Fix compartido retrocompatible del framework:** `LocalFlightOrigin` — la capa de vuelo compensa su
offset de montaje (`onGloballyPositioned`), por defecto `Offset.Zero` = idéntico a pantalla completa;
así el MISMO Stage renderiza bien dentro del Host del Studio (offset≠0) y en el board del juego. Además
el placeholder de vuelo ahora es VISIBLE (`flightCardPlaceholder`, helper único, sin duplicar) en ambos
NodeRenderers. Tests `:core:animation-compose` y `:core:animation` verdes (puros intactos). APK
ensamblado. FUERA de alcance: timeline/inspector/comparador/edición/parámetros/importación/catálogo auto.
**✅ Sprint "Animation Gallery Lab v3: laboratorio I+D por categorías" (23 Jul, DEMO):** la Galería pasa
de visor a LABORATORIO. Organiza por **categorías (carpetas) → variantes**, cada variante con **estado**
(Experimental/Candidata/Canon; **≤1 Canon/categoría**, invariante `require` en `GalleryCategory`; nada se
borra). **Categoría EVOLUCIÓN con 5 variantes REALES** (skill `aaa-animation-researcher`): EVO_001 Crystal
Bloom (Hearthstone/LoR), EVO_002 Energy Spiral (Genshin/Honkai gacha), EVO_003 Radiant Ascension (LoR
level-up), EVO_004 DNA Morph (anime), EVO_005 Celestial Burst (Marvel Snap+anime) — todas Experimental
(el desarrollador decidirá Canon). **Motor real, 1 sola maquinaria nueva (triad):** paso puro
`AnimationStep.Evolve(slotId,duration,visual)` + `EvolveVisual` (perillas puras rise/spin/flip/pulse/flash/
ring/hue, sin Compose) + request `AnimationRequest.Evolved(playerId,pokemonId,variantId)` + `EvolveRenderNode`
+ `EvolveExecutor` (frame-loop, como Move/DrawCard) + `EvolveNodeRenderer` (Canvas anillo/destello +
graphicsLayer carta; reusa `flightCardPlaceholder` + `LocalFlightOrigin`) + contribuidor `EvolveAnimations`
(mapa variantId→(dur,EvolveVisual); ids en `object EvolveVariants`). **5 variantes = 5 `EvolveVisual`
distintos, un único executor/renderer/pipeline** (identidad como DATOS, no código por variante). Registrado
en el composition root ÚNICO (`CanonicalAnimationContributors`+`canonicalStepExecutors`) → juego y Studio
comparten. UI (`AnimationGalleryLab.kt`): carpetas plegables + filas de variante con badge de estado +
Preview real + "Reproducir" (`director.submit(Evolved(variantId))`). Robar carta/Pokémon Básico quedan como
categorías Canon. Tests `:core:animation(+compose)` verdes (nuevo `EvolvePipelineTest`: Evolved recorre el
pipeline y limpia; las 5 resuelven a definiciones distintas). Sin cambios de gradle/deps. APK ensamblado.
**Método de la categoría (repetible):** investigar AAA → 5 variantes como `EvolveVisual` → probar en Studio
→ el desarrollador marca Canon. NO marcar Canon por cuenta propia (límite de la skill).

## 🗂️ ARQUITECTURA POR EXPANSIÓN (16 Jul 2026) — datos y efectos SEPARADOS por set
**REGLA:** cada expansión vive AISLADA; añadir un set nuevo NO toca los datos/efectos de otro.
**Cartas (datos):** ya NO existe el monolito `cartas-db.json` (borrado). Ahora jerarquía
**`cards/<serie-slug>/<expansion-slug>.json`** (1 archivo por expansión), p.ej.
`cards/scarlet-violet/151.json` (sv3pt5, 207), `cards/scarlet-violet/surging-sparks.json` (sv8, 252),
… + `cards/mega-evolution/mega-evolution.json`. Energías básicas (8 tipos) en **`cards/energies.json`**
(NO es expansión). Índice `cards/index.json` = `{sets:[{code,officialCode,serie,expansion,file}], energies}`.
**Los `id` internos de las cartas NO cambian** (siguen `sv3pt5-39`, etc. — son claves de efectos/barajas/holo);
el `officialCode` (MEW=151, SSP=Surging Sparks, SVI/PAL/OBF/PAR/TEF/TWM/SFA/SCR/DRI/SVP…) es SOLO metadato.
`CardRepository.load()` lee el índice y fusiona `sets[].file` + `energies` (fallback al monolito legado).
Reorganizar tras un scrape: `python tools/scripts/reorg_cards.py` (tabla `SETS` code→serie/exp/oficial adentro).
**Expansiones FUTURAS: agregarlas con su código oficial.** `CardIndexDto`/`CardSetRefDto` en CardDto.kt.
La app tiene 13 expansiones pero solo **151 está completa**; el resto (sobre todo sv8 Surging Sparks=252)
son las cartas de donde salen las barajas "Academia de Combate 2024".
**Efectos:** `EffectsDb.kt` es ahora un AGREGADOR delgado (`registry = buildMap { registerAcademiaDecks();
registerSet151() }`). Los registros de 151 viven en **`Set151Effects.kt`** (`registerSet151()`), los de las
barajas Academia en `AcademiaDecksEffects.kt`. Helpers de clave (`atkKey`/`abiKey`/`prompt`) y los `Effect`
compartidos entre sets (`SOLID`/`TRANQUIL`/`RESTART`/`CALMING`/`switch`) están top-level `internal` en
**`EffectKeys.kt`** (mismo paquete → sin cualificar). `EffectsDb.atkKey/abiKey` siguen públicos (tests).
**Añadir un set futuro:** crear `SetXXXEffects.kt` con `fun MutableMap<EffectId,Effect>.registerSetXXX(){…}`
+ una línea en el `buildMap` de EffectsDb. El inventario lee TODOS los `.kt` del paquete model (glob).

## ⚠️ REGLA PERMANENTE — EL DAÑO **NO** ES UN EFECTO (siempre, en todo el motor)
Un ataque de Pokémon tiene DOS cosas separadas: **(1) daño** (el número, sin texto) y **(2) efecto** (el
texto). **El daño NUNCA es un efecto.** Consecuencias que hay que respetar SIEMPRE al modelar/implementar:
- Un ataque "con daño" (solo número, sin texto) NO necesita `Effect` registrado — el motor calcula el daño base.
- Un ataque "con efecto" es el texto (estados, robar, buscar, mover Energía, snipe…). El daño extra/condicional
  del propio texto SÍ es daño, no "efecto de prevención".
- **Prevención/escudos:** "se evita el DAÑO" (Barrera Mímica, `preventDamageOnTurn`) ≠ "se evitan los EFECTOS"
  (Cubierta de Capullo). Kakuna evita EFECTOS pero el daño le sigue entrando; Mr. Mime evita DAÑO. Son gates
  distintos: uno frena el número, otro frena las ops de texto.
- `Effect.ignoresDefenderEffects` = el ataque ignora prevención/reducción/Herramientas del Defensor (afecta al
  DAÑO). NO confundir con ignorar el texto del defensor.

## 🧬 MECÁNICAS POR ÉPOCA + CATÁLOGO DE ATAQUES/HABILIDADES (17 Jul 2026)
**Objetivo:** la app tendrá TODAS las series/expansiones jugables con las reglas de SU época.
**Modelo de "poderes" (Card.kt):** una Habilidad ya NO es solo "Ability". `enum AbilityKind{ABILITY,
POKE_POWER, POKE_BODY}` con `suppressibleByAbilityLock` (=true SOLO para ABILITY). `Ability.kind`
(default ABILITY = cartas modernas SV). Poké-Power (activo) / Poké-Body (pasivo) son PRE-2011 y un
bloqueo moderno de Habilidades ("los Pokémon no tienen Habilidades", p.ej. Camino hacia la Cima/Klefki)
**NO** los apaga. **Rasgo Antiguo (Ancient Trait, era XY):** tipo `AncientTrait` + campo
`PokemonCard.ancientTrait` (SEPARADO de `abilities` a propósito → inmune a bloqueos de ataque/Habilidad
por construcción). Helper `PokemonCard.effectiveAbilities(abilityLockActive)` filtra las apagables cuando
haya bloqueo (lista para cuando se implemente `ModKind.BLOCK_ABILITY`, que HOY aún no se aplica en el motor).
**DTO/Mapper:** `AbilityDto.type` (etiqueta impresa) → `CardMapper.abilityKind()` (tolera acentos/guiones);
`EsDto.habilidades`/`EsAbilityDto` → nombres/textos ES de habilidades (antes la habilidad se mostraba en
inglés); `CardDto.rasgoAntiguo`/`AncientTraitDto` (forward-compat, SV no los trae). La clave de efecto
(`abiKey(id, nombreEN)`) SIGUE usando el nombre EN top-level → sin cambios en registros.
**CATÁLOGO separado de ataques/habilidades (worklist, NO lo carga la app):** `python tools/scripts/
gen_mechanics_db.py [code…]` genera `docs/mechanics/<serie>/<expansion>.json` = `{code, officialCode, serie,
expansion, counts, attacks[]{cardId,en,es,text,impl}, abilities[]{cardId,en,es,text,kind,impl}}` cruzando
el dataset × claves autoradas en el paquete model (mismo regex que el inventario). Sirve para saber, por
expansión, qué falta implementar. Regenerar tras autorar efectos o scrapear un set nuevo.

### 🚫 BLOQUEO DE HABILIDADES + ESTADIOS (17 Jul 2026) — fiel al TCG
**Rule Box:** `PokemonMechanic.hasRuleBox` (Normal=false; ex/EX/V/VMAX/VSTAR/GX/Radiant/Tera=true). Lo usa
Camino hacia la Cima ("los Pokémon CON caja de regla no tienen Habilidades").
**Bloqueo (`ModKind.BLOCK_ABILITY`, YA aplicado en el motor):** `PassiveModifier` gana `blockBothSides`
(default true = ambos lados; false = solo el lado rival del origen) y `blockOnlyRuleBox` (Path to the Peak).
GameEngine: `abilityLockSources(state)` recolecta fuentes del **Estadio** (`effects[stadium.effect].passives`)
y de habilidades/Herramientas en juego (Klefki). `isAbilityLocked(state, side, pip)`: el propio origen queda
exento, un Pokémon que él mismo aporta bloqueo NUNCA se bloquea, respeta alcance y caja de regla. Solo apaga
`AbilityKind.ABILITY` (vía `PokemonCard.effectiveAbilities`) → Poké-Power/Body y Rasgos Antiguos INMUNES.
Cableado en: `useAbility` (rechaza), `legalIntents` (no enumera), y `abilityPassives(state, side, pip)` — el
lector de pasivos AHORA filtra por bloqueo (antes `abilityPassives(pip)`; existe `abilityPassivesRaw` sin
filtrar SOLO para detectar fuentes, evita recursión). Los 3 llamadores (retirada) pasan state+side.
**Estadios (slot `GameState.stadium` + `stadiumOwner: Side?`, YA existían/añadido):** `playTrainer` despacha
`TrainerKind.Stadium` → `playStadium`: rechaza mismo-nombre en campo, descarta el anterior a la pila de SU
dueño, la carta NUEVA sale de la mano y ocupa el slot (NO va al descarte). `legalIntents` enumera Estadios
jugables (no requieren efecto registrado). Espejado en netplay (`GameStateDto.stadiumOwner`, framing incluido).
**151:** Camino de Bicis (sv3pt5-157) ya se coloca (su efecto 1/turno de robar aún NO implementado = estadio
inerte). Tests: `AbilityLockAndStadiumTest` (registry inyectado con efectos sintéticos de bloqueo).
**FUTURO:** cuando se scrapee Path to the Peak/Klefki, registrar su Effect con el `PassiveModifier(BLOCK_ABILITY,
…)` correspondiente; el motor ya hace el resto. Aún NO hay estadios con efecto ACTIVO 1/turno (Cycling Road).

## ⏩⏩ RETOMAR AQUÍ (al decir "continuemos") — EFECTOS SET 151, FASE 45 — 21 Jul 2026
**Contexto:** implementando TODOS los efectos del set 151 (`sv3pt5`) por fases. Inventario vivo en
**`docs/inventario-efectos-151.md`** (regenerar con `python tools/scripts/gen_inventario_151.py`, cruza
`cards/sv3pt5.json` × `Set151Effects.kt`; textos SIEMPRE del bloque `es` = español impreso, NUNCA inglés).
Progreso: **180/198 efectos únicos** (Fases 1–46, tests verdes). Registros en `Set151Effects.kt`
(NO en EffectsDb.kt, ver arriba). Base de daño puro ya funcionaba sin registro. **Faltan 18.**

### ✅ FASE 46 HECHA (30 Jul) — selección sobre la MANO del rival (Agarrador Mecánico)
**Agarrador Mecánico (Grabber) 162** (Objeto): el rival enseña su mano y pones 1 Pokémon que
encuentres allí en el FONDO de su baraja. Op nueva `EffectOp.PutOppHandPokemonToBottomOfDeck`
(data object). **Reusa `PendingDecision.SearchCards`** con un flag NUEVO `fromOpponentHand=true`:
`pendingFor` construye candidatos = Pokémon de la mano RIVAL (sin ninguno → no pausa, el Objeto se
juega igual); en `resolve`, la rama SearchCards enruta a `applyOpponentHandToBottom` (saca la carta
de la mano del rival → fondo de su baraja; emite `HandRevealed`). `side` de la decisión = quien
decide; el rival es `other()`. **Sin cambios en netplay** (host autoritativo; `SearchCards.toDto`
no envía el flag y el cliente colapsa a ChooseTargets) ni en SmartAgent (ya resuelve SearchCards con
`candidates.take(count)`). Registrado en bloque "Fase 46" (1 sola printing). 2 tests en `TrainerAbilityTest`.
**PATRÓN REUTILIZABLE para el resto del clúster mano-rival:** `SearchCards(fromOpponentHand=true)` +
applier dedicado. **Pendiente (deuda anotada):** Invitación de Erika 160 (Básico de la mano rival → SU
Banca + cambiarlo por su Activo) necesita un applier distinto (destino Banca rival + swap) y encadenar el
swap; y validar el render de candidatos "mano rival" en la UI online (feature:combat) — no verificable aquí.

### ✅ FASE 45 HECHA (30 Jul) — primitivo "el rival enseña su mano" (Zubat)
**Zubat *Eco Revelador/Revealing Echo* 41** (habilidad `oncePerTurn`+`activeOnly`): puramente informativa.
Op nueva `EffectOp.RevealOpponentHand` (data object) → el intérprete emite `GameEvent.HandRevealed(foeSide,
count)` SIN cambiar el estado. Evento nuevo en `engine:events` (solo `CombatLog` es exhaustivo sobre GameEvent →
2 ramas ES/EN; `toFxCue` tiene `else`→null, netplay no conmuta sobre GameEvent = sin ripple). Registrado en
bloque "Fase 45". 2 tests (revela nº de cartas + no altera la mano; rechaza segundo uso en el mismo turno).
Es el PRIMITIVO del clúster "manipular la mano/zonas del RIVAL": Agarrador Mecánico 162 e Invitación de Erika 160
lo extenderán con una decisión REAL sobre la mano rival (aún sin montar; requiere PendingDecision nueva + netplay + UI).

### ✅ FASE 44 HECHA (21 Jul) — "monedas del rival como cruz" (flip gateado)
**Psyduck *Cavilar/Overthink* 54** (sin daño): durante el próximo turno del rival, cada moneda que lance ese
jugador cuenta como CRUZ. Op nueva `EffectOp.ForceOpponentCoinsTailsNextTurn` (data object) → el intérprete fija
`GameState.coinsAsTailsSide = actingSide.other()` y `coinsAsTailsOnTurn = turn+1`. **Infra "flip gateado":**
helper `GameEngine.gatedFlip(s)` = cruz(false) si `s.coinsAsTailsSide==s.activeSide && s.coinsAsTailsOnTurn==s.turn`,
si no `rng.flipCoin()`. Reemplazados por `{ gatedFlip(state) }` SOLO los flips del jugador ACTIVO: coste-para-atacar
(~301), execute de ataque, execute de habilidad y `resolve` de decisión. **NO gateados** `applyDefenderRetaliation`
(857) ni Machamp *Agallas* (888) = tiradas del DEFENSOR en el turno del atacante. Campos espejados en netplay
`GameStateDto` (`coinsAsTailsSide`/`OnTurn`, framed en toDtoFor + toModel); no se limpian (el gate compara turno
exacto). 1 test (marca + fuerza cruz vs control sin marca). **Efecto colateral bueno:** también fuerza a cruz
las monedas "para poder atacar" (Tinta Cegadora) del rival si coincide.

### ✅ FASES 39–43 HECHAS (18 Jul) — resumen breve
- **F39** Meowth *Ven Aquí Ya* 52 — `CoinFlipSwapOppActiveWithChosen` (moneda → el atacante elige un Banca rival que pasa a Activo).
- **F40** Transferencia de Bill 156 — `SearchDeck.fromTop=8` (mira top 8, Pokémon → mano; búsqueda de profundidad limitada).
- **F41** Pegatinas de Energía 159 — `AttachFromRevealed.coinFlip` (enganche desde descarte a una moneda).
- **F42** Electrode *Cadena Bum Bum* 101 — `DiscardOwnToolsForDamage(perCard)` + flag `SearchCards.fromAttachedTools`
  (candidatos = Herramientas enganchadas a tus Pokémon; +40 crudo por cada una; base 20 pasa por Debilidad). NO tocó netplay.
- **F43** Seadra *Tinta Cegadora* 117 — `RequireCoinsToAttackNextTurn(target, coins)` + campos `PokemonInPlay.flipsToAttackOnTurn/
  Count` (espejados en netplay DTO). `GameEngine.attack` lanza N monedas al intentar atacar; cruz → el ataque no ocurre (turno igual acaba).
Además: se enderezó el guard de `EffectsDbTest` (sv3pt5-156 ya modelado → ahora apunta a sv3pt5-12 Bye-Bye Flight).

### ✅ FASE 38 HECHA (18 Jul) — SUBSISTEMA de override de Debilidad (Kabutops + Porygon)
**Nueva capacidad transversal:** `Damage.calculate` acepta `weaknessTypeOverride: EnergyType?` (cambia el TIPO
de la Debilidad, mantiene la cantidad) y `weaknessMultiplierOverride: Int?` (aplica la Debilidad como ×N). La
Debilidad efectiva parte de la impresa; si hay override de tipo sin Debilidad impresa (y sin multiplicador),
es inerte. **Wiring en `GameEngine.attack`:** `weaknessTypeOverride = defender.weaknessOverrideType` (campo NUEVO
por-instancia en PokemonInPlay); `weaknessMultiplierOverride` = primer `Effect.overridesDefenderWeaknessMultiplier`
(flag NUEVO) de una habilidad EN JUEGO del ATACANTE (vía `abilityEffects`, respeta bloqueo).
**Kabutops — Modo Ancestral/Ancient Way 141:** habilidad PASIVA `overridesDefenderWeaknessMultiplier=4` (no
persistente; se lee al atacar el lado de Kabutops). **Porygon — Conversión 4/Conversion 4 137:** op INTERACTIVA
`OverrideDefenderWeaknessType` → decisión NUEVA `PendingDecision.ChooseEnergyType(candidates: List<EnergyType>)`;
al resolver, el intérprete fija `weaknessOverrideType` en el Activo rival. **Campo por-instancia
`PokemonInPlay.weaknessOverrideType`** persiste HASTA que el Pokémon deja el Activo → RESET (=null) en la
**retirada** (GameEngine.retreat) y en el **gust** (EffectInterpreter GustDefenderChooseNewActive). La decisión
viaja por el canal estándar `chosen: List<CardId>` con **CardId centinela** `"energytype:XXX"` (helpers
`PendingDecision.encodeType/decodeType` + `candidateIds`). **Capas tocadas (decisión NUEVA = muchos `when`
exhaustivos):** modelo (PendingDecision + companion), intérprete (pendingFor emite / resolve fija / applyOp no-op),
GameEngine.validateChoice, netplay `GameStateDto` (`DecisionKindDto.CHOOSE_ENERGY_TYPE` + PokemonInPlayDto
`weaknessOverrideType: String?` en decl/toDto/toModel + import EnergyType), SmartAgent (toma el 1er tipo), UI
`CombatDecisions.EnergyTypePicker` (botón por tipo, `energyTypeLabelEs`) y las 2 ramas de `GameScreen` (clásica).
3 tests (Kabutops ×4 con/sin; Porygon pide-tipo→fija campo; override de tipo hace debilidad a un atacante Agua).
Compilan `:engine:*`, `:data:netplay`, `:feature:game`, `:app`. **PATRÓN REUTILIZABLE:** para futuras decisiones
que NO eligen cartas (elegir tipo/opción), usar el CardId-centinela por el canal `chosen` (evita tocar GameIntent).

### ✅ FASE 37 HECHA (17 Jul) — des-evolución del Activo rival
**Op nueva** `EffectOp.DeEvolveDefender` (data object, patrón `GiovanniCharisma`): apunta al Activo rival; si
está evolucionado (`evolutionStack` no vacía), su carta actual (fase más alta) vuelve a la mano de su dueño y el
Pokémon pasa a ser la carta inferior (`evolutionStack.last()`), CONSERVANDO daño/Energía/Herramientas/estados;
si no está evolucionado, no-op. Como los PS bajan a los de la fase inferior, el `handleKnockouts` posterior al
ataque puede noquearlo (probado). **Evento nuevo** `GameEvent.DeEvolved(side, from, to)` + ramas ES/EN en
`CombatLog` (único `when` exhaustivo sobre GameEvent; no hay otros). **Registrado (bloque "Fase 37"):** Aerodactyl
*Rayo Involutivo/Devolution Ray* 142 (base 100 puro + `DeEvolveDefender`). 2 tests (involución normal; KO porque la
fase Básica de 60 PS no aguanta 100 de daño). **NO toca netplay** (el host aplica todo en el flujo del ataque; el
evento es solo log). OJO: `updatePokemon` localiza el pip por el id ANTES de cambiar `card`; el nuevo `card.id`
queda en juego y la carta vieja va a la mano.

### ⏭️ SIGUIENTE (FASE 44) — candidatos y DEUDAS (faltan 21)
**Cluster "manipular la MANO/zonas del RIVAL" (mini-subsistema nuevo, reutilizable para 3 cartas):** Agarrador
Mecánico 162 (rival enseña mano → pones 1 Pokémon al FONDO de su baraja), Invitación de Erika 160 (rival enseña
mano → pones 1 Básico en SU Banca y lo cambias por su Activo), Zubat *Eco Revelador* 41 (habilidad: el rival enseña
su mano). Hoy `SearchCards`/`applySearch` operan sobre las zonas del lado que ACTÚA → hace falta una decisión que
elija sobre la mano RIVAL. **DEUDA #1 (misma familia):** Haunter *Espíritu Retorno* 93 (al evolucionar, 1 Partidario
del descarte RIVAL → su mano). **Estadio activable 1/turno (infra nueva):** Camino de Bicis 157 (descarta 1 Energía
Básica → roba; hoy es estadio inerte). **DEUDA #2:** HP máx. en la BARRA de la UI (cosmético; feature/game lee
`card.hp` impreso; exponer `effectiveMaxHp`). **DEUDA #3:** Kakuna *Cubierta de Capullo* 14 (evitar EFECTOS —no daño—
de ataques: gate por-op del efecto del ataque según objetivo). **Otros bounded:** Spearow *Ventaja Evolutiva* 21
(evolucionar en tu 1er turno si sales 2º — OJO: el motor NO enforcea hoy "no evolucionar tu 1er turno", así que
requiere primero esa regla), Psyduck *Cavilar* 54 (las monedas del rival cuentan como cruz su próximo turno),
Electrode *Cadena Bum Bum* ✅ hecho F42. **Mini-sistemas grandes:** Mew ex *Hackeo Genoma* 151 (copiar ataque rival),
Alakazam ex *Mano Dimensional* 65 (atacar desde Banca), Ditto *Inicio Transformador* 132 (buscar Básico y sustituirse),
Butterfree *Adiós Vuelo/Bye-Bye Flight* 12 (barajar un Banca rival + a sí mismo a los mazos), Fósiles 152/153/154
(jugar como Pokémon Básico), Chansey *Regalo Fortuito* 113 (interacción con Premios). Método de siempre: query python
→ DSL → registro → tests → suite → inventario.

### ✅ FASE 36 HECHA (17 Jul) — pasivos de habilidad condicionados por un aliado en juego (por nombre)
**Infra nueva:** helper `GameEngine.abilityEffects(state, side, pip): List<Effect>` (espejo de `abilityPassives`
pero devuelve los `Effect` de las habilidades EFECTIVAS, respetando el bloqueo) + helper `nameMatches(pip, needle)`
(ES/EN, `contains` ignore-case). Modelados como FLAGS de `Effect` (NO `PassiveModifier`, porque la condición es
por NOMBRE de aliado). **2 flags nuevos:** `freeAttackIfAllyNamed: String?` (Nidoking) y `boostAlliedAttackerNamed:
String?` + `boostAlliedAttackerAmount: Int` (Cubone). **Cableado:** (a) `effectiveAttackCost` gana parámetro
`side: Side` (2 call-sites actualizados: attack ~288 y legalIntents ~1142, ambos `state.activeSide`); si el
atacante tiene una habilidad con `freeAttackIfAllyNamed` y hay un aliado con ese nombre en juego → coste 0.
(b) En `attack()`, `allyBoost` = suma sobre `me.bench` de habilidades con `boostAlliedAttackerNamed` que casen el
nombre del atacante → se suma a `dmgBase + selfBonus + allyBoost` ANTES de Debilidad/Resistencia. **Registrados
(bloque "Fase 36"):** Nidoking *Rey Entusiasta/Enthusiastic King* 34 (`freeAttackIfAllyNamed="Nidoqueen"`),
Cubone *Ovación Ósea/Cheering Bone* 104 (`boostAlliedAttackerNamed="Marowak", 30`). 2 tests. **NO toca netplay**
(el host recalcula coste/daño en su flujo). Nombres EN de habilidad del top-level `habilidades[].name`.

### ✅ FASE 35 HECHA (17 Jul) — efectos AL FINAL del turno (nuevo hook `applyEndOfTurnEffects`)
**Infra nueva:** `GameEngine.endTurn` llama a **`applyEndOfTurnEffects(state, activeSide, events)`** JUSTO
después de `applyBetweenTurns` (Veneno/Quemadura) y ANTES de `handleKnockouts` → procesa el Activo del jugador
que TERMINA su turno. Hace 2 cosas: (a) **daño diferido** — si `active.delayedDamageOnTurn == state.turn`,
suma `delayedDamageAmount` de daño y limpia los campos (el KO lo resuelve el `handleKnockouts` posterior);
(b) **curación de Herramientas** — suma `Effect.healSelfEndOfTurnIfActive` de las Herramientas ancladas y
reduce el daño (cap 0), emite `GameEvent.Healed`. **Campos/ops nuevos:** `PokemonInPlay.delayedDamageOnTurn:
Int?` + `delayedDamageAmount: Int` (GameState.kt, espejados en netplay `GameStateDto` decl+toDto+toModel);
op `EffectOp.ScheduleDelayedDamage(target, amount)` (el intérprete la fija a `turn+1` en el/los target);
flag `Effect.healSelfEndOfTurnIfActive: Int` (Herramientas). **Registrados (bloque "Fase 35"):** Victreebel
*Ácido de Acción Lenta/Slow-Acting Acid* 71 (base 120 puro + `ScheduleDelayedDamage(OPP_ACTIVE, 120)`),
Restos/Leftovers 163 (Herramienta → `EffectId("sv3pt5-163")`, `healSelfEndOfTurnIfActive=20`). 2 tests
(FASE 35). OJO: las Herramientas/Entrenadores se registran con `EffectId(dto.id)` directo (NO atkKey/abiKey).

### ✅ FASE 34 HECHA (17 Jul) — "el ataque no hace nada si…" + premio extra al noquear
**3 flags nuevos en `Effect`, evaluados en `GameEngine.attack`:** (a) `noEffectUnlessSelfConfused` y (b)
`noEffectIfEvolvedThisTurn` → juntos forman `attackFizzles`; si true, `dmgBase = 0` (nuevo val, NO shadowea
`base`) Y el efecto NO se ejecuta (`if (effect != null && !attackFizzles)`). "No hace nada" cubre daño+efecto
(recordar: el daño NO es un efecto, pero "no hace nada" anula ambos). Slowbro se detecta con
`attacker.turnsInPlay == 0` (evolucionar/colocar resetea el contador; un Fase 1 activo con 0 turnos evolucionó
este turno). (c) `extraPrizeIfKo` → `attack` calcula `extraPrizes = 1` si el ataque hizo daño y lo pasa a
`handleKnockouts(foeSide, …, extraPrizes)` (param NUEVO, default 0), que lo suma a `prizesToTake`
(`coerceAtMost` los premios restantes). **Registrados (bloque "Fase 34"):** Primeape *Golpe Rabioso/Raging
Smash* 57 (`noEffectUnlessSelfConfused`), Slowbro *Placaje Relajado/Laid-Back Tackle* 80
(`noEffectIfEvolvedThisTurn`), Clefable *Más Luna/More Moon* 36 (`extraPrizeIfKo`). NO toca netplay (todo lo
resuelve el host en el flujo del ataque). 3 tests. OJO: el motor NO implementa la moneda de Confusión al
atacar, así que un Primeape Confundido pega directo (test sin moneda).

### ✅ FASE 33 HECHA (17 Jul) — KO-triggers en handleKnockouts (sobrevivir-KO / absorber Energía)
**Infra:** `GameEngine.handleKnockouts` gana parámetro `byAttack: Boolean = false`; SOLO los 2 call-sites del
ATAQUE (`attack()`, líneas ~389-390) pasan `byAttack = true` → así los KO por Veneno/Quemadura/retroceso no
disparan estos triggers (fiel a "por el daño de un ataque"). **2 flags nuevos en `Effect`:**
`survivesKoWithCoin` (Machamp) y `pullsEnergyFromKoAllyType: EnergyType?` (Raichu).
**Machamp — Agallas/Guts 68:** al inicio de `handleKnockouts` (tras confirmar KO por HP efectivo), si `byAttack`
y el Activo tiene la habilidad (no bloqueada), lanza `rng.flipCoin()`; con cara NO queda KO y sus PS restantes
pasan a 10 (`damage = maxHp - 10`) → `return` temprano con el superviviente. Con cruz sigue el flujo normal de KO.
**Raichu — Toma de Tierra/Electrical Grounding 26:** si `byAttack` y `state.activeSide != koSide` (= el KO lo
causó el ataque del RIVAL), busca en la Banca del lado noqueado un Pokémon con `pullsEnergyFromKoAllyType`; si lo
hay y el Activo noqueado tiene 1 Energía Básica de ese tipo, la mueve del noqueado a ese Pokémon de Banca ANTES
del descarte (emite `EnergyAttached`). Auto (el "puedes" siempre conviene). **NO toca netplay** (el host aplica
todo en el flujo del ataque; sin decisión ni campo nuevo de estado). 2 tests (bloque FASE 33). Nombres EN de
habilidad del top-level `habilidades[].name` (Machamp="Guts", Raichu="Electrical Grounding").

### ✅ FASE 32 HECHA (17 Jul) — "contragolpe" del Defensor + reflejo de daño diferido
**Hook nuevo** `GameEngine.applyDefenderRetaliation(state, defenderSide, receivedDamage, events)` — corre en
`attack()` DESPUÉS del efecto del ataque y ANTES de `handleKnockouts` (el Activo dañado aún está en juego, así
"incluso si queda KO" funciona). Dispara habilidades del Activo defensor con `actingSide = DEFENSOR` → sus ops
`OPP_ACTIVE` apuntan al ATACANTE. Respeta bloqueo de Habilidades. **Flags nuevos en `Effect`:**
`triggerOnActiveDamaged` (Hitmonchan) y `triggerOnActiveKO` (solo si el daño lo dejó KO; Weezing). Estas
habilidades NO se activan a mano (no van en legalIntents/useAbility). **Op nueva** `CoinFlipKnockOutTarget(target,
coinFlip)` (Weezing: cara → noquea al Atacante). **Registrados:** Hitmonchan *Contragolpe/Counterattack* 107
(`triggerOnActiveDamaged` + `Damage(OPP_ACTIVE, 30)`), Weezing *A Pasarlo Bomba/Let's Have a Blast* 110
(`triggerOnActiveKO` + `CoinFlipKnockOutTarget(OPP_ACTIVE)`).
**Reflejo DIFERIDO (Mewtwo *Barrera Reflectante/Reflective Barrier* 150 — es un ATAQUE, base 20):** op nueva
`ScheduleReflectDamageNextTurn` (data object) → el intérprete fija `PokemonInPlay.reflectDamageOnTurn = turn+1`
en el Activo propio. En `applyDefenderRetaliation`, si `active.reflectDamageOnTurn == state.turn`, el Atacante
recibe daño CRUDO = `receivedDamage` (el daño de ESTE ataque; se aplica vía `withPlayer`, emite `DamageDealt`).
Campo espejado en netplay `GameStateDto` (decl + toDto + toModel). 4 tests (bloque FASE 32).
**DIFERIDOS de la familia (hooks distintos, NO en Fase 32):** Machamp *Agallas* 68 (moneda para NO ser
Noqueado, PS→10 = hook "evitar-KO" en `handleKnockouts`), Raichu *Toma de Tierra* 26 (al ser Noqueado un
Pokémon TUYO por ataque rival, mueve 1 {L} del KO a Raichu = hook KO de Banca + mover Energía antes del
descarte). Requieren cada uno su propia maquinaria.

### ⚠️ DEUDA PENDIENTE (NO OLVIDAR) — arrastrada desde Fase 29/30/31
1. **Haunter *Spirit Return* (sv3pt5-93)** — habilidad interactiva al evolucionar: "pon 1 Partidario del
   descarte del RIVAL en su mano". DIFERIDA porque requiere maquinaria NUEVA de selección/movimiento sobre
   ZONAS DEL RIVAL (hoy `SearchCards`/`applySearch` operan sobre las zonas del lado que actúa). Es su propio
   mini-subsistema, no encaja en la familia `AttachFromRevealed`. Retomar cuando se aborde ese sistema.
2. **HP máximo en la UI (cosmético)** — el motor YA aplica el HP extra de habilidades condicionales en
   `effectiveMaxHp(state, side, pip)` (KO correcto), pero la BARRA DE VIDA de la UI (feature/game) muestra el
   HP IMPRESO → Wigglytuff ex *Cuerpo Expansivo* (+100 con Energía Especial) no se refleja visualmente. Falta
   exponer el HP efectivo al render. Buscar dónde la UI lee `card.hp` para la barra y usar el efectivo.
3. **Prevención de EFECTOS de ataque (subsistema nuevo)** — Kakuna *Cubierta de Capullo* (sv3pt5-14, "se evitan
   todos los EFECTOS de los ataques rivales a este Pokémon; el daño NO es un efecto"). Requiere gatear la
   APLICACIÓN de ops de ataque por-objetivo (hoy las ops de efecto no consultan un "escudo de efectos" del
   defensor). Distinto de `preventDamageOnTurn` (que solo frena daño). Diferido hasta montar ese gate.
   ✅ **RESUELTO (17 Jul, parte de esta deuda):** Mr. Mime *Barrera Mímica* (sv3pt5-122/-179) — como solo evita
   DAÑO (no efectos), se hizo con el flag `Effect.preventsDamageIfEnergyParity` comprobado en `GameEngine.attack`
   (escudo pasivo: `energyParityShield` si el Defensor tiene la habilidad no bloqueada y ambos Activos tienen
   igual nº de Energías → suma a `damagePrevented`; se salta con `ignoresDefenderEffects`). 1 test. NO tocó netplay.

### ✅ FASE 31 HECHA (17 Jul) — descartar Estadio + prevención condicional a atacante Básico
**Infra:** (a) op nueva `EffectOp.DiscardStadium` (data object) — el intérprete descarta el `state.stadium` a
la pila de su `stadiumOwner` (no-op si no hay), emite `GameEvent.StadiumDiscarded(side)` (evento NUEVO +
ramas ES/EN en `CombatLog`). (b) `PreventDamageNextTurn` gana flag `onlyFromBasic: Boolean` → el intérprete
fija un campo NUEVO `PokemonInPlay.preventBasicDamageOnTurn` (en vez de `preventDamageOnTurn`). En
`GameEngine.attack` el `finalAmount` ahora calcula `damagePrevented = preventDamageOnTurn==turn ||
(preventBasicDamageOnTurn==turn && attacker.card.isBasic)` (ambas se saltan si el ataque `ignoresDefenderEffects`).
Campo espejado en netplay `GameStateDto` (decl + toDto + toModel). **Registrados (bloque "Fase 31"):** Charmander
*Blazing Destruction* 4 y reprint 168 (`DiscardStadium`, sin daño base); Nidoqueen *Queen Press* 31 (90 +
`PreventDamageNextTurn(SELF, onlyFromBasic=true)`). 2 tests (Estadio→descarte del dueño; Básico hace 0 pero
Evolucionado sí daña). OJO: `GameEvent` es sellado → un `when` exhaustivo nuevo obligaría rama; hoy solo CombatLog.

### ✅ FASE 30 HECHA (17 Jul) — pasivos propios condicionales (HP / retirada)
**Infra:** `PassiveModifier` gana `requiresSpecialEnergy: Boolean` (≥1 Energía Especial unida). `effectiveMaxHp`
AHORA toma `(state, side, pip)` y suma EXTRA_HP de HERRAMIENTAS **y de HABILIDADES** condicionales (respeta el
bloqueo vía `abilityPassives`); nuevo helper `passiveEnergyConditionMet(pip, mod)` (evalúa `requiresEnergyType`
+ `requiresSpecialEnergy`). Único call-site actualizado: `handleKnockouts` (pasa `state, koSide`). **Registrados
(bloque "Fase 30"):** Wigglytuff ex *Cuerpo Expansivo/Expanding Body* 40 y reprint 187 (`EXTRA_HP 100, SELF,
requiresSpecialEnergy`); Zapdos ex *Flotación Voltaica/Voltaic Float* 192 y reprint 202 (`RETREAT_COST 0, SELF,
requiresEnergyType=LIGHTNING` — funcionó con solo registrar porque `effectiveRetreatCost` ya leía pasivos de
habilidad). 2 tests (Wigglytuff: 100 daño NO noquea con Especial / SÍ sin ella; Zapdos: retira con 1 {L} pese a
coste impreso 2, sin descartar Energía). OJO: `PassiveModifier` sigue con args NOMBRADOS en todos los call-sites.

### ✅ FASE 29 HECHA (17 Jul) — habilidades INTERACTIVAS al evolucionar
**BUG CRÍTICO corregido:** `GameEngine.evolve` DESCARTABA `res.pending` del efecto de evolución → ninguna
habilidad de evolución interactiva podía funcionar. Ahora, si el trigger deja `pending`, se devuelve
`EngineResult(working, events, pending=res.pending)` ANTES de `handleKnockouts` (NO cierra turno:
`endsTurnOnResolve` queda false, a diferencia de un ataque). **Op generalizado** `RevealAttachEnergy`:
`energyType` ahora NULLABLE (null = cualquier Básica) + flag `includeActive` (destino = Banca + Activo, "a tus
Pokémon"). El intérprete filtra energías con `energyType==null || type==` y usa `allInPlay` si `includeActive`.
Reusa la decisión `AttachFromRevealed` (ya existente, sin cambios en resolución ni netplay). **Registrados
(bloque "Fase 29"):** Gloom *Semi-Blooming Energy* 44 (`RevealAttachEnergy(3,3,null,null,includeActive=true)`),
Vileplume *Fully Blooming Energy* 45 (lookAt/maxAttach=8). 2 tests (pausa con la decisión correcta; resuelve
uniendo energía + rebaraja). Los call-sites previos de `RevealAttachEnergy` (Generador Eléctrico, tests)
compilan sin cambios (pasan `EnergyType` no-null a un `EnergyType?`).

### ✅ FASE 28 HECHA (17 Jul) — HOOK de habilidades "al evolucionar desde la mano" (AUTO, no interactivas)
**Infra nueva:** flag `Effect.triggerOnEvolve: Boolean` (Effects.kt). En `GameEngine.evolve` (tras construir el
`evolved`/`withPlayer`), se busca la habilidad del Pokémon evolucionado cuyo Effect tenga `triggerOnEvolve` y se
ejecuta AUTOMÁTICAMENTE vía `interpreter.execute(EffectSource(side, evoId), …)` + `handleKnockouts`. Estas
habilidades **NO** se activan a mano: excluidas de `legalIntents` (guard `!eff.triggerOnEvolve` junto a
activeOnly/oncePerTurn) y `useAbility` las RECHAZA. **Solo casos NO interactivos por ahora** (sin decisión/pending).
**Registrados (bloque "Fase 28" en Set151Effects.kt):** Gyarados *Indomable/Untamed One* 130
(`DiscardTopDeck(5, own=true)`), Hypno *Toma Hipnosis/Here for Hypnosis* 97 (`ApplyStatus(OPP_ACTIVE,[ASLEEP])`;
el "puedes" se aplica siempre = sin desventaja). 2 tests nuevos (bloque "FASE 28": el evo card se arma con
`mon(...).copy(stage=Stage1, evolvesFrom="Magikarp", abilities=[Ability(..., abiKey(id,EN))])`, Activo con
`turnsInPlay=1`, mano con la evolución, `GameIntent.Evolve(evoId, ontoId)`). **OJO netplay:** no tocado — el
efecto lo aplica el host autoritativo tras el intent Evolve (sin pending, sin DTO nuevo). Nombres EN de habilidad
del TOP-LEVEL `habilidades[].name` (Gyarados="Untamed One", Hypno="Here for Hypnosis").

### ⏭️ SIGUIENTE (FASE 29) — barrido (151/198, faltan 47)
**Diferido del propio mini-sistema evolve-trigger (INTERACTIVOS):** Gloom 44 / Vileplume 45 (mira top 3/8, une
Energías Básicas encontradas a tus Pokémon → decisión al evolucionar), Haunter 93 (Partidario del descarte rival
→ su mano). Requieren emitir una `PendingDecision` DESDE `evolve` (hoy el hook solo corre ops auto; hay que
propagar `res.pending` como interacción, igual que en attack/useAbility). **KO-triggers** (nuevo hook en
`handleKnockouts`): Machamp *Agallas* 68 (moneda para no ser Noqueado, PS→10), Raichu *Toma de Tierra* 26 (mueve 1
{L} del KO a Raichu), Weezing 110 (moneda→KO al atacante), Hitmonchan *Contragolpe* 107 (3 contadores al atacante),
Mewtwo *Barrera Reflectante* 150 (contadores = daño recibido, próximo turno). **Simples sin sistema:** Kabutops
*Modo Ancestral* 141 (Debilidad ×4 al Activo rival), Wigglytuff ex *Cuerpo Expansivo* 40 (+100 PS si Energía
Especial unida), Charmander *Destrucción Abrasadora* 4 (descarta 1 Estadio), condicionales "no hace nada si"
(Primeape 57 si no Confundido, Slowbro 80 si evolucionó este turno). Método de siempre: query python → DSL →
registro (bloque "Fase 29") → tests → suite → regenerar inventario.

### ✅ FASE 27 HECHA (17 Jul) — moneda → busca Pokémon a la Banca (ENCADENADO)
**Patrón nuevo reutilizable "coin-gated interactivo":** una decisión que, al resolver, LANZA la moneda y
ENCADENA otra decisión. Op `CoinFlipSearchToBench(flips, filter)` + decisión `CoinFlipThenSearch(side, prompt,
flips, filter, searchPrompt)`. `pendingFor` emite la moneda (sin candidatos). En `interpreter.resolve()` un
EARLY-RETURN especial la detecta: lanza `flips` monedas con `flip()` (emite `CoinFlipped`), cuenta caras y —si
≥1 cara y hay candidatos en el mazo— MONTA a mano una `PendingInteraction` con una `SearchCards(from=DECK,
dest=BENCH, count=caras)` como nueva `interaction`, preservando `remainingOps`/`endsTurnOnResolve`. GameEngine ya
soportaba el encadenado (`working.awaitingDecision`, línea ~474). **Netplay:** `CoinFlipThenSearch.toDto` → kind
`COIN_FLIP` (render-only; el guest ve el botón de moneda, el host lanza+encadena; la SearchCards resultante viaja
normal). **UI:** botón "¡LANZAR MONEDA!" en `CombatDecisions` (nueva) y `CoinTossOverlay` en `GameScreen` (clásica);
`decisionCount`/`DecisionPanel` con rama 0. **SmartAgent** resuelve con `ResolveDecision(emptyList())`.
**Registrado (bloque "Fase 27"):** Parasect *Filamentos Dispersos/Spread Filaments* 47 (`CoinFlipSearchToBench(2,
CardFilter(POKEMON, GRASS))`). 2 tests (2 caras → SearchCards count=2 y el {G} va a Banca; 0 caras → sin búsqueda).
**Aplica también a `applySearch` DECK→BENCH:** ya crea `PokemonInPlay` por cada PokemonCard y baraja el mazo.

### ⏭️ SIGUIENTE (FASE 28) — barrido (149/198, faltan 49)
Ya NO quedan candidatos "fáciles con op nueva pequeña" obvios: lo que resta son sobre todo **mini-sistemas**
diferidos. (a) **Habilidades "al evolucionar desde la mano"** (Gloom/Vileplume/Haunter/Hypno/Gyarados) = trigger
al momento de evolucionar (nuevo hook en `GameEngine.evolve` que dispare el efecto de la habilidad). (b) **KO-
triggers** (Machamp *Agallas*, Raichu *Conexión a Tierra*, Weezing, Hitmonchan *Contragolpe*, Mewtwo *Barrera
Reflectante*) = efectos al ser Noqueado (hook en `handleKnockouts`). Antes de codear, **regenerar inventario y
auditar** los 49 restantes con python por si quedan reimpresiones/daños-condicionales simples sin registrar.
Método de siempre: query python → DSL → registro (bloque "Fase 28") → tests → suite → regenerar inventario.

### ✅ FASE 26 HECHA (17 Jul) — descarte de la mano que ESCALA el daño
**Infra:** op nueva `DiscardFromHandForDamage(filter, maxCount, perCard)` (Effects.kt). REUSA la decisión
`PendingDecision.SearchCards` con `from=HAND, destination=DISCARD` + campo nuevo `damagePerDiscardToOppActive`.
`pendingFor` la emite (candidatos = `matching(hand, filter)`; si vacío no pausa → 0 descartadas = 0 daño).
`applySearch` GENERALIZADO para soportar `from=HAND` (antes solo DECK/DISCARD; añadido `fromHand`/`fromDeck`):
quita las elegidas de la mano, las manda al descarte y hace `perCard * nº descartadas` de daño CRUDO al Activo
rival (como el resto de daño por efecto: NO pasa por Debilidad/Resistencia; el motor resuelve el KO). **Netplay
INTACTO:** SearchCards ya viaja como render-only (`SEARCH_CARDS` → el guest lo reconstruye como ChooseTargets);
el descarte+daño los aplica el host autoritativo, así que el campo nuevo NO va en el DTO. **UI INTACTA:** el
panel de `SearchCards` (`SelectGrid`, `allowNone=true`) ya deja elegir de 0 a `count` de cualquier zona (la mano
incluida vía `cardOf`). AI (`SmartAgent`) toma `candidates.take(count)` → descarta el máximo (más daño).
**Registrado (bloque "Fase 26"):** Blastoise ex *Cañones Gemelos/Twin Cannons* 9 (base 0 +
`DiscardFromHandForDamage(CardFilter(ENERGY, WATER), 2, 140)`). 2 tests (2 {W}→280; 1 {W}→140).

### ⏭️ SIGUIENTE (FASE 27) — barrido (148/198, faltan 50)
**Parasect *Filamentos Dispersos/Spread Filaments* 47** (2 monedas → busca N Pokémon {G} = nº caras → Banca):
sigue diferido por el escollo "coin-gated interactivo" — el flip debe ocurrir ANTES de emitir la decisión de
búsqueda y `pendingFor` NO recibe `rng` (solo `applyOp`/`resolve` lo tienen). Posible enfoque: una decisión tipo
`CoinFlip` que, al resolver (donde SÍ hay `flip()`), calcule las caras y ENCADENE una `SearchCards` a Banca con
`count=caras` como continuación. **Diferido clásico (mini-sistemas al final):** habilidades "al evolucionar desde
la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados) y KO-triggers (Machamp *Agallas*, Raichu, Weezing, Hitmonchan
*Contragolpe*, Mewtwo *Barrera Reflectante*). Método de siempre: query python → DSL → registro → tests → suite.

### ✅ FASE 25 HECHA (17 Jul) — "si tu mano está vacía" (daño + estado)
**Infra:** op nueva `ApplyStatusIfEmptyHand(target, states)` (Effects.kt) — el intérprete aplica los estados
SOLO si la mano del jugador que ataca está vacía; si no, no-op. Reusa `applyStatus`. NO toca netplay.
**Registrado (bloque "Fase 25"):** Beedrill *Aguijón Nadir/Nadir Needle* 15 — `attackDamage = [DamageTerm(30),
DamageTerm(120, IfEmptyHand)]` (reusa la condición existente) + `ApplyStatusIfEmptyHand(OPP_ACTIVE,
[POISONED, PARALYZED])`. 2 tests nuevos (mano vacía → 150 + estados; con cartas → 30 sin estado).

### ⏭️ SIGUIENTE (FASE 26) — barrido (147/198, faltan 51)
**Diferidos con DECISIÓN (mini-sistemas):** **Blastoise ex *Cañones Gemelos/Twin Cannons* 9** (descarta
HASTA 2 {W} Básicas de la mano, 140 por descartada → decisión de descarte que ESCALA el daño; nuevo op +
PendingDecision). **Parasect *Filamentos Dispersos/Spread Filaments* 47** (2 monedas → busca N Pokémon {G} =
nº caras → Banca; el flip debe ocurrir ANTES de emitir la decisión y `pendingFor` no recibe `rng` → mismo
escollo "coin-gated interactivo" ya anotado). **Diferido clásico:** habilidades "al evolucionar desde la mano"
(Gloom/Vileplume/Haunter/Hypno/Gyarados) y KO-triggers (Machamp *Agallas*, Raichu, Weezing, Hitmonchan
*Contragolpe*, Mewtwo *Barrera Reflectante*) = mini-sistemas al final. Método de siempre.

### ✅ FASE 24 HECHA (16 Jul) — Gust "el rival elige el nuevo Activo"
**Infra:** op nueva `GustDefenderChooseNewActive` (data object, Effects.kt). El intérprete mueve el Activo rival
a SU Banca (`active=null`, `bench+=active`) y añade el lado rival a `state.pendingPromotion` → REUSA la
maquinaria de promoción tras KO: al terminar el ataque, `endTurn` pasa el turno al rival y el guard de
`apply()` (`awaitingPromotion` → solo `PromoteActive`) lo obliga a elegir su nuevo Activo (misma DIFERENCIA
que el KO: la promoción se difiere al inicio de su turno; fiel en la práctica). **GUARDA CLAVE:** si el Activo
ya quedó Noqueado por el daño base del ataque (`active.isKnockedOut`), el gust NO lo toca → lo procesa
`handleKnockouts` (va al descarte, no a la Banca). Netplay YA soportaba `pendingPromotion` (KO en PvP) → sin
cambios de red. **Registrados (bloque "Fase 24"):** Butterfree *Remolino/Whirlwind* 12 (60 + gust), Rhyhorn
*Oprimir/Push Down* 111 (20 + gust). 2 tests nuevos (gust normal + guarda de KO→descarte).

### ⏭️ SIGUIENTE (FASE 25) — barrido (146/198, faltan 52)
Candidatos: **Blastoise ex *Cañones Gemelos* 9** (descarta hasta 2 {W} de la mano, 140 c/u) = op con decisión
de descarte que escala daño. **Beedrill *Aguijón Nadir* 15** (+120 si mano vacía [reusa `IfEmptyHand`] +
Envenenado/Paralizado condicional a mano vacía → op de estado condicional). **Parasect *Filamentos Dispersos*
47** (2 monedas → busca N Pokémon {G} = nº caras → Banca). **Diferido:** habilidades "al evolucionar desde la
mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados), KO-triggers (Machamp *Agallas*, Raichu, Weezing, Hitmonchan
*Contragolpe*, Mewtwo *Barrera Reflectante*) = mini-sistemas al final. Método de siempre.

### ✅ FASE 23 HECHA (16 Jul) — recargo de Coste de Retirada / de ataque al Defensor
**Infra (patrón Fase 16):** 4 campos nuevos en `PokemonInPlay` (GameState.kt): `retreatCostBumpOnTurn/Amount`
y `attackCostBumpOnTurn/Amount`. 2 ops nuevas: `BumpDefenderRetreatCostNextTurn(amount)` y
`BumpDefenderAttackCostNextTurn(amount)` → el intérprete las fija al Activo rival (`turn+1`). En GameEngine:
`effectiveRetreatCost` suma el recargo de retirada (base+bump, salvo Flotación/Travesía que anulan);
NUEVO helper `effectiveAttackCost(atk, attacker, state)` = `convertedCost + bump` usado en `attack()` (chequeo
de energía) y en `legalIntents` (enum de ataques). OJO: `Attack` NO está importado por nombre simple en
GameEngine → usar `com.mineralord.tcg.engine.model.Attack` calificado. Espejado en netplay `GameStateDto`
(4 campos + toDto + toModel). **Registrados (bloque "Fase 23"):** Grimer *Presión Pegajosa/Gummy Press* 88
(10 + retirada +1), Muk *Prisión Viscosa/Sticky Jail* 89 (30 + ataque +1 Y retirada +1). 4 tests nuevos.

### ⏭️ SIGUIENTE (FASE 24) — barrido (144/198, faltan 54)
Candidatos: **Gust "el rival elige"** (Butterfree *Remolino* 12, Rhyhorn *Oprimir* 111) = mover Activo rival a
Banca y que el RIVAL promueva → reusar maquinaria `pendingPromotion`/`awaitingPromotion`+`promoteActive` (mover
active a banca y añadir el lado rival a `pendingPromotion`). **Blastoise ex *Cañones Gemelos* 9** (descarta hasta
2 {W} de la mano, 140 c/u) = op con decisión de descarte que escala daño. **Beedrill *Aguijón Nadir* 15** (+120
si mano vacía [reusa `IfEmptyHand`] + Envenenado/Paralizado condicional a mano vacía). **Diferido:** habilidades
"al evolucionar desde la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados), KO-triggers (Machamp *Agallas*, Raichu,
Weezing, Hitmonchan *Contragolpe*, Mewtwo *Barrera Reflectante*) = mini-sistemas al final. Método de siempre.

### ✅ FASE 22 HECHA (16 Jul) — bonus de daño "tu próximo turno +X"
**Infra (patrón Fase 16):** 2 campos nuevos en `PokemonInPlay` (GameState.kt): `attackBonusOnTurn: Int?` +
`attackBonusAmount: Int`. Op nueva `SelfAttackBonusNextTurn(amount)` (Effects.kt) → el intérprete fija ambos
al Pokémon origen (`turn+2` = próximo turno PROPIO). En `GameEngine.attack`, `selfBonus` se SUMA al daño base
ANTES de `Damage.calculate` (antes de Debilidad/Resistencia) si `attacker.attackBonusOnTurn == state.turn` y
`base>0`. Auto-expira. Espejado en netplay `GameStateDto` (campos + toDto + toModel). **Registrados (bloque
"Fase 22"):** Golem ex *Giro Dinámico/Dynamic Roll* 76 (50 + `SelfAttackBonusNextTurn(120)`), Hitmonchan
*Puño Exaltado/Excited Punch* 107 (60 + `SelfAttackBonusNextTurn(60)`; el bonus es específico de ese ataque
pero es el ÚNICO de daño de Hitmonchan → equivalente en la práctica). 2 tests nuevos (FASE 22).

### ⏭️ SIGUIENTE (FASE 23) — barrido (142/198, faltan 56)
Candidatos: **Gust "el rival elige"** (Butterfree *Remolino* 12, Rhyhorn *Oprimir* 111) = decisión del RIVAL
(sistema nuevo). **Blastoise ex *Cañones Gemelos* 9** (descarta hasta 2 {W} de la mano, 140 c/u) = op con
decisión de descarte que escala daño. **Beedrill *Aguijón Nadir* 15** (+120 si mano vacía [reusa `IfEmptyHand`]
+ Envenenado/Paralizado condicional a mano vacía → op de estado condicional). **Grimer *Presión Pegajosa* 88
/ Muk 89** (Coste de Retirada +{C} al Defensor el próximo turno = campo nuevo). **Diferido:** habilidades "al
evolucionar desde la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados), KO-triggers (Machamp *Agallas*, Raichu,
Weezing, Hitmonchan *Contragolpe*, Mewtwo *Barrera Reflectante*) = mini-sistemas al final. Método de siempre.

### ✅ FASE 21 HECHA (16 Jul) — moneda cara/cruz + búsqueda de tipos distintos
**Infra:** (a) Op nueva `CoinFlipDamageOrRecoil(bonusToOpp, recoilToSelf)` (Effects.kt) — cara: daño crudo
extra al Activo rival; cruz: rebote a SELF. Interpretada en `applyOp` (emite `CoinFlipped`). NO toca netplay.
(b) `EffectOp.SearchDeck.distinctTypes: Boolean` + `PendingDecision.SearchCards.distinctTypes` — el
intérprete `applySearch` recorta `picked` a UN Pokémon por tipo primario (`types.firstOrNull()`) cuando está
activo. `pendingFor` lo propaga. NETPLAY intacto: la búsqueda es render-only host-autoritativo, el recorte
lo hace el intérprete del host (el guest no corre applySearch), así que el flag NO viaja en el DTO. **Registrados
(bloque "Fase 21"):** Mankey *Saña/Thrash* 56 (20 base + `CoinFlipDamageOrRecoil(20,20)`), Eevee *Amigos
Coloridos/Colorful Friends* 133 (`SearchDeck(CardFilter(supertype=POKEMON), HAND, 3, distinctTypes=true)`).
2 tests nuevos (FASE 21). OJO: importar `EnergyType` en EffectInterpreter (faltaba).

### ⏭️ SIGUIENTE (FASE 22) — barrido (140/198, faltan 58)
Candidatos: **Gust "el rival elige"** (Butterfree *Remolino* 12, Rhyhorn *Oprimir* 111) = decisión del RIVAL
(sistema nuevo: mover Activo rival a Banca y que el RIVAL promueva). **Blastoise ex *Cañones Gemelos* 9**
(descarta hasta 2 {W} de la mano, 140 c/u) = op con decisión de descarte que escala daño. **Beedrill *Aguijón
Nadir* 15** (+120 si mano vacía [reusa `IfEmptyHand`] + Envenenado/Paralizado condicional a mano vacía).
**Buffs "próximo turno de este Pokémon +X"** (Golem ex *Giro Dinámico* 76, Hitmonchan *Puño Exaltado* 107) =
campo nuevo tipo `selfAttackBonusOnTurn`. **Diferido:** habilidades "al evolucionar desde la mano"
(Gloom/Vileplume/Haunter/Hypno/Gyarados), KO-triggers (Machamp *Agallas*, Raichu, Weezing, Hitmonchan
*Contragolpe*, Mewtwo *Barrera Reflectante*) = mini-sistemas al final. Método de siempre.

### ✅ FASE 20 HECHA (16 Jul) — ignorar Debilidad/Resistencia + curar-al-atacar
**Infra:** 3 flags nuevos en `Effect` (Effects.kt): `ignoresWeakness`, `ignoresResistance`,
`ignoresDefenderEffects`. `Damage.calculate` acepta `ignoreWeakness`/`ignoreResistance` (saltan esos
modificadores). En `GameEngine.attack` se lee `atkEffect=effects[atk.effect]`, se pasan los flags a
`calculate`, y si `ignoresDefenderEffects` el `finalAmount` NO resta prevención/reducción/Herramientas
(usa `dmg.finalAmount` crudo). NO toca netplay (sin decisión). **Registrados (bloque "Fase 20"):**
Staryu *Swift/Meteoros* 120 (30, los 3 flags true), Golem ex *Rock Blaster/Explosión Roca* 76 (180,
`ignoresResistance`), Kabutops *Draining Blade/Cuchilla Drenadora* 141 (100 + `Heal(SELF,Fixed(30))`,
reuso). 3 tests nuevos (FASE 20). OJO: `mon()` helper solo tiene param `weakness`; para Resistencia usar
`.copy(resistances=listOf(TypeModifier(tipo,"-30")))`.

### ⏭️ SIGUIENTE (FASE 21) — barrido (138/198, faltan 60)
Candidatos: **Gust "el rival elige"** (Butterfree *Remolino* 12, Rhyhorn *Oprimir* 111) = decisión del RIVAL
(sistema nuevo). **Eevee *Amigos Coloridos* 133** (busca 3 Pokémon de tipos DISTINTOS→mano) = SearchDeck con
lógica de tipos distintos. **Blastoise ex *Cañones Gemelos* 9** (descarta hasta 2 {W} de la mano, 140 c/u) =
op nueva con decisión. **Mankey *Saña* 56** (moneda: cara +20 daño / cruz 20 a sí mismo). **Diferido:** habilidades
"al evolucionar desde la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados), KO-triggers (Machamp *Agallas*,
Raichu, Weezing, Hitmonchan *Contragolpe*, Mewtwo *Barrera Reflectante*) = mini-sistemas al final. Método de siempre.

### ✅ FASE 19 HECHA (16 Jul) — daño por energía de tipo + KO por estado
**Infra:** (a) `Amount.PerCount.energyType: EnergyType?` (Effects.kt) — si `of==ENERGY_ATTACHED` y no es null,
`resolveAmount` cuenta solo Energías Básicas de ese tipo unidas al target (resto de contadores lo ignoran).
(b) Op nueva `KoIfStatus(target, status)` (Effects.kt) — si el target tiene el estado, `applyOp` le inflige
daño CRUDO = sus PS impresos (`(card as PokemonCard).hp` vía `damageTargets`), el motor resuelve KO/premios;
si no lo tiene, no-op. NO toca netplay (sin decisión). **Registrados (bloque "Fase 19" en Set151Effects.kt):**
Seaking *Aqua Horn* 119 (60 base + `ExtraDamage(PerCount(ENERGY_ATTACHED, SELF, 30, energyType=WATER))`),
Jynx ex *Heart-Stopping Kiss* 124 y 191 (`KoIfStatus(OPP_ACTIVE, ASLEEP)`). 2 tests nuevos (KO → asertar
`GameEvent.KnockedOut`; energía filtrada → `sumOf{amount}` porque ExtraDamage emite evento aparte del base).

### ⏭️ SIGUIENTE (FASE 20) — barrido de lo que queda (136/198, faltan 62)
Candidatos: **Gust "el rival elige"** (Butterfree *Whirlwind* 12, Rhyhorn *Oprimir* 111) = decisión del rival
(sistema nuevo por ataque). **Eevee *Amigos Coloridos* 133** (busca 3 Pokémon de tipos DISTINTOS→mano) = op
SearchDeck con lógica de tipos distintos. **Blastoise ex *Cañones Gemelos* 9** (descarta hasta 2 {W} de la mano,
140 por descartada) = op nueva con decisión. **Staryu *Meteoros* 120** (ignora Debilidad/Resistencia) = modelar
como `Damage(OPP_ACTIVE, Fixed(base))` crudo con base 0. **Coin-gated interactivo** (Meowth *Ven Aquí Ya* 52,
Pegatinas de Energía 159): OJO el flip debe ocurrir ANTES de emitir la decisión y `pendingFor` no recibe `flip`
→ requiere repensar el orden. **Diferido:** Victreebel 71, Nidoqueen 31, habilidades "al evolucionar desde la
mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados) = mini-sistema, al final. Método de siempre.

### ✅ FASE 18 HECHA (16 Jul) — buscar Partidario/Objeto, Energía a Banca, snipe
**Infra nueva pequeña:** (a) `CardFilter.trainerKind: TrainerCategory?` (enum nuevo `TrainerCategory{SUPPORTER,ITEM,
STADIUM,TOOL}` en Effects.kt) + `matching()` en EffectInterpreter lo casa vía `trainerCategoryOf(kind)` (mapea el
sellado `TrainerKind.*` → categoría). Así SearchDeck/RecoverFromDiscard distinguen Partidario vs Objeto.
(b) `AttachEnergyFromDiscard` con `target=OWN_BENCH`: `pendingFor` ahora también dispara en OWN_BENCH (candidatos =
`ps.bench`, no `allInPlay`) → decisión `AttachFromRevealed` fromDiscard. **Registrados (bloque "Fase 18" en EffectsDb):**
Jigglypuff *Lead* 39 (`SearchDeck(CardFilter(trainerKind=SUPPORTER), HAND, 1)`), Magneton *Junk Magnet* 82
(`RecoverFromDiscard(CardFilter(trainerKind=ITEM), 2)`), Scyther *Helpful Slash* 123 (20 base +
`AttachEnergyFromDiscard(1, GRASS, OWN_BENCH)`), Jolteon *Linear Attack* 135 (snipe 30, `ChooseTarget(OPP_ALL,1,
optional)+Damage(CHOSEN,30)`) y *Fighting Lightning* 135 (90+90 si ex/V, reusa `IfDefenderExOrV`). 5 tests nuevos
(bloque "FASE 18"; helper `supporter(id,name)` local). OJO: nombres EN top-level del JSON (Jigglypuff="Lead" no
"Liderazgo"; Magneton="Junk Magnet"; Scyther="Helpful Slash").

### ⏭️ SIGUIENTE (FASE 19) — barrido de lo que queda (134/198, faltan 64)
Candidatos: **Gust "el rival elige"** (Butterfree *Whirlwind* 12, Rhyhorn *Oprimir* 111 = mueve Activo rival a
Banca, el RIVAL elige nuevo Activo) = decisión del rival (sistema nuevo tipo `pendingPromotion` por ataque).
**Eevee *Amigos Coloridos* 133** (busca 3 Pokémon de tipos DISTINTOS→mano) = SearchDeck con lógica de tipos
distintos. **Seadra *Cuerno Aqua* 117** (+30 por Energía {W} unida) = PerCount filtrado por tipo (hoy
ENERGY_ATTACHED cuenta TODA la energía). **Blastoise ex *Cañones Gemelos* 9** (descarta hasta 2 {W} de la mano,
140 por descartada) = op nueva con decisión. **Diferido:** Victreebel 71, Nidoqueen 31 (prevención condicional al
tipo de atacante), habilidades "al evolucionar desde la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados) = al final.

### ✅ FASE 17 HECHA (16 Jul) — "+X si jugaste tal carta este turno"
2 DamageCondition nuevas (boolean, en `GameEngine.attack`, van con `attackDamage`): `IfSupporterPlayedThisTurn`
(reusa el `state.supporterPlayedThisTurn` ya existente) e `IfPlayedTrainerThisTurn(name)` (casa por `.contains`
ES/EN). Infra: nuevo `GameState.trainerNamesPlayedThisTurn: Set<String>` — se puebla en `GameEngine.playTrainer`
(añade `card.name.es` + `.en`) y se RESETEA en `endTurn` (junto a supporterPlayedThisTurn/energyAttached/abilities).
NO tocado netplay (es estado del host autoritativo; se recalcula en el flujo, no viaja como decisión). **Registrados:**
Wigglytuff ex *Friend Tackle* 40/187 (90 + 90 si Partidario), Rhydon *Charismatic Drill* 112 (40 + 140 si
"Giovanni"), Tangela *Tactful Tangling* 114/178 (10 + 60 si "Erika"). 2 tests nuevos (con `.copy(...)` del flag/set).

### ⏭️ SIGUIENTE (FASE 18) — barrido de lo que queda (130/198, faltan 68)
Candidatos: **Buscar al mazo→mano** (Jigglypuff *Liderazgo* 39 = 1 Partidario, Eevee *Amigos Coloridos* 133 = 3
Pokémon de tipos distintos) → SearchDeck a HAND; OJO `CardFilter` NO distingue Partidario de Objeto — para Jigglypuff
añadir `trainerKind` a CardFilter (o usar nameContains si el mazo lo permite); Eevee "tipos distintos" necesita
lógica especial. **Gust "el rival elige"** (Butterfree *Whirlwind* 12, Rhyhorn *Oprimir* 111 = mueve Activo rival a
Banca, el RIVAL elige nuevo Activo) = decisión del rival (sistema nuevo, como `pendingPromotion` pero por ataque).
**Curación/energía sueltas** y varios daños-condicionales que queden. **Diferido/programado:** Victreebel 71
(contadores al final del turno rival), Nidoqueen 31 (prevención condicional al tipo de atacante), habilidades "al
evolucionar desde la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados) = mini-sistema, al final. Método de siempre.

### ✅ FASE 16 HECHA (16 Jul) — reducción PARCIAL de daño el próximo turno
Op nueva `ReduceDamageNextTurn(amount)` (Effects.kt) + 2 campos nuevos en `PokemonInPlay`:
`damageReductionOnTurn: Int?` y `damageReductionAmount: Int` (GameState.kt). El intérprete fija ambos al
Pokémon origen (`turn+1`, amount). En `GameEngine.attack`, `selfReduction` se RESTA tras Debilidad/Resistencia
junto a `toolDamageReduction` (mismo punto, misma regla). Auto-expira (se compara con `state.turn`). Espejado en
netplay `GameStateDto` (campos + toDto + toModel). **Registrados:** Geodude *Stiffen* 74 (30, sin daño base),
Shellder *Shell Press* 90 (30 base + reduce 30), Cloyster *Protect Charge* 91 (80 base + reduce 80); y Squirtle
*Withdraw* printing 170 (`PreventDamageNextTurn(SELF, coinFlip=true)`, reuso — el 7/11 base ya estaban). 2 tests
nuevos. **Diferido:** Nidoqueen *Queen Press* 31 (evita daño SOLO de atacantes Básicos = prevención condicional
al tipo/fase del atacante, variante nueva).

### ⏭️ SIGUIENTE (FASE 17) — barrido de lo que queda (127/198, faltan 71)
Candidatos: **"+X si jugaste carta Y este turno"** (Wigglytuff ex *Friend Tackle* 40/187 Partidario, Rhydon
*Charismatic Drill* 112 Giovanni, Tangela *Sutil Enredo* 114/178 Erika) → rastrear cartas jugadas este turno
(nuevo `GameState.playedTrainerNamesThisTurn`? resetear al pasar turno) + condición `IfPlayedThisTurn(name)`.
**Buscar al mazo→mano** (Jigglypuff *Liderazgo* 39 = 1 Partidario, Eevee *Amigos Coloridos* 133 = 3 Pokémon de
tipos distintos) → SearchDeck a HAND (OJO: CardFilter NO distingue Partidario de Objeto → quizá añadir
`trainerKind` a CardFilter). **Gust "el rival elige"** (Butterfree *Whirlwind* 12, Rhyhorn *Oprimir* 111 = mueve
Activo rival a Banca, el RIVAL elige el nuevo Activo) = decisión del rival, sistema nuevo. **Diferido/programado**
Victreebel 71 (contadores al final del turno rival), Nidoqueen 31 (prevención condicional). Método de siempre.

### ✅ FASE 15 HECHA (16 Jul) — condicionales +X, reusos de ops y 3 ops nuevas
**3 DamageCondition nuevas** (boolean, evaluadas en `GameEngine.attack`, van con `attackDamage`/DamageTerm):
`IfSameHandSizeAsOpponent` (Ninetales ex *Mirrored Flames* 38/186 = 80+140), `IfMorePrizesThanOpponent`
(Pinsir *Reckless Throw* 127 = 90+90), `IfSelfBenchHasName(name)` (Electabuzz *Electro Combo* 125 +40 si Magmar,
Magmar *Flare Combo* 126 +80 si Electabuzz — casa por nombre ES/EN `.contains`). **3 ops nuevas** en Effects.kt
(inline en EffectInterpreter, sin decisiones → netplay intacto): `BounceOppActiveEnergyToHand(count)` (Omanyte
*Tentacular Return* 138/180, Energía del Activo rival→su mano), `DamagePerOppHandTrainer(per)` (Gengar
*Poltergeist* 94, 50 crudo × Entrenadores en mano rival — cuenta `Supertype.TRAINER`), `DiscardTopDeckDamagePerRetreat(count,
retreatEquals,per)` (Onix *Thumpalanche* 95, descarta 5 propias + 80 × Pokémon con `retreatCost.size==4`).
**Reusos (ops existentes) para reimpresiones/simples:** Charizard ex *Brave Wing* 183/199 (60+100 IfSelfHasDamage);
Alakazam ex *Mind Jack* 188/201 (90 + `ExtraDamage(PerCount(BENCH_COUNT,OPP_BENCH,30))` → OJO: ExtraDamage emite un
DamageDealt APARTE del base, en tests `sumOf{amount}` no `.first()`); Poliwhirl *Frog Hop* 176 (`CoinFlipDamage(1,60)`);
Kangaskhan ex 190 (`CoinFlipDamage(4,100)`); Arbok ex *Menacing Fangs* 185 (`OpponentDiscardsHand(2)`); Pikachu
*Charge* 173 (`SearchEnergyAttachSelf(LIGHTNING,1)`); Charmeleon *Fire Blast* 169 / Charizard *Explosive Vortex* 199
(`DiscardEnergy(SELF,n)`); Mr. Mime *Psypower* 179 (`PlaceCounters(3,OPP_ALL)`); Wartortle *Free Diving* 171
(`RecoverFromDiscard({W} básica,3)`). **OJO reimpresiones:** cada printing es un `id` distinto → hay que registrar
el `atkKey(id,nombreEN)` de CADA una (ej. Psypower 122 Y 179). GREP dups: `grep -oE 'atkKey\("sv3pt5-[0-9]+", "[^"]+"\)'
... | sort | uniq -d` (183 Explosive Vortex ya estaba en Fase 8→NO duplicar; 79 Sea Bathing tiene un dup PREEXISTENTE
inofensivo). 8 tests nuevos (bloque "FASE 15"; helper `trainer(id,name)`).

### ⏭️ SIGUIENTE (FASE 16) — barrido de lo que queda (124/198, faltan 74)
Candidatos: **reducción de daño el próximo turno del rival** ("los ataques hacen X menos a este Pokémon") — Geodude
74 (30), Shellder 90 (30), Cloyster 91 (80) → op/campo tipo `preventDamageOnTurn` pero PARCIAL (reduce, no evita) →
nuevo `damageReductionOnTurn(amount, turn)` en PokemonInPlay + resta en `GameEngine.attack` tras Debilidad/Resistencia
(donde ya se resta `toolDamageReduction`); espejar en GameStateDto. **Gust/switch rival:** Butterfree *Whirlwind* 12 /
Rhyhorn *Oprimir* 111 ("mueve el Activo rival a la Banca, el rival elige") = decisión del RIVAL. **"+X si jugaste
carta Y este turno"** (Wigglytuff 40/187 Partidario, Rhydon 112 Giovanni, Tangela 114/178 Erika) → necesita rastrear
cartas jugadas este turno (`GameState.playedThisTurn`?). **Buscar Pokémon/Partidario al mazo→mano** (Eevee 133,
Jigglypuff 39) = SearchDeck a HAND. **Diferido/programado** Victreebel 71 (contadores al final del turno rival).
Método: query python → DSL → EffectsDb (bloque "Fase 16") → tests → suite → regenerar inventario.

### ✅ FASE 14 HECHA (16 Jul) — moneda-hasta-cruz + mill + daño-antes-del-base
3 ops nuevas en Effects.kt + 3 branches inline en EffectInterpreter (NINGUNA pausa/decisión → netplay NO
tocado): **`CoinUntilTailsDamage(perHeads, confuseIfFirstTails=false)`** (Graveler *Rock Cannon* 75=40,
Exeggcute *Ball Roll* 102=30, Tentacruel *Tentacular Panic* 73=90 con `confuseIfFirstTails=true`); daño crudo
al Activo rival, emite CoinFlipped por tirada. **`CoinUntilTailsDraw`** (Magikarp *Splashy Splash* 129, roba 1
por cara). **`DiscardTopDeck(count, own)`** mill al descarte del dueño (Machop 66/Machoke 67,177/Kingler 99 =1
rival, Machamp 68 =2 rival, Dragonite *Dragon Pulse* 149 =2 propias). **Daño-antes-del-base:** campo nuevo
**`DamageTerm.perDefenderCounter`** (Effects.kt) sumado en `GameEngine.attack` como `amount + perDefenderCounter
* (defender.damage/10)` — se mide ANTES del daño base (no snowballea, a diferencia de ExtraDamage) → Rattata
*Gnaw the Wound* 19 (20+10/cont) y Raticate *Second Bite* 20 (30+30/cont) YA NO diferidos. 7 tests nuevos
(bloque "FASE 14", con `SeqRng(List<Boolean>)` para monedas — OJO `FixedRng(true)` haría BUCLE INFINITO en
moneda-hasta-cruz). **OJO test:** tras atacar el turno pasa y el rival roba → NO asertar `deck.size` tras un
mill (asertar `discard.size`). **DIFERIDO:** Onix *Thumpalanche* 95 (descarta 5 propias + 80 por cada Pokémon
con Coste de Retirada EXACTO 4 descartado así) — necesita inspeccionar las cartas descartadas + daño condicional.

### ⏭️ SIGUIENTE (FASE 15) — barrido de lo que queda (117/198)
Auditar inventario (81 únicos faltan). Candidatos: Onix 95 (arriba); Poltergeist (Gengar, daño ×Entrenadores
en mano rival → op que mira la mano rival); Victreebel *Slow-Acting Acid* 71 (contadores al FINAL del próximo
turno rival → efecto diferido/programado, sistema nuevo); habilidades "al evolucionar desde la mano"
(Gloom/Vileplume/Haunter/Hypno/Gyarados) = mini-sistema, al final. Método: query python → DSL → EffectsDb
(bloque "Fase 15") → tests → suite → regenerar inventario.

### ✅ FASE 13 HECHA (15 Jul) — DAÑO ESCALADO + varios (solo ops EXISTENTES)
Registrados con ops ya creadas (bloque "Fase 13" en EffectsDb): **daño +X por contador PROPIO** (Dodrio
*Ballistic Beak* 85 = `ExtraDamage(PerCount(DAMAGE_COUNTERS, SELF, 30))`, Tauros *Rage* 128 = ×10 SELF);
**+30 por Energía del Activo rival** (Exeggutor *Psychic* 103 = `ExtraDamage(PerCount(ENERGY_ATTACHED,
OPP_ACTIVE, 30))`); **monedas fijas** (Kangaskhan ex *Incessant Punching* 115 = `CoinFlipDamage(4, 100)`);
**unir Energía del descarte a sí mismo** (Arcanine *Torrid Torrent* 59 = `AttachEnergyFromDiscard(2, FIRE,
SELF)`); **gust** (Clefable *Follow Me* 36 = `ChooseTarget(OPP_BENCH,1,optional)+SwapOppActiveWithChosen`).
4 tests nuevos. **GOTCHA importante (bola de nieve):** `ExtraDamage(PerCount(DAMAGE_COUNTERS, OPP_ACTIVE))`
NO es fiel: en `GameEngine.attack` el daño BASE se aplica ANTES de correr el efecto, así que los contadores
del Activo rival se cuentan de MÁS. Por eso quedan **DIFERIDOS** Rattata *Gnaw the Wound* (19) y Raticate
*Second Bite* (20) — necesitan medir los contadores ANTES del daño base (nuevo DamageTerm por-cuenta
evaluado en attack()). El "+X por contador PROPIO" o "+X por Energía rival" NO snowballean (el daño al
rival no cambia esos conteos) → esos sí van con ExtraDamage. Ops útiles ya existentes para el barrido:
`ExtraDamage(Amount.PerCount(Counter.{BENCH_COUNT|ENERGY_ATTACHED|DAMAGE_COUNTERS}, target, mult))`,
`CoinFlipDamage(flips, perHeads)`, `AttachEnergyFromDiscard(count, type, target)`, `SwapOppActiveWithChosen`,
`DiscardEnergy`, `SearchDeck(CardFilter(supertype/isBasic/type/nameContains), zona, count)`, `PlaceCounters`.
**OJO CardFilter** solo tiene supertype/isBasic/type/nameContains (NO distingue Partidario vs Objeto).

### ⏭️ SIGUIENTE (FASE 14) — barrido de lo que queda (106/198)
Auditar el inventario. Candidatos con OPS NUEVAS pequeñas y de alto impacto (aparecen mucho en el set):
(a) **"lanza 1 moneda hasta que salga cruz, X daño por cara"** — Graveler 75, Exeggcute 102, Tentacruel 73,
Magikarp 129 (roba por cara) → op `CoinFlipUntilTailsDamage(perHeads)` / `...Draw`. (b) **"descarta las N
primeras cartas de la baraja del rival/tuya"** (mill) — Machop/Machoke/Kingler (1), Machamp (2), Dragonite
149 (2 propias), Onix 95 (5 propias + daño condicional) → op `DiscardTopDeck(side, n)`. (c) **daño medido
ANTES del daño base** (Rattata/Raticate, arriba). (d) habilidades "al evolucionar desde la mano"
(Gloom/Vileplume/Haunter/Hypno/Gyarados) = mini-sistema aparte, dejar al final. Método de siempre: query
python → extender DSL si hace falta → EffectsDb (bloque "Fase 14") → tests → suite → regenerar inventario.

### ✅ FASE 12 HECHA (15 Jul) — REPARTIR CONTADORES DE DAÑO (op `PlaceCounters`)
Op nueva `EffectOp.PlaceCounters(count, target)` (Effects.kt) + `PendingDecision.PlaceCounters(side,
prompt, candidates, count)` (PendingDecision.kt). `pendingFor` la emite (salta si no hay candidatos, como
`optional`); resolución en `applyPlaceCounters` (EffectInterpreter): `chosen` REPITE el id de cada Pokémon
UNA vez por contador (2 en A, 1 en B → `[A,A,B]`); agrupa por id y aplica `hits*10` de daño CRUDO (sin
Debilidad/Resistencia, evento `DamageDealt`); los KO los resuelve luego GameEngine. Cableado exhaustivo:
`GameEngine.validateChoice` (≤count, candidatos), `SmartAgent.resolveDecision` (round-robin sobre candidatos,
menos-HP primero en Master), netplay `DecisionKindDto.PLACE_COUNTERS` + `toDto`/`toModel` (rehidrata como
PlaceCounters real, no fallback), UI: `CombatDecisions.CounterSpread` (tocar Pokémon = +1 contador, badge
"+10/+20", confirmar exige repartir los N) y `GameScreen.DecisionPanel`/`decisionCount` (picker simple).
**Registrados:** Mr. Mime *Psypower* (sv3pt5-122, `PlaceCounters(3, OPP_ALL)`) y Gengar *Hollow Dive*
(sv3pt5-94, `PlaceCounters(3, OPP_BENCH)`). 3 tests nuevos. **Gengar aclarado (VERIFICADO en la carta
impresa 094/165):** Hollow Dive hace 110 de daño al Activo **Y ADEMÁS** pone 3 contadores en la Banca
rival — la carta hace las DOS cosas; el 110 lo aplica el motor desde `Attack.damage`, el efecto solo añade
los contadores → NO hay doble conteo. (La "contradicción" de la BD no lo era.) Poltergeist (Gengar
ataque 1) = daño por Entrenadores en la mano rival = otra op condicional aparte, aún sin hacer.

### ⏭️ SIGUIENTE (FASE 13) — barrido de lo que queda (99/198)
Regenerar inventario y auditar los ~99 efectos únicos que faltan. Candidatos probables: Poltergeist
(daño ×nº de Entrenadores en la mano rival → op nueva que mira la mano rival), habilidades "al evolucionar
desde la mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados, cada una un mini-sistema), y demás Entrenadores/
habilidades sueltas. Método de siempre: query python de candidatos → extender DSL si hace falta → registrar
en EffectsDb (bloque "Fase 13") → tests en `Set151EffectsTest` → correr suite → regenerar inventario.

### ✅ FASE 11 HECHA (15 Jul) — BUG DE ALTITUD arreglado + snipe/switch-self desbloqueados
**El arreglo del bug de altitud:** nuevo campo `EffectOp.ChooseTarget.optional: Boolean = false`
(Effects.kt). En `EffectInterpreter.pendingFor` (~línea 150), si los candidatos están vacíos **y**
`optional==true` → devuelve `null` (SALTA la op) en vez de dejar una decisión vacía que congelaba el
turno. El ataque hace su daño base igual; la parte dirigida se omite. **OJO:** NO se tocó `attack()` ni
`playTrainer()`/`useAbility()` en GameEngine — esos siguen rechazando `ChooseTargets` vacíos SIN
optional (correcto para Poción/Órdenes de Jefe). Marcados `optional=true` en los snipe existentes
(Multishot Lightning sv3pt5-145, Obsidian sv8-119, Blazing Flight sv3pt5-146, Bone Throw sv3pt5-105).
**Nota clave (gotcha):** los nombres EN de los ataques viven en `ataques[].name` (TOP-LEVEL) del JSON;
el bloque `es.ataques[].name` es SOLO español. Varios nombres supuestos estaban mal (Golbat="Skill Dive"
no "Dive Bomb"; Dewgong="Dual Splash"; Omastar="Aqua Split" [ES=Isoaqua]; Kadabra="Teleportation Attack";
Hitmonlee="Twister Kick"). SIEMPRE sacar el nombre EN del top-level antes de `atkKey`.
**Registrados (bloque "Fase 11" en EffectsDb):** snipe con `optional=true` — Golbat *Skill Dive* (42,
`ChooseTarget(OPP_ALL,1)+Damage(CHOSEN,40)`), Dewgong *Dual Splash* (87, OPP_ALL×2, 50), Omastar
*Aqua Split* (139, 90 base + OPP_BENCH×2 de 30); switch-self con `optional=true` — Kadabra *Teleportation
Attack* (64), Rapidash *Mach Turn* (78), Hitmonlee *Twister Kick* (106, +10 a cada rival vía
`Damage(OPP_ALL,10)` antes del switch). Todos `ChooseTarget(...,optional=true)+SwapActiveWithChosen`
(que hace no-op si no hay elegido). 4 tests nuevos en Set151EffectsTest (Fase 11).

### ⏭️ SIGUIENTE (FASE 12) — repartir contadores de daño (op nueva `PlaceCounters`)
Quedan diferidos **Gengar 94 *Embestida Hueca*** ("pon 3 contadores en los Pokémon en Banca del rival a
repartir") y **Mr. Mime 122 *Psicopoder*** ("3 contadores en los Pokémon del rival a repartir"). Necesitan
op nueva `PlaceCounters(count, target)` + una `PendingDecision` de REPARTO (distribuir N contadores de 10
entre varios Pokémon) con su UI y su espejo en netplay (DecisionKindDto). Es un mini-sistema nuevo (no solo
un registro): elegir cuántos contadores a cada objetivo, no solo "cuáles". Pensar bien la decisión antes de
codear. Poltergeist (Gengar ataque 1) = daño por cartas de Entrenador en la mano rival = otra op condicional
aparte. Método de siempre: query python de candidatos → extender DSL si hace falta → registrar en EffectsDb
(bloque "Fase 12") → tests en `Set151EffectsTest` → correr suite → `python tools/scripts/gen_inventario_151.py`.
**FASE 10 HECHA (15 Jul, ESTADOS/RESTRICCIONES/CURA):** op nueva `DefenderCannotAttackNextTurn`
(clon de DefenderCannotRetreatNextTurn; fija `cannotAttackOnTurn` del Activo rival = turno+1). Registros
(ops existentes salvo esa): Slowbro *Big Yawn* (80, ambos Activos Dormidos = ApplyStatus SELF+OPP_ACTIVE),
Nidoking *Venomous Impact* alt 174 (Envenenado), Venusaur ex *Dangerous Toxwhip* alt 198
(Confundido+Envenenado), Marowak *Boundless Power* (105) / Dragonair *Aqua Slash* (148,181) = NoAttackNextTurn,
Bellsprout *Bind Down* (69,185) = DefenderCannotRetreatNextTurn, Lickitung *Tongue-Tied* (108) =
DefenderCannotAttackNextTurn, Leech Seed alt 166/167 = Heal(SELF,20), Leaf Munch alt 172 = daño +30 si {G}.
3 tests nuevos. **Regla de altitud viva:** ataques con `ChooseTarget(OPP_BENCH/OWN_BENCH)` se RECHAZAN
enteros si no hay candidatos (banca vacía) — por eso se difieren switch-self, snipe a banca y "spread
counters" hasta resolver ese caso (que el ataque siga haciendo su daño base aunque no haya objetivo).
**FASE 9 HECHA (15 Jul, RECUPERAR DEL DESCARTE):** op nueva `EffectOp.RecoverFromDiscard(filter,count)`
(Effects.kt) → pausa con `PendingDecision.SearchCards(from=Zone.DISCARD, destination=HAND)`. El
intérprete: `pendingFor` filtra el DESCARTE (null si no hay candidatos, no pausa); `applySearch`
ahora respeta `d.from` (descarte = NO baraja, quita de discard, sin evento DeckShuffled); `matching`
extendido para casar `BasicEnergy.type` (antes solo tipos de Pokémon). Registrados: Golduck *Aquatic
Rescue* (55, hasta 4 Pokémon), Wartortle *Free Diving* (8, hasta 3 Energía {W} Básica), Snorlax
*Voraciousness*/Glotonería (habilidad 143, oncePerTurn, hasta 2 "Restos"). Netplay NO tocado: la
decisión es render-only y el descarte es zona pública (el host es autoritativo). 4 tests nuevos.
**FASE 8 HECHA (15 Jul, monedas fijas + curación):** bloque "Fase 8" en EffectsDb — Clefairy
*Moon-Viewing Invitation* (35, `SearchDeck(nameContains="Clefairy")→BENCH,3`), Ponyta *Collect* (77,
`DrawCards(1)`), Jigglypuff *Stompy Stomp* (39) / Goldeen *Triple Strike* (118) / Cubone *Hit Twice*
(104) / Kabuto *Double Scratch* (140) = `CoinFlipDamage(flips,perHeads)`, Ivysaur *Leech Seed* (2,
`Heal(SELF,20)`) y Slowpoke *Sea Bathing* (79, `Heal(SELF,30)+RemoveStatus(SELF)`). **OJO gordo
descubierto:** el bloque "Fase 3" (líneas ~258-283 de EffectsDb) YA registraba Call for Family/Beak
Catch/Fetch Family/Gather the Crew/Hop on My Back/Hyper Beam/Acid Spray/Destructive Flame → antes de
registrar un ataque, GREP en EffectsDb.kt para no duplicar. `switch-self` (Kadabra/Rapidash) OMITIDO:
`ChooseTarget(OWN_BENCH)` rechaza el ataque entero con banca vacía (bug de altitud). 5 tests nuevos.
**FASE 7 HECHA (15 Jul, HABILIDADES de retirada + Persian):** registradas en EffectsDb (bloque
"Fase 7: HABILIDADES") — Persian *Rocket Call* (sv3pt5-53, `SearchDeck(nameContains="Giovanni")→HAND`,
oncePerTurn); Flotación Glacial/Voltaica/Ígnea (Articuno 144 {W} / Zapdos ex 145 {L} / Moltres 146 {R})
y Dragonite *Jet Cruise* (149) = pasivo `RETREAT_COST` (coste de retirada 0); Omastar *Primordial
Tentacles* (139) = pasivo `NO_RETREAT`/OPP_ACTIVE. **Infra nueva:** `PassiveModifier.requiresEnergyType`
(pasivo condicional a tener ≥1 Energía del tipo); GameEngine cablea pasivos de HABILIDAD al retiro
(`abilityPassives`, `hasEnergyOfType`, `effectiveRetreatCost`, `retreatBlockedByOpponent`) en `retreat()`
y `legalIntents` (antes solo se aplicaban pasivos de Herramienta). 5 tests en `Set151EffectsTest`.
**Inventario:** regenerar con `python tools/scripts/gen_inventario_151.py` (cruza cartas-db × EffectsDb,
textos del bloque `es`, dedup de artes alternativos → 207 printings / 258 entradas / 198 únicos).
**Dónde vive todo:** ops del DSL en `engine/model/Effects.kt`; intérprete en
`engine/effects/EffectInterpreter.kt` (applyOp); daño condicional (`DamageCondition` sellada) evaluado en
`GameEngine.attack()` (~línea 262); registros en `engine/model/EffectsDb.kt` (secciones "SET 151 — Fase N");
campos temporales de Pokémon en `GameState.kt` (`cannotAttackOnTurn`/`preventDamageOnTurn`/`cannotRetreatOnTurn`)
espejados en `data/netplay/GameStateDto.kt`; tests en `engine/rules/.../Set151EffectsTest.kt` (helpers `mon()`,
`attack()`, `duel()`, `FixedRng`). Comando: `./gradlew :engine:model:test :engine:events:test
:engine:effects:test :engine:rules:test :data:netplay:test :data:cards:test` (JDK 17 en PATH). OJO
`EffectsDbTest` asume qué está/no registrado. OJO gotcha: tras atacar el turno pasa y el rival ROBA 1 al
empezar su turno (no asertar tamaño de mano rival tras un descarte; asertar descarte + evento).
**Ops ya creadas (Fases 1–6):** CoinFlipDamage, CoinFlipStatus, OpponentDiscardsHand, PreventDamageNextTurn,
DefenderCannotRetreatNextTurn, SearchEnergyAttachSelf, GiovanniCharisma; `DiscardEnergy` con
`energyType`/`coinFlip`; `DamageCondition`: IfDefenderEvolved/HasDamage, IfSelfHasDamage, IfEmptyHand,
IfDefenderType(tipo), IfDefenderExOrV.
**(histórico) FASE 7 — HABILIDADES (ya HECHA, ver arriba).** VERIFICADO que `UseAbility` está TOTALMENTE cableado: motor
`GameEngine.useAbility()` (~línea 384; respeta `activeOnly`/`oncePerTurn`, corre el intérprete con flip/shuffle)
y `legalIntents` lo enumera (~línea 744). O sea: una habilidad registrada en `EffectsDb` con `abiKey(id,nombreEN)`
que quepa en las ops FUNCIONA sin tocar el motor. Candidatas del set (query ya hecha):
- **FÁCIL YA (op existente):** Persian *Rocket Call* (sv3pt5-53) = `SearchDeck(CardFilter(nameContains="Giovanni"),
  Zone.HAND, 1)`, oncePerTurn. (Verificar que `nameContains` casa el nombre ES "Carisma de Giovanni").
- **Necesitan op/soporte nuevo:** coste de retirada 0 condicional a energía de un tipo (Articuno *Ice Float*
  {W} sv3pt5-144, Zapdos ex *Voltaic Float* {L} 145, Moltres *Flare Float* {R} 146) y Dragonite *Jet Cruise*
  (149, a todos tus Pokémon) → PassiveModifier RETREAT_COST=0 condicional; el motor aplica pasivos de
  Herramienta (REDUCE_DAMAGE/EXTRA_HP) pero NO pasivos de HABILIDAD todavía → hay que cablear eso en el
  cálculo de retiro. Omastar *Primordial Tentacles* (139, el Activo rival no puede retirarse mientras esté
  activo) = pasivo continuo similar. Snorlax *Voraciousness* (143, hasta 2 Restos del descarte a la mano) =
  op "buscar en descarte a la mano" (no existe; SearchDeck es solo mazo).
- **Complejas (una-carta):** Machamp *Guts* (moneda al ser noqueado), Raichu *Electrical Grounding* (mover
  energía al ser KO), Kakuna *Cocoon Cover* (evita efectos de ataque), habilidades "al evolucionar desde la
  mano" (Gloom/Vileplume/Haunter/Hypno/Gyarados) → cada una un mini-sistema; dejar para el final.
**Recordatorio de proceso:** cada fase = identificar candidatos con python, extender DSL si hace falta,
registrar en EffectsDb, tests en Set151EffectsTest, correr suite, regenerar inventario. Aplica a PvE **y** PvP.

## 🤖 IA PvE — rediseño de dificultad (15 Jul 2026)
Antes TODA dificultad era trivial: `SmartAgent` solo bajaba 1 Básico (si banca vacía), **atacaba
antes de desarrollar** (y atacar cierra el turno) → nunca construía tablero, no se retiraba, unía 1
energía a ciegas al Activo. Rediseño (`engine/rules/SmartAgent.kt`, tests verdes): el ataque va casi
al FINAL del orden de prioridad para las gamas altas, así primero desarrollan. Rasgos por dificultad:
Ultra+ = `developsBench` (LLENA la banca), `evolves`, `usesTrainers` (juega Entrenadores antes de
atacar), `usesTools`, `smartEnergy` (une la energía al Pokémon que saca MÁS daño tras recibirla, no
al Activo a ciegas). Master añade `retreats` (si el Activo no puede atacar pero un banca sí, se retira
y promueve a ese atacante) + Noqueo letal (elige el ataque letal más barato) + arrastre al objetivo de
menos HP. Pokéball/Súperball se quedan fáciles (banca solo si vacía, ataque flojo/cualquiera). El
driver `GameViewModel.advanceAi()` llama `decide()` en bucle (guard<120, delay 450ms/acción) hasta que
el turno pasa → el reorden funciona. La IA SOLO corre en PvE (PvP es humano vs humano, sin IA).
Default `PveConfig.difficulty = ULTRABALL`. PENDIENTE: la IA aún NO usa habilidades activables
(`UseAbility`) por seguridad (riesgo de bucle si no fueran oncePerTurn).

## 🚨 REGLA ABSOLUTA — TODO cambio aplica a PvE **Y** PvP (15 Jul 2026)
Cualquier cambio que el usuario pida se aplica SIEMPRE a los DOS modos: PvE (vs IA,
`GameViewModel`/`SmartAgent`) y PvP (online, `OnlineGameController`). Nunca a uno solo.
Ambos comparten `GameCore` + motor, y la UI de combate (`combat/CombatScreen`) es la misma para
los dos → verificar que el cambio llega a ambos caminos. ÚNICA excepción: si el usuario dice
literalmente "activa el modo prueba/desarrollador/experimentación".

## ⏩⏩ RETOMAR AQUÍ (PRIORIDAD ACTUAL) — REBUILD PANTALLA DE COMBATE — 12 Jul 2026
**Qué es:** rediseño TOTAL de la pantalla de combate, por CAPAS y sin fondo horneado, hecho
EN PARALELO a la clásica. Vive en `feature/game/src/main/kotlin/.../combat/`. Flag
**`USE_NEW_COMBAT_UI` (CombatScreen.kt) = true** → `MainActivity` (Screen.GAME) usa la nueva;
`false` = la clásica `GameScreen`. Contrato intacto: solo lee `GameController.ui` y emite por
sus métodos; NO toca motor/reglas/ViewModels.
**Archivos nuevos:** `combat/CombatScreen.kt` (orquestador + capas + jugar/atacar/retirar +
overlays), `CombatMat.kt` (tapete ORIGINAL dibujado en Compose: zona rival granate, LENTE
central curva con panal + rieles dorados usando arcos medidos 0.2594/0.2894 y 0.5635/0.5365,
zona jugador azul — sin bitmap `board_mat`), `CombatFields.kt` (OpponentField/PlayerField/
BenchStrip/PokemonSlot), `CombatComponents.kt` (CombatCard/CardBack/HpBar/StatusRow/EnergyBadge/
ZonePill/EmptySlot), `CombatDecisions.kt` (DecisionPanel: ChooseTargets/SearchCards/CoinFlip/
AttachFromRevealed pairing/MoveEnergy mínimo).
**Layout 1:1:** zonas colocadas por `BoardGeometry.NBox` (Modifier.place local = offset+size),
mismas cajas que la clásica (activos/banca panal 3+2/premios/mazo/descarte/mano). Mano = REUSA
`board.HandFan`+`HandFilterBar`+`sortedHandCards`/`handCatOf` (abanico, agrupación de copias con
contador, orden por supertipo, scroll, filtros por tipo).
**Interacción (como TCG Live):** TOCAR carta = VER A DETALLE (tu `CardDetailDialog` con holo+tilt
3D+zoom; se le añadió param opcional `bottomBar`). JUGAR = ARRASTRAR (energía→Pokémon, evolución→
pre-evolución, Básico→hueco Banca/Activo en setup, Entrenador→panel central, Objeto dirigido
Poción→Pokémon vía `playItemOn`, Herramienta→Pokémon vía `AttachTool`; reusa
`itemTargetChoose/toolAttachScope/cardTargetsPokemon` de GameControllerShared, `internal` mismo
módulo). ATACAR + RETIRARSE viven en el `bottomBar` del detalle del Activo propio (`ActiveActions`):
botones de ataque coloreados por TIPO del Pokémon (`typeColor`, texto por luminancia) con coste
en `EnergySphere` (esferas oficiales ya empaquetadas) + daño; RETIRARSE arrastra un Pokémon de
Banca sobre el Activo (`Retreat`). Promoción tras KO = tocar banca. En el detalle del Activo la
carta va ARRIBA y las acciones DEBAJO (no la tapan; zoom solo en visor sin acciones).
**Dorsos:** `CombatComponents.CardBack` usa `R.drawable.card_back_default` (reverso ya en el
proyecto) en TODO lo boca abajo. Cambiar el diseño = sustituir ese drawable.
**Info oculta:** el jugador NO inspecciona su mazo ni sus premios; SÍ su descarte y el del rival.
**PENDIENTES (guardados):**
1. **FX de combate** en CombatScreen (tarea #7): daño flotante+shake, embate, KO, moneda, premio,
   curación, consumiendo `vm.fx`/`FxCue`. La lógica ya existe en `GameScreen` (FloatingNumber/
   shake/lunge/ko/coin) → PORTAR/adaptar. No bloquea el juego.
2. **MoveEnergy** en `DecisionPanel` (hoy solo "Continuar" con lista vacía).
3. Pulido fino del tapete/HUD (curvatura/sombras) según feedback visual.
4. Herramientas por TOQUE (hoy solo por arrastre); objeto dirigido por toque va vía panel central+decisión.
**Build/deploy:** `$env:JAVA_HOME=Adoptium jdk-17`; `& C:\DOCUMENTOS\TCG-Live-Clone\gradlew.bat -p
C:\DOCUMENTOS\TCG-Live-Clone :app:assembleDebug` (ruta con ACENTOS, no el junction); instalar con
`C:\Users\pmmt9\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r ...\app-debug.apk`.
**Análisis PTCG Sim (referencia UX):** `analysis/knowledge-base.yaml` (KB completa).

## ⏩⏩ RETOMAR AQUÍ (motor holo/foil) — HOLO EN TODAS LAS CARTAS — 14 Jul 2026
**OBJETIVO del usuario:** todas las cartas (presentes Y futuras) indistinguibles de físicas en
holo, **en todas partes** (colección, mano, tablero, visor) pero **sin giroscopio salvo en el
visor a pantalla completa**. Máscaras reales de malie (uso personal). NO revertir TEMPORALes
(151 desbloqueado se queda). Compila (`:app:compileDebugKotlin` verde). FALTA build APK + prueba
en dispositivo (rendimiento y fidelidad).
**Lo NUEVO (Fases 1–4 hechas):**
1. **Resolver DINÁMICO (adiós manifiestos a mano)** — `tilt/MalieCatalog.kt`: dado `set.code`+número,
   baja UNA vez (cacheada en disco `cacheDir/holo/catalog/`) el `index.json` y el export es-ES del
   set de malie, y parsea por número las **impresiones** (front/foil/etch URLs REALES + `FoilType_
   FoilMask` leídos de `ext.tcgl.longFormID`). El export TRAE las URLs directas (`images.tcgl.png.
   {front,foil,etch}`), no hay que construirlas. `malieKey()` traduce interno→malie (`sv3pt5`→`sv3-5`,
   `svp`→`svbsp` [CONJETURA, verificar], resto `pt`→`-`). → cualquier set que malie tenga funciona
   solo; sets futuros = automáticos.
2. **`HoloAssets.resolve(ctx, appSetCode, number, rarity)`** usa el catálogo, **elige impresión por
   rareza** (`pickPrinting`: común=plana sin brillo; rara=holo/etched, nunca reverse), baja bitmaps
   (caché LRU 150MB). Fallback al manifiesto empaquetado del 151 si la red falla / set fuera de malie.
   El `load(ctx,"151",...)` viejo se conserva como fallback.
3. **`Modifier.holoAmbient(...)`** (HoloShader.kt): MISMO shader AGSL, brillo movido por barrido de
   TIEMPO elíptico (sin giroscopio). Para rejilla/mano/tablero. El visor sigue con `holoOverlay`+tilt.
4. **`HoloCardImage`** (core/designsystem, composable compartido): resuelve foil por set+número y
   pinta front real+`holoAmbient`; mientras baja (o si no hay foil) pinta arte plano por `imageUrl`.
   Params `enabled` (para limitar cuántas animan) e `intensity`.
5. **Cableado:** `CollectionScreen.BinderSlot` (rejilla, `setCode="sv3pt5"`, intensity .85). Combate:
   `CombatCard` ahora acepta `card: Card?` opcional → si viene y no es dorso usa `HoloCardImage`
   (número = `card.id.printed.raw.substringAfterLast('-')`); cableados activos/banca (CombatFields),
   slot+mano+detalle (CombatScreen). `CardDetailDialog` gana param `setCode` (default sv3pt5).
**PENDIENTE:**
1. **Build APK + probar en dispositivo**: rendimiento con MUCHAS cartas holo a la vez (LazyGrid/mano)
   — puede fundir GPU; Fase 5 = limitar shaders activos (solo visibles/tocada), downscale máscaras.
2. Verificar `svp`→`svbsp` y otros sets no-151 (probar carta de sv1/sv2).
3. Afinar patrón geométrico fino (SunPillar/Cosmos) — sigue aproximado.
4. Licencia: assets TCGL = uso personal; si se publica, generar máscaras propias.

## (histórico) MOTOR HOLO/FOIL — 11 Jul 2026
**Qué se construyó:** motor de acabados holográficos con giroscopio, aplicado SOLO en el
visor de carta a pantalla completa (`core/designsystem/CardDetailDialog.kt`), decisión
"solo carta activa". Compila; APK instalado en `3bf89e4f` (API 34, AGSL OK).
**Archivos nuevos** en `core/designsystem/src/main/kotlin/.../tilt/`:
- `Tilt.kt`, `TiltSensor.kt` (`rememberTilt()` giroscopio+spring, calibración base; seguro minSdk 26),
  `TiltModifier.kt` (`Modifier.tiltParallax`).
- `HoloShader.kt` (`Modifier.holoOverlay`, **AGSL/RuntimeShader API 33+**): muestrea el arte
  (`uContent`) + máscara real (`uMask`) + capa etch (`uEtch`); color/patrón por `uProfile`
  (finish) y `uFoilType` (0 flat_silver,1 sun_pillar,2 sv_holo,3 sv_ultra). Mezcla **screen**
  (no blanquea texto). `uTime` (shimmer con `withFrameNanos`).
- `Finish.kt` (`Finish` enum + `resolveFinish(rarity)` y `resolveFinish(rarity,foilType,foilMask)`
  — foil real manda; corrige comunes/infrecuentes reverse).
- `HoloAssets.kt` (**descarga bajo demanda + caché LRU 150MB**): lee `assets/holo/151/manifest.json`
  (41 KB, solo URLs → APK NO crece, sigue ~23,6 MB), baja front+mask(+etch) de `cdn.malie.io` y
  cachea en `cacheDir/holo/`. `HoloBitmaps(front,mask,etch,finish,foilCode)`.
**Datos:** `app/src/main/assets/holo/151/manifest.json` (207 cartas foil; campos f/m/e/t/k).
Regenerar con python sobre el JSON de malie: índice `cdn.malie.io/file/malie-io/tcgl/export/index.json`
→ set `v0.1.9.12/sv3-5.es-ES.json`. **curl funciona con `-H "User-Agent: Mozilla/5.0"`** (urllib da 403).
**Cableado:** `CardDetailDialog(rarity,cardNumber)` resuelve finish+assets async (`produceState`).
`CollectionScreen` pasa `rarity`+`number`; **filtro de rareza PROVISIONAL** (`RarityFilterBar`).
`CollectionViewModel`: **`owned=true` TEMPORAL** (set 151 desbloqueado) — REVERTIR a `count>0`.
**Estado exactitud 151:** ✅ máscara real (dónde brilla) + reverse comunes + capa etch + color por
foil.type. ⚠️ patrón geométrico fino (SunPillar/Cosmos exactos) sigue aproximado.
**PENDIENTE / a decidir:**
1. Confirmar legibilidad/nivel de brillo (último ajuste bajó intensidad + screen blend; Blastoise ex
   184 quedaba ilegible por los "rombos"=cross-hatch metálico, ya suavizado).
2. Revertir TEMPORALes: `owned=true` en `CollectionViewModel`; decidir si el filtro se queda.
3. **Licencia**: las capas foil son assets de **TCG Live** (uso personal/experimental). Si el proyecto
   se hace público → NO usarlos (volver a máscara procedimental o generar propias).
4. Opcional: afinar geometría de patrones por foil.type.
**Build/deploy:** `$env:JAVA_HOME=Adoptium jdk-17`; `.\gradlew.bat :app:assembleDebug`; adb en
`C:\Users\pmmt9\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Screencap: `screencap -p /sdcard/x.png`
+ `pull` (NO `exec-out > file` en PowerShell). Análisis previo (repos pokemon-cards-css/pokebox/PTCG Sim)
en `C:\Users\pmmt9\.claude\plans\act-a-como-un-arquitecto-jolly-wirth.md`.

## ⏩ RETOMAR AQUÍ (al decir "continuemos"/"continua") — actualizado 9 Jul 2026, 08:50
**Terminología (confirmada por el usuario):** PvE = vs IA (`GameViewModel` + `SmartAgent`);
PvP = humano vs humano (`OnlineGameController`, host-autoritativo Firestore). Ambos comparten
`GameCore` + el motor → las reglas son idénticas en los dos modos (regla del usuario: NO duplicar
lógica; cualquier fix aplica a ambos).

**Al decir "continuemos", hacer EN ESTE ORDEN:**
1. ~~**IA (PvE) sabe anclar Herramientas**~~ **HECHO (9 Jul)**: `GameEngine.legalIntents` genera
   `AttachTool` (tool en mano × Pokémon propio sin Herramienta; rival solo si `ToolTarget.ANY`).
   `SmartAgent` la ancla ANTES de atacar (paso 3b, preferente al Activo). Tests en
   `TrainerAbilityTest` (legalIntents ofrece/excluye + SmartAgent la elige). Suite JVM verde.
2. **← SIGUIENTE. PvP: log del host → guest** (`NetMessage.LogLine` de los eventos; hoy el guest no ve
   el registro) y **recompensas por modo** vía el hook ya listo `GameCore.onGameFinished(winner)`
   (PvE distinto de PvP).
**DIFERIDO a otro día/semana (NO la próxima sesión):** *probar las Herramientas en partida real*
= registrar en `EffectsDb` los pasivos de Herramientas concretas de las barajas (Pikachu/Armarouge/
Darkrai) para ver HP extra / reducción de daño jugando. El usuario lo hará más adelante.

**HECHO esta sesión (8-9 Jul) — compila, tests JVM verdes, APK en `app\build\outputs\apk\debug\`:**
- **Unificación PvE/PvP por COMPOSICIÓN** (no herencia: `GameViewModel` ya es `AndroidViewModel`):
  nuevo `GameCore` (Kotlin puro) es dueño del estado de UI, la preparación, el pipeline de intents
  (`applyLocal`/`commit`), FX y el hook `onGameFinished`. `GameViewModel`/`OnlineGameController`
  delegan; solo difieren en IA vs red. Helpers compartidos en `GameControllerShared.kt`
  (`toFxCue`/`playFx`, `lookupCard`, `cardNameOf`). Arregladas 2 divergencias: PvP host ahora emite
  TODOS los FX de combate (antes solo moneda) y usa `combatLog.render` (antes `toString()`).
- **Objetos DIRIGIDOS por arrastre (Poción)**: se sueltan sobre un Pokémon (no en panel gris) y se
  aplica el efecto en un gesto (`GameCore.applyItemOnTarget` = PlayTrainer + auto-resolver el
  `ChooseTarget`). `GameController.playItemOn`. Detección: `itemTargetChoose(card)` (efecto empieza
  con `ChooseTarget` a `OWN_*`). Nuevo `ChooseTarget.onlyDamaged` (Poción/Tranquil): solo Pokémon
  con daño; **el motor RECHAZA jugar el Entrenador/habilidad si la elección queda sin candidatos**
  (no gasta la carta). UI resalta solo objetivos válidos; excluidos del panel gris.
- **HERRAMIENTAS (Pokémon Tool) — reglas del RULEBOOK.pdf** (`C:\DOCUMENTOS\POKÉMON TCG\RULEBOOK.pdf`,
  texto extraíble con `pypdf`; p.11 sin límite/turno, p.28 máx.1/Pokémon, p.36 las `ANY` al rival):
  intent `AttachTool(tool,target)` + `GameEngine.attachTool` (rechaza si el Pokémon ya tiene una;
  propio, o rival solo si `ToolTarget.ANY`). **El motor ya aplica pasivos de Herramienta**
  `EXTRA_HP` (sube HP efectivo para el KO en `handleKnockouts`) y `REDUCE_DAMAGE` (baja daño tras
  Debilidad/Resistencia en `attack`) — helpers `toolMods/effectiveMaxHp/toolDamageReduction`. DTO
  `AttachTool` en `NetMessage`; `attachedTools` ya se serializaba; evento `ToolAttached` ya existía.
  UI: `canDrag` incluye Tool; se arrastran sobre Pokémon sin Herramienta; excluidas del panel gris
  (`cardTargetsPokemon` = Objeto dirigido O Herramienta). Tests nuevos en `TrainerAbilityTest`.
- **Arrastre en PREPARACIÓN**: los Básicos de la mano inicial se arrastran al Activo/Banca (antes
  botones). `GameScreen`: `activeSlotBounds`, slots de Banca vacíos también en setup, `HandFan`
  arrastrable en setup.
- OJO deuda: pasivos de HABILIDADES/ENERGÍAS aún NO se aplican (solo Herramientas). IA no ancla
  Herramientas todavía (item 1 de arriba).

## ⏩ (histórico) RETOMAR AQUÍ — 4 Jul 2026, 13:00
**EFECTOS DE LAS 3 BARAJAS: COMPLETO (Pikachu ✅ Armarouge ✅ Darkrai ✅).** APK compilado e
instalado en `3bf89e4f` el 7 Jul ~18:40. Falta VERIFICAR en dispositivo en juego real:
Jet Wing (no atacar sig. turno), Concentrated Fire (monedas), Flame Cloak/Passionate Singing
(bandeja de descarte), Mela (condicional KO), Cross-Cut (+dmg si Evolución), Órdenes de Jefe (gust).
- **Modo VERIFICACIÓN activo**: `GameViewModel.DEBUG_FULL_BENCH = true` → al pulsar JUGAR el
  tablero arranca directo con **5 en Banca ambos lados** (salta moneda/reparto/preparación) y
  siembra la mano con energías + evoluciones (de los básicos en juego) + 1 Entrenador, para probar
  arrastres/efectos. `GameSetup.debugFullBench()`. **Poner en false para juego normal.**
- **Interacciones nuevas (todas por ARRASTRE desde la mano, fuera de preparación)**:
  - Energía → cualquier Pokémon propio = adjuntar. Evolución (Fase1/2) → su pre-evolución = evolucionar.
    Entrenador (Partidario/Objeto) → **panel gris central** (`BoardGeometry.CenterPanel`) = jugar efecto
    + descarte. Cableado en `HandFan` (callbacks `onCardDrag*` + `canDrag`) y `GameScreen`
    (`dragCard`/`dragPos`/`targetBounds`/`centerBounds`, `isValidDrop`, `dropGlow`).
  - **Glow** = `Modifier.dropGlow(on)` (aura cian pulsante DIBUJADA ENCIMA con `drawWithContent`;
    si se dibuja como border normal la imagen de la carta lo tapa). Solo resalta objetivos VÁLIDOS.
  - Tocar (sin arrastrar) cualquier carta = **CardHdViewer** (carta HD a pantalla completa, sin texto).
- **REGLAS OFICIALES + UI detalle (7 Jul, 13:30) — `DEBUG_FULL_BENCH=false` YA (juego normal)**:
  - *Detalle con selección* (sustituye la lupa): `CardDetailOverlay` (GameScreen) = carta grande a
    pantalla completa + panel de acciones. En decisiones (Super Ball/Cinio/Switch) tocar una
    candidata la abre a detalle y se SELECCIONA ahí (`DetailButton`); selección elevada a
    `decisionSelected`/`decisionDetail` en GameScreen; multi-selección conserva "Confirmar".
  - *Ataques desde el detalle*: tocar el Activo propio (mi turno, sin decisión) abre el mismo
    `CardDetailOverlay` con energías en esferas (`EnergyOrbs`) + `AttackRow`s seleccionables.
  - *(7 Jul 13:40) Detalle a PANTALLA COMPLETA*: `CardDetailOverlay` ahora carta grande
    (`fillMaxWidth(0.86f)`) PEGADA al borde superior (Column top-packed con scroll) y el panel de
    acciones debajo. Antes salía chica y centrada.
  - *Ataques en inglés arreglado*: 41 ataques del dataset (SV1 sobre todo: Linear Attack, Collect,
    Sharp Fang…) no traían nombre ES → salían en inglés. Mapa `AttackNames` (ES por nombre EN) en
    `CardMapper`; se usa cuando `es.ataques[i].name` falta/está en blanco. `data\cards\CardMapper.kt`.
  - *Regla: no evolucionar 2 veces/turno + no evolucionar turno 1*: `GameEngine.evolve` pone
    `turnsInPlay=0` al evolucionar y rechaza si `state.turn==1`; `legalIntents` e `isValidDrop(...,turn)`
    lo reflejan.
  - *Inicio oficial*: `attack()` rechaza en `turn==1` (quien empieza no ataca su 1er turno; no roba
    porque el juego arranca en MAIN sin robo). **Ganador del volado elige orden**:
    `CoinPhase.CHOOSE_ORDER` + `vm.chooseFirst()`; si gana la IA, empieza ella (`firstSide`). Si
    empieza el rival, `confirmSetup` lanza `advanceAi()` (bucle de IA extraído de `applyAndAdvance`).
    **Mulligan**: `GameSetup.dealCounting` cuenta rebarajas; compensación oficial vía
    `GameSetup.drawExtra` (rival roba +1 por cada mulligan del otro). PENDIENTE: overlay visual de
    mulligan (revelar mano del rival) — solo está la mecánica + mensaje en el log.
- **BUGS ARREGLADOS (7 Jul, tarde) — 4 de juego**:
  - *Búsqueda/banca mostraban NOMBRES, no cartas* (Super Ball sv2-183, Cinio sv1-175, Switch
    sv1-194). `DecisionPanel` reescrito: dibuja IMÁGENES de carta (LazyRow), selección con marco
    dorado+✓, lupa por carta → `onInspect` abre `CardHdViewer`. Nuevo `vm.card(id)` resuelve la
    carta por id de INSTANCIA en cualquier zona del estado (cae a `repo[id.printed]`).
  - *No se podía poner Básico en Banca*: `canDrag` ahora incluye `PokemonCard` (Básicos);
    `GameScreen` dibuja los slots de Banca VACÍOS (`me_bench_empty_i`) que capturan bounds
    (`benchSlotBounds`) y se iluminan (`dropGlow`) al arrastrar un Básico; al soltar sobre uno →
    `GameIntent.PlayBasicToBench`. Helper `benchDropIndex()`. OJO: con `DEBUG_FULL_BENCH=true` la
    banca arranca LLENA (5) → poner en false para probar la colocación.
  - *No se podían usar ataques*: tocar el Activo propio EN MI TURNO (sin decisión) abre
    `ActiveActionSheet` (nuevo): arte + energías adjuntas en ESFERAS (`EnergyOrb`/`EnergyOrbs`) +
    lista de ataques (`AttackRow`: coste en esferas + daño), tocar ataque pagable → `GameIntent.Attack`.
    Fuera de mi turno/rival = `CardHdViewer` como antes.
  - *Dominguera (svp-114 = Picnicker) no giraba la moneda*: `EffectOp.CoinFlipDraw` en
    `EffectInterpreter.applyOp` ahora EMITE `GameEvent.CoinFlipped(side,heads)` además de robar →
    la animación de moneda (`FxCue.Coin`) se dispara y el efecto continúa. (Antes robaba en
    silencio.) Aplica a cualquier carta con CoinFlipDraw.
- **BUGS ARREGLADOS (7 Jul)**:
  - *No se podía adjuntar a un Pokémon evolucionado*: `targetBounds` (mapa CardId→Rect) solo se escribía;
    tras evolucionar quedaba la entrada OBSOLETA de la pre-evolución (mismo rect) y `firstOrNull` la
    devolvía → objetivo inválido. Fix: `dropTargetUnder` itera los Pokémon EN JUEGO actuales (ids vigentes)
    y consulta sus bounds; ignora entradas obsoletas por construcción. (Bug 1 "a veces no evoluciona" = en
    DEBUG la mano es aleatoria; la evolución no siempre se reparte — no es bug de lógica.)
  - *Mano: cartas deformadas y no se podía deslizar*. Deformación: la elevación del abanico usaba
    `padding(top=lift)` (suma alto de layout) y la caja de mano (alto fijo) comprimía la carta. Fix:
    elevar con capa de dibujo (`graphicsLayer`, pivote base) — no suma alto. Deslizar: `HandFan` usaba
    `detectDragGestures` (consume cualquier gesto). Fix: `detectVerticalDragGestures` → solo el arrastre
    VERTICAL levanta carta; el horizontal lo recibe el `horizontalScroll` de la fila. Abanico afinado a
    ref_0040 (sutil, colina central, sin recorte).
- **BUG GORDO ARREGLADO — identidad de instancia**: copias de una misma carta impresa compartían
  `CardId` → adjuntar/evolucionar/descartar una afectaba a TODAS. Fix: `CardId.withInstance(n)`/`.printed`,
  `Card.withId()`, y `GameViewModel.uniquify()` da id único a cada copia al construir el mazo
  (`buildDecks`). `cardName`/`combatLog` usan `id.printed`. **Toda comparación en juego es por instancia.**
- **OJO Pikachu ex Thunderbolt (svp-106)**: es DAÑO PURO (120), texto vacío en la carta real.
  NO descarta energía (el coste es requisito, no se gasta al atacar). Antes se registró por
  error `DiscardEnergy(SELF,MAX)` en `EffectsDb.kt` y "gastaba" 3 energías → RETIRADO (8 Jul).
- **BUGS UI ARREGLADOS (8 Jul, tras partida completa con la novia)**:
  - *Ver la mano en turno del rival*: `HandFan` gateaba el TOQUE con `enabled`. Ahora el
    `.clickable` es SIEMPRE activo (toque = inspeccionar HD); el ARRASTRE (jugar) sigue
    restringido por `enabled`/`canDrag`. Así se pueden leer las cartas de la mano en cualquier momento.
  - *Buscadores no revelaban lo agarrado (Cinio/Super Ball…)*: nuevo `SearchRevealOverlay` en
    `GameScreen`. Tras resolver una `SearchCards` con `destination==HAND`, se muestran los artes
    de las cartas elegidas ("AÑADISTE A TU MANO"). `DecisionPanel` ahora resuelve por callback
    `onConfirm` (antes llamaba `vm.onResolve` directo) para poder revelar; el camino de 1 sola
    carta (detalle) también revela. Helper `searchRevealFor(decision, ids, vm)`.
- **3 FEATURES HECHAS (8 Jul, tarde) — verificadas en compilación + APK instalado**:
  - *(1) Volado PVP: AMBOS eligen cara/cruz*. `OnlineGameController`: host y guest entran en
    `CHOOSING`; cada uno llama (`chooseCoin` vale para los dos). El host junta `hostCall`+`guestCall`
    y en `maybeResolveCoin()` lanza UNA moneda 50/50: si las llamadas DIFIEREN gana quien acertó;
    si son IGUALES, desempate 50/50. El GANADOR elige orden (`CHOOSE_ORDER`), el otro espera.
    `CoinFlipOverlay`: quitado el toast fijo "A la espera de Rival"; `SPINNING` ahora muestra
    `state.message` ("Esperando al rival…"). El modo vs IA (`GameViewModel`) NO se tocó.
  - *(2) BARRA DE TURNO amarilla curva (arco) bajo el Activo de quien juega* — `TurnArc` en
    `GameScreen` (Canvas). GEOMETRÍA MEDIDA EN REFERENCIAS (no inventada), fracciones del tablero:
    **Jugador** arco ∩ (domo): bordes y≈0.5635, pico central y≈0.5365 (ref_0040).
    **Rival** arco ∪ (valle): bordes y≈0.2594, valle central y≈0.2894 (ref_0052).
    Quad-bézier de borde a borde; tubo dorado `Color(0xFFF6C43C)` con halo + línea de brillo y
    pulso suave. Solo se enciende el lado de `state.activeSide`; oculto en setup/revelado/fin.
    Se dibuja tras el CenterPanel (queda BAJO cartas/HUD). Color medido ~RGB(248,196,66).
  - *(3) RETIRADA*: (a) botón "RETIRARSE" en el detalle del Activo (`actionSheet`) → elige el
    Pokémon de Banca que promueve (fila de artes tocables); (b) ARRASTRAR el Activo propio sobre
    un Pokémon de Banca lo retira promoviendo a ESE (`ActiveInBox` ahora usa `detectDragGestures`
    omnidireccional + reporta pos de raíz; `retreatTargetUnder`/`retreatHover` resaltan el destino).
    Gate: mi turno, sin decisión, `attachedEnergyCount >= retreatCost`. Motor:
    `GameEngine.retreat` ya descartaba el coste y curaba estados. (Antes el arrastre vertical
    promovía SIEMPRE al primer Pokémon de Banca; ahora eliges cuál.)
- **VOLADO INTERACTIVO EN EFECTOS (La Dominguera y similares) — HECHO (8 Jul, tarde)**:
  `EffectOp.CoinFlipDraw` ya NO es determinista: PAUSA con `PendingDecision.CoinFlip(side,prompt,
  ifHeads,ifTails)` (nuevo tipo en `PendingDecision.kt`). Fiel a TCG Live: TIRAS la moneda y
  DESPUÉS ocurre el efecto. `EffectInterpreter.pendingFor` la emite; `resolve()` lanza `flip()`
  (autoritativo), emite `GameEvent.CoinFlipped` y roba `ifHeads`/`ifTails`. Exhaustividad añadida en
  `GameEngine.validateChoice`, `SmartAgent.resolveDecision` (IA resuelve con lista vacía),
  `DecisionPanel`/`decisionCount` (GameScreen). UI: `CoinTossOverlay` (velo + moneda dorada +
  botón "¡LANZAR!") en `GameScreen`; al pulsar → `vm.onResolve(emptyList())` y el giro/cara lo
  anima el FX de moneda (CoinFlipFx). Netplay: `DecisionKindDto.COIN_FLIP` + campos `ifHeads/ifTails`
  en `PendingDecisionDto`; `toModel` rehidrata COIN_FLIP como `CoinFlip` (el resto siguen ChooseTargets
  render-only). **FX ONLINE**: `OnlineGameController.playCoinFx` reemite `CoinFlipped` local y (host)
  lo propaga por cable con `NetMessage.Fx("COIN", amount=heads?1:0)`; `onFx` lo reemite en el
  receptor. (Online NO tenía FX de juego; por ahora solo se propaga la MONEDA, no daño/KO.)
  El branch `applyOp(CoinFlipDraw)` quedó muerto (pendingFor lo intercepta) pero se conserva por
  exhaustividad. Tests JVM verdes.
- **Efectos Pikachu HECHOS**: Rotom *Linear Attack*
  (20 a 1 rival), Wattrel *Collect* (roba 1), **Switch** (sv1-194, intercambia Activo↔Banca), **Youngster**
  (sv1-198, baraja mano→mazo + roba 5), **Picnicker** (svp-114, moneda→roba 4/2), **Cambio/Switch en
  sus DOS printings** (sv1-194 y sv3pt5-206 = misma carta ES/EN). Ops nuevas en
  `Effects.kt`: `ShuffleHandIntoDeck`, `SwapActiveWithChosen`, `CoinFlipDraw`. El intérprete
  (`EffectInterpreter.execute/resolve`) ahora recibe `shuffle`/`flip` (RNG); `GameEngine` los pasa
  con `rng.shuffle`/`rng.flipCoin`. Registro en `EffectsDb.kt`.
- **Electric Generator (sv1-170) HECHO (7 Jul)**: op nueva `EffectOp.RevealAttachEnergy(lookAt,maxAttach,
  energyType,benchType)` + decisión nueva `PendingDecision.AttachFromRevealed(revealed,energyCandidates,
  benchCandidates,maxAttach)`. `EffectInterpreter.pendingFor` revela `deck.take(5)`, filtra Energía Rayo
  Básica y Banca tipo Rayo; si no hay nada que unir NO pausa (baraja en `applyOp`). Resolución
  `applyAttachFromRevealed`: `chosen` son PARES intercalados `[energía,destino,…]`; une, saca del mazo,
  baraja el resto. Registrado en `EffectsDb` (sv1-170). IA en `SmartAgent` auto-empareja round-robin.
  **UI = ARRASTRE** (a petición del usuario): `RevealAttachTray` en `GameScreen` muestra el top 5; las
  Energía Rayo se arrastran a un Pokémon Rayo de Banca reutilizando `dragCard/dragPos/targetBounds/glow`
  (validez restringida a `benchCandidates` vía `revealTargetUnder`); estado local `attachPairs`; "Listo"
  llama `vm.onResolve(pares aplanados)`. Tests: 2 en `EffectInterpreterTest` + `EffectsDbTest` actualizado.
- **PIKACHU 100% AUTOMATIZADA (7 Jul, tarde)**: auditadas TODAS las cartas de la baraja vs
  `cartas-db.json`. Cartas con efecto → hechas; el resto son daño puro (no requieren efecto).
  - **Kilowattrel Jet Wing (sv2-82) HECHO**: op nueva `EffectOp.NoAttackNextTurn` (data object) +
    campo `PokemonInPlay.cannotAttackOnTurn: Int?`. El intérprete lo fija a `state.turn + 2` (el turno
    intermedio es del rival). `GameEngine.attack()` rechaza si `attacker.cannotAttackOnTurn == state.turn`;
    `legalIntents` no ofrece ataques en ese turno. Auto-expira por comparación exacta (no hace falta
    limpiarlo). Reflejado también en `GameStateDto` (netplay). Tests: 1 en `EffectInterpreterTest` +
    1 en `EffectsDbTest`.
  - **Daño puro SIN efecto (correcto que no estén registrados)**: Voltorb sv2-66, Electrode sv2-67,
    Mareep svp-107, Flaaffy svp-108, Ampharos svp-109, Miraidon svp-148, Wattrel Glide sv1-77,
    Kilowattrel Peck sv2-82. Pikachu ex Thunderbolt / Rotom Linear Attack / Wattrel Collect ya estaban.
  - **De paso**: arreglado error de compilación PREEXISTENTE en `:data:netplay` (`PendingDecision.toDto`
    no tenía rama `AttachFromRevealed`, de la sesión del Generador Eléctrico). Nuevo `DecisionKindDto.
    ATTACH_FROM_REVEALED`. `:data:netplay:compileKotlin` ya pasa.
- **ARMAROUGE 100% AUTOMATIZADA (7 Jul, tarde)**: auditadas TODAS las cartas vs `cartas-db.json`.
  - **Efectos con ops YA existentes**: Armor Cannon (svp-105) y Fire Blast (sv1-34) = `DiscardEnergy(SELF,1)`;
    Take Down (sv3-40)=`Recoil(10)`, Blazing Shout (sv1-38)=`Recoil(30)`.
  - **Ops NUEVAS (Effects.kt)**:
    - `CoinsPerEnergyDamage(energyType, damagePerHeads)` → Torkoal Concentrated Fire (sv1-35, "80×"):
      lanza moneda por cada Energía Fuego adjunta, 80 por cara, al Activo rival. OJO: el daño base de
      ese ataque es `Damage.Variable` (=0 en `attack()`) → no hay doble conteo; todo el daño sale de la op.
    - `AttachEnergyFromDiscard(count, energyType, target)` → une Energía Básica del DESCARTE.
      Determinista si target=SELF/OWN_ACTIVE (Volcarona Flame Cloak sv3-41, 1 Fuego a sí mismo);
      interactivo si target=OWN_ALL → emite `AttachFromRevealed(fromDiscard=true)` (Skeledirge
      Passionate Singing sv1-38, hasta 2 a tus Pokémon).
  - **`AttachFromRevealed` generalizada** con `fromDiscard: Boolean`: false=mazo+barajar (Gen. Eléctrico),
    true=descarte sin barajar. `applyAttachFromRevealed` ramifica el origen. La UI (`RevealAttachTray`)
    y `revealTargetUnder` YA son genéricas (validan contra `benchCandidates`, que incluye el Activo) →
    Passionate Singing/Mela funcionan por arrastre SIN cambios de UI. SmartAgent auto-empareja igual.
  - **Mela (sv4-167) HECHO — Partidario condicional**: `Effect.requiresOwnKoLastTurn`. Rastreo de KO
    nuevo: `GameState.koedLastOppTurn: Set<Side>` (se rellena en `handleKnockouts` cuando te noquean
    en el turno RIVAL —no en auto-KO por recoil— y se limpia para el lado que ACABA su turno en
    `endTurn`). `playTrainer` y `legalIntents` rechazan/ocultan Mela si `activeSide !in koedLastOppTurn`.
    Efecto = `AttachEnergyFromDiscard(1,FIRE,OWN_ALL, thenDrawUpTo=6)`. El "**si lo haces** roba hasta 6"
    es ATÓMICO dentro de la op (param `thenDrawUpTo`): solo roba si se unió ≥1 Energía. Si no hay Fuego
    en el descarte, NO engancha y NO roba (verificado en test). Fiel al texto oficial.
    - **BUG UI de Mela ARREGLADO (9 Jul)** — el motor/efecto SIEMPRE fue correcto; fallaba la
      bandeja `RevealAttachTray` (GameScreen): (1) `revealedCards` se buscaba SOLO en
      `state.player.deck` → para `fromDiscard` (Mela/Canto Apasionado) salía VACÍA y no había
      energía que arrastrar (solo "Listo (no unir nada)") → Mela no hacía nada; ahora el origen
      es `discard` si `decision.fromDiscard`, si no `deck`. (2) El subtítulo estaba FIJO al texto
      del Generador Eléctrico ("Energía Rayo… de tu Banca") y aparecía en Mela; ahora se adapta
      por `fromDiscard` ("Energía del descarte a uno de tus Pokémon"). Verificado por captura del
      dispositivo (serial 3bf89e4f). APK reinstalado.
  - **Daño puro (correcto sin efecto)**: Houndour, Torkoal Stampede, Larvesta Flare, Volcarona Heat
    Blast, Fuecoco, Crocalor, Charcadet, y los 2º ataques de daño fijo.
  - Tests: 4 en `EffectInterpreterTest`, 1 en `EffectsDbTest`, 3 en `TrainerAbilityTest` (Mela:
    rechazo sin KO, engancha+roba con KO, y sin Fuego en descarte no roba).
- **DARKRAI 100% AUTOMATIZADA (7 Jul, tarde)** — con las 3 barajas queda CERRADA la automatización:
  - **Daño condicional EXACTO ("X+")**: nuevo DSL `Effect.attackDamage: List<DamageTerm>` +
    `DamageCondition{ALWAYS, IF_DEFENDER_EVOLVED}`. `GameEngine.attack()` usa la SUMA de términos
    aplicables como base y le aplica Debilidad/Resistencia UNA vez (por eso el bonus respeta la
    debilidad, a diferencia de `ExtraDamage` que suma crudo DESPUÉS). Seviper Cross-Cut (sv2-137)=
    50 (+50 si Evolución); Yveltal Cross-Cut (sv4-118)=30 (+60 si Evolución). OJO: "X+" parsea a
    `Damage.Variable` → base de carta 0, por eso el daño lo autora el efecto.
  - **Gust**: op nueva `EffectOp.SwapOppActiveWithChosen` (data object) → Órdenes de Jefe/Ghetsis
    (sv2-172): `ChooseTarget(OPP_BENCH,1)` + sube ese a Activo rival, su Activo baja a Banca.
  - **Ops existentes**: Yveltal Dark Edge (sv4-118)=`DiscardEnergy(SELF,1)`; Cyclizar Touring
    (sv1-164)=`DrawCards(2)`.
  - **Daño puro**: Darkrai ex (Wind of Darkness/Night Impact), Pawniard, Bisharp, Kingambit,
    Salandit, Salazzle, y 2os ataques fijos.
  - Tests: 1 en `EffectInterpreterTest` (gust), 1 en `EffectsDbTest`, 1 en `GameEngineTest`
    (Cross-Cut base vs Evolución + debilidad ×2).
- **AUTOMATIZACIÓN DE EFECTOS: COMPLETA** para Pikachu + Armarouge + Darkrai. Método de auditoría:
  `python` sobre `cartas-db.json` (campo `cartas` = LISTA de dicts; ataques con claves EN
  `name`/`text`/`damage`, habilidades `abilities`, Partidarios con texto en `reglas`).
- **BUILD APK (gotcha crítico del junction)**: los tests se corren por `C:\tcgdev` (junction) →
  contaminan los intermedios de `:engine:*`/`:data:netplay` con raíz junction y el DEXING del APK
  falla ("located outside root directory"). Antes de ensamblar el APK: `:clean` de los módulos
  tocados (o `clean` global) y `:app:assembleDebug` SIEMPRE desde la RUTA REAL con acentos.
- **Herramienta de auditoría**: script python con `tools/.venv` que parsea `data/cards/.../cartas-db.json`
  (dict por `id`, campos `ataques[].name/damage/text`, `habilidades`, `reglas`) para sacar textos reales.
- **Nombres de baraja**: "Pikachu/Armarouge/Darkrai ex Academia de Combate 2024" (`StarterDecks.kt`).
- **Tests JVM**: junction ASCII `C:\tcgdev` → raíz del repo (recreado esta sesión). Correr
  `& C:\tcgdev\gradlew.bat -p C:\tcgdev :engine:model:test :engine:effects:test :engine:rules:test`.
  Ojo `EffectsDbTest` asume qué está/no registrado — actualizar al implementar más.
- **Banca en PANAL 3+2 (medida 1:1, sin solapes)**: `BoardGeometry.MeBenchSlots/OppBenchSlots` (fila de 3
  cy≈0.628 jugador / 0.207 rival + fila de 2 cy≈0.751 / 0.100; rival más chico por escorzo). NO tocar.

## 🌐 PARTIDA ONLINE (Firestore, host-autoritativo) — EN CURSO (8 Jul)
- **BLINDAJE FLUJO DE BARAJAS (9 Jul)** — usuario reportó "el rival juega EXACTAMENTE mi
  baraja siendo yo anfitrión" (ambos con barajas propias distintas). Trazado: el motor y el
  transporte son CORRECTOS (host arma el rival desde `hello.deck` del invitado, no del suyo;
  `h2g`/`g2h` sin loopback; Hello se serializa entero). Sospecha nº1 = **perfiles/baraja
  activa DUPLICADOS** (posible sync de perfil por cuenta Google compartida → misma baraja
  activa). Hardening: `NetMessage.Hello` ahora lleva `deckName`; `OnlineGameScreen.loadDeck()`
  devuelve nombre+ids; el controlador **loguea la baraja de cada lado** (host y guest, este
  vía `onHostHello`) + guarda si el rival no envía baraja. **DECISIÓN USUARIO: que ambos elijan
  la MISMA baraja es VÁLIDO (espejo), NO es error** → nada de avisos de "barajas idénticas". El
  log (visible en el registro) dirá en la próxima prueba con qué baraja entra cada uno → confirma
  si es perfil duplicado o un bug real de estado. Compila; `:data:netplay:test` verde.
Plan aprobado en `C:\Users\pmmt9\.claude\plans\zesty-coalescing-brook.md`. Objetivo: 2 jugadores por
Internet, **ceremonia completa** (moneda sincro, prep en cada tel, mulligan) e **info oculta** (host
manda a cada quien su vista censurada). Diseño: HOST corre el motor y manda "fotos"; GUEST pinta y
manda `Intent`.
- **Firebase (Fase 0)**: proyecto CLI **`tcg-live-clone-net-4823`** creado; app Android registrada
  (appId `1:225372622583:android:0f17c4db2887149fcdcdf2`); **`app/google-services.json`** generado
  (en `.gitignore`). `firebase.json`+`firestore.rules`(abiertas SOLO `matches/**`)+`.firebaserc` listos.
  **HECHO**: base Firestore creada (por consola Firebase, la de Cloud daba error de permisos),
  reglas desplegadas: `matches/**` requiere `request.auth != null` (Auth ANÓNIMA). **PENDIENTE
  (1 interruptor del usuario)**: activar proveedor **Anonymous** en Firebase console →
  Authentication → Sign-in method (link: console.firebase.google.com/project/tcg-live-clone-net-4823/
  authentication/providers). Sin eso, `signInAnonymously()` del transporte falla en runtime.
  **HECHO: el usuario habilitó Anonymous el 8 Jul. Fase 0 100% cerrada.**
- **Fase 5 parcial**: mensajes de ceremonia añadidos a `NetMessage.kt` (CoinResult/ChooseOrder/
  SetupChoice/Ceremony/Deal) + compila. FALTA el `OnlineGameController`/VM que los orquesta.
- **Verificado que COMPILA** (8 Jul): `:data:netplay`, `:feature:game:compileDebugKotlin` y
  **`:data:netfirestore:compileDebugKotlin`** (contra SDK Firebase real) → todo verde. El proyecto
  NO queda roto en la pausa.
- **GOTCHA Kotlin (comentarios anidados)**: `/*` DENTRO de un KDoc abre un comentario ANIDADO en
  Kotlin. Escribir `matches/**` en un `/** … */` rompía con "Unclosed comment". Evitar `/*`/`/**`
  dentro de comentarios (fue el bug de FirestoreMatchTransport.kt).
- **Fases 5 y 6 HECHAS y PROBADAS EN 2 TELÉFONOS (8 Jul)**: `OnlineGameController` (host-autoritativo),
  `OnlineGameScreen` (lobby crear/unirse por código), botón "JUGAR ONLINE" en Home (hex verde,
  reusa el slot vacío), `Screen.ONLINE` en MainActivity, `TcgApplication` implementa
  `MatchFactoryProvider` (matchFactory **by lazy** — OJO: si es eager crashea porque toca Firebase
  antes de que su ContentProvider inicialice). **Se conectaron y jugaron por Internet.** APK en
  dispositivo el 8 Jul.
- **2 BUGS DE JUEGO ARREGLADOS (8 Jul, tras prueba real)**:
  - *CRASH del invitado al llegar snapshot del rival*: `GameStateDto.toGameState` hacía
    `repo[CardId(idInstancia)]` → null (repo indexado por id IMPRESO) → `error()`/crash. Fix:
    `CardRepository.byInstance(raw)` = `this[cid.printed]?.withId(cid)` en toda la rehidratación.
    Test nuevo en `NetplayRoundTripTest` con mazos uniquificados (ids de instancia) lo blinda.
  - *Moneda injusta (el host siempre "ganaba", el invitado no elegía)*: rediseño = el INVITADO
    llama cara/cruz (`NetMessage.CoinCall`), el HOST lanza 50/50 (`onGuestCoinCall`) y anuncia
    `CoinResult`. Ceremony("COIN") pone al guest en CHOOSING; el host espera. Ganador elige orden.
- **NUEVO RUMBO (decisión usuario 8 Jul)**: QUITAR anónimo + login con Google + sistema de AMIGOS
  con INVITACIONES a combate (en vez de código). Implica identidad real (Google Sign-In, ya funciona).
  Es una función grande PENDIENTE (lista de amigos + invitaciones + notificaciones en Firestore).
  El `OnlineGameController` (juego) se conserva; cambia el LOBBY (código → invitar amigo).
- **HECHO y verificado (tests JVM verdes)**:
  - Fase 1 (Gradle): Firebase BoM 33.7.0 + plugin `google-services` 4.4.2 en `libs.versions.toml`,
    root/app/settings; `:feature:game` depende de `:data:netplay`; nuevo módulo **`:data:netfirestore`**
    (android-lib) con firestore-ktx + coroutines-play-services.
  - Fase 2: **`FirestoreMatchTransport(Factory)`** en `:data:netfirestore`. Modelo: `matches/{code}`
    + subcolas `h2g`/`g2h` (doc = `{seq,payload,at}`); listener `orderBy(seq)` emite ADDED →
    `incoming: Flow<NetMessage>`; `send()` escribe; `close()` (host borra doc). Código corto 5 chars
    (alfabeto sin ambiguos). `MatchFactoryProvider` añadido a `:data:netplay/MatchTransport.kt`.
  - Fase 3: **censura+perspectiva** en `GameStateDto.kt`. `GameState.toDtoFor(viewer)` pone al viewer
    como PLAYER (abajo) y censura mano/mazo/premios del rival a solo CONTEO (`handCount/deckCount/
    prizeCount` nuevos). `toGameState` rellena zonas censuradas con `repo.all.first()` (placeholder,
    solo cuenta el tamaño; GameScreen pinta dorsos por `opponent.hand.size`). Test nuevo verde.
- **Fase 4 HECHA (compila)**: interfaz **`GameController`** (feature/game/GameController.kt) con
  ui/fx/chooseCoin/chooseFirst/chooseActive/clearActive/toggleBench/confirmSetup/onDealComplete/
  onIntent/onResolve/cardName/card. `GameViewModel` la implementa (añadido `override`). `GameScreen`
  y `DecisionPanel` reciben `vm: GameController` (default `viewModel<GameViewModel>()`). Sin cambio
  de comportamiento vs IA. **NOTA**: `legalIntents` NO está en la interfaz (GameScreen no lo usa).
- **FALTA (Fases 5-6, lo grande)**:
  - Fase 5: **`OnlineGameViewModel : GameController`** (máquina de ceremonia host/guest sobre el
    transporte; reusa `GameSetup/GameEngine`; guest corre `legalIntents` local solo-lectura).
    Mensajes de ceremonia nuevos en `NetMessage` (CoinResult/ChooseOrder/SetupChoice/Ready).
  - Fase 6: **`NetLobbyScreen`** (crear/unirse+código+mazo) + nav en `MainActivity`
    (ONLINE_LOBBY/ONLINE_GAME + botón Home) + `TcgApplication` crea `matchFactory` e implementa
    `MatchFactoryProvider`.
  - Fase 7: build APK (ruta con acentos), instalar en 2 dispositivos, jugar partida entera.
- **Ojo build**: `google-services.json` es obligatorio para el plugin; ya está. El compile NO necesita
  la API de Firestore (eso es runtime).

## 📌 REGLA — "REPLICAR" TCG Live (skill `replicar-tcglive`, 10 Jul 2026)
Al pedir **"replicar"** un comportamiento de TCG Live, seguir la skill **`replicar-tcglive`**
(`.claude/skills/replicar-tcglive/SKILL.md`): fuente única = videos Combate Completo 1/2
(conjunto activo = **Combate 1**; Combate 2 solo si el usuario lo pide). No grabar pantalla en
vivo. Consumir los mp4 con **`tools/scripts/vidref.py`** (`index` → hojas de contactos para
localizar TODAS las ocurrencias; `burst` → frames full-res para analizar). Analizar (reglas,
estados, transiciones, ritmo/animaciones, marcar incertidumbre) ANTES de codear; aplicar directo
al código, sin docs aparte; fidelidad 1:1 salvo variación pedida (que mantenga el estilo visual).

## (histórico) ⏩ RETOMAR AQUÍ — 2 Jul 2026, 08:20
**Réplica 1:1 del ciclo de combate de TCG Live.** Vamos construyendo por orden cronológico
y verificando frame-a-frame en dispositivo físico (serial `3bf89e4f`, 1080x2400).
- **HECHO y verificado**: Matchmaking → tablero → tiro de moneda → **reparto de 7 (animado)**
  → **PREPARACIÓN ON-BOARD** (A, nuevo) → **REVELADO inicial** (rival voltea + premios entran)
  → primer turno ("TU TURNO").
- **(A) PREPARACIÓN ON-BOARD — HECHA y verificada en dispositivo (2 Jul)**: se jubiló el
  `SetupOverlay` oscuro; ahora la selección se hace EN EL TABLERO igual que `ref_0030.png`:
  mi Activo boca arriba en `MeActive`, rival boca abajo (dorsos), banner blanco "Pulsa Listo
  para continuar cuando tengas todo preparado." + botón amarillo **LISTO** (habilitado al tener
  Activo), SIN premios aún. Al pulsar LISTO → el revelado que ya existía (rival voltea + entran
  6 premios) → combate. Implementación:
  - `GameSetup.provisional(player, activeId, benchIds, opponent)` construye un estado de tablero
    provisional (Activo/Banca colocados, SIN premios, `Phase.SETUP`) para poder DIBUJAR el tablero
    durante la preparación reutilizando todos los composables existentes.
  - `GameViewModel`: durante setup mantiene ese estado provisional en `state` (via `rebuildProvisional()`
    en `beginSetup`/`chooseActive`/`toggleBench`); nuevo `clearActive()` (tocar el Activo lo devuelve).
  - `GameScreen`: `inSetup = ui.setup != null`; el tablero se pinta (ya no hay early-return a overlay).
    Rival boca abajo con `RevealActive/RevealBench(flip=0)`; pilas de premios y "Acabar turno"
    ocultas en setup; mano interactiva → tocar un Básico abre `CardDetailSheet` con "Poner como
    Activo"/"Poner en Banca"; tocar Activo/Banca del tablero los retira. Banner nuevo `SetupPrompt`
    anclado en `BoardGeometry.SetupBanner` (NBox).
- **DÓNDE NOS QUEDAMOS / siguiente paso** (elegir entre):
  - **(B) Primer turno / robar carta**: animación de robo al empezar el turno.
  - **(C) Pantalla de MULLIGAN** (`ref_0024.png`): mano revelada del rival al declarar mulligan.
- **Si el usuario solo dice "continua" sin elegir**: pregunta B/C.
- **Cómo build+deploy+verificar** (rutas ABSOLUTAS; ver secciones de abajo):
  `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; & C:\DOCUMENTOS\TCG-Live-Clone\gradlew.bat -p C:\DOCUMENTOS\TCG-Live-Clone :app:assembleDebug -q`
  → `adb install -r ...\app\build\outputs\apk\debug\app-debug.apk`. **adb NO está en PATH**:
  usar `C:\Users\pmmt9\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- **Navegar a setup por adb** (coords 1080x2400): JUGAR `tap 540 1469` → wait 6s → CARA
  `tap 540 2081` → wait 8s → aparece la preparación on-board. Para colocar Activo: `uiautomator
  dump` y buscar el content-desc del Básico en `me_hand` (los tt:* exponen bounds); tocar la carta
  → tocar "Poner como Activo" → tocar LISTO (dcha del banner ~`tap 990 900`). **Screencap sin
  corromper**: `adb shell screencap -p /sdcard/x.png` + `adb pull` (NO `exec-out ... > file` en
  PowerShell: lo corrompe con UTF-16).
- Archivos clave tocados (sin commitear; el usuario decide cuándo): `engine/rules/.../GameSetup.kt`
  (+`provisional`), `feature/game/.../GameViewModel.kt`, `GameScreen.kt`, `board/BoardGeometry.kt`
  (+`SetupBanner`), `board/HandAndPanels.kt` (se BORRÓ `SetupOverlay/SetupCard/SetupChip`),
  + `DealOverlay.kt`, `CoinFlipOverlay.kt`, `MatchmakingScreen.kt`.

## Build Android (gotchas críticos)
- **JAVA_HOME ya está seteada a nivel User** → JDK 17 en `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`
  (las terminales nuevas la toman; las ya abiertas, no). Si por algo falta:
  `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"`.
- **La ruta del repo tiene acentos** (`POKÉMON`/`COLECCIÓN`) → rompe cosas de Gradle de dos formas
  distintas. Existe un junction ASCII `C:\tcgdev` → `...\android`. Regla que funciona:
  - **Tests JVM (`:data:*:test`, `:engine:*:test`) → corre desde `C:\tcgdev`** (la ruta con acentos
    corrompe el classpath de los workers de test: "Could not execute test class").
  - **Ensamblado del APK (`:app:assembleDebug`) → corre desde la ruta REAL con acentos**, no desde el
    junction (el junction rompe el dexing: "file located outside root directory").
  - No mezcles ambos en la misma invocación. Cada operación en su ruta.
  - **Si compilaste un módulo por el junction (tests) y luego ensamblas el APK por la ruta con acentos,
    el dexing falla** ("located outside root directory") porque los intermedios quedaron con raíz
    junction. Arreglo: `:modulo:clean` de los módulos tocados por el junction y reensamblar desde la
    ruta con acentos. Ideal: elegir UNA ruta por sesión de build.
- `gradle.properties` ya tiene `android.overridePathCheck=true` (AGP aborta si no, por los acentos).
- Todos los módulos compilan SDK 35 / JDK 17. Módulos `:engine:*` y `:data:cards`/`:data:gacha` son
  Kotlin puro JVM (testeables sin Android SDK).

## Arquitectura (módulos)
- `:engine:{model,events,effects,rules}` = motor de batalla puro y determinista (self-play IA-vs-IA
  ya funciona; aún SIN UI). `:data:{cards,gacha,profile}`. `:core:designsystem`.
  `:feature:{packs,decks}`. `:app` (navegación por enum `Screen`, sin Nav lib).
- Persistencia: `ProfileRepository` (DataStore Preferences) serializa con DTO locales `@Serializable`
  porque el modelo de dominio usa `value class` (CardId) no serializable directamente.
- Imágenes: Coil 2.7 (`coil.compose.AsyncImage`). Convención: solo arte ES (`artwork.smallEs`);
  si es null → **punto rojo** (la carta no existe en español). Imágenes ES de tcgdex.net
  (`/es/sv/<setcode>/<nº-3dig>/low.webp`; energías básicas vía Cenit Supremo swsh12.5).
- **Ojo: `SetInfo.code` guarda el NOMBRE del set, no el código corto** (el mapper usa `set.nombre`).
  Para agrupar/filtrar por expansión se usa ese nombre.
- Lógica pura testeable (validación, filtro/orden de cartas) vive en `:data:cards` (Kotlin puro);
  la UI en `:feature:decks` la consume. `:core:designsystem` ahora depende de `:engine:model`
  (para `EnergyType` en `TypeEmblem`/`DeckBox`).

## Geometría de cartas 1:1 (RETUNE 2 Jul) — medido en ref_0040
- **Fuente de medidas**: `referencias_live/combate1/ref_0040.png` (combate limpio, 1080x2400 =
  lienzo de referencia exacto) + `tools/refspec/board_start_ref.png` (tablero vacío auténtico).
  Método: cuadrícula normalizada + detección de rectángulos por saturación/brillo con OpenCV
  (`out/ref0040_det.png`), recortes a zoom de esquinas. Todas las cartas comparten `CardAspect
  = 106/148 = 0.716`.
- **Tamaños medidos (fracción del lienzo)**: Activo 0.22w×0.14h (centro rival y=0.338, mío
  y=0.485); Banca 0.172w×0.108h (fila centrada; rival y=0.206, mío y=0.617); Mano ~0.28w×0.176h
  (borde superior y≈0.82, solape 0.30·w); mano rival dorsos ~0.048h.
- **PREMIOS/MAZO/DESCARTE = cartas APAISADAS (horizontales)**, NO verticales (corrección 2 Jul
  1:30pm; el usuario lo detectó — la detección CV engañaba con recuadros, la VISTA manda). En TCG
  Live estas zonas van tumbadas en horizontal. `CardBackLandscape` = dorso girado 90° (tamaño
  intercambiado + `rotate(90f)`) → llena caja landscape. `PrizeStack` = 6 dorsos apaisados
  apilados con solape (badge interior: arriba jugador / abajo rival). `DeckPile` = dorso apaisado
  que llena su caja, SIN etiqueta de texto (solo badge). Aspecto `BoardGeometry.SideCardAspect =
  148/106 ≈ 1.40`. Cajas: premios `w0.15` (x0.010), mazo/descarte `w0.15 h0.062` (x0.840),
  apilados vertical (mazo arriba de descarte en mi lado). **Rival y jugador MISMO tamaño**; las
  del rival colocadas para verse enteras cerca del borde superior. Verificado en `out/zone_cmp2.png`.
- **Constantes**: `BoardGeometry.{BenchCardHFrac=0.108, HandCardHFrac=0.176, OppHandCardHFrac=
  0.048}`; `HandFan`/`OpponentHandFan` reciben tamaño de carta proporcional a `boardH`. Cajas
  de Activo/Banca == huella de la carta (aspecto ≈ CardAspect, la carta llena la caja).
- **Verificado en dispositivo** (side-by-side vs ref_0040 en `out/sbs2.png`): banca, banca rival,
  activos, premios (columnas), mazos/descartes y mano coinciden 1:1. Helper de navegación nuevo:
  `tools/scripts/goto_combat.py [serial] [out.png]` (JUGAR→CARA→coloca 1er Básico→LISTO→captura).

## Ciclo de combate (fases cronológicas TCG Live) — estado
- Flujo: HOME → `JUGAR` → **Matchmaking** → **tablero (mat)** → **tiro de moneda** →
  **reparto de 7 (animado)** → **preparación (Activo/Banca)** → **primer turno**.
- Todo cableado en `MainActivity` (enum `Screen`) + `GameViewModel` (coinFlip → dealing →
  setup → combate). El reparto (con mulligan) se resuelve en `init`; la ANIMACIÓN vive en
  `DealOverlay.kt` (mazo `MeDeck` → abanico `MeHand`, dorsos verdes que revelan arte al
  aterrizar). ViewModel: `beginDeal()` emite `dealing`, UI llama `onDealComplete()` al terminar.
- **Verificación en dispositivo (arnés `tcgtools`)**: el runner usa su propio adb → antes de
  `play` conviene `adb start-server`, `input keyevent KEYCODE_WAKEUP`, y exportar
  `TCG_ADB` al mismo binario (evita "no devices" transitorio por USB). Correr desde `tools/`
  con el venv `tools/.venv`. Guion del reparto: `tools/scripts/deal_capture.yaml`.
- **OJO capturas de animaciones**: `exec-out screencap` tarda ~0.35s/frame → NO muestrea
  bien animaciones cortas (~1.8s solo caen 1–2 frames). Para tuning fotograma-a-fotograma
  usar `screenrecord` + extracción (`capture.py`), no la ráfaga de `capture`.
- **Grabación frame-a-frame lista**: `tools/scripts/record_deal.py [serial]` graba con
  `screenrecord` EN UN HILO mientras el hilo principal navega (JUGAR→espera→CARA) y extrae
  ~18fps con OpenCV a `out/run_*/frames/`. El volado+resultado dura ~4s tras tocar CARA; el
  reparto cae ~f_100–160. Montaje rápido de tira de contactos con cv2 hconcat.
- **Reparto de 7 AFINADO (Jul 1)**: las cartas se distribuyen uniformes dentro de `MeHand`
  con inset de media carta (`spacing=(handW-cardW)/(n-1)`, borde izq = `handLeft+spacing*i`)
  → sin recorte lateral; barrido mazo(der)→mano(abajo), revelado al aterrizar. Verificado.
- **REVELADO inicial hecho (Jul 1)**: tras `confirmSetup()` el VM pone `revealing=true` 2s;
  GameScreen anima con `revealFlip` (Animatable 0→1, delay 350 + tween 520): el Activo/Banca
  del RIVAL se voltean (dorso→arte) vía `FlipCard` (rotationY 0→180 + contra-rotación de la
  cara a 180 para no espejar; `cameraDistance`), y las 2 pilas de premios entran con
  `alpha=revealFlip`. `myTurn` y el banner de turno se bloquean mientras `revealing`.
  Verificado con `record_reveal.py` (dorso→canto~90°→arte + premios). Nota: el "C" naranja
  sobre una carta es el placeholder de carga de Coil (arte aún no bajado), NO un bug.
- **Referencias de flujo real** en `referencias_live/combate1/` (134 frames auténticos de
  TCG Live). Flujo real de prep: MULLIGAN (mano revelada del rival si aplica) → prep EN EL
  TABLERO (mi Activo boca arriba, rival boca abajo, banner "Pulsa Listo…" + botón LISTO,
  SIN premios aún) → al pulsar LISTO: revelado + reparto de 6 premios → primer turno.
  HECHO (2 Jul): la SELECCIÓN de Activo/Banca ya es on-board con banner+LISTO (se jubiló
  `SetupOverlay`; ver "RETOMAR AQUÍ"). Falta cronológicamente: MULLIGAN y animación de robo.

## Estado / roadmap
- Hecho: Inicio, Cartadex (binder 151), Sobres (gacha + límite diario), BARAJAS it.1 (gestor: ver,
  marcar activa/favorita; sembrado con 3 starters Battle Academy).
- Hecho: **EffectsDb** (base de efectos autorados, DSL determinista) cableada al motor y a la
  resolución de ataques (commit be95478). 49 efectos portados del kit JS; `EffectInterpreter`
  ejecuta el DSL; `EffectsDbTest` valida registro y cobertura. Pendiente sólo: `git push`.
- Siguiente natural: **editor de barajas** (añadir/quitar, crear/borrar, FILTROS) y/o **pantalla de
  combate** que cablee `:engine:rules` + `GreedyAgent` al tablero vertical (botón JUGAR sigue TODO).
- Referencias de vídeo del usuario en `C:\DOCUMENTOS\POKÉMON TCG\TCG LIVE VS MI APP CLON\...` (no
  versionadas). Replicar look con arte ORIGINAL propio, nunca assets de TPC.
