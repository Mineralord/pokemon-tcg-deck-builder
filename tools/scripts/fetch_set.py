# -*- coding: utf-8 -*-
"""Descarga una expansion COMPLETA y la escribe en el esquema del repo (igual que 151.json).

Fuentes (identicas a como se construyo 151):
  - BASE en ingles: pokemontcg.io  (estructura, tipos, rareza, coste, imagenes EN, ilustrador...)
  - Bloque `es`   : TCGdex ES       (nombre, ataques/habilidades ES, imagenes ES)

Uso:
    python tools/scripts/fetch_set.py sv4 paradox-rift.json
      arg1 = codigo interno del set (prefijo de id de carta: sv4, sv5, ...)
      arg2 = nombre de archivo de salida bajo cards/scarlet-violet/

El id interno de las cartas NO cambia (sv4-1, sv4-2, ...). El codigo oficial de
Pokemon Company va SOLO como metadato del indice (ya presente).
"""
import io, sys, os, json, time, urllib.request, urllib.error

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT_DIR = os.path.join(ROOT, 'data', 'cards', 'src', 'main', 'resources', 'cards', 'scarlet-violet')

# code interno -> (codigo pokemontcg.io, codigo tcgdex, serie-slug tcgdex)
# tcgdex usa "sv04" y ruta de assets "sv/sv04"; pokemontcg.io usa "sv4".
SET_MAP = {
    'sv4': ('sv4', 'sv04', 'sv'),
    'sv5': ('sv5', 'sv05', 'sv'),
    'sv6': ('sv6', 'sv06', 'sv'),
    'sv7': ('sv7', 'sv07', 'sv'),
    'sv8': ('sv8', 'sv08', 'sv'),
}

UA = {'User-Agent': 'tcg-clone-fetcher/1.0'}


def get_json(url, tries=4, delay=0.6):
    last = None
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode('utf-8'))
        except Exception as e:  # noqa
            last = e
            time.sleep(delay * (i + 1))
    raise RuntimeError(f'Fallo GET {url}: {last}')


def fetch_ptcg_base(ptcg_code):
    """Todas las cartas EN de pokemontcg.io (paginado)."""
    cards, page = [], 1
    while True:
        url = f'https://api.pokemontcg.io/v2/cards?q=set.id:{ptcg_code}&pageSize=250&page={page}'
        d = get_json(url)
        data = d.get('data', [])
        if not data:
            break
        cards.extend(data)
        if len(cards) >= d.get('totalCount', len(cards)):
            break
        page += 1
    return cards


def fetch_tcgdex_es(tcg_code):
    """Detalle ES por carta de TCGdex, indexado por localId (str sin ceros)."""
    s = get_json(f'https://api.tcgdex.net/v2/es/sets/{tcg_code}')
    out = {}
    brief = s.get('cards', [])
    for i, c in enumerate(brief, 1):
        cid = c['id']  # p.ej. sv04-025
        det = get_json(f'https://api.tcgdex.net/v2/es/cards/{cid}')
        local = str(int(det.get('localId', c.get('localId', '0')))) if str(det.get('localId', '')).isdigit() else det.get('localId')
        out[local] = det
        if i % 25 == 0:
            print(f'  TCGdex ES: {i}/{len(brief)}')
        time.sleep(0.05)
    return out


def es_block(det, set_name_es):
    """Construye el bloque `es` (identico a 151) desde el detalle TCGdex ES."""
    if det is None:
        return None
    img = det.get('image')  # base sin extension
    abilities = det.get('abilities') or []
    attacks = det.get('attacks') or []
    return {
        'nombre': det.get('name'),
        'habilidades': [{'name': a.get('name'), 'text': a.get('effect')} for a in abilities],
        'ataques': [{'name': a.get('name'), 'text': a.get('effect')} for a in attacks],
        'efecto': det.get('effect'),
        'imagenChica': f'{img}/low.webp' if img else None,
        'imagenGrande': f'{img}/high.webp' if img else None,
        'setNombre': set_name_es,
    }


def map_card(c, es_by_local, set_name_es):
    """pokemontcg.io -> esquema del repo (claves ES), + bloque `es` de TCGdex."""
    st = c.get('set', {})
    imgs = c.get('images', {})
    num = c.get('number', '')
    det = es_by_local.get(str(int(num))) if str(num).isdigit() else es_by_local.get(num)
    return {
        'id': c['id'],
        'nombre': c.get('name'),
        'supertipo': c.get('supertype'),
        'fase': ', '.join(c.get('subtypes', [])) if c.get('subtypes') else None,
        'evolucionaDe': c.get('evolvesFrom'),
        'ps': c.get('hp'),
        'tipos': c.get('types', []),
        'habilidades': [
            {'name': a.get('name'), 'text': a.get('text'), 'type': a.get('type')}
            for a in c.get('abilities', [])
        ],
        'ataques': [
            {
                'name': a.get('name'),
                'cost': a.get('cost', []),
                'convertedEnergyCost': a.get('convertedEnergyCost', 0),
                'damage': a.get('damage'),
                'text': a.get('text'),
            }
            for a in c.get('attacks', [])
        ],
        'debilidades': [{'type': w.get('type'), 'value': w.get('value')} for w in c.get('weaknesses', [])],
        'resistencias': [{'type': r.get('type'), 'value': r.get('value')} for r in c.get('resistances', [])],
        'costoRetirada': c.get('retreatCost', []),
        'retiradaConvertida': c.get('convertedRetreatCost'),
        'descripcionPokedex': c.get('flavorText'),
        'numeroPokedex': c.get('nationalPokedexNumbers', []),
        'numeroCarta': num,
        'rareza': c.get('rarity'),
        'ilustrador': c.get('artist'),
        'marcaRegulacion': c.get('regulationMark'),
        'reglas': c.get('rules', []),
        'imagenChica': imgs.get('small'),
        'imagenGrande': imgs.get('large'),
        'set': {
            'nombre': st.get('name'),
            'serie': st.get('series'),
            'simbolo': st.get('images', {}).get('symbol'),
            'logo': st.get('images', {}).get('logo'),
        },
        'legalidad': c.get('legalities', {}),
        'es': es_block(det, set_name_es),
    }


def main():
    if len(sys.argv) < 3:
        print('uso: python tools/scripts/fetch_set.py <codigo> <archivo_salida.json>')
        sys.exit(1)
    code, out_file = sys.argv[1], sys.argv[2]
    if code not in SET_MAP:
        print(f'codigo desconocido {code}; agregalo a SET_MAP'); sys.exit(1)
    ptcg_code, tcg_code, _ = SET_MAP[code]

    print(f'[1/3] pokemontcg.io base ({ptcg_code})...')
    base = fetch_ptcg_base(ptcg_code)
    print(f'      {len(base)} cartas EN')

    print(f'[2/3] TCGdex ES ({tcg_code})...')
    es_set = get_json(f'https://api.tcgdex.net/v2/es/sets/{tcg_code}')
    set_name_es = es_set.get('name')
    es_by_local = fetch_tcgdex_es(tcg_code)
    print(f'      {len(es_by_local)} cartas ES · set ES = "{set_name_es}"')

    print('[3/3] mapeando y escribiendo...')
    base_sorted = sorted(base, key=lambda c: int(c['number']) if str(c.get('number', '')).isdigit() else 9999)
    cartas = [map_card(c, es_by_local, set_name_es) for c in base_sorted]
    missing_es = [c['id'] for c in cartas if c['es'] is None]
    doc = {'totalCartas': len(cartas), 'cartas': cartas}
    out_path = os.path.join(OUT_DIR, out_file)
    with open(out_path, 'w', encoding='utf-8') as f:
        json.dump(doc, f, ensure_ascii=False, indent=1)
    print(f'OK -> {out_path}  ({len(cartas)} cartas; sin bloque es: {len(missing_es)})')
    if missing_es:
        print('   sin ES:', ', '.join(missing_es[:15]), '...' if len(missing_es) > 15 else '')


if __name__ == '__main__':
    main()
