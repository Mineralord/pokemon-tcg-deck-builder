# Pokémon TCG Live — Clon nativo (Android)

Proyecto nativo Android (Kotlin / Jetpack Compose / Clean Architecture / MVI),
**en paralelo** a la PWA web del repo raíz. Blueprint completo en
`~/.claude/plans/quiero-que-shimmering-sutherland.md`.

## Estado (Fase 1, en curso)

Módulos ya andamiados (Kotlin puro JVM — compilables/testeables **sin Android SDK**):

| Módulo | Contenido |
|--------|-----------|
| `:engine:model` | Modelo hiper-categorizado: `Card` (Pokémon/Entrenador/Energía), `PokemonMechanic` (ex/V/VMAX/VSTAR/GX/Tera), DSL de efectos (`EffectOp`), `GameState` inmutable. |
| `:engine:events` | `GameEvent` sellado (stream para log+animaciones+IA) + `CombatLog` (renderizado ES/EN del "Registro del Combate"). |

Pendiente (siguientes iteraciones): `:engine:effects` (intérprete), `:engine:rules`
(máquina de fases + turn-loop), `:data:*`, `:core:*`, `:feature:*`, `:app`,
`assets-pipeline`.

## Requisitos de toolchain (NO instalados en esta máquina)

Para compilar/ejecutar hace falta:
1. **JDK 17+** (Temurin/Adoptium recomendado). Define `JAVA_HOME`.
2. **Android SDK** (vía Android Studio o command-line tools). Define `ANDROID_HOME`.
   - Necesario solo para los módulos Android (`:app`, `:feature:*`, `:core:ui`…),
     **no** para los módulos `:engine:*`.
3. **Gradle**: se usa el *wrapper* (`./gradlew`). Genéralo una vez con un Gradle
   global instalado: `gradle wrapper --gradle-version 8.11`.

## Compilar / testear el motor (solo JDK, sin Android SDK)

```bash
cd android
./gradlew :engine:model:test :engine:events:test
```

Los módulos `:engine:*` son JVM puros: validan reglas a velocidad de unidad y
permiten un arnés de self-play headless (IA vs IA) en CI.
