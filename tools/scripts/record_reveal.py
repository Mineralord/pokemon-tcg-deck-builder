"""Graba el REVELADO inicial (rival voltea sus Pokemon + entran premios).

Navega: JUGAR -> CARA -> (giro+resultado+reparto) -> preparacion -> elige Activo ->
graba mientras pulsa ¡COMBATIR! (= Listo). El revelado dura ~2s tras confirmar.

Uso:  python scripts/record_reveal.py [serial]
"""
from __future__ import annotations

import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tcgtools import APP_ID
from tcgtools.adb import Adb
from tcgtools.capture import extract_frames, new_run_dir, screenrecord
from tcgtools.device import DeviceController

SERIAL = sys.argv[1] if len(sys.argv) > 1 else None
OUT_ROOT = Path(__file__).resolve().parents[1] / "out"

adb = Adb(serial=SERIAL)
dev = DeviceController(adb, app_id=APP_ID)
run_dir = new_run_dir(OUT_ROOT)
print(f"-> run: {run_dir}")

# 1) Hasta la eleccion de moneda.
dev.relaunch(settle=3.5)
dev.tap_text("JUGAR", fallback=(540, 1469), settle=0.5)
time.sleep(5.0)

# 2) CARA -> giro+resultado+reparto -> aparece la preparacion.
cara = dev.bounds_of("CARA")
adb.tap(*(cara.center if cara else (540, 2081)))
time.sleep(7.0)  # spin(1.6)+result(2.4)+reparto(~1.8)+settle -> overlay de setup

# 3) Elige el primer Activo (habilita ¡COMBATIR!).
dev.tap_text("Activo", which=0, settle=0.7)

# 4) Resuelve la coord de ¡COMBATIR! ANTES de grabar.
comb = dev.bounds_of("COMBATIR", substring=True)
comb_xy = comb.center if comb else (540, 1180)
print(f"-> COMBATIR en {comb_xy} ({'bounds' if comb else 'fallback'})")

# 5) Graba el revelado (~2s) mientras confirma.
rec: dict[str, Path] = {}
def _record() -> None:
    rec["mp4"] = screenrecord(adb, run_dir, seconds=5, size="1080x2400")

t = threading.Thread(target=_record)
t.start()
time.sleep(0.5)
adb.tap(*comb_xy)
t.join()
print(f"-> video: {rec['mp4']}")

frames = extract_frames(rec["mp4"], run_dir / "frames", every_n=2, prefix="f")
print(f"[OK] {len(frames)} fotogramas -> {run_dir / 'frames'}")
