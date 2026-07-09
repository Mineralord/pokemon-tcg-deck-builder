"""Comparación visual referencia vs. captura (OpenCV).

Tres métricas por fotograma:
    - SSIM: similitud estructural global (0..1, 1 = idéntico).
    - pixel_pct: fracción de píxeles cuya diferencia supera un umbral (0..1).
    - offsets: desalineación en px de elementos clave vía template matching.

Normaliza siempre la captura al tamaño de la referencia antes de comparar
(referencia canónica: referencias/*.png a 1280x2844; captura del clon a 1080x2400,
mismo aspecto ~0.450).
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import cv2
import numpy as np
from skimage.metrics import structural_similarity as ssim

# Umbral por defecto de SSIM para marcar PASS/FAIL de un checkpoint.
DEFAULT_SSIM_PASS = 0.98
# Diferencia normalizada (0..1) por encima de la cual un píxel cuenta como distinto.
DEFAULT_PIXEL_THRESHOLD = 0.15


@dataclass
class TemplateOffset:
    name: str
    dx: int
    dy: int
    score: float  # confianza del match (0..1)


@dataclass
class DiffResult:
    ref: str
    shot: str
    ssim: float
    pixel_pct: float
    passed: bool
    heatmap: Optional[str] = None
    offsets: list[TemplateOffset] = field(default_factory=list)

    def as_dict(self) -> dict:
        return {
            "ref": self.ref,
            "shot": self.shot,
            "ssim": round(self.ssim, 4),
            "pixel_pct": round(self.pixel_pct, 4),
            "passed": self.passed,
            "heatmap": self.heatmap,
            "offsets": [o.__dict__ for o in self.offsets],
        }


def _load(path: Path) -> np.ndarray:
    img = cv2.imread(str(path))
    if img is None:
        raise FileNotFoundError(f"No se pudo leer la imagen: {path}")
    return img


def diff(
    ref: Path,
    shot: Path,
    out_dir: Path,
    *,
    ssim_pass: float = DEFAULT_SSIM_PASS,
    pixel_threshold: float = DEFAULT_PIXEL_THRESHOLD,
    write_heatmap: bool = True,
) -> DiffResult:
    """Compara una referencia contra una captura y devuelve las métricas.

    Genera un mapa de calor (JET) donde el rojo marca las mayores diferencias.
    """
    a = _load(ref)
    b = _load(shot)
    # Normaliza la captura al tamaño de la referencia.
    if b.shape[:2] != a.shape[:2]:
        b = cv2.resize(b, (a.shape[1], a.shape[0]), interpolation=cv2.INTER_AREA)

    ga = cv2.cvtColor(a, cv2.COLOR_BGR2GRAY)
    gb = cv2.cvtColor(b, cv2.COLOR_BGR2GRAY)
    score, dmap = ssim(ga, gb, full=True)
    dmap = 1.0 - dmap  # 0 = igual, 1 = distinto
    pixel_pct = float((dmap > pixel_threshold).mean())

    heatmap_path: Optional[str] = None
    if write_heatmap:
        out_dir.mkdir(parents=True, exist_ok=True)
        heat = cv2.applyColorMap((np.clip(dmap, 0, 1) * 255).astype("uint8"), cv2.COLORMAP_JET)
        # Mezcla sobre la referencia para dar contexto visual.
        blended = cv2.addWeighted(a, 0.55, heat, 0.45, 0.0)
        dest = out_dir / f"{ref.stem}__vs__{shot.stem}_diff.png"
        cv2.imwrite(str(dest), blended)
        heatmap_path = str(dest)

    return DiffResult(
        ref=ref.name,
        shot=shot.name,
        ssim=float(score),
        pixel_pct=pixel_pct,
        passed=bool(score >= ssim_pass),
        heatmap=heatmap_path,
    )


def locate_templates(
    shot: Path,
    templates: dict[str, Path],
    expected: dict[str, tuple[int, int]],
) -> list[TemplateOffset]:
    """Localiza cada template en la captura y mide su offset vs. la posición esperada.

    templates: {nombre: ruta_png_recortado}
    expected:  {nombre: (x, y) esperado de la esquina superior-izquierda}
    Reporta la desalineación (dx, dy) en px y la confianza del match.
    """
    img = _load(shot)
    offsets: list[TemplateOffset] = []
    for name, tpl_path in templates.items():
        tpl = _load(tpl_path)
        res = cv2.matchTemplate(img, tpl, cv2.TM_CCOEFF_NORMED)
        _, max_val, _, max_loc = cv2.minMaxLoc(res)
        ex, ey = expected.get(name, (max_loc[0], max_loc[1]))
        offsets.append(
            TemplateOffset(name=name, dx=max_loc[0] - ex, dy=max_loc[1] - ey, score=float(max_val))
        )
    return offsets


def compare_pairs(
    pairs: list[tuple[Path, Path]],
    out_dir: Path,
    *,
    ssim_pass: float = DEFAULT_SSIM_PASS,
    pixel_threshold: float = DEFAULT_PIXEL_THRESHOLD,
) -> list[DiffResult]:
    """Compara una lista de (referencia, captura) y devuelve todos los resultados."""
    return [
        diff(ref, shot, out_dir, ssim_pass=ssim_pass, pixel_threshold=pixel_threshold)
        for ref, shot in pairs
    ]
