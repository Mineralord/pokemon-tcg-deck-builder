# Napkin — Pokémon TCG (Colección y Generador de Mazos)

Runbook curado. Solo guía recurrente de alto valor.

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
