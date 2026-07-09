# Napkin — Pokémon TCG (Colección y Generador de Mazos)

Runbook curado. Solo guía recurrente de alto valor.

## ⏩ RETOMAR AQUÍ (al decir "continuemos"/"continua") — actualizado 9 Jul 2026, 08:50
**Terminología (confirmada por el usuario):** PvE = vs IA (`GameViewModel` + `SmartAgent`);
PvP = humano vs humano (`OnlineGameController`, host-autoritativo Firestore). Ambos comparten
`GameCore` + el motor → las reglas son idénticas en los dos modos (regla del usuario: NO duplicar
lógica; cualquier fix aplica a ambos).

**Al decir "continuemos", hacer EN ESTE ORDEN:**
1. **IA (PvE) sabe anclar Herramientas**: añadir `GameIntent.AttachTool` a `GameEngine.legalIntents`
   (tool en mano × Pokémon propio sin Herramienta) + heurística en `SmartAgent`.
2. **PvP: log del host → guest** (`NetMessage.LogLine` de los eventos; hoy el guest no ve el registro)
   y **recompensas por modo** vía el hook ya listo `GameCore.onGameFinished(winner)` (PvE distinto de PvP).
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
