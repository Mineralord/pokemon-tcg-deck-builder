package com.mineralord.tcg.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.core.designsystem.blockPassThroughTouches
import com.mineralord.tcg.core.designsystem.TcgTheme
import com.mineralord.tcg.core.designsystem.motion.AnimationTheme
import com.mineralord.tcg.core.designsystem.motion.MotionScreen
import com.mineralord.tcg.data.profile.PlayerProfile
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.feature.decks.DecksScreen
import com.mineralord.tcg.feature.game.MatchmakingScreen
import com.mineralord.tcg.feature.game.OnlineGameScreen
import com.mineralord.tcg.feature.packs.PacksScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Subpantallas que se APILAN sobre las páginas principales (a pantalla completa, sin barra inferior).
 * Las 4 páginas principales (Inicio · Cartas · Amigos · Logros) viven en el [HorizontalPager] y no
 * están aquí. "Menú" es un popup, no una página.
 */
private enum class Screen { STORE, PACKS, COSMETICS, COSMETIC_COLLECTION, DECKS, DECK_SELECT, ROOM, MATCHMAKING, GAME, ONLINE, PROFILE, FRIENDS }

/** Nº de páginas principales navegables por deslizamiento. */
private const val MAIN_PAGES = 4

/** Datos de una invitación recibida vía la notificación (al tocarla entras directo a la sala). */
data class InviteExtras(val roomCode: String, val fromUid: String, val fromName: String)

class MainActivity : ComponentActivity() {
    // Invitación llegada por intent (toque de notificación): la observa [AppShell] para entrar a la sala.
    private val inviteFromIntent = mutableStateOf<InviteExtras?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        MatchInviteNotifications.ensureChannel(this)
        readInvite(intent)
        // Permiso de notificaciones (Android 13+) para avisar cuando un sobre esté listo.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        // Inmersivo GLOBAL (todas las pantallas, actuales y futuras): oculta las barras
        // de estado y navegación como TCG Live. El tablero ocupa la pantalla completa.
        hideSystemBars()
        setContent {
            TcgTheme {
                AnimationTheme {
                    AppShell(inviteFromIntent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readInvite(intent)
    }

    /** Extrae la invitación a combate de los extras del intent (si viene de tocar la notificación). */
    private fun readInvite(intent: Intent?) {
        val room = intent?.getStringExtra(MatchInviteNotifications.EXTRA_ROOM_CODE) ?: return
        val from = intent.getStringExtra(MatchInviteNotifications.EXTRA_FROM_UID) ?: return
        val name = intent.getStringExtra(MatchInviteNotifications.EXTRA_FROM_NAME).orEmpty()
        inviteFromIntent.value = InviteExtras(room, from, name)
        // Consumido: evita re-disparar al rotar / reabrir.
        intent.removeExtra(MatchInviteNotifications.EXTRA_ROOM_CODE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-oculta al recuperar el foco (tras notificaciones, diálogos o volver de fondo).
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

/** Código de sala legible (5 chars, sin caracteres ambiguos), igual estilo que el transporte real. */
private fun genRoomCode(): String =
    (1..5).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")

@Composable
private fun AppShell(inviteFromIntent: androidx.compose.runtime.MutableState<InviteExtras?>) {
    // Páginas principales (Inicio · Cartas · Amigos · Logros): se navegan por DESLIZAMIENTO
    // horizontal (HorizontalPager) y comparten la barra inferior, siempre presente.
    val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { MAIN_PAGES })

    // Subpantallas apiladas ENCIMA (a pantalla completa, sin barra). El retroceso las cierra.
    val overlay = remember { androidx.compose.runtime.mutableStateListOf<Screen>() }
    fun push(to: Screen) { if (overlay.lastOrNull() != to) overlay.add(to) }
    fun replace(to: Screen) { if (overlay.isNotEmpty()) overlay[overlay.lastIndex] = to else overlay.add(to) }
    fun back() { if (overlay.isNotEmpty()) overlay.removeAt(overlay.lastIndex) }

    // Código de expansión para abrir DIRECTAMENTE el rasgado (desde el escaparate de la Home).
    var packsDirectCode by remember { mutableStateOf<String?>(null) }
    // Selector de dificultad PvE.
    var pickingDifficulty by remember { mutableStateOf(false) }
    // Nonce que IDENTIFICA la partida actual: sube cada vez que se ENTRA a un combate
    // (PvE/PvP). Fuerza un subárbol nuevo por partida → GameViewModel/controller frescos,
    // evitando que la partida anterior (ya terminada) quede pegada al reabrir el combate.
    var matchNonce by remember { mutableStateOf(0) }
    // Continuación tras elegir baraja (PvE o PvP).
    var afterDeckSelect by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Popup "Menú" (placeholder por ahora).
    var menuOpen by remember { mutableStateOf(false) }
    // El visor de info de carta pide ocultar la barra de navegación (para que NUNCA tape su X).
    var chromeHidden by remember { mutableStateOf(false) }
    // Código de la sala multijugador actual (distinto del ID de amigo). Se renueva al abrir una sala
    // como anfitrión; al aceptar una invitación toma el código de la sala del que invita.
    var roomCode by remember { mutableStateOf("") }
    // Rol en la sala actual: true = anfitrión (creó la sala), false = invitado (aceptó una invitación).
    var roomAsHost by remember { mutableStateOf(true) }

    val ctx = LocalContext.current
    val profileRepo = remember { ProfileRepository(ctx.applicationContext) }
    val profile by profileRepo.profile.collectAsState(initial = PlayerProfile())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Publica tu identidad (código + nombre) en el directorio de amigos de forma GLOBAL: así, estés
    // donde estés en la app, al cambiar tu nombre tus amigos lo ven en vivo (no solo en el Hub).
    val friendsRepo = remember { com.mineralord.tcg.data.netfirestore.FirestoreFriendsRepository() }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { profileRepo.ensureFriendCode() } }
    // Publica identidad + última conexión + total de cartas. Se refresca al cambiar nombre/código o
    // la colección, y en cada vuelta a primer plano (heartbeat de "última conexión").
    suspend fun publishPresence() {
        if (profile.friendCode.isNotBlank()) {
            runCatching {
                friendsRepo.publishSelf(profile.friendCode, profile.username, profile.owned.values.sum())
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(profile.friendCode, profile.username, profile.owned) {
        publishPresence()
    }
    val presenceLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(presenceLifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START) scope.launch { publishPresence() }
        }
        presenceLifecycle.lifecycle.addObserver(obs)
        onDispose { presenceLifecycle.lifecycle.removeObserver(obs) }
    }

    // Música de ambientación de Kanto (FRLG) en el menú. Se silencia en combates y en la
    // apertura de sobres; el banner superior izquierdo muestra el tema que suena.
    val music = rememberMenuMusic()
    val battleMusic = rememberBattleMusic()
    val packMusic = rememberPackOpeningMusic()
    val menuTitle by music.title.collectAsState(initial = null)
    val battleTitle by battleMusic.title.collectAsState(initial = null)
    val packTitle by packMusic.title.collectAsState(initial = null)
    // El banner muestra el tema que suene: combate y apertura de sobres tienen prioridad sobre el de menú.
    val musicTitle = battleTitle ?: packTitle ?: menuTitle
    androidx.compose.runtime.LaunchedEffect(overlay.lastOrNull()) {
        val topScreen = overlay.lastOrNull()
        val inCombat = topScreen == Screen.GAME || topScreen == Screen.ONLINE
        val inPacks = topScreen == Screen.PACKS
        // Menú: todo salvo combate y sobres. Combate: solo en la partida. Apertura de sobres: en PACKS.
        music.setScreenAllows(!inCombat && !inPacks)
        battleMusic.setScreenAllows(inCombat)
        packMusic.setScreenAllows(inPacks)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { profileRepo.seedBalancesOnce() }

    // Sobres gratis disponibles (regeneración +1 cada 12 h, tope 6): se calcula del monedero real
    // con el reloj CONFIABLE. Alimenta el contador del escaparate de la Home ("n/6" / "Al máximo").
    val freePacks by androidx.compose.runtime.produceState(initialValue = 6) {
        val regen = com.mineralord.tcg.data.gacha.PackRegen()
        val clock = com.mineralord.tcg.data.profile.TrustedClock(ctx.applicationContext)
        profileRepo.wallet.collect { w ->
            value = if (w == null) regen.maxPacks else regen.available(w, clock.nowMs())
        }
    }

    // ---- Invitaciones a combate (banner global + notificación) ----
    val friendsVm = androidx.lifecycle.viewmodel.compose.viewModel<FriendsViewModel>()
    val matchInvites by friendsVm.matchInvites.collectAsState()
    val topScreenNow = overlay.lastOrNull()
    val inCombatNow = topScreenNow == Screen.GAME || topScreenNow == Screen.ONLINE
    val topInvite = matchInvites.maxByOrNull { it.ts }

    // ---- Sala compartida (lobby real en Firestore: ambos jugadores se ven) ----
    val roomRepo = remember { com.mineralord.tcg.data.netfirestore.FirestoreRoomRepository() }
    var myUid by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        myUid = runCatching { roomRepo.myUid() }.getOrDefault("")
        // Limpieza GRATIS de salas huérfanas: barre las antiguas al arrancar (sin servidor).
        runCatching { roomRepo.sweepStaleRooms() }
    }
    val myName = profile.username.ifBlank { "Entrenador" }
    val myDeckName = profile.decks.firstOrNull { it.id == profile.activeDeckId }?.name
        ?: profile.decks.firstOrNull()?.name.orEmpty()

    // Miembros y estado del lobby EN VIVO para la sala actual.
    val roomMembers by remember(roomCode) {
        if (roomCode.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
        else roomRepo.membersFlow(roomCode)
    }.collectAsState(initial = emptyList())
    val roomStatus by remember(roomCode) {
        if (roomCode.isBlank()) kotlinx.coroutines.flow.flowOf("")
        else roomRepo.lobbyStatusFlow(roomCode)
    }.collectAsState(initial = "")

    val localReady = roomMembers.firstOrNull { it.uid == myUid }?.ready == true
    val canStart = roomMembers.size >= 2 && roomMembers.all { it.ready }
    val inRoom = overlay.lastOrNull() == Screen.ROOM

    // Mantén sincronizada TU baraja elegida con tu miembro de la sala (para que el rival la vea).
    androidx.compose.runtime.LaunchedEffect(inRoom, roomCode, myDeckName) {
        if (inRoom && roomCode.isNotBlank()) runCatching { roomRepo.setDeck(roomCode, myDeckName) }
    }

    // Arranque sincronizado: cuando el lobby pasa a "started", AMBOS caen al tapete.
    var navedToMat by remember(roomCode) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(roomStatus) {
        if (roomStatus == "started" && !navedToMat && roomCode.isNotBlank()) {
            navedToMat = true
            matchNonce++
            if (overlay.lastOrNull() == Screen.ROOM) replace(Screen.ONLINE) else push(Screen.ONLINE)
        }
    }

    // Publica una notificación del sistema (con sonido "shiny") por cada invitación nueva.
    val notified = remember { androidx.compose.runtime.mutableStateMapOf<String, Long>() }
    androidx.compose.runtime.LaunchedEffect(matchInvites) {
        matchInvites.forEach { inv ->
            if (notified[inv.fromUid] != inv.ts) {
                notified[inv.fromUid] = inv.ts
                MatchInviteNotifications.show(ctx, inv.fromUid, inv.fromName, inv.roomCode)
            }
        }
    }

    fun enterRoomAsGuest(code: String, fromUid: String) {
        roomCode = code
        roomAsHost = false
        friendsVm.clearMatchInvite(fromUid)
        MatchInviteNotifications.cancel(ctx, fromUid)
        scope.launch { runCatching { roomRepo.enterAsGuest(code, myName, myDeckName) } }
        if (overlay.lastOrNull() != Screen.ROOM) push(Screen.ROOM)
    }

    // Toque de la notificación: entra directo a la sala del que invita.
    androidx.compose.runtime.LaunchedEffect(inviteFromIntent.value) {
        val e = inviteFromIntent.value ?: return@LaunchedEffect
        enterRoomAsGuest(e.roomCode, e.fromUid)
        inviteFromIntent.value = null
    }

    // Pantalla de carga inicial: espera a que el PRIMER tema del menú esté listo para sonar (se
    // descarga en streaming la primera vez), con un tope de 6 s por si no hay red. Cuando está
    // lista invita a TOCAR para entrar; el toque la desvanece con una transición elegante.
    var appReady by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        withTimeoutOrNull(6000) { music.title.first { !it.isNullOrBlank() } }
        appReady = true
    }

    fun goToPage(i: Int) { scope.launch { pager.animateScrollToPage(i) } }

    // Retroceso: cierra popup → cierra overlay → vuelve a Inicio si estás en otra página.
    BackHandler(enabled = menuOpen || overlay.isNotEmpty() || pager.currentPage != 0) {
        when {
            menuOpen -> menuOpen = false
            overlay.isNotEmpty() -> back()
            else -> goToPage(0)
        }
    }

    // Desenfoque del fondo mientras el menú lateral está abierto (se anima suavemente).
    val menuBlur by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (menuOpen) 18.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(260),
        label = "menuBlur",
    )

    CompositionLocalProvider(
        com.mineralord.tcg.core.designsystem.LocalBottomBarHider provides { hidden -> chromeHidden = hidden },
    ) {
    Box(Modifier.fillMaxSize()) {
        // ---- Base: páginas principales con barra inferior compartida ----
        Column(Modifier.fillMaxSize().blur(menuBlur)) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth().weight(1f),
                // Deslizamiento AAA puro (sin crossfade): cada página se mueve con el dedo.
                beyondViewportPageCount = 1,
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        balances = profile.balances,
                        avatarColors = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.AVATAR)
                            ?.let { com.mineralord.tcg.data.cosmetics.Cosmetics.repo[it] }
                            ?.colors?.map { androidx.compose.ui.graphics.Color(it) },
                        onCartadex = { /* Misiones: popup por implementar */ },
                        onTienda = { push(Screen.STORE) },
                        onBarajas = { push(Screen.DECKS) },
                        onPerfil = { push(Screen.PROFILE) },
                        onJugar = { afterDeckSelect = { pickingDifficulty = true }; push(Screen.DECK_SELECT) },
                        onJugarOnline = {
                            val code = genRoomCode(); roomCode = code; roomAsHost = true
                            scope.launch { runCatching { roomRepo.enterAsHost(code, myName, myDeckName) } }
                            push(Screen.ROOM)
                        },
                        onAmigos = { goToPage(2) },
                        // Tocar un sobre lleva DIRECTO a "Rasga el sobre" de ESA expansión (sin selector).
                        onOpenPack = { code -> packsDirectCode = code; push(Screen.PACKS) },
                        freePacks = freePacks,
                    )
                    1 -> CollectionScreen(
                        onExit = { goToPage(0) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    2 -> ComunidadScreen(
                        modifier = Modifier.fillMaxSize(),
                        // "Amigos" abre la pantalla experimental de amigos como subpantalla.
                        onAmigos = { push(Screen.FRIENDS) },
                    )
                    else -> PartidasScreen(
                        modifier = Modifier.fillMaxSize(),
                        // Versus → hub PvP (elige baraja → online). Individual → PvE (baraja → dificultad).
                        onVersus = {
                            val code = genRoomCode(); roomCode = code; roomAsHost = true
                            scope.launch { runCatching { roomRepo.enterAsHost(code, myName, myDeckName) } }
                            push(Screen.ROOM)
                        },
                        onIndividual = { afterDeckSelect = { pickingDifficulty = true }; push(Screen.DECK_SELECT) },
                        onBarajas = { push(Screen.DECKS) },
                    )
                }
            }
            // La barra base se oculta cuando un visor de carta está abierto (su X nunca debe taparse).
            if (!chromeHidden) {
                MainBottomNav(
                    current = pager.currentPage,
                    onSelect = { goToPage(it) },
                    onMenu = { menuOpen = true },
                )
            }
        }

        // ---- Overlays (subpantallas a pantalla completa) ----
        // La capa de overlays BLOQUEA los toques hacia la base (pager): al abrir una subpantalla,
        // solo esa es tocable, nunca los elementos de la pantalla anterior que quedan debajo.
        val top = overlay.lastOrNull()
        if (top != null) {
            MotionScreen(
                targetState = top,
                modifier = Modifier.fillMaxSize().blockPassThroughTouches(),
            ) { target ->
                when (target) {
                    Screen.ROOM -> {
                        val friendList by friendsVm.friends.collectAsState()
                        val myColors = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.AVATAR)
                            ?.let { com.mineralord.tcg.data.cosmetics.Cosmetics.repo[it] }
                            ?.colors?.map { androidx.compose.ui.graphics.Color(it) }
                        // Miembros reales de la sala (ambos jugadores en vivo). Si aún no cargan, me muestro yo.
                        val players = if (roomMembers.isEmpty()) {
                            listOf(
                                com.mineralord.tcg.feature.game.LobbyPlayer(
                                    name = myName,
                                    role = "Baraja: ${myDeckName.ifBlank { "—" }}",
                                    online = true, ready = localReady, isHost = roomAsHost, isLocal = true,
                                    avatarColors = myColors,
                                ),
                            )
                        } else {
                            roomMembers.map { m ->
                                com.mineralord.tcg.feature.game.LobbyPlayer(
                                    name = m.name.ifBlank { "Entrenador" },
                                    role = "Baraja: ${m.deckName.ifBlank { "—" }}",
                                    online = true,
                                    ready = m.ready,
                                    isHost = m.isHost,
                                    isLocal = m.uid == myUid,
                                    avatarColors = if (m.uid == myUid) myColors else null,
                                )
                            }
                        }
                        com.mineralord.tcg.feature.game.FriendLobbyScreen(
                            modifier = Modifier.fillMaxSize(),
                            roomCode = roomCode.ifBlank { "------" },
                            players = players,
                            friends = friendList.map {
                                com.mineralord.tcg.feature.game.LobbyFriend(
                                    uid = it.uid,
                                    name = it.username.ifBlank { "Entrenador" },
                                    status = "En línea",
                                )
                            },
                            canStart = canStart,
                            localReady = localReady,
                            onToggleReady = { ready -> scope.launch { runCatching { roomRepo.setReady(roomCode, ready) } } },
                            onBack = {
                                val leaving = roomCode
                                scope.launch { runCatching { roomRepo.leave(leaving) } }
                                roomCode = ""
                                back()
                            },
                            onShareCode = {
                                val clip = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                                clip.setPrimaryClip(android.content.ClipData.newPlainText("Código de sala", roomCode))
                                android.widget.Toast.makeText(ctx, "Código de sala copiado: $roomCode", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onInviteFriend = { friend ->
                                friendsVm.inviteToMatch(friend.uid, roomCode)
                                android.widget.Toast.makeText(ctx, "Invitación enviada a ${friend.name}", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onCardLimit = { /* Detalle de límite de cartas: por implementar. */ },
                            // Cada jugador elige SU baraja en la sala: abre el selector (fija la baraja
                            // activa); al volver se sincroniza a su miembro de la sala.
                            onChooseDeck = { afterDeckSelect = null; push(Screen.DECK_SELECT) },
                            myDeckName = myDeckName,
                            // INICIAR solo señaliza el arranque; ambos caen al tapete por el estado del lobby.
                            onStart = { scope.launch { runCatching { roomRepo.start(roomCode) } } },
                        )
                    }
                    Screen.MATCHMAKING -> MatchmakingScreen(
                        deckName = "Mega-Charizard X ex",
                        onCancel = { back() },
                        onMatched = { matchNonce++; replace(Screen.GAME) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Screen.GAME -> MatchScope(matchNonce) {
                        com.mineralord.tcg.feature.game.combat.CombatScreen(
                            onExit = { back() },
                            vm = androidx.lifecycle.viewmodel.compose.viewModel<com.mineralord.tcg.feature.game.GameViewModel>(),
                            modifier = Modifier.fillMaxSize(),
                            matThemeId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.TAPETE),
                            sleeveId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.FUNDA),
                            victoryEffectId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.VICTORIA),
                            defeatEffectId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.DERROTA),
                            winCristales = com.mineralord.tcg.data.profile.EconomyRules.PVE_WIN_CRISTALES,
                            winMonedas = com.mineralord.tcg.data.profile.EconomyRules.PVE_WIN_MONEDAS,
                            lossCristales = com.mineralord.tcg.data.profile.EconomyRules.PVE_LOSS_CRISTALES,
                            lossMonedas = com.mineralord.tcg.data.profile.EconomyRules.PVE_LOSS_MONEDAS,
                        )
                    }
                    Screen.ONLINE -> MatchScope(matchNonce) {
                        OnlineGameScreen(
                            onExit = {
                                // Al salir de la partida, abandona la sala (borra tu miembro y, si eras
                                // el último, la sala entera) para no dejar salas huérfanas.
                                val leaving = roomCode
                                scope.launch { runCatching { roomRepo.leave(leaving) } }
                                roomCode = ""
                                back()
                            },
                            modifier = Modifier.fillMaxSize(),
                            // Sala de amigos: ambos convergen en la misma partida con el código de sala.
                            autoCode = roomCode.ifBlank { null },
                            autoHost = roomAsHost,
                        )
                    }
                    Screen.STORE -> SubScreen(title = "Tienda", onHome = { back() }) {
                        StoreHubScreen(
                            balances = profile.balances,
                            onSobres = { packsDirectCode = null; push(Screen.PACKS) },
                            onCosmeticos = { push(Screen.COSMETICS) },
                            onColeccion = { push(Screen.COSMETIC_COLLECTION) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Screen.PACKS -> {
                        // Modo directo (desde Home): pantalla limpia sin barra "‹ Inicio". Modo tienda: con barra.
                        if (packsDirectCode != null) {
                            PacksScreen(
                                modifier = Modifier.fillMaxSize(),
                                directExpansionCode = packsDirectCode,
                                onExit = { back() },
                            )
                        } else {
                            SubScreen(title = "Tienda · Sobres", onHome = { back() }) {
                                PacksScreen(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    Screen.COSMETICS -> SubScreen(title = "Tienda · Cosméticos", onHome = { back() }) {
                        CosmeticStoreScreen(
                            profile = profile,
                            onBuy = { c -> scope.launch { profileRepo.buyCosmetic(c) } },
                            // Toggle: si ya está equipado, lo quita (vuelve al aspecto por defecto).
                            onEquip = { c -> scope.launch {
                                if (profile.equippedIn(c.category) == c.id) profileRepo.unequipCosmetic(c.category)
                                else profileRepo.equipCosmetic(c.category, c.id)
                            } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Screen.COSMETIC_COLLECTION -> SubScreen(title = "Colección de cosméticos", onHome = { back() }) {
                        CosmeticCollectionScreen(
                            profile = profile,
                            // Toggle: tocar el equipado lo quita (vuelve al aspecto por defecto).
                            onEquip = { c -> scope.launch {
                                if (profile.equippedIn(c.category) == c.id) profileRepo.unequipCosmetic(c.category)
                                else profileRepo.equipCosmetic(c.category, c.id)
                            } },
                            onToggleFavorite = { c -> scope.launch { profileRepo.toggleCosmeticFavorite(c.id) } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Screen.DECKS -> DecksScreen(
                        modifier = Modifier.fillMaxSize(),
                        onHome = { back() },
                    )
                    Screen.DECK_SELECT -> DecksScreen(
                        modifier = Modifier.fillMaxSize(),
                        onHome = { afterDeckSelect = null; back() },
                        selectionMode = true,
                        onSelect = {
                            back()
                            val next = afterDeckSelect
                            afterDeckSelect = null
                            next?.invoke()
                        },
                    )
                    Screen.PROFILE -> {
                        val repo = com.mineralord.tcg.data.cosmetics.Cosmetics.repo
                        fun equipped(cat: com.mineralord.tcg.data.cosmetics.CosmeticCategory) =
                            repo[profile.equippedIn(cat)]
                        ProfileScreen(
                            modifier = Modifier.fillMaxSize(),
                            onBack = { back() },
                            onOpenCollection = { back(); goToPage(1) },
                            equippedAvatar = equipped(com.mineralord.tcg.data.cosmetics.CosmeticCategory.AVATAR),
                            equippedFrame = equipped(com.mineralord.tcg.data.cosmetics.CosmeticCategory.MARCO),
                            equippedBackground = equipped(com.mineralord.tcg.data.cosmetics.CosmeticCategory.FONDO),
                            equippedBadge = equipped(com.mineralord.tcg.data.cosmetics.CosmeticCategory.BADGE),
                        )
                    }
                    Screen.FRIENDS -> FriendsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBack = { back() },
                    )
                }
            }
        }

        // En la pantalla de SOBRES se muestra la barra de navegación de la app (como la referencia):
        // se dibuja ENCIMA del overlay para que sea tocable; al elegir una página cierra el overlay.
        if (top == Screen.PACKS && !chromeHidden) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                MainBottomNav(
                    current = pager.currentPage,
                    onSelect = { overlay.clear(); goToPage(it) },
                    onMenu = { menuOpen = true },
                )
            }
        }

        if (pickingDifficulty) {
            DifficultyDialog(
                onPick = { difficulty ->
                    com.mineralord.tcg.feature.game.PveConfig.difficulty = difficulty
                    pickingDifficulty = false
                    push(Screen.MATCHMAKING)
                },
                onDismiss = { pickingDifficulty = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        MenuOverlay(
            visible = menuOpen,
            onDismiss = { menuOpen = false },
            userId = profile.friendCode.ifBlank { "----------" },
            playerName = profile.username.ifBlank { "Entrenador" },
            avatarColors = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.AVATAR)
                ?.let { com.mineralord.tcg.data.cosmetics.Cosmetics.repo[it] }
                ?.colors?.map { androidx.compose.ui.graphics.Color(it) },
        )

        // Banner del tema que suena (arriba a la izquierda), por encima de todo.
        MenuMusicBanner(title = musicTitle, modifier = Modifier.align(Alignment.TopStart))

        // Banner GLOBAL de solicitud de combate: en el menú y en cualquier pantalla salvo en combate.
        // ✓ entra a la sala del que invita; ✗ la descarta.
        MatchRequestBanner(
            visible = topInvite != null && !inCombatNow,
            fromName = topInvite?.fromName.orEmpty(),
            onAccept = { topInvite?.let { enterRoomAsGuest(it.roomCode, it.fromUid) } },
            onReject = {
                topInvite?.let {
                    friendsVm.clearMatchInvite(it.fromUid)
                    MatchInviteNotifications.cancel(ctx, it.fromUid)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Pantalla de carga inicial: cubre todo mientras la app prepara la música; al estar lista
        // invita a tocar para entrar y se desvanece suavemente con el toque.
        AnimatedVisibility(
            visible = !entered,
            enter = androidx.compose.animation.EnterTransition.None,
            exit = fadeOut(animationSpec = tween(650)),
        ) {
            SplashScreen(
                ready = appReady,
                onEnter = { if (appReady) entered = true },
            )
        }
    }
    }
}

/**
 * Pantalla de carga/bienvenida. Mientras [ready] es false muestra el indicador de carga (da
 * tiempo a que el primer tema del menú se descargue y quede listo). Al estar lista invita a TOCAR
 * para entrar: el toque dispara [onEnter], que desvanece la pantalla hacia el menú principal.
 */
@Composable
private fun SplashScreen(ready: Boolean, onEnter: () -> Unit) {
    // Latido suave del texto "Toca para entrar" una vez lista la app.
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "splashPulse")
    val promptAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "splashPromptAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TcgColors.Navy, TcgColors.Ink),
                ),
            )
            .clickable(
                enabled = ready,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onEnter,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Pokémon TCG",
                color = TcgColors.Gold,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
            )
            Spacer(Modifier.height(32.dp))
            // Transición elegante entre el indicador de carga y la invitación a tocar.
            androidx.compose.animation.Crossfade(targetState = ready, label = "splashState") { isReady ->
                if (isReady) {
                    Text(
                        "Toca para entrar",
                        color = Color.White.copy(alpha = promptAlpha),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = TcgColors.Gold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp),
                        )
                        Text(
                            "Preparando la partida…",
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aísla el ciclo de vida de UNA partida (PvE/PvP). Da a su contenido un [ViewModelStore]
 * PROPIO (no el del Activity), scopeado a esta composición: al salir del combate se limpia,
 * así el [GameViewModel] (cuyo `init` arma el tablero) se recrea de cero en la siguiente
 * partida en vez de quedar pegado al estado final de la anterior. El [key] por [nonce]
 * garantiza un subárbol nuevo por partida (también renueva el `remember` del controller PvP).
 */
@Composable
private fun MatchScope(nonce: Int, content: @Composable () -> Unit) {
    // La Application es necesaria para construir GameViewModel (AndroidViewModel): el owner
    // propio DEBE aportarla vía factory + extras, igual que hace el Activity por defecto.
    val app = LocalContext.current.applicationContext as android.app.Application
    key(nonce) {
        val owner = remember {
            object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
                override val viewModelStore = ViewModelStore()
                override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
                    ViewModelProvider.AndroidViewModelFactory.getInstance(app)
                override val defaultViewModelCreationExtras: CreationExtras
                    get() = MutableCreationExtras().apply {
                        set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, app)
                    }
            }
        }
        DisposableEffect(Unit) { onDispose { owner.viewModelStore.clear() } }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
    }
}

/** Página principal aún no implementada (p. ej. Logros). Placeholder con el chasis claro. */
@Composable
private fun ComingSoonPage(title: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(TcgColors.Cream),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = TcgColors.Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("Próximamente", color = TcgColors.Ink.copy(alpha = 0.5f), fontSize = 14.sp)
        }
    }
}

/** Envoltorio de pantalla secundaria con barra superior y botón de Inicio. */
@Composable
private fun SubScreen(title: String, onHome: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(TcgColors.Navy)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TcgColors.Navy)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ Inicio",
                color = TcgColors.Parchment,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onHome),
            )
            Text(
                title,
                color = TcgColors.Parchment,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        content()
    }
}
