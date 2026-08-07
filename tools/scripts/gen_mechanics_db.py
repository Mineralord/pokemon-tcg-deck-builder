"""Genera el CATALOGO de ataques y habilidades separado por serie/expansion.

Lee el indice de expansiones (cards/index.json) y, por cada set, extrae a
`docs/mechanics/<serie-slug>/<expansion-file>` dos arrays:

    attacks[]   -> { cardId, en, es, text, impl }
    abilities[] -> { cardId, en, es, text, kind, impl }

`kind` clasifica el poder por epoca: ABILITY (moderno), POKE_POWER o POKE_BODY
(pre-2011), leido del campo `type` de cada habilidad del dataset.

`impl` = true si el efecto ya esta autorado en el motor (su clave existe en el
paquete de efectos: "<id>#atk:<en>" para ataques, "<id>#abi:<en>" para habilidades).

Este catalogo NO lo carga la app: es un indice derivado / worklist para implementar
expansiones completas en el futuro (cada carta trae su texto EN/ES a mano).

Uso:
    python tools/scripts/gen_mechanics_db.py            # todas las expansiones del indice
    python tools/scripts/gen_mechanics_db.py sv3pt5     # solo esos `code`
"""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CARDS_DIR = ROOT / "data/cards/src/main/resources/cards"
INDEX = CARDS_DIR / "index.json"
EFFECTS_DIR = ROOT / "engine/model/src/main/kotlin/com/mineralord/tcg/engine/model"
OUT_ROOT = ROOT / "docs/mechanics"


def load_registered():
    """Claves de efecto ya autoradas en el motor (todos los .kt del paquete model)."""
    txt = "\n".join(p.read_text(encoding="utf-8") for p in EFFECTS_DIR.glob("*.kt"))
    keys = set()
    for m in re.finditer(r'atkKey\("([^"]+)",\s*"([^"]+)"\)', txt):
        keys.add(f"{m.group(1)}#atk:{m.group(2)}")
    for m in re.finditer(r'abiKey\("([^"]+)",\s*"([^"]+)"\)', txt):
        keys.add(f"{m.group(1)}#abi:{m.group(2)}")
    for m in re.finditer(r'EffectId\("([^"]+)"\)', txt):
        keys.add(m.group(1))
    return keys


def ability_kind(raw):
    s = (raw or "").strip().lower().replace("é", "e").replace("-", " ")
    if "poke power" in s or "pokemon power" in s:
        return "POKE_POWER"
    if "poke body" in s:
        return "POKE_BODY"
    return "ABILITY"


def build_set(cards, reg):
    attacks, abilities = [], []
    for c in cards:
        cid = c.get("id", "")
        es = c.get("es", {}) or {}
        esats = es.get("ataques") or []
        for j, a in enumerate(c.get("ataques") or []):
            esa = esats[j] if j < len(esats) else {}
            attacks.append(dict(
                cardId=cid,
                en=a.get("name", ""),
                es=(esa.get("name") or a.get("name") or ""),
                text=(esa.get("text") or a.get("text") or "").strip(),
                impl=f"{cid}#atk:{a.get('name')}" in reg,
            ))
        eshabs = es.get("habilidades") or []
        for j, h in enumerate(c.get("habilidades") or []):
            esh = eshabs[j] if j < len(eshabs) else {}
            abilities.append(dict(
                cardId=cid,
                en=h.get("name", ""),
                es=(esh.get("name") or h.get("name") or ""),
                text=(esh.get("text") or h.get("text") or "").strip(),
                kind=ability_kind(h.get("type")),
                impl=f"{cid}#abi:{h.get('name')}" in reg,
            ))
    return attacks, abilities


def main():
    wanted = set(sys.argv[1:])
    index = json.loads(INDEX.read_text(encoding="utf-8"))
    reg = load_registered()
    total_sets = 0
    for ref in index.get("sets", []):
        if wanted and ref.get("code") not in wanted:
            continue
        src = CARDS_DIR / ref["file"]
        if not src.exists():
            print(f"skip {ref.get('code')}: falta {ref['file']}")
            continue
        data = json.loads(src.read_text(encoding="utf-8"))
        cards = data.get("cartas", data) if isinstance(data, dict) else data
        attacks, abilities = build_set(cards, reg)
        out = OUT_ROOT / ref["file"]
        out.parent.mkdir(parents=True, exist_ok=True)
        payload = dict(
            code=ref.get("code"),
            officialCode=ref.get("officialCode"),
            serie=ref.get("serie"),
            expansion=ref.get("expansion"),
            counts=dict(
                attacks=len(attacks), attacksImpl=sum(a["impl"] for a in attacks),
                abilities=len(abilities), abilitiesImpl=sum(a["impl"] for a in abilities),
            ),
            attacks=attacks,
            abilities=abilities,
        )
        out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        total_sets += 1
        print(f"OK {ref.get('code'):8} -> {out.relative_to(ROOT)}  "
              f"atk={len(attacks)}({payload['counts']['attacksImpl']} impl) "
              f"abi={len(abilities)}({payload['counts']['abilitiesImpl']} impl)")
    print(f"--- {total_sets} expansiones escritas en {OUT_ROOT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
