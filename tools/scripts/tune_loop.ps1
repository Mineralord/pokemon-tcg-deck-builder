# Bucle cerrado de ajuste posicional 1:1 (arranque de tablero post-JUGAR).
#
# Encadena: deploy -> play (navega al tablero) -> measure (bounds reales por tag)
# -> layout-compare (caja a caja contra refspec/board_start.json) y abre el reporte.
#
# Iteracion: si hay elementos FAIL, el reporte sugiere la caja corregida; aplica esos
# valores en feature/.../board/BoardGeometry.kt (NBox) y refspec/board_start.json,
# y vuelve a correr este script. Criterio de parada: TODOS los elementos PASS
# (dcenter < TolCenter, IoU >= TolIou).
#
#   powershell tools\scripts\tune_loop.ps1 [-Serial <serial>] [-SkipDeploy]
param(
  [string]$Serial = "",
  [switch]$SkipDeploy,
  [double]$TolCenter = 0.01,
  [double]$TolIou = 0.95
)
$ErrorActionPreference = "Stop"
$toolsDir = Split-Path -Parent $PSScriptRoot          # ...\tools
$py = Join-Path $toolsDir ".venv\Scripts\python.exe"
if (-not (Test-Path $py)) { throw "No existe el venv: $py (crea con: py -m venv .venv; pip install -e .)" }
$serialArg = @(); if ($Serial) { $serialArg = @("--serial", $Serial) }

# 0) Verifica que haya un dispositivo conectado.
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$devs = (& $adb devices) | Select-String "\tdevice$"
if (-not $devs) { throw "No hay dispositivo ADB conectado. Conecta el telefono (depuracion USB) y reintenta." }

if (-not $SkipDeploy) {
  Write-Host "== deploy =="
  & $py -m tcgtools deploy
  if ($LASTEXITCODE -ne 0) { throw "deploy fallo" }
}

Write-Host "== play (navegar al tablero) =="
& $py -m tcgtools play (Join-Path $toolsDir "scripts\combat_1.yaml") @serialArg
if ($LASTEXITCODE -ne 0) { throw "play fallo" }

# Run mas reciente creado por 'play'.
$run = Get-ChildItem (Join-Path $toolsDir "out") -Directory -Filter "run_*" |
  Sort-Object Name -Descending | Select-Object -First 1
if (-not $run) { throw "No se encontro run_* en tools\out" }
Write-Host "-> run: $($run.Name)"

Write-Host "== measure (bounds reales del clon) =="
& $py -m tcgtools measure --run $run.Name @serialArg
if ($LASTEXITCODE -ne 0) { throw "measure fallo" }

Write-Host "== layout-compare (posicional 1:1) =="
& $py -m tcgtools layout-compare --run $run.Name --tol-center $TolCenter --tol-iou $TolIou

$report = Join-Path $run.FullName "layout_report.html"
if (Test-Path $report) {
  Write-Host "Reporte: $report"
  Start-Process $report
}
