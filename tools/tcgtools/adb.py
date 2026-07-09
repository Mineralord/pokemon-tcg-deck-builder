"""Wrapper de bajo nivel sobre adb.exe.

Localiza el binario `adb` (env vars o rutas conocidas de Windows) y ofrece helpers
delgados para las operaciones que usa el resto del tooling: shell, screencap, tap,
pull y volcado de la jerarquía UI. subprocess puro, sin dependencias nativas.

Porta la resolución de ruta de `_refcap/board.ps1`:
    $adb = "$env:LOCALAPPDATA\\Android\\Sdk\\platform-tools\\adb.exe"
"""
from __future__ import annotations

import os
import shutil
import subprocess
from pathlib import Path
from typing import Optional


class AdbError(RuntimeError):
    """adb devolvió un código de salida distinto de cero o no se pudo localizar."""


def _candidate_paths() -> list[Path]:
    """Rutas donde buscar adb.exe, en orden de preferencia."""
    out: list[Path] = []
    # 1) Variable explícita para el tooling.
    if os.environ.get("TCG_ADB"):
        out.append(Path(os.environ["TCG_ADB"]))
    # 2) SDK de Android estándar.
    for base_env in ("ANDROID_HOME", "ANDROID_SDK_ROOT", "LOCALAPPDATA"):
        base = os.environ.get(base_env)
        if not base:
            continue
        if base_env == "LOCALAPPDATA":
            out.append(Path(base) / "Android" / "Sdk" / "platform-tools" / "adb.exe")
        else:
            out.append(Path(base) / "platform-tools" / "adb.exe")
    return out


def resolve_adb() -> str:
    """Devuelve la ruta a un adb ejecutable o lanza AdbError con un mensaje claro."""
    for p in _candidate_paths():
        if p.is_file():
            return str(p)
    on_path = shutil.which("adb")
    if on_path:
        return on_path
    raise AdbError(
        "No se encontró adb.exe. Instala platform-tools o define TCG_ADB con la ruta "
        "completa (p.ej. %LOCALAPPDATA%\\Android\\Sdk\\platform-tools\\adb.exe)."
    )


class Adb:
    """Interfaz mínima a un dispositivo vía adb."""

    def __init__(self, serial: Optional[str] = None, adb_path: Optional[str] = None):
        self.adb = adb_path or resolve_adb()
        self.serial = serial

    def _base(self) -> list[str]:
        cmd = [self.adb]
        if self.serial:
            cmd += ["-s", self.serial]
        return cmd

    def run(self, *args: str, capture: bool = True) -> str:
        """Ejecuta `adb <args>` y devuelve stdout (texto). Lanza AdbError si falla."""
        proc = subprocess.run(
            self._base() + list(args),
            capture_output=capture,
            text=True,
        )
        if proc.returncode != 0:
            raise AdbError(
                f"adb {' '.join(args)} → código {proc.returncode}\n{proc.stderr.strip()}"
            )
        return proc.stdout if capture else ""

    def shell(self, *args: str) -> str:
        return self.run("shell", *args)

    def devices(self) -> list[str]:
        """Lista los seriales de dispositivos conectados (estado 'device')."""
        out = self.run("devices")
        serials = []
        for line in out.splitlines()[1:]:
            parts = line.split()
            if len(parts) == 2 and parts[1] == "device":
                serials.append(parts[0])
        return serials

    def screencap(self, dest: Path) -> Path:
        """Captura la pantalla y la guarda como PNG en `dest`.

        Usa `exec-out screencap -p` para transferir bytes crudos sin pasar por
        /sdcard (más rápido y sin residuos en el dispositivo).
        """
        dest.parent.mkdir(parents=True, exist_ok=True)
        proc = subprocess.run(
            self._base() + ["exec-out", "screencap", "-p"],
            stdout=dest.open("wb"),
            stderr=subprocess.PIPE,
        )
        if proc.returncode != 0:
            raise AdbError(f"screencap falló: {proc.stderr.decode(errors='ignore')}")
        return dest

    def tap(self, x: int, y: int) -> None:
        self.shell("input", "tap", str(x), str(y))

    def pull(self, remote: str, local: Path) -> Path:
        local.parent.mkdir(parents=True, exist_ok=True)
        self.run("pull", remote, str(local))
        return local

    def dump_ui(self) -> str:
        """Vuelca la jerarquía UI actual y devuelve el XML como texto.

        Equivale a `uiautomator dump` + `pull` de board.ps1, pero sin archivo
        temporal en el host: leemos el XML directo con `exec-out cat`.
        """
        self.shell("uiautomator", "dump", "/sdcard/ui.xml")
        return self.run("exec-out", "cat", "/sdcard/ui.xml")

    def force_stop(self, app_id: str) -> None:
        self.shell("am", "force-stop", app_id)

    def launch(self, app_id: str) -> None:
        self.shell("monkey", "-p", app_id, "-c", "android.intent.category.LAUNCHER", "1")
