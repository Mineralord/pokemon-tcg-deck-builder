"""Medicion del layout REAL del clon en el dispositivo.

Lee las cajas de cada elemento etiquetado (content-desc "tt:<tag>") del volcado de
uiautomator y las normaliza al tamano de pantalla, produciendo un JSON con el mismo
formato que el spec de referencia (`refspec/board_start.json`) para compararlos caja
a caja.
"""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

from .adb import Adb
from .device import DeviceController


def screen_size(adb: Adb) -> tuple[int, int]:
    """Devuelve (w, h) de la pantalla via `wm size` (soporta 'Override size')."""
    out = adb.shell("wm", "size")
    # Formato: "Physical size: 1080x2400" (+ opcional "Override size: ...").
    sizes = re.findall(r"(\d+)x(\d+)", out)
    if not sizes:
        raise RuntimeError(f"No se pudo leer el tamano de pantalla: {out!r}")
    w, h = sizes[-1]  # override gana si existe
    return int(w), int(h)


def measure_layout(dev: DeviceController, screen_w: int, screen_h: int) -> dict[str, Any]:
    """Vuelca la UI y normaliza cada caja etiquetada al tamano de pantalla."""
    rects = dev.bounds_by_tag()
    elements: dict[str, dict[str, float]] = {}
    for tag, r in rects.items():
        elements[tag] = {
            "x": round(r.left / screen_w, 5),
            "y": round(r.top / screen_h, 5),
            "w": round((r.right - r.left) / screen_w, 5),
            "h": round((r.bottom - r.top) / screen_h, 5),
        }
    return {
        "meta": {"screen_w": screen_w, "screen_h": screen_h, "n": len(elements)},
        "elements": elements,
    }


def measure_to_file(adb: Adb, dev: DeviceController, out_path: Path) -> dict[str, Any]:
    w, h = screen_size(adb)
    layout = measure_layout(dev, w, h)
    Path(out_path).write_text(
        json.dumps(layout, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    return layout
