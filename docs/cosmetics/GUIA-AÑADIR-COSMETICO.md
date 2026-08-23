# Guía — Añadir un cosmético (sin tocar código)

El sistema de cosméticos es **data-driven**: un cosmético nuevo se añade con **metadata JSON**
(y su asset si no es procedural), sin escribir Kotlin ni recompilar lógica. Esta guía permite
a cualquiera ampliar la biblioteca sin entender todo el proyecto.

## Dónde viven los datos

```
data/cosmetics/src/main/resources/cosmetics/
  index.json        # lista los packs que componen el catálogo
  core.json         # cosméticos base (procedurales, GREEN)
  restricted.json   # SOLO metadata de assets no distribuibles (sin binarios)
```

El catálogo lo carga `CosmeticRepository.load()` (clase `Cosmetics.repo`), igual que
`CardRepository` carga las cartas.

## Paso a paso

### 1. Elige el pack (o crea uno)
Añade el ítem a un pack existente (`core.json`) o crea `mi-pack.json` y regístralo en
`index.json`:
```json
{ "packs": ["core.json", "restricted.json", "mi-pack.json"] }
```

### 2. Escribe la metadata del cosmético
Añade una entrada al array `items` del pack:
```json
{
  "id": "avatar-glaciar",
  "category": "AVATAR",
  "rarity": "EPICO",
  "name": "Glaciar",
  "renderer": "avatar",
  "colors": ["0xFF80D8FF", "0xFF0277BD"]
}
```

### Campos
| Campo | Obligatorio | Valores |
|---|---|---|
| `id` | ✅ | **Estable, único, no cambia nunca.** Recomendado jerárquico: `avatar-glaciar` o `cosmetic.sleeve.type.fire.001` |
| `category` | ✅ | `AVATAR`, `MARCO`, `FONDO`, `TAPETE`, `FUNDA`, `CAJA`, `MONEDA`, `BADGE`, `EMOTE`, `VICTORIA`, `DERROTA` |
| `rarity` | — | `COMUN`, `POCO_COMUN`, `RARO`, `EPICO`, `LEGENDARIO`, `MITICO` (default `COMUN`) |
| `name` | ✅ | Nombre mostrado |
| `colors` | — | Paleta ARGB como hex `"0xFFRRGGBB"` (para renderizadores procedurales) |
| `prestige` | — | `true` → precio ×3 (§14.5) |
| `status` | — | `ACTIVE` (default), `AVAILABLE`, `EXPERIMENTAL`, `ARCHIVED`, `LEGACY`, `RESTRICTED` |
| `license` | — | `GREEN` (default), `YELLOW`, `RED`, `ARCHIVE_ONLY` |
| `assetType` | — | `PROCEDURAL` (default), `IMAGE`, `LOTTIE`, `AUDIO`, `MODEL3D` |
| `renderer` | — | id del renderizador procedural (`avatar`, `marco`, `fondo`, `mat.theme`, `sleeve`, `caja`) |
| `assetRef` | — | ruta/URL del binario (solo si `assetType` ≠ PROCEDURAL) |
| `priceOverride` | — | precio explícito en Monedas; si falta, se deriva de la rareza (§14.3) |
| `source`, `author`, `originalUrl`, `sourceRepository`, `acquisitionMethod`, `unlockMethod`, `collection`, `subcategory`, `animated`, `releaseDate`, `assetVersion`, `metadataVersion` | — | trazabilidad / versionado |

### 3. Si es procedural (recomendado, GREEN)
No hace falta ningún archivo de imagen. El `renderer` indica cómo dibujarlo:
- `avatar`/`marco`/`fondo`/`caja` → previsualización por paleta (`app/Store.kt`).
- `mat.theme` (TAPETE) → tema del tablero en `feature:combat/CosmeticThemes.kt`
  (`matThemeFor`). Para un aspecto en partida distinto del genérico, añade el mapeo del `id`.
- `sleeve` (FUNDA) → dorso procedural en `feature:combat/CosmeticThemes.kt` (`sleeveFor`).

### 4. Si usa un asset (GREEN/CC0 o YELLOW/CC-BY)
1. Registra la fuente en `docs/cosmetics/ASSET-LEDGER.md` con su licencia.
2. Coloca el binario donde toque (Coil para imágenes remotas → `assetRef` = URL; o `res`).
3. Pon `assetType` (`IMAGE`/`AUDIO`/`LOTTIE`), `assetRef`, `license`, `author`, `originalUrl`.
4. **Nunca** empaquetes RED/ARCHIVE-ONLY (ver ledger). Esos se catalogan como `RESTRICTED`
   sin binario (`assetRef: null`).

### 5. Verifica
```
./gradlew :data:cosmetics:test :app:assembleDebug
```
El test valida carga, ids estables y filtros. En el APK: Tienda → Cosméticos → tu categoría
→ debe aparecer, comprarse y equiparse.

## Reglas de oro
- **Los `id` no cambian jamás** (los saves y la nube los referencian). Para mejorar el arte,
  sube `assetVersion`; no reutilices un id para otra cosa (§29/§30).
- **Nada se borra**: para retirar algo, cámbialo a `ARCHIVED`/`LEGACY` (Museo/Legado §22).
- **Solo GREEN se empaqueta.** Ante duda de licencia, no lo incluyas.
