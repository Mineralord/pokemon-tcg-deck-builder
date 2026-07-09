"""Generación de reportes de comparación (JSON + HTML).

El HTML muestra, por fotograma: referencia, captura, mapa de calor de diferencias
y las tres métricas, con marca PASS/FAIL. Materializa el "reporte de discrepancias"
que pide el requerimiento.
"""
from __future__ import annotations

import json
from pathlib import Path

from .compare import DiffResult


def write_json(results: list[DiffResult], out_dir: Path) -> Path:
    dest = out_dir / "report.json"
    payload = {
        "summary": summarize(results),
        "frames": [r.as_dict() for r in results],
    }
    dest.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
    return dest


def summarize(results: list[DiffResult]) -> dict:
    n = len(results)
    passed = sum(1 for r in results if r.passed)
    avg_ssim = sum(r.ssim for r in results) / n if n else 0.0
    return {
        "frames": n,
        "passed": passed,
        "failed": n - passed,
        "avg_ssim": round(avg_ssim, 4),
    }


def _rel(path: str | None, base: Path) -> str:
    if not path:
        return ""
    try:
        return Path(path).resolve().relative_to(base.resolve()).as_posix()
    except ValueError:
        return Path(path).as_posix()


def write_html(results: list[DiffResult], out_dir: Path, *, refs_dir: Path | None = None) -> Path:
    dest = out_dir / "report.html"
    s = summarize(results)
    rows = []
    for r in results:
        badge = "pass" if r.passed else "fail"
        ref_src = f"{refs_dir.as_posix()}/{r.ref}" if refs_dir else r.ref
        offsets = ", ".join(f"{o.name}: Δ({o.dx},{o.dy}) {o.score:.2f}" for o in r.offsets) or "—"
        rows.append(
            f"""
      <div class="frame {badge}">
        <div class="meta">
          <span class="tag {badge}">{badge.upper()}</span>
          <b>{r.ref}</b> vs {r.shot}
          &nbsp;·&nbsp; SSIM <b>{r.ssim:.4f}</b>
          &nbsp;·&nbsp; pixel-diff <b>{r.pixel_pct*100:.2f}%</b>
          &nbsp;·&nbsp; offsets {offsets}
        </div>
        <div class="imgs">
          <figure><img src="{ref_src}" loading="lazy"><figcaption>referencia</figcaption></figure>
          <figure><img src="{_rel(r.shot, out_dir)}" loading="lazy"><figcaption>captura</figcaption></figure>
          <figure><img src="{_rel(r.heatmap, out_dir)}" loading="lazy"><figcaption>diferencia</figcaption></figure>
        </div>
      </div>"""
        )

    html = f"""<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<title>TCG Live Clone — Reporte de fidelidad 1:1</title>
<style>
  body {{ font-family: system-ui, sans-serif; margin: 0; background:#0e1116; color:#e6e6e6; }}
  header {{ padding:16px 24px; background:#04152b; border-bottom:2px solid #E4AB5E; }}
  header h1 {{ margin:0; font-size:18px; }}
  header .sum {{ margin-top:6px; font-size:13px; opacity:.85; }}
  .frame {{ padding:16px 24px; border-bottom:1px solid #232833; }}
  .meta {{ font-size:13px; margin-bottom:8px; }}
  .imgs {{ display:flex; gap:12px; }}
  figure {{ margin:0; text-align:center; font-size:11px; opacity:.8; }}
  img {{ max-height:360px; border:1px solid #333; background:#000; }}
  .tag {{ padding:2px 8px; border-radius:4px; font-weight:700; font-size:11px; }}
  .tag.pass {{ background:#1e7a3c; }} .tag.fail {{ background:#a12630; }}
  .frame.fail {{ background:#1a1113; }}
</style></head>
<body>
<header>
  <h1>Reporte de fidelidad 1:1 — Pokémon TCG Live Clone</h1>
  <div class="sum">{s['frames']} fotogramas · {s['passed']} PASS · {s['failed']} FAIL ·
     SSIM promedio {s['avg_ssim']:.4f}</div>
</header>
{''.join(rows)}
</body></html>"""
    dest.write_text(html, encoding="utf-8")
    return dest


def write_reports(results: list[DiffResult], out_dir: Path, *, refs_dir: Path | None = None) -> tuple[Path, Path]:
    return write_json(results, out_dir), write_html(results, out_dir, refs_dir=refs_dir)
