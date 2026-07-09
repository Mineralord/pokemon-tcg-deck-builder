"""Runner guionizado por línea de tiempo.

Lee un YAML con una lista de pasos y los ejecuta en orden, emulando la línea de
tiempo de los videos de referencia. Cada paso `capture` genera un checkpoint
nombrado; luego `compare` empareja cada checkpoint con su fotograma de referencia
por nombre (no por índice ciego).

Pasos soportados (clave `op`):
    relaunch                              -> force-stop + launch
    tap_text   text= [which=] [substring=] [fallback=[x,y]] [settle=]
    tap_xy     x= y= [settle=]
    wait       seconds=
    capture    name=                      -> screencap → <run>/<name>.png
    assert_screen name= [ref=]            -> capture + marca para comparación

Un paso `assert_screen` es un `capture` que además registra contra qué frame de
referencia debe compararse (campo `ref`, por defecto = name).
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

from .device import DeviceController


@dataclass
class Checkpoint:
    name: str
    shot: Path
    ref: str | None = None  # nombre/patrón del frame de referencia esperado


@dataclass
class RunResult:
    run_dir: Path
    checkpoints: list[Checkpoint] = field(default_factory=list)


def load_script(path: Path) -> list[dict[str, Any]]:
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    steps = data.get("steps", data) if isinstance(data, dict) else data
    if not isinstance(steps, list):
        raise ValueError(f"{path}: se esperaba una lista de pasos ('steps').")
    return steps


def run_script(dev: DeviceController, script: Path, run_dir: Path) -> RunResult:
    steps = load_script(script)
    result = RunResult(run_dir=run_dir)

    for i, step in enumerate(steps):
        op = step.get("op")
        if not op:
            raise ValueError(f"Paso {i}: falta 'op'. Recibido: {step!r}")

        if op == "relaunch":
            dev.relaunch(settle=float(step.get("settle", 3.0)))

        elif op == "tap_text":
            fb = step.get("fallback")
            dev.tap_text(
                step["text"],
                which=int(step.get("which", 0)),
                substring=bool(step.get("substring", False)),
                fallback=(int(fb[0]), int(fb[1])) if fb else None,
                settle=float(step.get("settle", 0.7)),
            )

        elif op == "tap_xy":
            dev.tap_xy(int(step["x"]), int(step["y"]), settle=float(step.get("settle", 0.3)))

        elif op == "wait":
            time.sleep(float(step["seconds"]))

        elif op in ("capture", "assert_screen"):
            name = step["name"]
            shot = run_dir / f"{name}.png"
            dev.adb.screencap(shot)
            ref = step.get("ref", name) if op == "assert_screen" else None
            result.checkpoints.append(Checkpoint(name=name, shot=shot, ref=ref))

        else:
            raise ValueError(f"Paso {i}: op desconocido {op!r}.")

    return result
