# Kit de porteo de efectos (desde la web jubilada)

Material de referencia, ya **validado**, para llevar el motor de efectos del APK
al 100% de cobertura por carta. Proviene de la versión web (PWA) que fue retirada
el 2026-06-25. La UI/PWA/Firebase/multijugador de la web se eliminó; esto es lo
único que valía la pena conservar.

## Qué hay aquí

| Archivo | Qué es |
|---|---|
| `efectos-db.js` | **Efectos autorados por carta** (49 cartas): ataques, habilidades, entrenadores (`jugar`) y pasivos (estadios/herramientas). Es el oro: no reinventar. |
| `efectos-dsl.md` | Spec del DSL de efectos (catálogo de operaciones). |
| `efectos-dsl.js` | Gramática + validador del DSL. |
| `efectos-motor.js` | Intérprete que ejecuta los programas de efecto. |
| `efectos-pasivos.js` | Resolución de modificadores pasivos. |
| `cobertura-efectos.js` | Reporte de cobertura (autorado / solo-daño / pendiente). |
| `replica-tcglive.md` | Notas de fidelidad 1:1 con Pokémon TCG Live. |
| `tests/` | 14 suites: reglas del juego (`reglas*.test.js`) + efectos. Comportamiento esperado. |

## Mapa web (JS) → APK (Kotlin)

- `efectos-dsl.js`  → `engine/model/Effects.kt` (catálogo cerrado, ya existe).
- `efectos-motor.js` → `engine/effects/EffectInterpreter.kt`.
- reglas de `tests/reglas*.test.js` → `engine/rules/GameEngine.kt` + `Damage.kt`.
- `efectos-db.js` → falta el **EffectRegistry** que enlace `EffectId(cardId)` →
  programa de efecto. Hoy `CardMapper.kt` crea ataques/habilidades con
  `effect = null` ("se enlazará al EffectRegistry en fase de efectos").

## Cobertura conocida al momento del corte (web)

- Ataques: 12 autorados + 193 solo-daño = ~32% sin manual. **428 pendientes.**
- Habilidades: 15 autoradas / 68 pendientes.
- Entrenadores: 22 autorados / 51 pendientes.

> Nota: `cobertura-efectos.js` usa rutas relativas (`../data/...`, `../js/...`)
> de la estructura web original; aquí son solo referencia de lógica, no se
> ejecutan tal cual.

## Reglas oficiales

`RULEBOOK.pdf` (oficial, ~50 MB) vive en la carpeta padre
`C:\DOCUMENTOS\POKÉMON TCG\` — fuera del repo, intacto.
