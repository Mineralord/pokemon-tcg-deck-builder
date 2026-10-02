package com.mineralord.tcg.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mineralord.tcg.core.designsystem.TcgColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * Un tema de la banda sonora. [album] es el identificador del álbum abierto en el Internet
 * Archive; [path] la ruta del fichero dentro de él; [title] el nombre que muestra el banner.
 */
internal data class Track(val title: String, val album: String, val path: String)

private const val KANTO = "pkmn-frlg-soundtrack"   // Rojo Fuego / Verde Hoja
private const val HOENN = "pkmn-rse-soundtrack"     // Rubí / Zafiro / Esmeralda

/**
 * Ambientación (pueblos, rutas, ciudades, cuevas e interiores) de **Kanto** (FRLG) y **Hoenn**
 * (RSE). SIN combates, victorias ni jingles. Se reproduce al azar mezclando ambas regiones.
 */
private val MENU_PLAYLIST = listOf(
    // --- Kanto (Rojo Fuego / Verde Hoja) ---
    Track("Pueblo Paleta", KANTO, "Disc 1/04 - Pallet Town.mp3"),
    Track("Laboratorio del Prof. Oak", KANTO, "Disc 1/06 - Oak Pokémon Lab.mp3"),
    Track("Ruta 1", KANTO, "Disc 1/12 - Route 1.mp3"),
    Track("Ciudad Plateada", KANTO, "Disc 1/15 - Pewter City.mp3"),
    Track("Centro Pokémon", KANTO, "Disc 1/16 - Pokémon Center.mp3"),
    Track("Bosque Verde", KANTO, "Disc 1/19 - Viridian Forest.mp3"),
    Track("Gimnasio Pokémon", KANTO, "Disc 1/23 - Pokémon Gym.mp3"),
    Track("Ruta 3", KANTO, "Disc 1/28 - Route 3.mp3"),
    Track("Monte Moon", KANTO, "Disc 1/30 - Mt. Moon.mp3"),
    Track("Ciudad Celeste", KANTO, "Disc 1/31 - Cerulean City.mp3"),
    Track("Ruta 24", KANTO, "Disc 1/32 - Route 24.mp3"),
    Track("Ciudad Carmín", KANTO, "Disc 1/35 - Vermilion City.mp3"),
    Track("S.S. Anne", KANTO, "Disc 1/36 - S.S. Anne.mp3"),
    Track("En Bicicleta (Kanto)", KANTO, "Disc 1/37 - Cycling.mp3"),
    Track("Ruta 11", KANTO, "Disc 1/38 - Route 11.mp3"),
    Track("Pueblo Lavanda", KANTO, "Disc 1/39 - Lavender Town.mp3"),
    Track("Torre Pokémon", KANTO, "Disc 1/40 - Pokémon Tower.mp3"),
    Track("Ciudad Azulona", KANTO, "Disc 1/41 - Celadon City.mp3"),
    Track("Guarida del Team Rocket", KANTO, "Disc 1/46 - Rocket Hideout.mp3"),
    Track("Silph S.A.", KANTO, "Disc 1/49 - Silph Co..mp3"),
    Track("Surf (Kanto)", KANTO, "Disc 1/51 - Surf.mp3"),
    Track("Isla Canela", KANTO, "Disc 1/53 - Cinnabar Island.mp3"),
    Track("Mansión Pokémon", KANTO, "Disc 1/54 - Pokémon Mansion.mp3"),
    Track("Centro de Red Pokémon", KANTO, "Disc 1/55 - Pokémon Network Center.mp3"),
    Track("Islas Sete (Islas 4 y 5)", KANTO, "Disc 1/57 - Sevii Islands Four & Five Islands.mp3"),
    Track("Islas Sete", KANTO, "Disc 1/61 - Sevii Islands.mp3"),
    Track("Islas Sete (Islas 6 y 7)", KANTO, "Disc 1/62 - Sevii Islands Six & Seven Islands.mp3"),
    Track("Calle Victoria", KANTO, "Disc 1/68 - Victory Road.mp3"),
    Track("Torre Pokémon (1997)", KANTO, "Disc 2/06 - Pokémon Tower 1997.mp3"),
    // --- Hoenn (Rubí / Zafiro / Esmeralda) ---
    Track("Villa Raíz", HOENN, "Disc 1/05 - Littleroot Town.mp3"),
    Track("Laboratorio del Prof. Abedul", HOENN, "Disc 1/06 - Birch Pokémon Lab.mp3"),
    Track("Ruta 101", HOENN, "Disc 1/11 - Route 101.mp3"),
    Track("Pueblo Escaso", HOENN, "Disc 1/12 - Oldale Town.mp3"),
    Track("Centro Pokémon (Hoenn)", HOENN, "Disc 1/13 - Pokémon Center.mp3"),
    Track("Ciudad Petalia", HOENN, "Disc 1/20 - Petalburg City.mp3"),
    Track("Ruta 104", HOENN, "Disc 1/22 - Route 104.mp3"),
    Track("Bosque Petalia", HOENN, "Disc 1/23 - Petalburg Woods.mp3"),
    Track("Ciudad Férrica", HOENN, "Disc 1/27 - Rustboro City.mp3"),
    Track("Travesía marina", HOENN, "Disc 1/29 - Crossing the Sea.mp3"),
    Track("Pueblo Azuliza", HOENN, "Disc 1/30 - Dewford Town.mp3"),
    Track("Ciudad Portual", HOENN, "Disc 1/32 - Slateport City.mp3"),
    Track("Museo Oceánico", HOENN, "Disc 1/33 - Oceanic Museum.mp3"),
    Track("Ruta 110", HOENN, "Disc 1/34 - Route 110.mp3"),
    Track("En Bicicleta (Hoenn)", HOENN, "Disc 1/35 - Cycling.mp3"),
    Track("Pueblo Verdegal", HOENN, "Disc 1/41 - Verdanturf Town.mp3"),
    Track("Ruta 113", HOENN, "Disc 1/42 - Route 113.mp3"),
    Track("Pueblo Pardal", HOENN, "Disc 1/44 - Fallarbor Town.mp3"),
    Track("Teleférico", HOENN, "Disc 1/45 - Cable Car.mp3"),
    Track("Monte Chimenea", HOENN, "Disc 1/46 - Mt. Chimney.mp3"),
    Track("Ruta 111", HOENN, "Disc 1/48 - Route 111.mp3"),
    Track("Gimnasio Pokémon (Hoenn)", HOENN, "Disc 1/49 - Pokémon Gym.mp3"),
    Track("Surf (Hoenn)", HOENN, "Disc 1/54 - Surf.mp3"),
    Track("Ruta 119", HOENN, "Disc 2/01 - Route 119.mp3"),
    Track("Ciudad Arborada", HOENN, "Disc 2/02 - Fortree City.mp3"),
    Track("Ruta 120", HOENN, "Disc 2/03 - Route 120.mp3"),
    Track("Zona Safari", HOENN, "Disc 2/05 - Safari Zone.mp3"),
    Track("Ciudad Calagua", HOENN, "Disc 2/07 - Lilycove City.mp3"),
    Track("Monte Pírico", HOENN, "Disc 2/15 - Mt. Pyre.mp3"),
    Track("Monte Pírico (exterior)", HOENN, "Disc 2/18 - Mt. Pyre Exterior.mp3"),
    Track("Buceo", HOENN, "Disc 2/26 - Dive.mp3"),
    Track("Arrecípolis", HOENN, "Disc 2/27 - Sootopolis City.mp3"),
    Track("Cueva Origen", HOENN, "Disc 2/28 - Cave of Origin.mp3"),
    Track("Ciudad Colosalia", HOENN, "Disc 2/31 - Ever Grande City.mp3"),
    Track("Barco Abandonado", HOENN, "Disc 2/40 - Abandoned Ship.mp3"),
    Track("Calle Victoria (Hoenn)", HOENN, "Disc 2/42 - Victory Road.mp3"),
    Track("Zona de Combate (Esmeralda)", HOENN, "Disc 3 (Emerald)/04 - Battle Frontier.mp3"),
)

/**
 * Temas de **COMBATE** de Kanto (FRLG) y Hoenn (RSE). Suenan solo durante las partidas.
 */
private val BATTLE_PLAYLIST = listOf(
    // --- Kanto ---
    Track("¡Combate! (Entrenador)", KANTO, "Disc 1/09 - Battle! (Trainer) .mp3"),
    Track("¡Combate! (Pokémon salvaje)", KANTO, "Disc 1/13 - Battle! (Wild Pokémon).mp3"),
    Track("¡Combate tenso!", KANTO, "Disc 1/24 - Tense Battle!.mp3"),
    Track("¡Combate! (Líder de Gimnasio)", KANTO, "Disc 1/25 - Battle! (Gym Leader).mp3"),
    Track("¡Combate! (Pokémon legendario)", KANTO, "Disc 1/52 - Battle! (Legendary Pokémon).mp3"),
    Track("¡Combate! (Mewtwo)", KANTO, "Disc 1/66 - Battle! (Mewtwo).mp3"),
    Track("¡Combate final! (Rival)", KANTO, "Disc 1/69 - Final Battle! (Rival).mp3"),
    Track("¡Combate! (Deoxys)", KANTO, "Disc 2/02 - Battle! (Deoxys).mp3"),
    Track("La fuerza de un Líder de Gimnasio", KANTO, "Disc 2/07 - Strength of a Gym Leader.mp3"),
    // --- Hoenn ---
    Track("¡Combate! (Pokémon salvaje) [Hoenn]", HOENN, "Disc 1/09 - Battle! (Wild Pokémon).mp3"),
    Track("¡Combate! (Entrenador) [Hoenn]", HOENN, "Disc 1/17 - Battle! (Trainer).mp3"),
    Track("¡Combate! (Equipo Aqua/Magma)", HOENN, "Disc 1/25 - Battle! (Team Aqua-Team Magma).mp3"),
    Track("¡Combate! (Líder de Gimnasio) [Hoenn]", HOENN, "Disc 1/50 - Battle! (Gym Leader).mp3"),
    Track("¡Combate! (Rival)", HOENN, "Disc 2/11 - Battle! (Brendan-May).mp3"),
    Track("¡Combate! (Líderes Equipo Aqua/Magma)", HOENN, "Disc 2/22 - Battle! (Team Aqua-Team Magma Leaders).mp3"),
    Track("¡Combate! (Pokémon superancestral)", HOENN, "Disc 2/29 - Battle! (Super-Ancient Pokémon).mp3"),
    Track("¡Combate! (Regis)", HOENN, "Disc 2/38 - Battle! (Regirock-Regice-Registeel).mp3"),
    Track("¡Combate! (Alto Mando)", HOENN, "Disc 2/45 - Battle! (Elite Four).mp3"),
    Track("¡Combate! (Campeón)", HOENN, "Disc 2/47 - Battle! (Champion).mp3"),
    Track("¡Combate! (As del Frente)", HOENN, "Disc 3 (Emerald)/15 - Battle! (Frontier Brain).mp3"),
    Track("¡Combate! (Mew)", HOENN, "Disc 3 (Emerald)/17 - Battle! (Mew).mp3"),
)

/** Temas de **EVOLUCIÓN** (Kanto + Hoenn). Suenan durante la apertura de sobres. */
private val EVOLUTION_PLAYLIST = listOf(
    Track("Evolución (Kanto)", KANTO, "Disc 1/33 - Evolution.mp3"),
    Track("Evolución (Hoenn)", HOENN, "Disc 2/12 - Evolution.mp3"),
)

private const val MUSIC_VOLUME = 1f

/**
 * Caché en DISCO de la música (modo offline). Descarga cada tema la primera vez y lo reproduce
 * en local a partir de entonces. Para **no ocupar mucho espacio** mantiene un tope de tamaño
 * ([MAX_BYTES]) con desalojo LRU: al superarlo borra los temas menos usados recientemente.
 * Compartida por todos los reproductores (menú y combate escriben en la misma carpeta).
 */
private object MusicCache {
    /** Tope del caché de música: ~100 MB (buena parte de la banda sonora en local). */
    private const val MAX_BYTES = 100L * 1024 * 1024
    private var dir: File? = null
    private val locks = HashMap<String, Any>()

    private fun dir(ctx: Context): File =
        dir ?: File(ctx.cacheDir, "music").apply { mkdirs() }.also { dir = it }

    private fun key(track: Track): String =
        Integer.toHexString(("${track.album}|${track.path}").hashCode()) + ".mp3"

    fun fileFor(ctx: Context, track: Track): File = File(dir(ctx), key(track))

    fun isCached(ctx: Context, track: Track): Boolean =
        fileFor(ctx, track).let { it.exists() && it.length() > 0 }

    /** Devuelve el fichero local del tema, descargándolo (bloqueante) si aún no está en caché. */
    fun ensure(ctx: Context, track: Track): File? {
        val file = fileFor(ctx, track)
        if (file.exists() && file.length() > 0) {
            file.setLastModified(System.currentTimeMillis()) // "tocar" → LRU
            return file
        }
        val lock = synchronized(locks) { locks.getOrPut(file.path) { Any() } }
        synchronized(lock) {
            if (file.exists() && file.length() > 0) return file
            val url = "https://archive.org/download/${track.album}/" + Uri.encode(track.path, "/")
            val tmp = File(file.path + ".part")
            return runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000; readTimeout = 15_000; instanceFollowRedirects = true
                }
                conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                conn.disconnect()
                if (tmp.length() <= 0) { tmp.delete(); null }
                else {
                    tmp.renameTo(file)
                    file.setLastModified(System.currentTimeMillis())
                    trim(ctx)
                    file
                }
            }.getOrElse { tmp.delete(); null }
        }
    }

    /** Si el caché supera el tope, borra ficheros del más antiguo al más nuevo hasta caber. */
    private fun trim(ctx: Context) {
        val files = dir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".mp3") }?.toMutableList() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return
        files.sortBy { it.lastModified() } // más antiguo primero
        for (f in files) {
            if (total <= MAX_BYTES) break
            val len = f.length()
            if (f.delete()) total -= len
        }
    }
}

/**
 * Reproductor de música. Transmite temas al azar desde el álbum abierto del Internet Archive,
 * cacheándolos en disco para **modo offline** (ver [MusicCache]). Cuando uno termina arranca otro
 * distinto. Expone el título actual para el banner. Se silencia en combates / apertura de sobres
 * vía [setScreenAllows]; respeta el ciclo de vida (se pausa en segundo plano) con [setForeground].
 * La API se usa desde el hilo principal.
 */
class MenuMusicController internal constructor(context: Context, private val tracks: List<Track>) {
    private val appContext = context.applicationContext
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _title = MutableStateFlow<String?>(null)
    /** Título del tema que SUENA ahora (null mientras carga o cuando está en silencio). */
    val title: StateFlow<String?> = _title

    private var player: MediaPlayer? = null
    private var lastIndex = -1
    private var generation = 0
    private var released = false

    private var screenAllows = false
    private var foreground = true

    /** La pantalla actual PERMITE música (menú) o no (combate / apertura de sobres). */
    fun setScreenAllows(allow: Boolean) { if (screenAllows != allow) { screenAllows = allow; apply() } }

    /** La app está en primer plano. En segundo plano se pausa para no sonar fuera de foco. */
    fun setForeground(fg: Boolean) { if (foreground != fg) { foreground = fg; apply() } }

    private fun shouldPlay() = screenAllows && foreground

    private fun apply() {
        if (released) return
        if (shouldPlay()) resumeOrStart() else pauseKeeping()
    }

    private fun resumeOrStart() {
        val p = player
        if (p == null) startNext()
        else runCatching { if (!p.isPlaying) p.start() }
    }

    private fun pauseKeeping() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    /** Elige el siguiente índice al azar, SESGADO hacia los ya cacheados (offline fluido + menos descargas). */
    private fun nextIndex(): Int {
        if (tracks.size == 1) return 0
        val cached = tracks.indices.filter { it != lastIndex && MusicCache.isCached(appContext, tracks[it]) }
        if (cached.isNotEmpty() && Random.nextInt(100) < 65) return cached.random()
        var i: Int
        do { i = Random.nextInt(tracks.size) } while (i == lastIndex)
        return i
    }

    private fun startNext() {
        if (released) return
        runCatching { player?.release() }
        player = null
        _title.value = null
        val index = nextIndex()
        lastIndex = index
        val track = tracks[index]
        val gen = ++generation
        io.launch {
            val file = MusicCache.ensure(appContext, track)
            withContext(Dispatchers.Main) {
                // Descartar si ya cambió de tema, se liberó, o entramos en combate mientras cargaba.
                if (released || gen != generation) return@withContext
                if (file == null) { skipSoon(); return@withContext }
                if (!shouldPlay()) return@withContext // apply() reintentará al volver al menú (ya cacheado)
                playFile(file, track.title)
            }
        }
    }

    private fun playFile(file: File, trackTitle: String) {
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnPreparedListener { mp ->
                if (shouldPlay()) {
                    mp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                    mp.start()
                    _title.value = trackTitle
                }
            }
            setOnCompletionListener { startNext() }
            setOnErrorListener { _, _, _ -> skipSoon(); true }
        }
        player = mp
        runCatching {
            mp.setDataSource(file.absolutePath)
            mp.prepareAsync()
        }.onFailure { skipSoon() }
    }

    /** Ante un fallo (red/formato), salta al siguiente tras un respiro (evita bucle agresivo). */
    private fun skipSoon() {
        runCatching { player?.release() }
        player = null
        _title.value = null
        android.os.Handler(appContext.mainLooper).postDelayed({
            if (!released && shouldPlay()) startNext()
        }, 1500)
    }

    fun release() {
        released = true
        runCatching { io.cancel() }
        runCatching { player?.release() }
        player = null
        _title.value = null
    }
}

/** Crea el reproductor de música de MENÚ (ambientación de Kanto + Hoenn). */
@Composable
fun rememberMenuMusic(): MenuMusicController = rememberMusic(MENU_PLAYLIST)

/** Crea el reproductor de música de COMBATE (temas de batalla de Kanto + Hoenn). */
@Composable
fun rememberBattleMusic(): MenuMusicController = rememberMusic(BATTLE_PLAYLIST)

/** Crea el reproductor de música de EVOLUCIÓN (para la apertura de sobres). */
@Composable
fun rememberEvolutionMusic(): MenuMusicController = rememberMusic(EVOLUTION_PLAYLIST)

/** Crea un controlador ligado a la composición y al ciclo de vida; lo libera al salir. */
@Composable
private fun rememberMusic(tracks: List<Track>): MenuMusicController {
    val ctx = LocalContext.current
    val controller = remember { MenuMusicController(ctx, tracks) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> controller.setForeground(true)
                Lifecycle.Event.ON_STOP -> controller.setForeground(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }
    return controller
}

/**
 * Banner superior izquierdo con el nombre del tema. Aparece al empezar un tema nuevo y se
 * desvanece solo tras unos segundos (reaparece con cada cambio de pista).
 */
@Composable
fun MenuMusicBanner(title: String?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    // Cada vez que cambia el título (nuevo tema) se muestra y se auto-oculta a los 7 s.
    LaunchedEffect(title) {
        if (title.isNullOrBlank()) { visible = false; return@LaunchedEffect }
        visible = true
        kotlinx.coroutines.delay(7000)
        visible = false
    }
    AnimatedVisibility(
        visible = visible && !title.isNullOrBlank(),
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(TcgColors.Navy.copy(alpha = 0.82f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("♪", color = TcgColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text(
                title.orEmpty(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
