"""Navega hasta el COMBATE (tras la preparacion on-board) y captura el tablero.

JUGAR -> CARA -> (setup on-board) -> prueba cartas de la mano hasta abrir la hoja
de colocacion -> "Poner como Activo" -> LISTO -> captura el tablero de combate.

Uso:  python scripts/goto_combat.py [serial] [out_name.png]
"""
from __future__ import annotations
import re, subprocess, sys, time
from pathlib import Path

ADB = r"C:\Users\pmmt9\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = sys.argv[1] if len(sys.argv) > 1 else "3bf89e4f"
OUT = sys.argv[2] if len(sys.argv) > 2 else "combat.png"
OUTDIR = Path(__file__).resolve().parents[1] / "out"


def adb(*a: str) -> str:
    return subprocess.run([ADB, "-s", SERIAL, *a], capture_output=True, text=True).stdout


def tap(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(int(x)), str(int(y)))


def dump() -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/u.xml")
    return adb("shell", "cat", "/sdcard/u.xml")


def nodes(xml: str):
    for m in re.finditer(r'content-desc="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        d, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
        yield d, (x1 + x2) // 2, (y1 + y2) // 2, (x1, y1, x2, y2)


def find_text(xml: str, text: str):
    m = re.search(re.escape(text) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def screencap(name: str) -> None:
    adb("shell", "screencap", "-p", "/sdcard/s.png")
    subprocess.run([ADB, "-s", SERIAL, "pull", "/sdcard/s.png", str(OUTDIR / name)], capture_output=True)


adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
adb("shell", "monkey", "-p", "com.mineralord.tcg", "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(4)
tap(540, 1469)          # JUGAR
time.sleep(6)
tap(540, 2081)          # CARA
time.sleep(8)           # spin + result + deal -> setup on-board

# Prueba cada carta de la mano hasta que abra la hoja (Poner como Activo).
xml = dump()
hand = [n for n in nodes(xml) if n[3][1] > 1900 and n[3][3] > 2100 and n[0] not in ("", None)]
placed = False
for d, cx, cy, _ in hand:
    tap(cx, cy)
    time.sleep(0.7)
    s = dump()
    btn = find_text(s, "Poner como Activo")
    if btn:
        tap(*btn)
        time.sleep(0.8)
        placed = True
        print(f"[OK] Activo colocado: {d}")
        break
    # cerrar hoja si se abrio con otra carta (dismiss)
    x = find_text(s, "✕")
    if x:
        tap(*x); time.sleep(0.4)

if not placed:
    print("[WARN] no se encontro un Basico jugable en la mano")

# LISTO (a la derecha del banner de preparacion).
s = dump()
listo = find_text(s, "LISTO")
if listo:
    tap(*listo)
    print(f"[OK] LISTO en {listo}")
time.sleep(4)
screencap(OUT)
print(f"[OK] captura -> out/{OUT}")
