"""Interfaz de línea de comandos de tcgtools.

    python -m tcgtools deploy                 # build :app:assembleDebug + adb install
    python -m tcgtools play <script.yaml>     # reproduce un combate y captura checkpoints
    python -m tcgtools capture [--count N]     # bucle de screencap suelto
    python -m tcgtools compare --run <dir> --refs <dir>   # compara y genera reporte
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

import click

from . import APP_ID
from . import annotate as annotate_mod
from . import layoutdiff
from .adb import Adb, AdbError
from .capture import capture_session, new_run_dir, screencap_loop
from .compare import compare_pairs
from .device import DeviceController
from .measure import measure_to_file
from .report import write_reports
from .timeline import run_script

# tools/tcgtools/cli.py -> repo root son 3 niveles arriba.
REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_ROOT = REPO_ROOT / "tools" / "out"
DEFAULT_REFS = REPO_ROOT / "referencias"
LIVE_REFS = REPO_ROOT / "referencias_live"
REFSPEC_DIR = REPO_ROOT / "tools" / "refspec"
DEFAULT_SPEC = REFSPEC_DIR / "board_start.json"
DEFAULT_REF_IMG = REFSPEC_DIR / "board_start_ref.png"

# Package de la app oficial Pokémon TCG Live (fuente de verdad para referencias).
TCGL_PKG = "com.pokemon.pokemontcgl"


@click.group()
def cli() -> None:
    """Automatización y verificación visual 1:1 del clon de TCG Live."""


@cli.command()
def deploy() -> None:
    """Ensambla el APK debug e instálalo (porta _refcap/deploy.ps1)."""
    java_home = os.environ.get(
        "JAVA_HOME", r"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
    )
    env = {**os.environ, "JAVA_HOME": java_home}
    gradlew = REPO_ROOT / "gradlew.bat"
    click.echo(f"-> build :app:assembleDebug (JAVA_HOME={java_home})")
    r = subprocess.run(
        [str(gradlew), "-p", str(REPO_ROOT), ":app:assembleDebug", "-q"], env=env
    )
    if r.returncode != 0:
        raise click.ClickException("BUILD FAILED")
    apk = REPO_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    Adb().run("install", "-r", str(apk))
    click.echo("INSTALLED")


@cli.command()
@click.argument("script", type=click.Path(exists=True, path_type=Path))
@click.option("--serial", default=None, help="Serial del dispositivo (si hay varios).")
def play(script: Path, serial: str | None) -> None:
    """Reproduce un timeline YAML y guarda los checkpoints capturados."""
    dev = DeviceController(Adb(serial=serial), app_id=APP_ID)
    run_dir = new_run_dir(OUT_ROOT)
    click.echo(f"-> run: {run_dir}")
    result = run_script(dev, script, run_dir)

    manifest = [
        {"name": c.name, "shot": c.shot.name, "ref": c.ref}
        for c in result.checkpoints
    ]
    (run_dir / "checkpoints.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    click.echo(f"[OK] {len(manifest)} checkpoints capturados. Ahora: compare --run {run_dir.name}")


@cli.command()
@click.option("--app", default=TCGL_PKG, show_default=True, help="Package a grabar (por defecto TCG Live oficial).")
@click.option("--launch/--no-launch", default=False, help="Relanzar la app antes de grabar.")
@click.option("--seconds", default=120.0, show_default=True, help="Duración de la sesión.")
@click.option("--interval", default=1.0, show_default=True, help="Segundos entre capturas.")
@click.option("--min-sharpness", default=60.0, show_default=True, help="Umbral de nitidez (Laplaciano); 0 = no filtrar.")
@click.option("--name", default=None, help="Nombre del subdirectorio destino en referencias_live/.")
@click.option("--serial", default=None)
def refcap(app, launch, seconds, interval, min_sharpness, name, serial):
    """Graba una sesión de la app OFICIAL TCG Live como set de referencia 1:1.

    Toma screencaps PNG lossless a la resolución nativa del teléfono mientras
    juegas, descartando frames borrosos/transición. Juega el combate durante la
    ventana de grabación; los frames nítidos quedan en referencias_live/<name>/.
    """
    from datetime import datetime

    adb = Adb(serial=serial)
    if launch:
        adb.force_stop(app)
        import time as _t

        _t.sleep(1)
        adb.launch(app)
        click.echo(f"-> lanzada {app}; esperando 12s a que cargue…")
        _t.sleep(12)

    sub = name or f"live_{datetime.now():%Y%m%d_%H%M%S}"
    out_dir = LIVE_REFS / sub
    click.echo(f"-> grabando {seconds:.0f}s cada {interval:.1f}s en {out_dir} (¡juega ahora!)")
    kept = capture_session(
        adb, out_dir, duration=seconds, interval=interval,
        min_sharpness=min_sharpness, prefix="ref",
    )
    click.echo(f"[OK] {len(kept)} frames nítidos guardados en {out_dir}")


@cli.command()
@click.option("--count", default=10, help="Número de capturas.")
@click.option("--interval", default=1.0, help="Segundos entre capturas.")
@click.option("--serial", default=None)
def capture(count: int, interval: float, serial: str | None) -> None:
    """Bucle de screencap suelto (sin guion)."""
    run_dir = new_run_dir(OUT_ROOT)
    frames = screencap_loop(Adb(serial=serial), run_dir, count=count, interval=interval)
    click.echo(f"[OK] {len(frames)} capturas en {run_dir}")


@cli.command()
@click.option("--run", "run_name", required=True, help="Nombre del run dentro de tools/out/.")
@click.option("--refs", type=click.Path(path_type=Path), default=DEFAULT_REFS, show_default=True)
@click.option("--ssim-pass", default=0.98, show_default=True)
def compare(run_name: str, refs: Path, ssim_pass: float) -> None:
    """Compara los checkpoints de un run contra los frames de referencia."""
    run_dir = OUT_ROOT / run_name if not Path(run_name).is_absolute() else Path(run_name)
    manifest_path = run_dir / "checkpoints.json"
    if not manifest_path.exists():
        raise click.ClickException(f"No hay checkpoints.json en {run_dir}. ¿Corriste 'play'?")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    pairs: list[tuple[Path, Path]] = []
    for c in manifest:
        if not c.get("ref"):
            continue  # checkpoints tipo 'capture' sin referencia asociada
        shot = run_dir / c["shot"]
        matches = sorted(refs.glob(f"{c['ref']}*"))
        if not matches:
            click.echo(f"  [WARN] sin referencia para {c['name']} (patrón {c['ref']}*)", err=True)
            continue
        pairs.append((matches[0], shot))

    if not pairs:
        raise click.ClickException("No se pudo emparejar ningún checkpoint con una referencia.")

    results = compare_pairs(pairs, run_dir, ssim_pass=ssim_pass)
    json_path, html_path = write_reports(results, run_dir, refs_dir=refs)
    passed = sum(1 for r in results if r.passed)
    click.echo(f"[OK] {passed}/{len(results)} PASS · reporte: {html_path}")


@cli.command()
@click.option("--spec", type=click.Path(path_type=Path), default=DEFAULT_SPEC, show_default=True)
@click.option("--ref", "ref_img", type=click.Path(path_type=Path), default=DEFAULT_REF_IMG, show_default=True)
@click.option("--out", "out_path", type=click.Path(path_type=Path), default=None,
              help="PNG de salida (por defecto refspec/board_start_overlay.png).")
def annotate(spec: Path, ref_img: Path, out_path: Path | None) -> None:
    """Dibuja las cajas del spec sobre el frame de referencia para validarlas a ojo.

    Bucle de anotacion: edita refspec/board_start.json, corre esto, abre el overlay,
    ajusta y repite hasta que cada caja encuadre su elemento en el frame real.
    """
    if not spec.exists():
        raise click.ClickException(f"No existe el spec {spec}. Crea refspec/board_start.json.")
    out = out_path or (REFSPEC_DIR / "board_start_overlay.png")
    spec_data = annotate_mod.load_spec(spec)
    result = annotate_mod.render_overlay(spec_data, ref_img, out)
    click.echo(f"[OK] overlay -> {result} ({len(spec_data['elements'])} cajas)")


@cli.command()
@click.option("--run", "run_name", required=True, help="Run dentro de tools/out/ donde guardar la medicion.")
@click.option("--serial", default=None, help="Serial del dispositivo (si hay varios).")
def measure(run_name: str, serial: str | None) -> None:
    """Mide el layout REAL del clon en el dispositivo (bounds por testTag)."""
    run_dir = OUT_ROOT / run_name if not Path(run_name).is_absolute() else Path(run_name)
    run_dir.mkdir(parents=True, exist_ok=True)
    adb = Adb(serial=serial)
    dev = DeviceController(adb, app_id=APP_ID)
    layout = measure_to_file(adb, dev, run_dir / "measured_layout.json")
    n = layout["meta"]["n"]
    click.echo(f"[OK] {n} elementos medidos -> {run_dir / 'measured_layout.json'}")
    if n == 0:
        click.echo("[WARN] 0 elementos: verifica testTagsAsResourceId y que el tablero este visible.", err=True)


@cli.command(name="layout-compare")
@click.option("--run", "run_name", required=True, help="Run con measured_layout.json.")
@click.option("--spec", type=click.Path(path_type=Path), default=DEFAULT_SPEC, show_default=True)
@click.option("--ref", "ref_img", type=click.Path(path_type=Path), default=DEFAULT_REF_IMG, show_default=True)
@click.option("--tol-center", default=0.01, show_default=True)
@click.option("--tol-iou", default=0.95, show_default=True)
def layout_compare(run_name: str, spec: Path, ref_img: Path, tol_center: float, tol_iou: float) -> None:
    """Compara caja-a-caja el clon medido contra el spec de referencia (posicional 1:1)."""
    run_dir = OUT_ROOT / run_name if not Path(run_name).is_absolute() else Path(run_name)
    measured_path = run_dir / "measured_layout.json"
    if not measured_path.exists():
        raise click.ClickException(f"No hay measured_layout.json en {run_dir}. Corre 'measure' primero.")
    ref_spec = json.loads(spec.read_text(encoding="utf-8"))
    measured = json.loads(measured_path.read_text(encoding="utf-8"))
    diffs = layoutdiff.compare_layout(ref_spec, measured, tol_center=tol_center, tol_iou=tol_iou)
    json_path, html_path = layoutdiff.write_reports(
        diffs, run_dir, ref_img, tol_center=tol_center, tol_iou=tol_iou
    )
    s = layoutdiff.summarize(diffs)
    click.echo(f"[OK] {s['passed']}/{s['elements']} PASS · IoU medio {s['avg_iou']} · reporte: {html_path}")
    if not s["all_pass"]:
        click.echo("    Elementos fuera de tolerancia (ver 'caja sugerida' en el reporte):")
        for d in diffs:
            if not d.passed and d.suggested:
                click.echo(f"      {d.name}: dcenter={d.dcenter} iou={d.iou} -> {d.suggested}")


def main() -> None:
    # La consola de Windows suele ser cp1252 y no puede imprimir caracteres no-latin1.
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
        except (AttributeError, ValueError):
            pass
    try:
        cli()
    except AdbError as e:
        click.echo(f"ADB: {e}", err=True)
        sys.exit(2)


if __name__ == "__main__":
    main()
