"""Comparacion posicional caja-a-caja entre el clon medido y el spec de referencia.

Metrica principal (acordada con el usuario): fidelidad POSICIONAL, no de pixel. Por
cada elemento se calcula:
  - dcenter: distancia euclidea entre centros (normalizada 0..1).
  - dsize:   diferencia relativa de tamano (|dw|+|dh|).
  - iou:     interseccion sobre union de las cajas normalizadas.
PASS si dcenter < tol_center e iou >= tol_iou. Ademas se sugiere el valor de caja
corregido para el layout del clon (suponiendo que su NBox == caja de referencia):
  suggested = ref + (ref - measured)   (correccion de primer orden del residual).

Emite layout_report.json + layout_report.html + un overlay ref(verde)/clon(rojo).
"""
from __future__ import annotations

import json
import math
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Optional

import cv2


@dataclass
class ElementDiff:
    name: str
    ref: Optional[dict[str, float]]
    measured: Optional[dict[str, float]]
    dcenter: Optional[float]
    dsize: Optional[float]
    iou: Optional[float]
    passed: bool
    suggested: Optional[dict[str, float]]
    note: str = ""


def _center(b: dict[str, float]) -> tuple[float, float]:
    return b["x"] + b["w"] / 2.0, b["y"] + b["h"] / 2.0


def _iou(a: dict[str, float], b: dict[str, float]) -> float:
    ax2, ay2 = a["x"] + a["w"], a["y"] + a["h"]
    bx2, by2 = b["x"] + b["w"], b["y"] + b["h"]
    ix1, iy1 = max(a["x"], b["x"]), max(a["y"], b["y"])
    ix2, iy2 = min(ax2, bx2), min(ay2, by2)
    iw, ih = max(0.0, ix2 - ix1), max(0.0, iy2 - iy1)
    inter = iw * ih
    union = a["w"] * a["h"] + b["w"] * b["h"] - inter
    return inter / union if union > 0 else 0.0


def _suggested(ref: dict[str, float], meas: dict[str, float]) -> dict[str, float]:
    """Correccion de primer orden: acerca el clon a la referencia por su residual."""
    return {k: round(2 * ref[k] - meas[k], 5) for k in ("x", "y", "w", "h")}


def compare_layout(
    ref_spec: dict[str, Any],
    measured: dict[str, Any],
    *,
    tol_center: float = 0.01,
    tol_iou: float = 0.95,
) -> list[ElementDiff]:
    ref_els: dict[str, dict[str, float]] = ref_spec["elements"]
    meas_els: dict[str, dict[str, float]] = measured["elements"]
    diffs: list[ElementDiff] = []
    for name, ref in ref_els.items():
        meas = meas_els.get(name)
        if meas is None:
            diffs.append(ElementDiff(name, ref, None, None, None, None, False, None,
                                     "sin medicion (falta testTag en el clon o no visible)"))
            continue
        rc, mc = _center(ref), _center(meas)
        dcenter = math.hypot(rc[0] - mc[0], rc[1] - mc[1])
        dsize = abs(ref["w"] - meas["w"]) + abs(ref["h"] - meas["h"])
        iou = _iou(ref, meas)
        passed = dcenter < tol_center and iou >= tol_iou
        diffs.append(ElementDiff(
            name, ref, meas, round(dcenter, 5), round(dsize, 5), round(iou, 4),
            passed, None if passed else _suggested(ref, meas),
        ))
    # Elementos medidos que no estan en el spec (informativo).
    for name in meas_els.keys() - ref_els.keys():
        diffs.append(ElementDiff(name, None, meas_els[name], None, None, None, False, None,
                                 "medido pero no esta en el spec de referencia"))
    return diffs


def render_overlay(ref_image: Path, diffs: list[ElementDiff], out_path: Path) -> Optional[Path]:
    img = cv2.imread(str(ref_image))
    if img is None:
        return None
    h, w = img.shape[:2]

    def _px(b: dict[str, float]):
        return (int(b["x"] * w), int(b["y"] * h),
                int((b["x"] + b["w"]) * w), int((b["y"] + b["h"]) * h))

    for d in diffs:
        if d.ref:
            l, t, r, bo = _px(d.ref)
            cv2.rectangle(img, (l, t), (r, bo), (60, 220, 60), 2)  # ref = verde
        if d.measured:
            l, t, r, bo = _px(d.measured)
            col = (60, 220, 60) if d.passed else (60, 60, 220)     # clon = verde/rojo
            cv2.rectangle(img, (l, t), (r, bo), col, 2)
    cv2.imwrite(str(out_path), img)
    return out_path


def summarize(diffs: list[ElementDiff]) -> dict[str, Any]:
    scored = [d for d in diffs if d.iou is not None]
    passed = sum(1 for d in diffs if d.passed)
    avg_iou = round(sum(d.iou for d in scored) / len(scored), 4) if scored else 0.0
    avg_dc = round(sum(d.dcenter for d in scored) / len(scored), 5) if scored else None
    return {
        "elements": len(diffs),
        "passed": passed,
        "failed": len(diffs) - passed,
        "avg_iou": avg_iou,
        "avg_dcenter": avg_dc,
        "all_pass": passed == len(diffs),
    }


def write_reports(diffs: list[ElementDiff], out_dir: Path, ref_image: Path,
                  *, tol_center: float, tol_iou: float) -> tuple[Path, Path]:
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    overlay = render_overlay(ref_image, diffs, out_dir / "layout_overlay.png")
    summary = summarize(diffs)

    payload = {
        "summary": summary,
        "tolerance": {"center": tol_center, "iou": tol_iou},
        "elements": [asdict(d) for d in diffs],
    }
    json_path = out_dir / "layout_report.json"
    json_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")

    rows = []
    for d in diffs:
        badge = "PASS" if d.passed else "FAIL"
        color = "#2e7d32" if d.passed else "#b3261e"
        sug = ""
        if d.suggested:
            sug = " ".join(f"{k}={v}" for k, v in d.suggested.items())
        rows.append(
            f"<tr><td>{d.name}</td>"
            f"<td style='color:{color};font-weight:bold'>{badge}</td>"
            f"<td>{d.dcenter if d.dcenter is not None else '-'}</td>"
            f"<td>{d.iou if d.iou is not None else '-'}</td>"
            f"<td>{d.dsize if d.dsize is not None else '-'}</td>"
            f"<td><code>{sug}</code></td>"
            f"<td>{d.note}</td></tr>"
        )
    ov_rel = overlay.name if overlay else ""
    html = f"""<!doctype html><html><head><meta charset='utf-8'>
<title>Layout 1:1 report</title>
<style>body{{background:#12151c;color:#e6e6e6;font-family:system-ui,Arial;margin:20px}}
h1{{color:#e4ab5e}} table{{border-collapse:collapse;width:100%;margin-top:14px}}
td,th{{border:1px solid #333;padding:6px 8px;font-size:13px;text-align:left}}
th{{background:#1c2230;color:#e4ab5e}} code{{color:#7fd1ff}}
img{{max-width:420px;border:1px solid #333;margin-top:12px}}
.sum{{font-size:15px}}</style></head><body>
<h1>Fidelidad posicional 1:1 &mdash; arranque de tablero</h1>
<p class='sum'>PASS <b>{summary['passed']}/{summary['elements']}</b> &middot;
IoU medio <b>{summary['avg_iou']}</b> &middot; &Delta;centro medio <b>{summary['avg_dcenter']}</b>
&middot; tol centro &lt;{tol_center}, IoU &ge;{tol_iou} &middot;
{"<span style='color:#2e7d32'>TODOS PASAN</span>" if summary['all_pass'] else "<span style='color:#b3261e'>faltan elementos</span>"}</p>
<p>Overlay: verde = referencia, rojo = clon (fuera de tolerancia).</p>
<img src='{ov_rel}'>
<table><tr><th>elemento</th><th>estado</th><th>&Delta;centro</th><th>IoU</th><th>&Delta;tam</th><th>caja sugerida</th><th>nota</th></tr>
{''.join(rows)}</table></body></html>"""
    html_path = out_dir / "layout_report.html"
    html_path.write_text(html, encoding="utf-8")
    return json_path, html_path
