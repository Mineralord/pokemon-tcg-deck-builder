#!/usr/bin/env python
"""vidref — extractor de frames de los videos de referencia de TCG Live.

No hay ffmpeg en PATH; usamos OpenCV (ya instalado en tools/.venv). Fuente de verdad para
"replicar": los mp4 COMBATE COMPLETO 1/2. Como no puedo (Claude) leer video, esta tool
saca imágenes fijas en dos niveles:

  index : hojas de contactos (miniaturas en cuadrícula, etiqueta MM:SS) para LOCALIZAR
          todas las ocurrencias de un comportamiento y sus timestamps.
  burst : frames full-res 1080x2400 de un tramo [--from,--to] para ANALIZAR a fondo.
  at    : un frame suelto en --time (comprobación rápida).

Uso:
  py tools/scripts/vidref.py index 1
  py tools/scripts/vidref.py burst 1 --from 0:28 --to 0:34 --name robo --fps 12
  py tools/scripts/vidref.py at 1 --time 0:30
"""
from __future__ import annotations
import argparse, sys
from pathlib import Path
import cv2

REPO_ROOT = Path(__file__).resolve().parents[2]
LIVE_REFS = REPO_ROOT / "referencias_live"
VIDEO_DIR = Path(r"C:\DOCUMENTOS\POKÉMON TCG\TCG LIVE VS MI APP CLON\POKEMON TCG LIVE")
SOURCES = {
    "1": ("COMBATE COMPLETO 1.mp4", "combate1"),
    "2": ("COMBATE COMPLETO 2.mp4", "combate2"),
    "3": ("VISTA DE ESTADIO Y HERRAMIENTA.mp4", "estadio_herramienta"),
    "4": ("PTCGP ESCOGER SERIE Y EXPANSION + ABRIR SOBRE + REGISTRO DE CARTA NUEVA EN LA COLECCION.mp4", "ptcgp_flujo"),
    "5": ("BARAJAS PRIMERA VISTA.mp4", "barajas_primera_vista"),
    "6": ("EDITOR DE BARAJAS.mp4", "editor_barajas"),
    "7": (r"MIS BARAJAS\VIDEO 2 BARAJAS.mp4", "barajas_v2"),
    "8": (r"MIS BARAJAS\VIDEO 3 BARAJAS.mp4", "barajas_v3"),
}


def parse_ts(s: str) -> float:
    """'MM:SS', 'H:MM:SS' o segundos -> segundos (float)."""
    parts = str(s).split(":")
    parts = [float(p) for p in parts]
    sec = 0.0
    for p in parts:
        sec = sec * 60 + p
    return sec


def fmt_ts(sec: float) -> str:
    sec = int(round(sec))
    return f"{sec // 60:02d}:{sec % 60:02d}"


def resolve(src: str):
    if src not in SOURCES:
        sys.exit(f"Fuente desconocida '{src}'. Usa 1 o 2.")
    fname, outdir = SOURCES[src]
    path = VIDEO_DIR / fname
    if not path.exists():
        sys.exit(f"No existe el video: {path}")
    return path, LIVE_REFS / outdir


def open_cap(path: Path):
    cap = cv2.VideoCapture(str(path))
    if not cap.isOpened():
        sys.exit(f"OpenCV no pudo abrir: {path}")
    fps = cap.get(cv2.CAP_PROP_FPS) or 24.0
    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    return cap, fps, total


def cmd_index(args):
    path, outdir = resolve(args.src)
    index_dir = outdir / "_index"
    index_dir.mkdir(parents=True, exist_ok=True)
    cap, fps, total = open_cap(path)
    step = max(1, int(round(fps / args.fps)))      # muestreo secuencial (evita seeks VBR)
    thumb_w = args.thumb_w
    per_sheet = args.cols * args.rows

    thumbs, first_ts = [], None
    sheet_idx = 0
    fidx = 0
    ok, frame = cap.read()

    def flush():
        nonlocal thumbs, first_ts, sheet_idx
        if not thumbs:
            return
        last_ts = first_ts + (len(thumbs) - 1) * (step / fps)
        h, w = thumbs[0].shape[:2]
        blank = 0 * thumbs[0]
        cells = thumbs + [blank] * (per_sheet - len(thumbs))
        rows_img = []
        for r in range(args.rows):
            row_cells = cells[r * args.cols:(r + 1) * args.cols]
            rows_img.append(cv2.hconcat(row_cells))
        sheet = cv2.vconcat(rows_img)
        name = f"sheet_{fmt_ts(first_ts).replace(':','')}-{fmt_ts(last_ts).replace(':','')}.png"
        cv2.imwrite(str(index_dir / name), sheet)
        print(f"  {name}  ({len(thumbs)} thumbs)")
        thumbs, first_ts = [], None
        sheet_idx += 1

    print(f"Indexando {path.name} @ {args.fps}fps -> {index_dir}")
    while ok:
        if fidx % step == 0:
            ts = fidx / fps
            if first_ts is None:
                first_ts = ts
            h, w = frame.shape[:2]
            th = int(thumb_w * h / w)
            thumb = cv2.resize(frame, (thumb_w, th), interpolation=cv2.INTER_AREA)
            cv2.putText(thumb, fmt_ts(ts), (6, 22), cv2.FONT_HERSHEY_SIMPLEX,
                        0.6, (0, 255, 0), 2, cv2.LINE_AA)
            thumbs.append(thumb)
            if len(thumbs) == per_sheet:
                flush()
        fidx += 1
        ok, frame = cap.read()
    flush()
    cap.release()
    print("Listo.")


def cmd_burst(args):
    path, outdir = resolve(args.src)
    name = args.name or f"burst_{fmt_ts(parse_ts(args.frm)).replace(':','')}"
    dest = outdir / name
    dest.mkdir(parents=True, exist_ok=True)
    cap, fps, total = open_cap(path)
    t0, t1 = parse_ts(args.frm), parse_ts(args.to)
    step = max(1, int(round(fps / args.fps)))
    f0, f1 = int(t0 * fps), int(t1 * fps)
    cap.set(cv2.CAP_PROP_POS_FRAMES, max(0, f0 - 1))
    print(f"Ráfaga {path.name} {fmt_ts(t0)}-{fmt_ts(t1)} @ {args.fps}fps -> {dest}")
    fidx = f0
    n = 0
    while fidx <= f1:
        ok, frame = cap.read()
        if not ok:
            break
        if (fidx - f0) % step == 0:
            ms = int(round(fidx / fps * 1000))
            cv2.imwrite(str(dest / f"f_{ms:07d}.png"), frame)
            n += 1
        fidx += 1
    cap.release()
    print(f"Listo. {n} frames en {dest}")


def cmd_at(args):
    path, outdir = resolve(args.src)
    dest = outdir / "_index"
    dest.mkdir(parents=True, exist_ok=True)
    cap, fps, total = open_cap(path)
    t = parse_ts(args.time)
    cap.set(cv2.CAP_PROP_POS_FRAMES, int(t * fps))
    ok, frame = cap.read()
    cap.release()
    if not ok:
        sys.exit("No se pudo leer el frame.")
    out = dest / f"at_{fmt_ts(t).replace(':','')}.png"
    cv2.imwrite(str(out), frame)
    print(f"{out}  {frame.shape[1]}x{frame.shape[0]}")


def main():
    ap = argparse.ArgumentParser(description="Extractor de frames de referencia TCG Live")
    sub = ap.add_subparsers(dest="cmd", required=True)

    pi = sub.add_parser("index", help="hojas de contactos de todo el video")
    pi.add_argument("src", choices=["1", "2", "3", "4", "5", "6", "7", "8"])
    pi.add_argument("--fps", type=float, default=1.0)
    pi.add_argument("--cols", type=int, default=8)
    pi.add_argument("--rows", type=int, default=5)
    pi.add_argument("--thumb-w", type=int, default=180)
    pi.set_defaults(func=cmd_index)

    pb = sub.add_parser("burst", help="frames full-res de un tramo")
    pb.add_argument("src", choices=["1", "2", "3", "4", "5", "6", "7", "8"])
    pb.add_argument("--from", dest="frm", required=True)
    pb.add_argument("--to", required=True)
    pb.add_argument("--fps", type=float, default=12.0)
    pb.add_argument("--name", default=None)
    pb.set_defaults(func=cmd_burst)

    pa = sub.add_parser("at", help="un frame suelto")
    pa.add_argument("src", choices=["1", "2", "3", "4", "5", "6", "7", "8"])
    pa.add_argument("--time", required=True)
    pa.set_defaults(func=cmd_at)

    args = ap.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
