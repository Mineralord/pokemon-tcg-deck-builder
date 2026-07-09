# tcgtools — automatización + verificación visual 1:1

Herramientas Python para (1) controlar el dispositivo vía ADB y reproducir un
combate guionizado, (2) capturar fotogramas y (3) compararlos pixel-a-pixel contra
los fotogramas de referencia de Pokémon TCG Live, emitiendo un reporte de
discrepancias.

## Instalación

```powershell
cd tools
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e .
python -m tcgtools --help
```

Requisitos: Python ≥ 3.9 y `adb` accesible. tcgtools busca `adb.exe` en
`TCG_ADB`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, `%LOCALAPPDATA%\Android\Sdk\platform-tools`
o el PATH.

## Comandos

| Comando | Qué hace |
|---------|----------|
| `python -m tcgtools refcap --launch --seconds 180 --name combate1` | Graba la app **oficial** TCG Live como referencia 1:1 (PNG lossless nativo, filtra borrosos). Juega mientras graba. Salida en `referencias_live/<name>/`. |
| `python -m tcgtools deploy` | `gradlew :app:assembleDebug` + `adb install -r` (porta `_refcap/deploy.ps1`). |
| `python -m tcgtools play tools/scripts/combat_1.yaml` | Reproduce el timeline y captura los checkpoints en `tools/out/run_<ts>/`. |
| `python -m tcgtools capture --count 10 --interval 1` | Bucle de screencap suelto. |
| `python -m tcgtools compare --run run_<ts> --refs referencias` | Compara los checkpoints con los frames de referencia (SSIM de píxel, métrica secundaria) y genera `report.html` + `report.json`. |
| `python -m tcgtools annotate` | Dibuja las cajas de `refspec/board_start.json` sobre el frame canónico → `refspec/board_start_overlay.png` (bucle de anotación visual). |
| `python -m tcgtools measure --run run_<ts>` | Mide las cajas **reales** del clon en el dispositivo (por `content-desc="tt:<tag>"`) → `measured_layout.json`. |
| `python -m tcgtools layout-compare --run run_<ts>` | Compara **posición 1:1** caja a caja contra `refspec/board_start.json` → `layout_report.html/json` + overlay. |

## Fidelidad POSICIONAL 1:1 (arranque de tablero)

Meta acordada: no "100% de píxeles" (inalcanzable — el clon usa vectores/Coil y TCG
Live render 3D real), sino que **cada elemento coincida en caja normalizada** dentro de
tolerancia (centro < 1%, IoU ≥ 0.95). El contrato vive en `refspec/board_start.json`
(espejo de `feature/game/.../board/BoardGeometry.kt` → `NBox`).

```powershell
# Bucle completo (deploy -> play -> measure -> layout-compare -> abre reporte):
powershell tools\scripts\tune_loop.ps1
```

Iteración: si hay elementos FAIL, el reporte muestra la **caja sugerida**; aplícala en
`BoardGeometry.kt` (y sincroniza `board_start.json`) y repite hasta que todos pasen.
Refina el spec de referencia a ojo con `annotate` viendo el overlay sobre el frame real.

## Flujo típico

Referencia contra la **app oficial** TCG Live (fuente de verdad, misma resolución
del teléfono, sin frames borrosos):

```powershell
# 1) Graba un combate real en TCG Live (juega tú durante la ventana)
python -m tcgtools refcap --launch --seconds 180 --name combate1
# 2) Despliega y reproduce el mismo estado en el clon
python -m tcgtools deploy
python -m tcgtools play tools/scripts/combat_1.yaml
# 3) Compara clon vs referencia real
python -m tcgtools compare --run run_<ts> --refs referencias_live/combate1
```

El set histórico `referencias/` (frames pre-extraídos con ffmpeg) contiene muchos
frames oscuros/borrosos de transición; **prefiere `referencias_live/`** capturado
con `refcap`.

Abre `tools/out/run_.../report.html` para ver referencia · captura · mapa de calor
de diferencias, con SSIM, % de píxeles distintos y offsets por checkpoint.

## Timelines (`scripts/*.yaml`)

Lista de pasos (`op`): `relaunch`, `tap_text`, `tap_xy`, `wait`, `capture`,
`assert_screen`. `assert_screen` captura y asocia el checkpoint a un frame de
referencia (campo `ref`, resuelto como prefijo dentro de `--refs`). Las anclas de
navegación son los textos reales en español de la UI (`JUGAR`, `Activo`,
`¡COMBATIR!`); si la UI cambia, actualiza el YAML.

## Salidas

Todo va a `tools/out/` (gitignoreado). El código de `tools/` sí se versiona.

## Módulos

- `adb.py` — wrapper de `adb.exe`.
- `device.py` — `DeviceController`: relanzar app, leer bounds del volcado UI, tap por texto.
- `capture.py` — screencap en bucle / screenrecord + extracción de frames con OpenCV (sin ffmpeg).
- `timeline.py` — runner de guiones YAML.
- `compare.py` — SSIM + pixel-diff + template matching.
- `report.py` — reporte JSON + HTML.
- `sample_colors.py` — muestreo de color de regiones para pixel-lock de `TcgColors`/`BattleTheme`.
