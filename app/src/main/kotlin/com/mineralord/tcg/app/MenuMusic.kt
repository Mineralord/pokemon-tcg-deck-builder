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

/**
 * Un tema de la banda sonora. [album] es el identificador del álbum abierto en el Internet
 * Archive; [path] la ruta del fichero dentro de él; [title] el nombre que muestra el banner.
 */
internal data class Track(val title: String, val album: String, val path: String)

private const val KANTO = "pkmn-frlg-soundtrack"   // Rojo Fuego / Verde Hoja
private const val HOENN = "pkmn-rse-soundtrack"     // Rubí / Zafiro / Esmeralda
private const val SINNOH = "pkmn-dppt-soundtrack"   // Diamante / Perla / Platino
private const val JOHTO = "pkmn-hgss-soundtrack"    // Oro HeartGold / Plata SoulSilver
private const val UNOVA = "pkmn-black-white-soundtrack" // Blanco / Negro

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
    Track("Pueblo Hojaverde (Día) (Sinnoh)", SINNOH, "Disc 1/05 - Twinleaf Town (Day).mp3"),
    Track("Ruta 201 (Día) (Sinnoh)", SINNOH, "Disc 1/07 - Route 201 (Day).mp3"),
    Track("Pueblo Arena (Día) (Sinnoh)", SINNOH, "Disc 1/14 - Sandgem Town (Day).mp3"),
    Track("Ciudad Jubileo (Día) (Sinnoh)", SINNOH, "Disc 1/23 - Jubilife City (Day).mp3"),
    Track("Ruta 203 (Día) (Sinnoh)", SINNOH, "Disc 1/26 - Route 203 (Day).mp3"),
    Track("Ciudad Pirita (Día) (Sinnoh)", SINNOH, "Disc 1/30 - Oreburgh City (Day).mp3"),
    Track("Pueblo Flor (Día) (Sinnoh)", SINNOH, "Disc 1/38 - Floaroma Town (Day).mp3"),
    Track("Ruta 205 (Día) (Sinnoh)", SINNOH, "Disc 1/40 - Route 205 (Day).mp3"),
    Track("Ciudad Vetusta (Día) (Sinnoh)", SINNOH, "Disc 1/45 - Eterna City (Day).mp3"),
    Track("Ruta 206 (Día) (Sinnoh)", SINNOH, "Disc 1/53 - Route 206 (Day).mp3"),
    Track("Ciudad Corazón (Día) (Sinnoh)", SINNOH, "Disc 1/54 - Hearthome City (Day).mp3"),
    Track("Ruta 209 (Día) (Sinnoh)", SINNOH, "Disc 1/55 - Route 209 (Day).mp3"),
    Track("Pueblo Sosiego (Día) (Sinnoh)", SINNOH, "Disc 1/57 - Solaceon Town (Day).mp3"),
    Track("Ruta 210 (Día) (Sinnoh)", SINNOH, "Disc 1/59 - Route 210 (Day).mp3"),
    Track("Ciudad Rocavelo (Día) (Sinnoh)", SINNOH, "Disc 1/60 - Veilstone City (Day).mp3"),
    Track("Ciudad Canal (Sinnoh)", SINNOH, "Disc 1/62 - Canalave City.mp3"),
    Track("Ruta 216 (Día) (Sinnoh)", SINNOH, "Disc 1/63 - Route 216 (Day).mp3"),
    Track("Ciudad Puntaneva (Día) (Sinnoh)", SINNOH, "Disc 1/64 - Snowpoint City (Day).mp3"),
    Track("Ciudad Marina (Día) (Sinnoh)", SINNOH, "Disc 1/73 - Sunyshore City (Day).mp3"),
    Track("Ruta 225 (Día) (Sinnoh)", SINNOH, "Disc 1/78 - Route 225 (Day).mp3"),
    Track("Ruta 228 (Día) (Sinnoh)", SINNOH, "Disc 1/79 - Route 228 (Day).mp3"),
    Track("Pueblo Hojaverde (Noche) (Sinnoh)", SINNOH, "Disc 2/01 - Twinleaf Town (Night).mp3"),
    Track("Ruta 201 (Noche) (Sinnoh)", SINNOH, "Disc 2/02 - Route 201 (Night).mp3"),
    Track("Pueblo Arena (Noche) (Sinnoh)", SINNOH, "Disc 2/05 - Sandgem Town (Night).mp3"),
    Track("Ciudad Jubileo (Sinnoh)", SINNOH, "Disc 2/07 - Jubilife City.mp3"),
    Track("Ciudad Canal (Noche) (Sinnoh)", SINNOH, "Disc 2/11 - Canalave City (Night).mp3"),
    Track("Ruta 203 (Noche) (Sinnoh)", SINNOH, "Disc 2/13 - Route 203 (Night).mp3"),
    Track("Ciudad Pirita (Noche) (Sinnoh)", SINNOH, "Disc 2/15 - Oreburgh City (Night).mp3"),
    Track("Ruta 205 (Noche) (Sinnoh)", SINNOH, "Disc 2/16 - Route 205 (Night).mp3"),
    Track("Ciudad Vetusta (Noche) (Sinnoh)", SINNOH, "Disc 2/18 - Eterna City (Night).mp3"),
    Track("Pueblo Flor (Noche) (Sinnoh)", SINNOH, "Disc 2/22 - Floaroma Town (Night).mp3"),
    Track("Pueblo Sosiego (Noche) (Sinnoh)", SINNOH, "Disc 2/24 - Solaceon Town (Night).mp3"),
    Track("Ruta 206 (Noche) (Sinnoh)", SINNOH, "Disc 2/26 - Route 206 (Night).mp3"),
    Track("Ciudad Rocavelo (Noche) (Sinnoh)", SINNOH, "Disc 2/28 - Veilstone City (Night).mp3"),
    Track("Ruta 209 (Noche) (Sinnoh)", SINNOH, "Disc 2/33 - Route 209 (Night).mp3"),
    Track("Ciudad Puntaneva (Noche) (Sinnoh)", SINNOH, "Disc 2/34 - Snowpoint City (Night).mp3"),
    Track("Ruta 216 (Noche) (Sinnoh)", SINNOH, "Disc 2/35 - Route 216 (Night).mp3"),
    Track("Ruta 210 (Noche) (Sinnoh)", SINNOH, "Disc 2/38 - Route 210 (Night).mp3"),
    Track("Ciudad Marina (Noche) (Sinnoh)", SINNOH, "Disc 2/40 - Sunyshore City (Night).mp3"),
    Track("Ciudad Corazón (Noche) (Sinnoh)", SINNOH, "Disc 2/44 - Hearthome City (Night).mp3"),
    Track("Ruta 228 (Noche) (Sinnoh)", SINNOH, "Disc 2/53 - Route 228 (Night).mp3"),
    Track("Ruta 225 (Noche) (Sinnoh)", SINNOH, "Disc 2/57 - Route 225 (Night).mp3"),
    Track("Ruta 206 (oculto) (Sinnoh)", SINNOH, "Disc 4 (Hidden Tracks)/10 - Route 206 [Hidden Track].mp3"),
    Track("Ciudad Vetusta (oculto) (Sinnoh)", SINNOH, "Disc 4 (Hidden Tracks)/13 - Eterna City [Hidden Track].mp3"),
    Track("Ruta 205 (Día, oculto) (Sinnoh)", SINNOH, "Disc 4 (Hidden Tracks)/14 - Route 205 (Day) [Hidden Track].mp3"),
    Track("Ruta 205 (Noche, oculto) (Sinnoh)", SINNOH, "Disc 4 (Hidden Tracks)/15 - Route 205 (Night) [Hidden Track].mp3"),
    Track("Pueblo Primavera (Johto)", JOHTO, "Disc 1/04 - New Bark Town.mp3"),
    Track("Ruta 29 (Johto)", JOHTO, "Disc 1/09 - Route 29.mp3"),
    Track("Ciudad Cerezo (Johto)", JOHTO, "Disc 1/13 - Cherrygrove City.mp3"),
    Track("Ruta 30 (Johto)", JOHTO, "Disc 1/20 - Route 30.mp3"),
    Track("Ciudad Malva (Johto)", JOHTO, "Disc 1/22 - Violet City.mp3"),
    Track("Pueblo Azalea (Johto)", JOHTO, "Disc 1/33 - Azalea Town.mp3"),
    Track("Ruta 34 (Johto)", JOHTO, "Disc 1/36 - Route 34.mp3"),
    Track("Ciudad Trigal (Johto)", JOHTO, "Disc 1/41 - Goldenrod City.mp3"),
    Track("Ciudad Iris (Johto)", JOHTO, "Disc 1/62 - Ecruteak City.mp3"),
    Track("Ruta 38 (Johto)", JOHTO, "Disc 1/68 - Route 38.mp3"),
    Track("Ciudad Orquídea (Johto)", JOHTO, "Disc 1/73 - Cianwood City.mp3"),
    Track("Ruta 42 (Johto)", JOHTO, "Disc 1/75 - Route 42.mp3"),
    Track("Ruta 26 (Kanto)", JOHTO, "Disc 2/01 - Route 26.mp3"),
    Track("Ciudad Carmín (Kanto)", JOHTO, "Disc 2/03 - Vermilion City.mp3"),
    Track("Pueblo Lavanda (Kanto)", JOHTO, "Disc 2/05 - Lavender Town.mp3"),
    Track("Ciudad Celeste (Kanto)", JOHTO, "Disc 2/09 - Cerulean City.mp3"),
    Track("Ruta 24 (Kanto)", JOHTO, "Disc 2/10 - Route 24.mp3"),
    Track("Ciudad Azulona (Kanto)", JOHTO, "Disc 2/14 - Celadon City.mp3"),
    Track("Ruta 11 (Kanto)", JOHTO, "Disc 2/16 - Route 11.mp3"),
    Track("Ciudad Plateada (Kanto)", JOHTO, "Disc 2/20 - Pewter City.mp3"),
    Track("Ruta 3 (Kanto)", JOHTO, "Disc 2/21 - Route 3.mp3"),
    Track("Ruta 1 (Kanto)", JOHTO, "Disc 2/24 - Route 1.mp3"),
    Track("Pueblo Paleta (Kanto)", JOHTO, "Disc 2/25 - Pallet Town.mp3"),
    Track("Ruta 47 (Johto)", JOHTO, "Disc 2/31 - Route 47.mp3"),
    Track("Radio: Ruta 101 (Johto)", JOHTO, "Disc 2/69 - Radio - Route 101.mp3"),
    Track("Radio: Ruta 201 (Johto)", JOHTO, "Disc 2/70 - Radio - Route 201.mp3"),
    Track("Pueblo Primavera (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/04 - New Bark Town [GB Sounds].mp3"),
    Track("Ruta 29 (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/07 - Route 29 [GB Sounds].mp3"),
    Track("Ciudad Cerezo (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/10 - Cherrygrove City [GB Sounds].mp3"),
    Track("Ruta 30 (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/16 - Route 30 [GB Sounds].mp3"),
    Track("Ciudad Malva (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/17 - Violet City [GB Sounds].mp3"),
    Track("Pueblo Azalea (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/23 - Azalea Town [GB Sounds].mp3"),
    Track("Ruta 34 (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/26 - Route 34 [GB Sounds].mp3"),
    Track("Ciudad Trigal (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/30 - Goldenrod City [GB Sounds].mp3"),
    Track("Ciudad Iris (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/41 - Ecruteak City [GB Sounds].mp3"),
    Track("Ruta 38 (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/47 - Route 38 [GB Sounds].mp3"),
    Track("Ruta 42 (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/50 - Route 42 [GB Sounds].mp3"),
    Track("Ruta 26 (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/60 - Route 26 [GB Sounds].mp3"),
    Track("Ciudad Carmín (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/62 - Vermilion City [GB Sounds].mp3"),
    Track("Pueblo Lavanda (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/64 - Lavender Town [GB Sounds].mp3"),
    Track("Ciudad Celeste (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/67 - Cerulean City [GB Sounds].mp3"),
    Track("Ruta 24 (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/68 - Route 24 [GB Sounds].mp3"),
    Track("Ciudad Azulona (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/70 - Celadon City [GB Sounds].mp3"),
    Track("Ruta 11 (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/71 - Route 11 [GB Sounds].mp3"),
    Track("Ciudad Plateada (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/74 - Pewter City [GB Sounds].mp3"),
    Track("Ruta 3 (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/75 - Route 3 [GB Sounds].mp3"),
    Track("Ruta 1 (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/78 - Route 1 [GB Sounds].mp3"),
    Track("Pueblo Paleta (8 bits) (Kanto)", JOHTO, "Disc 3 (GB Sounds)/79 - Pallet Town [GB Sounds].mp3"),
    Track("Ruta 47 (8 bits) (Johto)", JOHTO, "Disc 3 (GB Sounds)/84 - Route 47 [GB Sounds].mp3"),
    Track("Pueblo Arcilla (Teselia)", UNOVA, "Disc 1/07 - Nuvema Town.mp3"),
    Track("Ruta 1 (Teselia)", UNOVA, "Disc 1/14 - Route 1.mp3"),
    Track("Pueblo Terracota (Teselia)", UNOVA, "Disc 1/18 - Accumula Town.mp3"),
    Track("Ruta 2 (Primavera) (Teselia)", UNOVA, "Disc 1/24 - Route 2 (Spring).mp3"),
    Track("Ruta 2 (Verano) (Teselia)", UNOVA, "Disc 1/25 - Route 2 (Summer).mp3"),
    Track("Ciudad Gres (Teselia)", UNOVA, "Disc 1/32 - Striaton City.mp3"),
    Track("Ciudad Esmalte (Teselia)", UNOVA, "Disc 1/43 - Nacrene City.mp3"),
    Track("Ciudad Porcelana (Teselia)", UNOVA, "Disc 1/53 - Castelia City.mp3"),
    Track("Ruta 4 (Primavera) (Teselia)", UNOVA, "Disc 1/55 - Route 4 (Spring).mp3"),
    Track("Ciudad Mayólica (Teselia)", UNOVA, "Disc 1/57 - Nimbasa City.mp3"),
    Track("Ciudad Fayenza (Teselia)", UNOVA, "Disc 2/02 - Driftveil City.mp3"),
    Track("Ruta 6 (Primavera) (Teselia)", UNOVA, "Disc 2/04 - Route 6 (Spring).mp3"),
    Track("Ruta 6 (Verano) (Teselia)", UNOVA, "Disc 2/05 - Route 6 (Summer).mp3"),
    Track("Ciudad Loza (Teselia)", UNOVA, "Disc 2/09 - Mistralton City.mp3"),
    Track("Ciudad Teja (Teselia)", UNOVA, "Disc 2/14 - Icirrus City.mp3"),
    Track("Ruta 4 (Verano) (Teselia)", UNOVA, "Disc 2/18 - Route 4 (Summer).mp3"),
    Track("Ciudad Caolín (Negro) (Teselia)", UNOVA, "Disc 2/24 - Opelucid City (Black).mp3"),
    Track("Ciudad Caolín (Blanco) (Teselia)", UNOVA, "Disc 2/25 - Opelucid City (White).mp3"),
    Track("Ruta 10 (Teselia)", UNOVA, "Disc 2/26 - Route 10.mp3"),
    Track("Ruta 2 (Otoño) (Teselia)", UNOVA, "Disc 3/02 - Route 2 (Autumn).mp3"),
    Track("Ruta 2 (Invierno) (Teselia)", UNOVA, "Disc 3/03 - Route 2 (Winter).mp3"),
    Track("Ruta 4 (Otoño) (Teselia)", UNOVA, "Disc 3/11 - Route 4 (Autumn).mp3"),
    Track("Ruta 6 (Otoño) (Teselia)", UNOVA, "Disc 3/34 - Route 6 (Autumn).mp3"),
    Track("Ruta 6 (Invierno) (Teselia)", UNOVA, "Disc 3/35 - Route 6 (Winter).mp3"),
    Track("Pueblo Biscuit (Teselia)", UNOVA, "Disc 3/37 - Anville Town.mp3"),
    Track("Ruta 12 (Primavera) (Teselia)", UNOVA, "Disc 3/39 - Route 12 (Spring).mp3"),
    Track("Ruta 12 (Verano) (Teselia)", UNOVA, "Disc 3/40 - Route 12 (Summer).mp3"),
    Track("Ciudad Negra (Teselia)", UNOVA, "Disc 3/46 - Black City.mp3"),
    Track("Pueblo Arenisca (Otoño/Invierno/Primavera) (Teselia)", UNOVA, "Disc 3/55 - Undella Town (Autumn-Winter-Spring).mp3"),
    Track("Pueblo Arenisca (Verano) (Teselia)", UNOVA, "Disc 3/56 - Undella Town (Summer).mp3"),
    Track("Ruta 12 (Otoño) (Teselia)", UNOVA, "Disc 4/03 - Route 12 (Autumn).mp3"),
    Track("Ruta 12 (Invierno) (Teselia)", UNOVA, "Disc 4/04 - Route 12 (Winter).mp3"),
    Track("Pueblo Ladrillo (Teselia)", UNOVA, "Disc 4/05 - Lacunosa Town.mp3"),
    Track("Ruta 4 (Invierno) (Teselia)", UNOVA, "Disc 4/08 - Route 4 (Winter).mp3"),
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
    Track("¡Combate! (Pokémon salvaje) [Sinnoh]", SINNOH, "Disc 1/10 - Battle! (Wild Pokémon).mp3"),
    Track("¡Combate! (Entrenador) [Sinnoh]", SINNOH, "Disc 1/21 - Battle! (Trainer).mp3"),
    Track("¡Combate! (Rival) [Sinnoh]", SINNOH, "Disc 1/27 - Battle! (Rival).mp3"),
    Track("¡Combate! (Líder de Gimnasio) [Sinnoh]", SINNOH, "Disc 1/33 - Battle! (Gym Leader).mp3"),
    Track("¡Combate! (Equipo Galaxia) [Sinnoh]", SINNOH, "Disc 1/42 - Battle! (Team Galactic).mp3"),
    Track("¡Combate! (Comandante del Equipo Galaxia) [Sinnoh]", SINNOH, "Disc 1/47 - Battle! (Team Galactic Commander).mp3"),
    Track("¡Combate! (Jefe del Equipo Galaxia) [Sinnoh]", SINNOH, "Disc 1/66 - Battle! (Team Galactic Boss).mp3"),
    Track("¡Combate! (Dialga/Palkia) [Sinnoh]", SINNOH, "Disc 1/72 - Battle! (Dialga-Palkia).mp3"),
    Track("¡Combate! (Azelf/Mesprit/Uxie) [Sinnoh]", SINNOH, "Disc 2/37 - Battle! (Azelf-Mesprit-Uxie).mp3"),
    Track("¡Combate! (Pokémon legendario) [Sinnoh]", SINNOH, "Disc 2/59 - Battle! (Legendary Pokémon).mp3"),
    Track("¡Combate! (Alto Mando) [Sinnoh]", SINNOH, "Disc 2/64 - Battle! (Elite Four).mp3"),
    Track("¡Combate! (Campeón) [Sinnoh]", SINNOH, "Disc 2/67 - Battle! (Champion).mp3"),
    Track("¡Combate! (Giratina) [Sinnoh]", SINNOH, "Disc 3 (Platinum)/13 - Battle! (Giratina).mp3"),
    Track("¡Combate! (As del Frente) [Sinnoh]", SINNOH, "Disc 3 (Platinum)/31 - Battle! (Frontier Brain).mp3"),
    Track("¡Combate! (Regirock/Regice/Registeel) [Sinnoh]", SINNOH, "Disc 3 (Platinum)/33 - Battle! (Regirock-Regice-Registeel).mp3"),
    Track("¡Combate! (Arceus) [Sinnoh]", SINNOH, "Disc 4 (Hidden Tracks)/03 - Battle! (Arceus).mp3"),
    Track("¡Combate! (Líder de Gimnasio) (oculto) [Sinnoh]", SINNOH, "Disc 4 (Hidden Tracks)/04 - Battle! (Gym Leader) [Hidden Track].mp3"),
    Track("¡Combate! (Pokémon salvaje) [Johto]", JOHTO, "Disc 1/10 - Battle! (Wild Pokémon - Johto).mp3"),
    Track("¡Combate! (Entrenador) [Johto]", JOHTO, "Disc 1/18 - Battle! (Trainer - Johto) .mp3"),
    Track("¡Combate! (Team Rocket) [Johto]", JOHTO, "Disc 1/35 - Battle! (Team Rocket).mp3"),
    Track("¡Combate! (Rival) [Johto]", JOHTO, "Disc 1/38 - Battle! (Rival).mp3"),
    Track("¡Combate! (Líder de Gimnasio) [Johto]", JOHTO, "Disc 1/43 - Battle! (Gym Leader - Johto).mp3"),
    Track("¡Combate! (Raikou) [Johto]", JOHTO, "Disc 1/70 - Battle! (Raikou).mp3"),
    Track("¡Combate! (Entei) [Johto]", JOHTO, "Disc 1/85 - Battle! (Entei).mp3"),
    Track("¡Combate! (Ho-Oh) [Johto]", JOHTO, "Disc 1/89 - Battle! (Ho-Oh).mp3"),
    Track("¡Combate! (Líder de Gimnasio) (Kanto) [Johto]", JOHTO, "Disc 2/04 - Battle! (Gym Leader - Kanto).mp3"),
    Track("¡Combate! (Pokémon salvaje) (Kanto) [Johto]", JOHTO, "Disc 2/07 - Battle! (Wild Pokémon - Kanto).mp3"),
    Track("¡Combate! (Suicune) [Johto]", JOHTO, "Disc 2/13 - Battle! (Suicune).mp3"),
    Track("¡Combate! (Entrenador) (Kanto) [Johto]", JOHTO, "Disc 2/29 - Battle! (Trainer - Kanto) .mp3"),
    Track("¡Combate! (As del Frente) [Johto]", JOHTO, "Disc 2/60 - Battle! (Frontier Brain).mp3"),
    Track("¡Combate! (Lugia) [Johto]", JOHTO, "Disc 2/75 - Battle! (Lugia).mp3"),
    Track("¡Combate! (Campeón) [Johto]", JOHTO, "Disc 2/78 - Battle! (Champion).mp3"),
    Track("¡Combate! (Pokémon superancestral) [Johto]", JOHTO, "Disc 2/82 - Battle! (Super-Ancient Pokémon).mp3"),
    Track("¡Combate! (Pokémon salvaje) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/08 - Battle! (Wild Pokémon - Johto) [GB Sounds].mp3"),
    Track("¡Combate! (Campeón) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/105 - Battle! (Champion) [GB Sounds].mp3"),
    Track("¡Combate! (Entrenador) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/14 - Battle! (Trainer - Johto) [GB Sounds].mp3"),
    Track("¡Combate! (Team Rocket) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/25 - Battle! (Team Rocket) [GB Sounds].mp3"),
    Track("¡Combate! (Rival) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/28 - Battle! (Rival) [GB Sounds].mp3"),
    Track("¡Combate! (Líder de Gimnasio) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/32 - Battle! (Gym Leader - Johto) [GB Sounds].mp3"),
    Track("¡Combate! (Raikou/Entei/Suicune) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/56 - Battle! (Raikou-Entei-Suicune) [GB Sounds].mp3"),
    Track("¡Combate! (Líder de Gimnasio) (Kanto) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/63 - Battle! (Gym Leader - Kanto) [GB Sounds].mp3"),
    Track("¡Combate! (Pokémon salvaje) (Kanto) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/66 - Battle! (Wild Pokémon - Kanto) [GB Sounds].mp3"),
    Track("¡Combate! (Entrenador) (Kanto) (8 bits) [Johto]", JOHTO, "Disc 3 (GB Sounds)/82 - Battle! (Trainer - Kanto) [GB Sounds].mp3"),
    Track("¡Combate! (Cheren/Bel) [Teselia]", UNOVA, "Disc 1/08 - Battle! (Cheren-Bianca).mp3"),
    Track("¡Combate! (Pokémon salvaje) [Teselia]", UNOVA, "Disc 1/15 - Battle! (Wild Pokémon).mp3"),
    Track("¡Combate! (Entrenador) [Teselia]", UNOVA, "Disc 1/28 - Battle! (Trainer).mp3"),
    Track("¡Combate! (Equipo Plasma) [Teselia]", UNOVA, "Disc 1/36 - Battle! (Team Plasma).mp3"),
    Track("¡Combate! (Líder de Gimnasio) [Teselia]", UNOVA, "Disc 1/46 - Battle! (Gym Leader).mp3"),
    Track("¡Combate! (N) [Teselia]", UNOVA, "Disc 1/61 - Battle! (N).mp3"),
    Track("¡Combate! (Alto Mando) [Teselia]", UNOVA, "Disc 2/30 - Battle! (Elite Four).mp3"),
    Track("¡Combate! (Reshiram/Zekrom) [Teselia]", UNOVA, "Disc 2/40 - Battle! (Reshiram-Zekrom).mp3"),
    Track("¡Combate! (Gueti) [Teselia]", UNOVA, "Disc 2/43 - Battle! (Ghetsis).mp3"),
    Track("¡Combate! (Entrenador del Metro de Combate) [Teselia]", UNOVA, "Disc 3/14 - Battle! (Battle Subway Trainer).mp3"),
    Track("¡Combate! (Pokémon legendario) [Teselia]", UNOVA, "Disc 3/36 - Battle! (Legendary Pokémon).mp3"),
    Track("¡Combate! (Cynthia) [Teselia]", UNOVA, "Disc 3/58 - Battle! (Cynthia).mp3"),
    Track("¡Combate! (Pokémon salvaje fuerte) [Teselia]", UNOVA, "Disc 3/60 - Battle! (Strong Wild Pokémon).mp3"),
    Track("¡Combate! (Kyurem) [Teselia]", UNOVA, "Disc 4/12 - Battle! (Kyurem).mp3"),
    Track("¡Combate! (Campeón) [Teselia]", UNOVA, "Disc 4/17 - Battle! (Champion).mp3"),
)

/** Música de **APERTURA DE SOBRES**: el jingle de evolución (Kanto + Hoenn) suena al rasgar sobres. */
private val PACK_OPENING_PLAYLIST = listOf(
    Track("Evolución (Kanto)", KANTO, "Disc 1/33 - Evolution.mp3"),
    Track("Evolución (Hoenn)", HOENN, "Disc 2/12 - Evolution.mp3"),
    Track("Evolución (Sinnoh)", SINNOH, "Disc 1/49 - Evolution.mp3"),
    Track("Evolución (Johto)", JOHTO, "Disc 1/39 - Evolution.mp3"),
    Track("Evolución (Teselia)", UNOVA, "Disc 1/38 - Evolution.mp3"),
)

private const val MUSIC_VOLUME = 1f

/** Es de día entre las 06:00 y las 19:59 (hora local del dispositivo); si no, es de noche. */
private fun isDaytime(): Boolean = java.time.LocalTime.now().hour in 6..19

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
class MenuMusicController internal constructor(
    context: Context,
    private val tracks: List<Track>,
    /** Si true, filtra por franja horaria: de día solo suenan temas (Day), de noche solo (Night);
     *  los temas sin variante día/noche suenan siempre. Solo para la ambientación del menú. */
    private val filterDayNight: Boolean = false,
) {
    private val appContext = context.applicationContext
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _title = MutableStateFlow<String?>(null)
    /** Título del tema que SUENA ahora (null mientras carga o cuando está en silencio). */
    val title: StateFlow<String?> = _title

    private var player: MediaPlayer? = null
    private var lastIndex = -1
    private var generation = 0
    private var released = false

    /** Bolsa barajada: cola de índices pendientes. Garantiza oír TODA la playlist antes de repetir. */
    private val bag = ArrayDeque<Int>()

    private var screenAllows = false
    private var foreground = true

    /**
     * La pantalla actual PERMITE música (menú) o no (combate / apertura de sobres). Al ENTRAR a una
     * pantalla con música (empezar un combate, volver al menú tras combatir) SIEMPRE arranca un tema
     * NUEVO; al salir, se pausa. El paso a segundo plano/primer plano NO reinicia (eso lo hace
     * [setForeground], que reanuda el mismo tema).
     */
    fun setScreenAllows(allow: Boolean) {
        if (screenAllows == allow) return
        screenAllows = allow
        if (released) return
        if (allow && foreground) startNext() else pauseKeeping()
    }

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

    /**
     * Siguiente índice tomado de una **bolsa barajada**: suena toda la playlist en orden aleatorio
     * antes de repetir cualquier tema. Al vaciarse se rebaraja (evitando empezar por el último oído,
     * para que no suene dos veces seguidas en la frontera entre barajadas).
     */
    private fun nextIndex(): Int {
        if (tracks.size == 1) return 0
        if (bag.isEmpty()) {
            // Franja horaria: de día descarta los temas (Night) y viceversa; los temas sin variante
            // día/noche siempre entran. Se evalúa al rellenar la bolsa, así se adapta al pasar el día.
            val pool = tracks.indices.filter { dayNightOk(tracks[it]) }.ifEmpty { tracks.indices.toList() }
            val order = pool.shuffled().toMutableList()
            if (order.size > 1 && order.first() == lastIndex) { order[0] = order[1].also { order[1] = order[0] } }
            bag.addAll(order)
        }
        return bag.removeFirst()
    }

    /** ¿El tema casa con la franja horaria actual? (sin variante día/noche → siempre sí). */
    private fun dayNightOk(t: Track): Boolean {
        if (!filterDayNight) return true
        val day = t.path.contains("(Day)")
        val night = t.path.contains("(Night)")
        if (!day && !night) return true
        return if (isDaytime()) day else night
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
        prefetchNext(gen)
    }

    /** Descarga por adelantado el siguiente tema de la bolsa, para que el cambio sea fluido offline. */
    private fun prefetchNext(gen: Int) {
        val next = bag.firstOrNull() ?: return
        io.launch {
            if (!released && gen == generation) MusicCache.ensure(appContext, tracks[next])
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

/** Crea el reproductor de música de MENÚ (ambientación multirregión, filtrada por día/noche). */
@Composable
fun rememberMenuMusic(): MenuMusicController = rememberMusic(MENU_PLAYLIST, filterDayNight = true)

/** Crea el reproductor de música de COMBATE (temas de batalla de Kanto + Hoenn). */
@Composable
fun rememberBattleMusic(): MenuMusicController = rememberMusic(BATTLE_PLAYLIST)

/** Crea el reproductor de música de APERTURA DE SOBRES (jingle de evolución Kanto + Hoenn). */
@Composable
fun rememberPackOpeningMusic(): MenuMusicController = rememberMusic(PACK_OPENING_PLAYLIST)

/** Crea un controlador ligado a la composición y al ciclo de vida; lo libera al salir. */
@Composable
private fun rememberMusic(tracks: List<Track>, filterDayNight: Boolean = false): MenuMusicController {
    val ctx = LocalContext.current
    val controller = remember { MenuMusicController(ctx, tracks, filterDayNight) }
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
