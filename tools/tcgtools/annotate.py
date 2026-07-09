"""Anotacion del spec de layout de referencia (arranque de tablero TCG Live).

El tablero y los tapetes de TCG Live son constantes, asi que el spec de referencia
se anota una sola vez sobre un frame canonico (post-JUGAR) y queda como la fuente de
verdad de las cajas objetivo (target) contra las que se mide el clon.

Como no hay GUI de anotacion en este entorno, el flujo es un bucle visual:

    python -m tcgtools annotate overlay      # dibuja las cajas del JSON sobre el frame
    # -> abre refspec/board_start_overlay.png, ajusta el JSON, repite

Cada caja es [x, y, w, h] normalizada en 0..1 (origen arriba-izquierda), relativa al
lienzo de referencia 1080x2400 (mismo que BoardGeometry.RefW/RefH).
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import cv2
import numpy as np

# Paleta estable por elemento (BGR) para distinguir cajas en el overlay.
_PALETTE = [
    (60, 220, 60), (60, 60, 220), (220, 60, 60), (60, 220, 220),
    (220, 60, 220), (220, 220, 60), (255, 160, 60), (160, 60, 255),
    (60, 160, 255), (120, 220, 160),
]


def load_spec(path: Path) -> dict[str, Any]:
    """Carga board_start.json."""
    return json.loads(Path(path).read_text(encoding="utf-8"))


def save_spec(path: Path, spec: dict[str, Any]) -> None:
    Path(path).write_text(
        json.dumps(spec, indent=2, ensure_ascii=False), encoding="utf-8"
    )


def denorm(box: dict[str, float], w: int, h: int) -> tuple[int, int, int, int]:
    """Normalizada [x,y,w,h] -> pixeles (l, t, r, b) en el lienzo dado."""
    l = int(round(box["x"] * w))
    t = int(round(box["y"] * h))
    r = int(round((box["x"] + box["w"]) * w))
    b = int(round((box["y"] + box["h"]) * h))
    return l, t, r, b


def render_overlay(spec: dict[str, Any], ref_image: Path, out_path: Path) -> Path:
    """Dibuja todas las cajas del spec sobre el frame de referencia y lo guarda.

    El resultado es un PNG inspeccionable a ojo para validar/refinar las cajas.
    """
    img = cv2.imread(str(ref_image))
    if img is None:
        raise FileNotFoundError(f"No se pudo leer la imagen de referencia: {ref_image}")
    h, w = img.shape[:2]
    elements: dict[str, dict[str, float]] = spec["elements"]

    for i, (name, box) in enumerate(elements.items()):
        color = _PALETTE[i % len(_PALETTE)]
        l, t, r, b = denorm(box, w, h)
        cv2.rectangle(img, (l, t), (r, b), color, 2)
        # Etiqueta con fondo para legibilidad.
        label = name
        (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.4, 1)
        ly = max(t - 4, th + 2)
        cv2.rectangle(img, (l, ly - th - 2), (l + tw + 2, ly + 2), color, -1)
        cv2.putText(img, label, (l + 1, ly), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 0, 0), 1, cv2.LINE_AA)

    out_path = Path(out_path)
    cv2.imwrite(str(out_path), img)
    return out_path
