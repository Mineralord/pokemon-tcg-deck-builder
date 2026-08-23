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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private enum class Screen { HOME, COLLECTION, STORE, PACKS, COSMETICS, DECKS, MATCHMAKING, GAME, ONLINE, PROFILE }

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
    var screen by remember { mutableStateOf(Screen.HOME) }
    // Selector de dificultad PvE: se muestra al pulsar JUGAR y, al elegir, pasa a matchmaking.
    var pickingDifficulty by remember { mutableStateOf(false) }

    // Saldos económicos (Fase 2, Cap. 6). Comparten el DataStore de proceso con el resto de pantallas.
    val ctx = LocalContext.current
    val profileRepo = remember { ProfileRepository(ctx.applicationContext) }
    val profile by profileRepo.profile.collectAsState(initial = PlayerProfile())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Garantiza la concesión inicial de saldos (Cristales + Monedas) aunque el jugador
    // no abra la pantalla de sobres: la Tienda de Cosméticos necesita Monedas.
    androidx.compose.runtime.LaunchedEffect(Unit) { profileRepo.seedBalancesOnce() }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Box(Modifier.fillMaxSize()) {
    // Transición coherente entre pantallas (crossfade Motion). Cada estado se resuelve dentro.
    MotionScreen(targetState = screen, modifier = Modifier.fillMaxSize()) { target ->
    when (target) {
        Screen.HOME -> HomeScreen(
            modifier = Modifier.fillMaxSize(),
            balances = profile.balances,
            avatarColors = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.AVATAR)
                ?.let { com.mineralord.tcg.data.cosmetics.Cosmetics.repo[it] }
                ?.colors?.map { androidx.compose.ui.graphics.Color(it) },
            onCartadex = { screen = Screen.COLLECTION },
            onTienda = { screen = Screen.STORE },
            onBarajas = { screen = Screen.DECKS },
            onPerfil = { screen = Screen.PROFILE },
            onJugar = { pickingDifficulty = true },
            onJugarOnline = { screen = Screen.ONLINE },
        )
        Screen.MATCHMAKING -> MatchmakingScreen(
            deckName = "Mega-Charizard X ex",
            onCancel = { screen = Screen.HOME },
            onMatched = { screen = Screen.GAME },
            modifier = Modifier.fillMaxSize(),
        )
        // Combate vs IA: pantalla canónica de combate.
        Screen.GAME -> com.mineralord.tcg.feature.game.combat.CombatScreen(
            onExit = { screen = Screen.HOME },
            vm = androidx.lifecycle.viewmodel.compose.viewModel<com.mineralord.tcg.feature.game.GameViewModel>(),
            modifier = Modifier.fillMaxSize(),
            matThemeId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.TAPETE),
            sleeveId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.FUNDA),
            victoryEffectId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.VICTORIA),
            defeatEffectId = profile.equippedIn(com.mineralord.tcg.data.cosmetics.CosmeticCategory.DERROTA),
        )
        Screen.ONLINE -> OnlineGameScreen(
            onExit = { screen = Screen.HOME },
            modifier = Modifier.fillMaxSize(),
        )
        Screen.COLLECTION -> CollectionScreen(
            onExit = { screen = Screen.HOME },
            modifier = Modifier.fillMaxSize(),
        )
        Screen.STORE -> SubScreen(title = "Tienda", onHome = { screen = Screen.HOME }) {
            StoreHubScreen(
                balances = profile.balances,
                onSobres = { screen = Screen.PACKS },
                onCosmeticos = { screen = Screen.COSMETICS },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Screen.PACKS -> SubScreen(title = "Tienda · Sobres", onHome = { screen = Screen.STORE }) {
            PacksScreen(modifier = Modifier.fillMaxSize())
        }
        Screen.COSMETICS -> SubScreen(title = "Tienda · Cosméticos", onHome = { screen = Screen.STORE }) {
            CosmeticStoreScreen(
                profile = profile,
                onBuy = { c -> scope.launch { profileRepo.buyCosmetic(c) } },
                onEquip = { c -> scope.launch { profileRepo.equipCosmetic(c.category, c.id) } },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Screen.DECKS -> SubScreen(title = "Barajas", onHome = { screen = Screen.HOME }) {
            DecksScreen(modifier = Modifier.fillMaxSize())
        }
        Screen.PROFILE -> SubScreen(title = "Perfil", onHome = { screen = Screen.HOME }) {
            ProfileScreen(modifier = Modifier.fillMaxSize())
        }
    }
    }

    if (pickingDifficulty) {
        DifficultyDialog(
            onPick = { difficulty ->
                com.mineralord.tcg.feature.game.PveConfig.difficulty = difficulty
                pickingDifficulty = false
                screen = Screen.MATCHMAKING
            },
            onDismiss = { pickingDifficulty = false },
            modifier = Modifier.fillMaxSize(),
        )
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
