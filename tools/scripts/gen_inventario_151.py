"""Regenera docs/inventario-efectos-151.md cruzando cartas-db.json x EffectsDb.kt.

Reglas:
- Textos SIEMPRE del bloque `es` (espanol impreso).
- Una "entrada con logica de efecto" = ataque con texto EN no vacio, cualquier
  habilidad, o Entrenador con efecto/reglas.
- "Registrada" = su clave existe en EffectsDb:
    ataque    -> "<id>#atk:<nombreEn>"
    habilidad -> "<id>#abi:<nombreEn>"
    entrenador-> "<id>"
- Efectos UNICOS = dedup por (nombreCartaEs, tipo, nombreEfectoEs) para juntar
  artes alternativos.
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / "data/cards/src/main/resources/cards/scarlet-violet/151.json"
# Efectos separados por expansión: se leen todos los .kt del paquete model.
EFFECTS_DIR = ROOT / "engine/model/src/main/kotlin/com/mineralord/tcg/engine/model"
OUT = ROOT / "docs/inventario-efectos-151.md"

def load_registered():
    txt = "\n".join(p.read_text(encoding="utf-8") for p in EFFECTS_DIR.glob("*.kt"))
    keys = set()
    for m in re.finditer(r'atkKey\("([^"]+)",\s*"([^"]+)"\)', txt):
        keys.add(f"{m.group(1)}#atk:{m.group(2)}")
    for m in re.finditer(r'abiKey\("([^"]+)",\s*"([^"]+)"\)', txt):
        keys.add(f"{m.group(1)}#abi:{m.group(2)}")
    for m in re.finditer(r'EffectId\("([^"]+)"\)', txt):
        keys.add(m.group(1))
    return keys

def main():
    data = json.loads(DB.read_text(encoding="utf-8"))
    cards = data["cartas"] if isinstance(data, dict) else data
    reg = load_registered()

    printings = [c for c in cards if c.get("id", "").startswith("sv3pt5-")]

    entries = []  # dict: id, cardEs, kind, nameEs, textEs, implemented
    for c in printings:
        cid = c["id"]
        es = c.get("es", {})
        cardEs = es.get("nombre") or c.get("nombre") or cid
        # ataques
        ats = c.get("ataques") or []
        esats = es.get("ataques") or []
        for j, a in enumerate(ats):
            en_text = (a.get("text") or "").strip()
            if not en_text:
                continue  # dano puro
            esa = esats[j] if j < len(esats) else {}
            entries.append(dict(
                id=cid, cardEs=cardEs, kind="Pokémon",
                nameEs=(esa.get("name") or a.get("name") or "?"),
                textEs=(esa.get("text") or "").strip(),
                implemented=f"{cid}#atk:{a.get('name')}" in reg,
            ))
        # habilidades
        habs = c.get("habilidades") or []
        eshabs = es.get("habilidades") or []
        for j, h in enumerate(habs):
            esh = eshabs[j] if j < len(eshabs) else {}
            entries.append(dict(
                id=cid, cardEs=cardEs, kind="Pokémon",
                nameEs=(esh.get("name") or h.get("name") or "?"),
                textEs=(esh.get("text") or "").strip(),
                implemented=f"{cid}#abi:{h.get('name')}" in reg,
            ))
        # entrenadores (sin ataques/habilidades): efecto/reglas
        supert = (c.get("supertipo") or "").lower()
        if "pok" not in supert and (es.get("efecto") or c.get("reglas")):
            entries.append(dict(
                id=cid, cardEs=cardEs, kind="Trainer",
                nameEs="Trainer",
                textEs=(es.get("efecto") or "").strip(),
                implemented=cid in reg,
            ))

    total_reg = sum(1 for e in entries if e["implemented"])

    # Unicos: dedup por (cardEs, kind, nameEs)
    uniq = {}
    for e in entries:
        k = (e["cardEs"], e["kind"], e["nameEs"])
        u = uniq.setdefault(k, dict(e, implemented=False))
        if e["implemented"]:
            u["implemented"] = True
    uniq_vals = list(uniq.values())
    uniq_impl = [u for u in uniq_vals if u["implemented"]]
    uniq_miss = [u for u in uniq_vals if not u["implemented"]]

    L = []
    L.append("# Inventario de efectos — Expansión 151 (`sv3pt5`)")
    L.append("")
    L.append("> Textos tomados del bloque en español (`es`) del catálogo — idénticos a la carta impresa en español.")
    L.append("")
    L.append(f"- Printings del set en catálogo: **{len(printings)}** (incluye artes alternativos).")
    L.append(f"- Entradas con lógica de efecto (nivel printing): **{len(entries)}**.")
    L.append(f"- Registradas en `EffectsDb`: **{total_reg}**  ·  Sin registrar: **{len(entries)-total_reg}**.")
    L.append(f"- Efectos ÚNICOS (deduplicando artes alternativos): **{len(uniq_vals)}**  ·  con ≥1 printing implementado: **{len(uniq_impl)}**  ·  totalmente ausentes: **{len(uniq_miss)}**.")
    L.append("")
    L.append("## Efectos únicos SIN implementar")
    L.append("")
    L.append("| Carta | Tipo | Nombre | Texto (español) |")
    L.append("|---|---|---|---|")
    for u in sorted(uniq_miss, key=lambda x: _numkey(x["id"])):
        L.append(f"| {u['cardEs']} (`{u['id']}`) | {u['kind']} | {u['nameEs']} | {u['textEs']} |")
    L.append("")
    L.append("## Efectos con al menos un printing implementado")
    L.append("")
    for u in sorted(uniq_impl, key=lambda x: x["cardEs"].lower()):
        tipo = "Trainer" if u["kind"] == "Trainer" else ("Habilidad" if _is_ability(u) else "Ataque")
        L.append(f"- {u['cardEs']} — {tipo}: {u['nameEs']}")
    L.append("")
    OUT.write_text("\n".join(L), encoding="utf-8")
    print(f"OK -> {OUT}")
    print(f"printings={len(printings)} entradas={len(entries)} reg={total_reg} unicos={len(uniq_vals)} impl={len(uniq_impl)} miss={len(uniq_miss)}")

# helpers
def _numkey(cid):
    m = re.search(r"-(\d+)", cid)
    return int(m.group(1)) if m else 0

# marca de habilidad: guardamos kind Pokemon para ataques y habilidades por igual,
# distinguimos por presencia en habilidades del catalogo (heuristica via texto tag).
_ABILITY_NAMES = set()
def _is_ability(u):
    return u["nameEs"] in _ABILITY_NAMES

if __name__ == "__main__":
    # precarga de nombres de habilidad (es) para clasificar la lista final
    data = json.loads(DB.read_text(encoding="utf-8"))
    cards = data["cartas"] if isinstance(data, dict) else data
    for c in cards:
        if not c.get("id", "").startswith("sv3pt5-"):
            continue
        for h in (c.get("es", {}).get("habilidades") or []):
            if h.get("name"):
                _ABILITY_NAMES.add(h["name"])
    main()
