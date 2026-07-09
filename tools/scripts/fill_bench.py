"""Navega a la preparacion on-board y LLENA la banca (Activo + hasta 5 Basicos)
para verificar el layout en PANAL (honeycomb 3+2). Captura el tablero en setup.

JUGAR -> CARA -> setup -> coloca 1er Basico como Activo -> el resto "Poner en Banca"
hasta llenar (5) o agotar Basicos -> captura.

Uso:  python scripts/fill_bench.py [serial] [out_name.png]
"""
from __future__ import annotations
import re, subprocess, sys, time
from pathlib import Path

ADB = r"C:\Users\pmmt9\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = sys.argv[1] if len(sys.argv) > 1 else "3bf89e4f"
OUT = sys.argv[2] if len(sys.argv) > 2 else "bench_fill.png"
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
        d = m.group(1)
        x1, y1, x2, y2 = map(int, m.groups()[1:])
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


def hand_cards(xml: str):
    # cartas de la mano: nodos con content-desc no vacio en la franja inferior
    return [n for n in nodes(xml) if n[3][1] > 1900 and n[3][3] > 2100 and n[0]]


adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
adb("shell", "monkey", "-p", "com.mineralord.tcg", "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(4)
tap(540, 1469)          # JUGAR
time.sleep(6)
tap(540, 2081)          # CARA
time.sleep(8)           # spin + result + deal -> setup on-board

tried: set[str] = set()
placed_active = False
benched = 0
for _ in range(30):
    if benched >= 5:
        break
    xml = dump()
    cards = [c for c in hand_cards(xml) if c[0] not in tried]
    if not cards:
        break
    d, cx, cy, _ = cards[0]
    tried.add(d)
    tap(cx, cy)
    time.sleep(0.7)
    s = dump()
    if not placed_active:
        btn = find_text(s, "Poner como Activo")
        if btn:
            tap(*btn); time.sleep(0.7)
            placed_active = True
            print(f"[OK] Activo: {d}")
            continue
    btn = find_text(s, "Poner en Banca")
    if btn:
        tap(*btn); time.sleep(0.7)
        benched += 1
        print(f"[OK] Banca {benched}: {d}")
        continue
    # no jugable / banca llena: cierra hoja
    for close in ("Banca llena", "✕", "✖"):
        x = find_text(s, close)
        if x:
            tap(*x); time.sleep(0.3); break

print(f"[DONE] activo={placed_active} banca={benched}")
time.sleep(1)
screencap(OUT)
print(f"[OK] captura -> out/{OUT}")
