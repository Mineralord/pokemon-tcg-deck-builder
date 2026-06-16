#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
============================================================
  DESCARGADOR DE CARTAS — Pokémon TCG (para el armador de mazos)
============================================================

Qué hace este script:
  - Se conecta a la base de datos oficial Pokémon TCG API.
  - Busca CADA una de tus cartas con sus 25 campos completos
    (ataques, costos de energía, daño, debilidad, retirada,
     habilidad, ilustrador, rareza, pokédex, etc.).
  - Genera dos archivos:
       * cartas-db.js   -> el que tu app carga al abrirla
       * cartas-db.json -> el mismo contenido, por si quieres revisarlo
  - Si alguna carta no la encuentra, la anota en "revisar.txt"
    para que la afinemos juntos (NO inventa datos).

Cómo usarlo: lee la "Guía paso a paso" que viene junto a este archivo.
============================================================
"""

import json
import re
import time
import sys
import urllib.parse
import urllib.request
import urllib.error
from datetime import datetime

# En Windows la consola suele usar cp1252 y no puede mostrar emojis.
# Forzamos la salida de texto a UTF-8 para evitar UnicodeEncodeError.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

# ============================================================
#  PASO 1 — PEGA AQUÍ TU API KEY (entre las comillas)
#  La obtienes gratis en: https://dev.pokemontcg.io
#  Si la dejas vacía, igual funciona pero más lento.
# ============================================================
API_KEY = ""

API = "https://api.pokemontcg.io/v2"

# Campos que queremos de cada carta (los 25 que pediste, sin precios)
SELECT = ",".join([
    "id", "name", "supertype", "subtypes", "hp", "types",
    "evolvesFrom", "abilities", "attacks", "weaknesses", "resistances",
    "retreatCost", "convertedRetreatCost", "set", "number", "artist",
    "rarity", "flavorText", "nationalPokedexNumbers", "images",
    "legalities", "rules", "regulationMark",
])

# ============================================================
#  TU INVENTARIO  (name = nombre exacto, qty = cantidad,
#  hint = de qué producto viene para elegir la versión correcta)
#  hint: "151" = set 151 ; "battle" = Battle Academy 2024
# ============================================================
OWNED = [
    # ---- BARAJA PIKACHU (Battle Academy 2024) ----
    ("Pikachu ex", 1, "battle"), ("Voltorb", 9, "151"), ("Electrode", 10, "151"),
    ("Mareep", 4, "battle"), ("Flaaffy", 3, "battle"), ("Ampharos", 2, "battle"),
    ("Rotom", 1, "151"), ("Wattrel", 2, "151"), ("Kilowattrel", 1, "151"),
    ("Miraidon", 1, "battle"),
    ("Electric Generator", 1, "151"), ("Great Ball", 8, "151"), ("Jacq", 2, "151"),
    ("Nemona", 8, "151"), ("Nest Ball", 2, "151"), ("Picnicker", 4, "battle"),
    ("Potion", 4, "151"), ("Switch", 5, "151"), ("Youngster", 6, "151"),
    ("Basic Lightning Energy", 18, "energy"),
    # ---- BARAJA DARKRAI (Battle Academy 2024) ----
    ("Seviper", 2, "151"), ("Darkrai ex", 1, "battle"), ("Pawniard", 4, "battle"),
    ("Bisharp", 3, "battle"), ("Kingambit", 2, "battle"), ("Yveltal", 2, "151"),
    ("Salandit", 4, "151"), ("Salazzle", 3, "151"), ("Cyclizar", 1, "151"),
    ("Boss's Orders", 1, "151"), ("Basic Darkness Energy", 18, "energy"),
    # ---- CARTAS 151 SUELTAS ----
    ("Venusaur ex", 4, "151"), ("Charizard ex", 2, "151"), ("Blastoise ex", 2, "151"),
    ("Arbok ex", 2, "151"), ("Ninetales ex", 5, "151"), ("Wigglytuff ex", 1, "151"),
    ("Alakazam ex", 4, "151"), ("Tentacool", 6, "151"), ("Tentacruel", 4, "151"),
    ("Geodude", 6, "151"), ("Graveler", 5, "151"), ("Golem ex", 1, "151"),
    ("Ponyta", 9, "151"), ("Rapidash", 7, "151"), ("Slowpoke", 3, "151"),
    ("Slowbro", 6, "151"), ("Magnemite", 4, "151"), ("Magneton", 5, "151"),
    ("Farfetch'd", 6, "151"), ("Doduo", 5, "151"), ("Dodrio", 8, "151"),
    ("Seel", 7, "151"), ("Dewgong", 2, "151"), ("Grimer", 7, "151"),
    ("Muk", 4, "151"), ("Shellder", 5, "151"), ("Cloyster", 6, "151"),
    ("Gastly", 5, "151"), ("Haunter", 8, "151"), ("Gengar", 6, "151"),
    ("Onix", 2, "151"), ("Drowzee", 9, "151"), ("Hypno", 4, "151"),
    ("Krabby", 5, "151"), ("Kingler", 5, "151"), ("Exeggcute", 6, "151"),
    ("Exeggutor", 2, "151"), ("Cubone", 7, "151"), ("Marowak", 3, "151"),
    ("Hitmonlee", 6, "151"), ("Hitmonchan", 5, "151"), ("Lickitung", 6, "151"),
    ("Koffing", 7, "151"), ("Weezing", 6, "151"), ("Rhyhorn", 3, "151"),
    ("Rhydon", 5, "151"), ("Chansey", 3, "151"), ("Tangela", 6, "151"),
    ("Kangaskhan ex", 3, "151"), ("Horsea", 4, "151"), ("Seadra", 7, "151"),
    ("Goldeen", 5, "151"), ("Seaking", 6, "151"), ("Staryu", 3, "151"),
    ("Starmie", 4, "151"), ("Mr. Mime", 3, "151"), ("Scyther", 7, "151"),
    ("Jynx ex", 2, "151"), ("Electabuzz", 3, "151"), ("Magmar", 6, "151"),
    ("Pinsir", 5, "151"), ("Tauros", 5, "151"), ("Magikarp", 4, "151"),
    ("Gyarados", 4, "151"), ("Lapras", 3, "151"), ("Ditto", 6, "151"),
    ("Eevee", 4, "151"), ("Vaporeon", 8, "151"), ("Jolteon", 6, "151"),
    ("Flareon", 4, "151"), ("Porygon", 5, "151"), ("Omanyte", 4, "151"),
    ("Zapdos ex", 4, "151"), ("Mew ex", 1, "151"),
    # ---- ENTRENADORES 151 SUELTOS ----
    ("Erika's Invitation", 3, "151"), ("Giovanni's Charisma", 1, "151"),
]

# Tipos de energía básica para generar su registro sin tener que buscarlos
ENERGY_TYPES = {
    "Basic Lightning Energy": "Lightning",
    "Basic Darkness Energy": "Darkness",
}

# ============================================================
#  VERSIONES EXACTAS POR ID (cuando importa la versión concreta).
#  Se buscan directamente por su ID en la API, sin adivinar.
#  Para añadir más: "Nombre exacto": "idset-numero"
# ============================================================
IDS_EXACTOS = {
    "Pikachu ex": "svp-106",   # promo de Battle Academy 2024
    "Darkrai ex": "svp-110",   # promo de Battle Academy 2024
}


def norm(s):
    """Normaliza un nombre para comparar (minúsculas, comillas rectas)."""
    return (s or "").lower().strip().replace("\u2019", "'").replace("\u2018", "'")


def api_get(path):
    """Hace una petición GET a la API y devuelve el JSON (con reintentos)."""
    url = f"{API}/{path}"
    # La API rechaza (403) el User-Agent por defecto de Python; usamos uno normal.
    headers = {"User-Agent": "Mozilla/5.0 (descargador-cartas)"}
    if API_KEY and API_KEY != "PEGA_TU_API_KEY_AQUI":
        headers["X-Api-Key"] = API_KEY
    for intento in range(4):
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode("utf-8"))
        except Exception as e:
            espera = 3 * (intento + 1)
            print(f"   ⚠️  Error ({e}). Reintentando en {espera}s...")
            time.sleep(espera)
    print("   ❌ No se pudo conectar tras varios intentos.")
    return None


def resolver_set_ids(nombre_set):
    """Busca el/los IDs de un set por su nombre."""
    q = urllib.parse.quote(f'name:"{nombre_set}"')
    data = api_get(f"sets?q={q}&pageSize=50")
    if not data or "data" not in data:
        return []
    return [(s["id"], s.get("name", ""), s.get("releaseDate", "")) for s in data["data"]]


def descargar_set(set_id):
    """Descarga TODAS las cartas de un set (con paginación) y sus 25 campos."""
    cartas = []
    page = 1
    while True:
        q = urllib.parse.quote(f"set.id:{set_id}")
        data = api_get(f"cards?q={q}&select={SELECT}&page={page}&pageSize=250")
        if not data or not data.get("data"):
            break
        cartas.extend(data["data"])
        if len(data["data"]) < 250:
            break
        page += 1
        time.sleep(0.3)
    return cartas


def carta_por_id(cid):
    """Descarga una carta concreta por su ID (con sus 25 campos)."""
    data = api_get(f"cards/{cid}?select={SELECT}")
    if data and isinstance(data.get("data"), dict):
        return data["data"]
    return None


def buscar_carta_global(nombre):
    """Busca una carta por nombre en TODA la base de datos (no solo en los sets
    objetivo). Útil para cartas de productos que no están catalogados en la API,
    como Battle Academy 2024. Devuelve solo coincidencias exactas de nombre."""
    q = urllib.parse.quote(f'name:"{nombre}"')
    data = api_get(f"cards?q={q}&select={SELECT}&pageSize=250")
    if not data or not data.get("data"):
        return []
    return [c for c in data["data"] if norm(c.get("name")) == norm(nombre)]


def puntua_global(c):
    """Elige la mejor versión real de una carta: legal en Standard, de la era
    Escarlata y Púrpura, no promocional y con la marca de regulación más nueva."""
    p = 0
    if (c.get("legalities") or {}).get("standard") == "Legal":
        p += 1000
    sid = (c.get("set") or {}).get("id", "")
    snom = norm((c.get("set") or {}).get("name", ""))
    if sid.startswith("sv"):
        p += 100
    if sid.endswith("p") or "promo" in snom:
        p -= 50
    rm = (c.get("regulationMark") or "").upper()
    if rm:
        p += ord(rm[0])  # H > G > F ... (más nuevo, mayor puntaje)
    return p


def a_formato_app(c, qty):
    """Convierte una carta de la API al formato que usa tu app (25 campos)."""
    s = c.get("set", {})
    return {
        "id": c.get("id"),
        "nombre": c.get("name"),
        "cantidad": qty,
        "supertipo": c.get("supertype"),
        "fase": ", ".join(c.get("subtypes", []) or []),
        "evolucionaDe": c.get("evolvesFrom"),
        "ps": c.get("hp"),
        "tipos": c.get("types", []),
        "habilidades": c.get("abilities", []),
        "ataques": c.get("attacks", []),
        "debilidades": c.get("weaknesses", []),
        "resistencias": c.get("resistances", []),
        "costoRetirada": c.get("retreatCost", []),
        "retiradaConvertida": c.get("convertedRetreatCost"),
        "descripcionPokedex": c.get("flavorText"),
        "numeroPokedex": c.get("nationalPokedexNumbers", []),
        "numeroCarta": c.get("number"),
        "rareza": c.get("rarity"),
        "ilustrador": c.get("artist"),
        "marcaRegulacion": c.get("regulationMark"),
        "reglas": c.get("rules", []),
        "imagenChica": (c.get("images") or {}).get("small"),
        "imagenGrande": (c.get("images") or {}).get("large"),
        "set": {
            "nombre": s.get("name"),
            "serie": s.get("series"),
            "simbolo": (s.get("images") or {}).get("symbol"),
            "logo": (s.get("images") or {}).get("logo"),
        },
        "legalidad": c.get("legalities", {}),
    }


# ============================================================
#  TRADUCCIÓN AL ESPAÑOL (TCGdex)
#  La API principal (pokemontcg.io) solo trae los textos en inglés.
#  TCGdex sí tiene los nombres y efectos OFICIALES de ataques y
#  habilidades en español. Emparejamos cada carta por set + número.
# ============================================================
TCGDEX = "https://api.tcgdex.net/v2/es/cards"

# Mapa de IDs de set: Pokémon TCG API -> TCGdex
TCGDEX_SETS = {
    "sv1": "sv01", "sv2": "sv02", "sv3": "sv03", "sv3pt5": "sv03.5", "sv4": "sv04",
    "sv4pt5": "sv04.5", "sv5": "sv05", "sv6": "sv06", "sv6pt5": "sv06.5", "sv7": "sv07",
    "sv8": "sv08", "sv8pt5": "sv08.5", "sv9": "sv09", "sv10": "sv10", "svp": "svp",
    "me1": "me01", "me2": "me02", "me2pt5": "me02.5", "me3": "me03", "me4": "me04",
}


def _tcgdex_set(sid):
    if sid in TCGDEX_SETS:
        return TCGDEX_SETS[sid]
    # Conversión genérica para sets "sv" no listados (svN -> svNN, svNpt5 -> svNN.5)
    m = re.match(r"^sv(\d+)(pt5)?$", sid)
    if m:
        return f"sv{int(m.group(1)):02d}" + (".5" if m.group(2) else "")
    return None


def tcgdex_id(pid):
    """Convierte un id de Pokémon TCG (p. ej. 'sv3pt5-100') al de TCGdex ('sv03.5-100')."""
    if not pid or "-" not in pid:
        return None
    sid, num = pid.rsplit("-", 1)
    tset = _tcgdex_set(sid)
    if not tset:
        return None
    try:
        return f"{tset}-{int(num):03d}"
    except ValueError:
        return f"{tset}-{num}"


_cache_es = {}


def descargar_es(pid):
    """Descarga los datos en ESPAÑOL de una carta desde TCGdex (nombre y los
    nombres/efectos de ataques y habilidades). Devuelve None si no se encuentra."""
    tid = tcgdex_id(pid)
    if not tid:
        return None
    if tid in _cache_es:
        return _cache_es[tid]
    headers = {"User-Agent": "Mozilla/5.0 (descargador-cartas)"}
    datos = None
    for intento in range(3):
        try:
            req = urllib.request.Request(f"{TCGDEX}/{tid}", headers=headers)
            with urllib.request.urlopen(req, timeout=30) as r:
                d = json.loads(r.read().decode("utf-8"))
            datos = {
                "nombre": d.get("name"),
                "habilidades": [{"name": a.get("name"), "text": a.get("effect")}
                                for a in (d.get("abilities") or [])],
                "ataques": [{"name": a.get("name"), "text": a.get("effect")}
                            for a in (d.get("attacks") or [])],
                "efecto": d.get("effect"),  # texto de cartas de Entrenador
            }
            break
        except urllib.error.HTTPError as e:
            if e.code == 404:
                break  # esa carta no está en TCGdex; no insistimos
            time.sleep(2)
        except Exception:
            time.sleep(2)
    _cache_es[tid] = datos
    return datos


def main():
    print("=" * 56)
    print("  DESCARGANDO TUS CARTAS — Pokémon TCG")
    print("=" * 56)
    if API_KEY == "PEGA_TU_API_KEY_AQUI":
        print("⚠️  No pusiste API key. Funcionará, pero más lento.\n")
    else:
        print("✅ API key detectada.\n")

    # 1) Resolver los sets que necesitamos
    print("→ Buscando los sets en la base de datos...")
    sets_objetivo = {}
    for nombre in ["151", "Battle Academy 2024 (Pikachu)",
                   "Battle Academy 2024 (Darkrai)", "Battle Academy"]:
        for sid, snombre, fecha in resolver_set_ids(nombre):
            sets_objetivo[sid] = snombre
        time.sleep(0.3)
    print(f"   Sets encontrados: {len(sets_objetivo)}")
    for sid, snom in sets_objetivo.items():
        print(f"     • {snom}  [{sid}]")

    # 2) Descargar todas las cartas de esos sets
    print("\n→ Descargando cartas de cada set...")
    indice = {}  # norm(nombre) -> lista de printings
    for sid, snom in sets_objetivo.items():
        cartas = descargar_set(sid)
        print(f"   {snom}: {len(cartas)} cartas")
        for c in cartas:
            indice.setdefault(norm(c["name"]), []).append(c)
        time.sleep(0.3)

    # 3) Emparejar con tu inventario
    print("\n→ Emparejando con tu inventario...")
    db, faltantes, recuperadas = [], [], []
    for nombre, qty, hint in OWNED:
        # Energías básicas: las generamos directamente
        if hint == "energy":
            db.append({
                "id": f"energy-{norm(nombre).replace(' ', '-')}",
                "nombre": nombre, "cantidad": qty, "supertipo": "Energy",
                "fase": "Basic Energy", "tipos": [ENERGY_TYPES.get(nombre, "Colorless")],
                "ataques": [], "habilidades": [], "debilidades": [], "resistencias": [],
                "costoRetirada": [], "reglas": [],
            })
            continue

        # Versión exacta fijada por ID (p. ej. promos de Battle Academy 2024)
        if nombre in IDS_EXACTOS:
            c = carta_por_id(IDS_EXACTOS[nombre])
            time.sleep(0.2)
            if c:
                set_usado = (c.get("set") or {}).get("name", "?")
                recuperadas.append((nombre, qty, hint, f"{set_usado} [ID fijado: {c.get('id')}]"))
                db.append(a_formato_app(c, qty))
                continue
            print(f"   ⚠️  No se pudo descargar el ID fijado para {nombre}")

        candidatos = indice.get(norm(nombre), [])
        if not candidatos:
            # No está en los sets objetivo (p. ej. Battle Academy 2024 no existe
            # en la API). La buscamos en toda la base y tomamos la mejor versión
            # REAL disponible (no se inventan datos).
            globales = buscar_carta_global(nombre)
            time.sleep(0.2)
            if not globales:
                faltantes.append((nombre, qty, hint))
                continue
            mejor = sorted(globales, key=puntua_global, reverse=True)[0]
            set_usado = (mejor.get("set") or {}).get("name", "?")
            recuperadas.append((nombre, qty, hint, set_usado))
            db.append(a_formato_app(mejor, qty))
            continue

        # Elegir la mejor versión según el "hint"
        def puntua(c):
            sid = (c.get("set") or {}).get("id", "")
            snom = norm((c.get("set") or {}).get("name", ""))
            p = 0
            if hint == "151" and ("151" in snom or sid.startswith("sv3pt5")):
                p += 10
            if hint == "battle" and "battle academy" in snom:
                p += 10
            return p

        mejor = sorted(candidatos, key=puntua, reverse=True)[0]
        db.append(a_formato_app(mejor, qty))

    # 3.5) Enriquecer con ESPAÑOL desde TCGdex (nombres de ataques/habilidades)
    print("\n→ Descargando los textos en español (TCGdex)...")
    con_es = 0
    for c in db:
        cid = c.get("id") or ""
        if cid.startswith("energy-"):
            continue
        es = descargar_es(cid)
        if es:
            c["es"] = es
            con_es += 1
        time.sleep(0.15)
    print(f"   {con_es} de {len(db)} cartas con datos en español")

    # 4) Guardar archivos
    salida = {
        "generadoEl": datetime.now().strftime("%Y-%m-%d %H:%M"),
        "totalCartas": len(db),
        "cartas": db,
    }
    with open("cartas-db.json", "w", encoding="utf-8") as f:
        json.dump(salida, f, ensure_ascii=False, indent=2)
    with open("cartas-db.js", "w", encoding="utf-8") as f:
        f.write("window.CARTAS_DB = ")
        json.dump(salida, f, ensure_ascii=False, indent=2)
        f.write(";")

    if faltantes or recuperadas:
        with open("revisar.txt", "w", encoding="utf-8") as f:
            if recuperadas:
                f.write("Cartas recuperadas desde OTRO set (Battle Academy 2024 y\n")
                f.write("algunas reediciones NO están catalogadas en la API; se usó\n")
                f.write("la mejor versión real disponible). Revisa que sea la correcta:\n\n")
                for nombre, qty, hint, set_usado in recuperadas:
                    f.write(f"  - {nombre} (x{qty}, origen: {hint})  ->  set usado: {set_usado}\n")
                f.write("\n")
            if faltantes:
                f.write("Cartas que NO se encontraron en ningún set:\n\n")
                for nombre, qty, hint in faltantes:
                    f.write(f"  - {nombre} (x{qty}, origen: {hint})\n")

    # 5) Resumen
    print("\n" + "=" * 56)
    print(f"✅ LISTO. {len(db)} cartas guardadas en cartas-db.js y cartas-db.json")
    if recuperadas:
        print(f"ℹ️  {len(recuperadas)} cartas se recuperaron desde otro set (ver revisar.txt)")
    if faltantes:
        print(f"⚠️  {len(faltantes)} cartas NO se encontraron (ver revisar.txt)")
    if not faltantes:
        print("🎉 Se encontraron TODAS tus cartas.")
    print("=" * 56)
    print("\nSiguiente paso: envíale a Claude el archivo cartas-db.json")
    print("para que conecte la app y muestre todos los datos.\n")


if __name__ == "__main__":
    main()
