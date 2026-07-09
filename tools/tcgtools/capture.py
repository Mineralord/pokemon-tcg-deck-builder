"""Captura de fotogramas del dispositivo.

Dos modos:
    (a) screencap en bucle a intervalo fijo (checkpoints puntuales).
    (b) screenrecord en el dispositivo → pull → extracción de frames con OpenCV.

Se usa OpenCV (cv2.VideoCapture) para extraer frames del .mp4 y así evitar la
dependencia de ffmpeg, que hoy no está instalado en el entorno.
"""
from __future__ import annotations

import time
from datetime import datetime
from pathlib import Path
from typing import Optional

from .adb import Adb


def new_run_dir(base: Path) -> Path:
    """Crea tools/out/run_<timestamp>/ y lo devuelve."""
    run = base / f"run_{datetime.now():%Y%m%d_%H%M%S}"
    run.mkdir(parents=True, exist_ok=True)
    return run


def screencap_loop(
    adb: Adb,
    out_dir: Path,
    *,
    count: int,
    interval: float = 1.0,
    prefix: str = "frame",
) -> list[Path]:
    """Toma `count` capturas separadas `interval` segundos. Devuelve las rutas."""
    out_dir.mkdir(parents=True, exist_ok=True)
    frames: list[Path] = []
    for i in range(count):
        dest = out_dir / f"{prefix}_{i:04d}.png"
        adb.screencap(dest)
        frames.append(dest)
        if i < count - 1:
            time.sleep(interval)
    return frames


def laplacian_sharpness(path: Path) -> float:
    """Varianza del Laplaciano: proxy de nitidez. Alto = nítido, bajo = borroso."""
    import cv2

    img = cv2.imread(str(path), cv2.IMREAD_GRAYSCALE)
    if img is None:
        return 0.0
    return float(cv2.Laplacian(img, cv2.CV_64F).var())


def capture_session(
    adb: Adb,
    out_dir: Path,
    *,
    duration: float,
    interval: float = 1.0,
    min_sharpness: float = 0.0,
    prefix: str = "live",
) -> list[Path]:
    """Captura screencaps durante `duration` s mientras el usuario juega.

    Si `min_sharpness` > 0, descarta (borra) los frames borrosos por debajo del
    umbral de varianza del Laplaciano, quedándose solo con frames nítidos aptos
    como referencia 1:1. Devuelve las rutas conservadas.
    """
    out_dir.mkdir(parents=True, exist_ok=True)
    kept: list[Path] = []
    start = time.time()
    i = 0
    while time.time() - start < duration:
        dest = out_dir / f"{prefix}_{i:04d}.png"
        adb.screencap(dest)
        if min_sharpness > 0 and laplacian_sharpness(dest) < min_sharpness:
            dest.unlink(missing_ok=True)  # descarta borroso/transición
        else:
            kept.append(dest)
        i += 1
        time.sleep(interval)
    return kept


def screenrecord(
    adb: Adb,
    out_dir: Path,
    *,
    seconds: int,
    bit_rate: str = "8M",
    size: Optional[str] = None,
) -> Path:
    """Graba `seconds` s con screenrecord, lo baja y devuelve la ruta del .mp4.

    screenrecord se ejecuta en el dispositivo; hay que esperar a que termine antes
    de hacer pull. `size` opcional como "1080x2400".
    """
    remote = "/sdcard/_rec.mp4"
    args = ["shell", "screenrecord", "--bit-rate", bit_rate, "--time-limit", str(seconds)]
    if size:
        args += ["--size", size]
    args.append(remote)
    # Bloqueante: adb no retorna hasta que screenrecord termina (time-limit).
    adb.run(*args)
    local = out_dir / "recording.mp4"
    return adb.pull(remote, local)


def extract_frames(
    video: Path,
    out_dir: Path,
    *,
    every_n: int = 1,
    prefix: str = "frame",
) -> list[Path]:
    """Extrae 1 de cada `every_n` frames del video con OpenCV (sin ffmpeg)."""
    import cv2  # import perezoso: capture puede usarse sin CV instalado

    out_dir.mkdir(parents=True, exist_ok=True)
    cap = cv2.VideoCapture(str(video))
    if not cap.isOpened():
        raise RuntimeError(f"OpenCV no pudo abrir el video: {video}")
    frames: list[Path] = []
    idx = kept = 0
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            if idx % every_n == 0:
                dest = out_dir / f"{prefix}_{kept:04d}.png"
                cv2.imwrite(str(dest), frame)
                frames.append(dest)
                kept += 1
            idx += 1
    finally:
        cap.release()
    return frames
