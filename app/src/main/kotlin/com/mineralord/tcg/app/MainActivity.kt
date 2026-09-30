package com.mineralord.tcg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineralord.tcg.core.designsystem.TcgColors
import com.mineralord.tcg.core.designsystem.TcgTheme
import com.mineralord.tcg.core.designsystem.motion.AnimationTheme
import com.mineralord.tcg.core.designsystem.motion.MotionScreen
import com.mineralord.tcg.data.profile.PlayerProfile
import com.mineralord.tcg.data.profile.ProfileRepository
import com.mineralord.tcg.feature.decks.DecksScreen
import com.mineralord.tcg.feature.game.MatchmakingScreen
import com.mineralord.tcg.feature.game.OnlineGameScreen
import com.mineralord.tcg.feature.packs.PacksScreen
import kotlinx.coroutines.launch

/**
 * Subpantallas que se APILAN sobre las páginas principales (a pantalla completa, sin barra inferior).
 * Las 4 páginas principales (Inicio · Cartas · Amigos · Logros) viven en el [HorizontalPager] y no
 * están aquí. "Menú" es un popup, no una página.
 */
private enum class Screen { STORE, PACKS, COSMETICS, COSMETIC_COLLECTION, DECKS, DECK_SELECT, MATCHMAKING, GAME, ONLINE, PROFILE, FRIENDS }

/** Nº de páginas principales navegables por deslizamiento. */
private const val MAIN_PAGES = 4

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
                    AppShell()
                }
            }
        }
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

@Composable
private fun AppShell() {
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

    val ctx = LocalContext.current
    val profileRepo = remember { ProfileRepository(ctx.applicationContext) }
    val profile by profileRepo.profile.collectAsState(initial = PlayerProfile())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) { profileRepo.seedBalancesOnce() }

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
                        onJugarOnline = { afterDeckSelect = { matchNonce++; push(Screen.ONLINE) }; push(Screen.DECK_SELECT) },
                        onAmigos = { goToPage(2) },
                        // Tocar un sobre lleva DIRECTO a "Rasga el sobre" de ESA expansión (sin selector).
                        onOpenPack = { code -> packsDirectCode = code; push(Screen.PACKS) },
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
                        onVersus = { afterDeckSelect = { matchNonce++; push(Screen.ONLINE) }; push(Screen.DECK_SELECT) },
                        onIndividual = { afterDeckSelect = { pickingDifficulty = true }; push(Screen.DECK_SELECT) },
                        onBarajas = { push(Screen.DECKS) },
                    )
                }
            }
            MainBottomNav(
                current = pager.currentPage,
                onSelect = { goToPage(it) },
                onMenu = { menuOpen = true },
            )
        }

        // ---- Overlays (subpantallas a pantalla completa) ----
        val top = overlay.lastOrNull()
        if (top != null) {
            MotionScreen(targetState = top, modifier = Modifier.fillMaxSize()) { target ->
                when (target) {
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
                            onExit = { back() },
                            modifier = Modifier.fillMaxSize(),
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
                            onEquip = { c -> scope.launch { profileRepo.equipCosmetic(c.category, c.id) } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Screen.COSMETIC_COLLECTION -> SubScreen(title = "Colección de cosméticos", onHome = { back() }) {
                        CosmeticCollectionScreen(
                            profile = profile,
                            onEquip = { c -> scope.launch { profileRepo.equipCosmetic(c.category, c.id) } },
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
