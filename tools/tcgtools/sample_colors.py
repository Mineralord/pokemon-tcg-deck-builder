"""Muestreo de color de regiones en fotogramas de referencia.

Reporta el color promedio (hex ARGB para Android) de una o varias regiones
rectangulares dentro de un frame de referencia, para reemplazar los "seed values"
de TcgColors/BattleTheme por valores muestreados de referencias/.

Las regiones se dan en fracciones (0..1) del ancho/alto para ser independientes de
la resolución del frame. Formato: name=x0,y0,x1,y1
"""
from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np


def average_color(img: np.ndarray, frac: tuple[float, float, float, float]) -> tuple[int, int, int]:
    """Devuelve (R, G, B) promedio de la región fraccional del frame."""
    h, w = img.shape[:2]
    x0, y0, x1, y1 = frac
    xa, xb = int(x0 * w), int(x1 * w)
    ya, yb = int(y0 * h), int(y1 * h)
    region = img[ya:yb, xa:xb]
    b, g, r = region.reshape(-1, 3).mean(axis=0)
    return int(round(r)), int(round(g)), int(round(b))


def android_hex(rgb: tuple[int, int, int], alpha: int = 0xFF) -> str:
    """Formatea (R,G,B) como color Compose 0xAARRGGBB."""
    r, g, b = rgb
    return f"0x{alpha:02X}{r:02X}{g:02X}{b:02X}"


def sample(frame: Path, regions: dict[str, tuple[float, float, float, float]]) -> dict[str, str]:
    img = cv2.imread(str(frame))
    if img is None:
        raise FileNotFoundError(f"No se pudo leer: {frame}")
    return {name: android_hex(average_color(img, frac)) for name, frac in regions.items()}
