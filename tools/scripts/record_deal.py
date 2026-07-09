"""Graba la animacion del reparto de 7 cartas con screenrecord (frame-a-frame).

screenrecord es bloqueante, asi que grabamos en un hilo mientras el hilo principal
navega hasta la eleccion de moneda y toca CARA; el giro+resultado+reparto quedan
dentro de la ventana de grabacion. Luego se extraen los fotogramas del .mp4 con
OpenCV (sin ffmpeg).

Uso:  python scripts/record_deal.py [serial]
"""
from __future__ import annotations

import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))  # tools/ al path

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

# 1) Navega hasta la eleccion de moneda (CHOOSING).
dev.relaunch(settle=3.5)
dev.tap_text("JUGAR", fallback=(540, 1469), settle=0.5)
time.sleep(5.0)  # matchmaking (~3.5s) + tablero + moneda

# 2) Resuelve la coordenada de CARA ANTES de grabar (evita uiautomator durante la
#    grabacion, que anade latencia/contencion de adb).
cara = dev.bounds_of("CARA")
cara_xy = cara.center if cara else (540, 2180)
print(f"-> CARA en {cara_xy} ({'bounds' if cara else 'fallback'})")

# 3) Graba en un hilo; el reparto arranca ~4s tras tocar CARA y dura ~1.8s.
rec: dict[str, Path] = {}
def _record() -> None:
    rec["mp4"] = screenrecord(adb, run_dir, seconds=9, size="1080x2400")

t = threading.Thread(target=_record)
t.start()
time.sleep(0.5)  # deja arrancar screenrecord
adb.tap(*cara_xy)
t.join()
print(f"-> video: {rec['mp4']}")

# 4) Extrae fotogramas (1 de cada 2) para revisar la animacion.
frames = extract_frames(rec["mp4"], run_dir / "frames", every_n=2, prefix="f")
print(f"[OK] {len(frames)} fotogramas -> {run_dir / 'frames'}")
