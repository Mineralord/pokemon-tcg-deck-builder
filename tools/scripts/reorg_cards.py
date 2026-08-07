# -*- coding: utf-8 -*-
"""Reorganiza data/cards/.../cards/ a jerarquia serie/expansion + energias aparte.

- Entrada: los archivos planos actuales cards/<code>.json (uno por prefijo de id).
- Salida:  cards/<serie-slug>/<expansion-slug>.json  (uno por expansion)
           cards/energies.json                        (energias basicas, NO expansion)
           cards/index.json                           (indice con metadatos)
Los id internos de las cartas NO se tocan (siguen siendo sv3pt5-39, etc.). El codigo
oficial de Pokemon Company va SOLO como metadato del indice/archivo.
"""
import io, sys, json, os, glob, shutil
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CARDS = os.path.join(ROOT, 'data', 'cards', 'src', 'main', 'resources', 'cards')

# code -> (serie, serie_slug, expansion, expansion_slug, officialCode)
SETS = {
    'sv1':    ('Scarlet & Violet', 'scarlet-violet', 'Scarlet & Violet', 'scarlet-violet', 'SVI'),
    'sv2':    ('Scarlet & Violet', 'scarlet-violet', 'Paldea Evolved', 'paldea-evolved', 'PAL'),
    'sv3':    ('Scarlet & Violet', 'scarlet-violet', 'Obsidian Flames', 'obsidian-flames', 'OBF'),
    'sv3pt5': ('Scarlet & Violet', 'scarlet-violet', '151', '151', 'MEW'),
    'sv4':    ('Scarlet & Violet', 'scarlet-violet', 'Paradox Rift', 'paradox-rift', 'PAR'),
    'sv5':    ('Scarlet & Violet', 'scarlet-violet', 'Temporal Forces', 'temporal-forces', 'TEF'),
    'sv6':    ('Scarlet & Violet', 'scarlet-violet', 'Twilight Masquerade', 'twilight-masquerade', 'TWM'),
    'sv6pt5': ('Scarlet & Violet', 'scarlet-violet', 'Shrouded Fable', 'shrouded-fable', 'SFA'),
    'sv7':    ('Scarlet & Violet', 'scarlet-violet', 'Stellar Crown', 'stellar-crown', 'SCR'),
    'sv8':    ('Scarlet & Violet', 'scarlet-violet', 'Surging Sparks', 'surging-sparks', 'SSP'),
    'sv10':   ('Scarlet & Violet', 'scarlet-violet', 'Destined Rivals', 'destined-rivals', 'DRI'),
    'svp':    ('Scarlet & Violet', 'scarlet-violet', 'Scarlet & Violet Black Star Promos', 'black-star-promos', 'SVP'),
    'me1':    ('Mega Evolution', 'mega-evolution', 'Mega Evolution', 'mega-evolution', 'ME1'),
}

# Energias basicas (tcgdex swsh12.5). Se agregan las que faltan a las 3 existentes.
ENERGY_NUM = {'Grass': 152, 'Fire': 153, 'Water': 154, 'Lightning': 155,
              'Psychic': 156, 'Fighting': 157, 'Darkness': 158, 'Metal': 159}
ENERGY_ES = {'Grass': 'Energía Planta Básica', 'Fire': 'Energía Fuego Básica',
             'Water': 'Energía Agua Básica', 'Lightning': 'Energía Rayo Básica',
             'Psychic': 'Energía Psíquica Básica', 'Fighting': 'Energía Lucha Básica',
             'Darkness': 'Energía Oscura Básica', 'Metal': 'Energía Metálica Básica'}

def energy_card(t):
    n = ENERGY_NUM[t]
    return {
        'id': f'energy-basic-{t.lower()}-energy',
        'nombre': f'Basic {t} Energy', 'cantidad': 0, 'supertipo': 'Energy',
        'fase': 'Basic Energy', 'tipos': [t], 'ataques': [], 'habilidades': [],
        'debilidades': [], 'resistencias': [], 'costoRetirada': [], 'reglas': [],
        'es': {'nombre': ENERGY_ES[t],
               'imagenChica': f'https://assets.tcgdex.net/es/swsh/swsh12.5/{n}/low.webp',
               'imagenGrande': f'https://assets.tcgdex.net/es/swsh/swsh12.5/{n}/high.webp'},
    }

def main():
    # cargar cartas planas actuales por code
    by_code = {}
    for fn in glob.glob(os.path.join(CARDS, '*.json')):
        base = os.path.basename(fn)
        if base == 'index.json':
            continue
        code = base[:-5]
        data = json.load(open(fn, encoding='utf-8'))
        by_code[code] = data.get('cartas', [])

    # limpiar salida vieja
    for fn in glob.glob(os.path.join(CARDS, '*.json')):
        os.remove(fn)
    for sub in ('scarlet-violet', 'mega-evolution'):
        p = os.path.join(CARDS, sub)
        if os.path.isdir(p):
            shutil.rmtree(p)

    index_sets = []
    for code, (serie, serie_slug, exp, exp_slug, official) in SETS.items():
        cartas = by_code.get(code, [])
        if not cartas:
            print(f'  AVISO: sin cartas para {code}'); continue
        rel = f'{serie_slug}/{exp_slug}.json'
        os.makedirs(os.path.join(CARDS, serie_slug), exist_ok=True)
        payload = {
            'meta': {'code': code, 'officialCode': official, 'serie': serie, 'expansion': exp},
            'totalCartas': len(cartas), 'cartas': cartas,
        }
        json.dump(payload, open(os.path.join(CARDS, rel), 'w', encoding='utf-8'),
                  ensure_ascii=False, indent=1)
        index_sets.append({'code': code, 'officialCode': official, 'serie': serie,
                           'expansion': exp, 'file': rel})
        print(f'  {rel}: {len(cartas)} cartas [{official}]')

    # energias: las 3 existentes ya venian en by_code como energy-basic-*; unificamos
    # y agregamos las 5 que faltan -> los 8 tipos basicos.
    energies = [energy_card(t) for t in ['Grass', 'Fire', 'Water', 'Lightning',
                                         'Psychic', 'Fighting', 'Darkness', 'Metal']]
    json.dump({'meta': {'kind': 'basic-energy'}, 'totalCartas': len(energies),
               'cartas': energies},
              open(os.path.join(CARDS, 'energies.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    print(f'  energies.json: {len(energies)} energias basicas')

    json.dump({'sets': index_sets, 'energies': 'energies.json'},
              open(os.path.join(CARDS, 'index.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    total = sum(s and 1 for s in index_sets)
    print(f'index.json -> {len(index_sets)} expansiones + energies.json')

if __name__ == '__main__':
    main()
